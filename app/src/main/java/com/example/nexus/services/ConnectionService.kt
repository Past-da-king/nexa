package com.example.nexus.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.nexus.R
import com.example.nexus.data.DtnStore
import com.example.nexus.data.repositories.*
import com.example.nexus.di.AppJson
import com.example.nexus.models.*
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.nio.charset.StandardCharsets
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ConnectionService : Service() {

    @Inject
    lateinit var securityRepository: SecurityRepository
    @Inject
    lateinit var messageRepository: MessageRepository
    @Inject
    lateinit var userRepository: UserRepository
    @Inject
    lateinit var contactRepository: ContactRepository
    @Inject
    lateinit var dtnStore: DtnStore
    @Inject
    lateinit var dtnSettingsRepository: DtnSettingsRepository
    @Inject
    lateinit var channelRepository: ChannelRepository
    @Inject
    lateinit var connectionRepository: ConnectionRepository

    private val binder = LocalBinder()
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var connectionsClient: ConnectionsClient
    private val SERVICE_ID = "com.uct.nexa.SERVICE_ID"

    private val _discoveredPeers = MutableStateFlow<List<Peer>>(emptyList())
    val discoveredPeers = _discoveredPeers.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Idle")
    val connectionStatus = _connectionStatus.asStateFlow()

    private val _pendingConnectionRequest = MutableStateFlow<Peer?>(null)
    val pendingConnectionRequest = _pendingConnectionRequest.asStateFlow()

    // This now represents all established connections, not just a single one.
    val connectedPeers = MutableStateFlow<Map<String, Peer>>(emptyMap())

    private val _connectedPeer = MutableStateFlow<Peer?>(null)
    val connectedPeer: StateFlow<Peer?> = _connectedPeer.asStateFlow()

    private val _isConnectionSecure = MutableStateFlow(false)
    val isConnectionSecure: StateFlow<Boolean> = _isConnectionSecure.asStateFlow()

    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()



    fun setPermissionsGranted(granted: Boolean) {
        _permissionsGranted.value = granted
    }

    private val pendingConnections = mutableMapOf<String, ConnectionInfo>()

    inner class LocalBinder : Binder() {
        fun getService(): ConnectionService = this@ConnectionService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.d("ConnectionService", "Service created. Initializing Nearby Connections.")
        connectionsClient = Nearby.getConnectionsClient(this)
        startForegroundService()
        this.startTtlPruning()

        serviceScope.launch {
            connectedPeers.collect { peersMap ->
                _connectedPeer.value = peersMap.values.firstOrNull()
                _isConnectionSecure.value = peersMap.isNotEmpty()
            }
        }

        // Network operations will start once permissions are granted
        Log.d("ConnectionService", "Waiting for permissions to start advertising and discovery.")

        serviceScope.launch {
            connectionRepository.permissionsGranted.collect { granted ->
                if (granted) {
                    Log.d("ConnectionService", "Permissions granted. Starting advertising and discovery.")
                    startAdvertising()
                    startDiscovery()
                } else {
                    Log.w("ConnectionService", "Permissions not yet granted. Delaying network operations.")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ConnectionService", "Service destroyed. Stopping all network activities.")
        serviceJob.cancel()
        stopAdvertising()
        stopDiscovery()
        connectionsClient.stopAllEndpoints()
    }

    // ALGORITHM 1: SEND (The Sender's Only Action)
    private fun send(destinationId: String, content: Any, recipientPublicKeyString: String? = null) {
        serviceScope.launch {
            try {
                val messageType = when (content) {
                    is Message -> if(destinationId.startsWith("GROUP_")) DtnMessageType.GROUP_MESSAGE else DtnMessageType.DIRECT_MESSAGE
                    is TransmissionPayload.ChannelInvite -> DtnMessageType.CHANNEL_INVITE
                    is TransmissionPayload.Handshake -> when(content.handshakeType){
                        HandshakeType.FRIEND_REQUEST -> DtnMessageType.FRIEND_REQUEST
                        HandshakeType.FRIEND_ACCEPT -> DtnMessageType.FRIEND_ACCEPT
                        HandshakeType.DISCOVERY -> DtnMessageType.DISCOVERY_HANDSHAKE
                        else -> throw IllegalArgumentException("Invalid handshake type for DTN message")
                    }
                    is TransmissionPayload.MessageRequest -> DtnMessageType.MESSAGE_REQUEST
                    else -> throw IllegalArgumentException("Unsupported content type for DTN message")
                }
                Log.d("NexaProtocol-Send", "Initiating send for messageType: $messageType, destination: $destinationId")

                val contentJson = when (content) {
                    is Message -> AppJson.encodeToString(content)
                    is TransmissionPayload.ChannelInvite -> AppJson.encodeToString(content)
                    is TransmissionPayload.Handshake -> AppJson.encodeToString(content)
                    is TransmissionPayload.MessageRequest -> AppJson.encodeToString(content)
                    else -> throw IllegalArgumentException("Unsupported content type for serialization: ${content::class.simpleName}")
                }
                var finalPayload = contentJson // Default to unencrypted payload
                var isPayloadE2EEncrypted = false

                // Determine if payload needs E2E encryption and get recipient's public key
                val finalRecipientPublicKeyBytes: ByteArray? = if (recipientPublicKeyString != null) {
                    Log.d("NexaProtocol-Send", "Using provided recipientPublicKeyString for E2E encryption.")
                    Base64.decode(recipientPublicKeyString, Base64.DEFAULT)
                } else when (messageType) {
                    DtnMessageType.DIRECT_MESSAGE, DtnMessageType.FRIEND_REQUEST, DtnMessageType.FRIEND_ACCEPT, DtnMessageType.CHANNEL_INVITE -> {
                        // For these types, destinationId is a stableId of a single user
                        val contact = contactRepository.getContactById(destinationId)
                        contact?.publicKeyString?.let { Base64.decode(it, Base64.DEFAULT) }
                    }
                    // For group messages, public announcements, discovery handshakes, profile updates,
                    // the payload is either public or requires a group key (not implemented for E2E yet)
                    else -> null
                }

                if (finalRecipientPublicKeyBytes != null) {
                    Log.d("NexaProtocol-Send", "Attempting E2E encryption for payload of messageType: $messageType")
                    val e2eEncryptedContent = securityRepository.encrypt(contentJson, finalRecipientPublicKeyBytes)
                    if (e2eEncryptedContent != null) {
                        finalPayload = Base64.encodeToString(e2eEncryptedContent, Base64.DEFAULT)
                        isPayloadE2EEncrypted = true
                        Log.d("NexaProtocol-Send", "Payload E2E encrypted successfully.")
                    } else {
                        Log.e("NexaProtocol-Send", "Failed to E2E encrypt payload for $destinationId. Sending unencrypted.")
                        // Continue with unencrypted payload if E2E fails
                    }
                } else {
                    Log.d("NexaProtocol-Send", "Payload for messageType: $messageType will be sent unencrypted (E2E). Metadata is hop-by-hop encrypted.")
                }

                val dtnMessage = DtnMessage(
                    id = UUID.randomUUID().toString(),
                    source = userRepository.getUserId() ?: return@launch,
                    destination = destinationId,
                    payload = finalPayload, // This now holds E2E encrypted content if applicable
                    messageType = messageType,
                    ttl = System.currentTimeMillis() + dtnSettingsRepository.dtnSettings.first().ttl,
                    hopCount = dtnSettingsRepository.dtnSettings.first().hopCount,
                    timestamp = System.currentTimeMillis()
                )
                Log.d("NexaProtocol-Send", "DtnMessage created: ID=${dtnMessage.id}, Source=${dtnMessage.source}, Dest=${dtnMessage.destination}, Type=${dtnMessage.messageType}, E2E_Payload=${isPayloadE2EEncrypted}")

                dtnStore.addMessage(dtnMessage)
                Log.d("NexaProtocol-Send", "Stored DTN message ${dtnMessage.id} in local store.")

                // Immediately try to deliver or forward
                route(dtnMessage, null)
                Log.d("NexaProtocol-Send", "Handed DTN message ${dtnMessage.id} to routing engine.")
            } catch (e: Exception) {
                Log.e("NexaProtocol-Send", "Failed to create or store DTN Message", e)
            }
        }
    }

    // ALGORITHM 2: ON-RECEIVE (The Node's Only Action)
    private fun onReceive(dtnMessage: DtnMessage, senderPeer: Peer) {
        serviceScope.launch {
            Log.d("NexaProtocol-OnReceive", "Received DtnMessage ID=${dtnMessage.id} from ${senderPeer.name} (StableID: ${senderPeer.stableId}).")
            // 1. Prevent Loops
            if (dtnStore.getMessage(dtnMessage.id) != null) {
                Log.d("NexaProtocol-OnReceive", "Discarding duplicate message ${dtnMessage.id}. Already processed or forwarded.")
                return@launch
            }

            // 2. Store It
            dtnStore.addMessage(dtnMessage)
            Log.d("NexaProtocol-OnReceive", "Stored new message ${dtnMessage.id} from ${senderPeer.name} in local store.")

            // 3. Check Destination
            val isForMe = when {
                dtnMessage.destination == userRepository.getUserId() -> {
                    Log.d("NexaProtocol-OnReceive", "Message ${dtnMessage.id} is for me (MY_USER_ID match).")
                    true
                }
                dtnMessage.destination == "NEXA_BROADCAST_ALL" -> {
                    Log.d("NexaProtocol-OnReceive", "Message ${dtnMessage.id} is for me (NEXA_BROADCAST_ALL).")
                    true
                }
                dtnMessage.destination.startsWith("GROUP_") -> {
                    val myChannels = channelRepository.getAllChannels().first()
                    val isMember = myChannels.any { it.id == dtnMessage.destination }
                    if (isMember) {
                        Log.d("NexaProtocol-OnReceive", "Message ${dtnMessage.id} is for me (Group ${dtnMessage.destination} and I am a member).")
                    } else {
                        Log.d("NexaProtocol-OnReceive", "Message ${dtnMessage.id} is NOT for me (Group ${dtnMessage.destination} but I am not a member).")
                    }
                    isMember
                }
                else -> {
                    Log.d("NexaProtocol-OnReceive", "Message ${dtnMessage.id} is NOT for me (Destination: ${dtnMessage.destination}).")
                    false
                }
            }

            // 4. Process or Forward
            if (isForMe) {
                Log.d("NexaProtocol-OnReceive", "Message ${dtnMessage.id} is for me. Handing to processPayload.")
                processPayload(dtnMessage, senderPeer)
            }

            // 5. Always Forward
            Log.d("NexaProtocol-OnReceive", "Handing DtnMessage ${dtnMessage.id} to routing engine for forwarding.")
            route(dtnMessage, senderPeer)
        }
    }

    // ALGORITHM 3: ROUTE (The Core Forwarding Logic)
    private fun route(dtnMessage: DtnMessage, senderPeer: Peer?) {
        serviceScope.launch {
            Log.d("NexaProtocol-Route", "Routing DtnMessage ID=${dtnMessage.id}, Current Hops=${dtnMessage.hopCount}, Dest=${dtnMessage.destination}. Received from ${senderPeer?.name ?: "(Self)"}.")
            // 1. Check TTL
            if (dtnMessage.hopCount <= 0) {
                Log.d("NexaProtocol-Route", "Hop count exhausted for ${dtnMessage.id}. Not forwarding.")
                return@launch
            }

            // 2. Decrement hop count
            val forwardMessage = dtnMessage.copy(hopCount = dtnMessage.hopCount - 1)
            Log.d("NexaProtocol-Route", "Decremented hop count for ${dtnMessage.id} to ${forwardMessage.hopCount}.")

            // 3. Direct Delivery Optimization
            val directRecipientPeer = connectedPeers.value.values.find { it.stableId == dtnMessage.destination }
            if (directRecipientPeer != null) {
                Log.d("NexaProtocol-Route", "Direct delivery optimization: Recipient ${directRecipientPeer.name} is currently connected. Sending directly.")
                sendToPeer(directRecipientPeer, forwardMessage)
                return@launch
            }

            // 4. Broadcast Forwarding
            Log.d("NexaProtocol-Route", "Recipient ${dtnMessage.destination} not directly connected. Broadcasting ${forwardMessage.id} to all other connected peers.")
            connectedPeers.value.values.forEach { peer ->
                if (peer.stableId != senderPeer?.stableId) {
                    Log.d("NexaProtocol-Route", "Forwarding ${forwardMessage.id} to peer ${peer.name} (StableID: ${peer.stableId}).")
                    sendToPeer(peer, forwardMessage)
                } else {
                    Log.d("NexaProtocol-Route", "Skipping forwarding ${forwardMessage.id} back to sender ${peer.name}.")
                }
            }
        }
    }

    private fun processPayload(dtnMessage: DtnMessage, senderPeer: Peer) {
        serviceScope.launch {
            try {
                Log.d("NexaProtocol-Process", "Processing DtnMessage ID=${dtnMessage.id}, Type=${dtnMessage.messageType}, Source=${dtnMessage.source}")
                var decryptedPayloadJson = dtnMessage.payload // Default to already decrypted (if not E2E)

                // Determine if payload needs E2E decryption
                val needsE2EDecryption = when (dtnMessage.messageType) {
                    DtnMessageType.DIRECT_MESSAGE, DtnMessageType.FRIEND_REQUEST, DtnMessageType.FRIEND_ACCEPT, DtnMessageType.CHANNEL_INVITE -> true
                    else -> false
                }

                if (needsE2EDecryption) {
                    Log.d("NexaProtocol-Process", "Attempting E2E decryption for payload of messageType: ${dtnMessage.messageType}")
                    val e2eEncryptedPayloadBytes = Base64.decode(dtnMessage.payload, Base64.DEFAULT)
                    val decryptedJson = securityRepository.decrypt(e2eEncryptedPayloadBytes, "nexa_handshake".toByteArray(StandardCharsets.UTF_8)) // Context info might need to be dynamic
                    if (decryptedJson != null) {
                        decryptedPayloadJson = decryptedJson
                        Log.d("NexaProtocol-Process", "Payload E2E decrypted successfully.")
                    } else {
                        Log.e("NexaProtocol-Process", "Failed to E2E decrypt payload for message ${dtnMessage.id}. Cannot process.")
                        return@launch
                    }
                } else {
                    Log.d("NexaProtocol-Process", "Payload for messageType: ${dtnMessage.messageType} is not E2E encrypted. Processing directly.")
                }

                when (dtnMessage.messageType) {
                    DtnMessageType.DIRECT_MESSAGE, DtnMessageType.GROUP_MESSAGE -> {
                        val msg = AppJson.decodeFromString<Message>(decryptedPayloadJson) // Use decryptedPayloadJson
                        val conversationId = if (dtnMessage.messageType == DtnMessageType.DIRECT_MESSAGE) dtnMessage.source else dtnMessage.destination
                        messageRepository.saveMessage(msg.copy(isSentByMe = false), conversationId, dtnMessage.source)
                        Log.d("NexaProtocol-Process", "Saved message ID=${msg.id} to conversation $conversationId.")
                    }
                    DtnMessageType.FRIEND_REQUEST -> {
                        val handshake = AppJson.decodeFromString<TransmissionPayload.Handshake>(decryptedPayloadJson) // Use decryptedPayloadJson
                        val newContact = Contact(handshake.stableId, handshake.name, handshake.publicKeyString, ContactStatus.REQUEST_RECEIVED)
                        contactRepository.storeRequest(newContact)
                        Log.d("NexaProtocol-Process", "Stored new friend request from ${handshake.stableId}.")
                    }
                    DtnMessageType.FRIEND_ACCEPT -> {
                        val handshake = AppJson.decodeFromString<TransmissionPayload.Handshake>(decryptedPayloadJson) // Use decryptedPayloadJson
                        contactRepository.updateContactStatusToFriend(handshake.stableId)
                        Log.d("NexaProtocol-Process", "Updated contact ${handshake.stableId} to FRIEND status.")
                    }
                    DtnMessageType.CHANNEL_INVITE -> {
                        val invite = AppJson.decodeFromString<TransmissionPayload.ChannelInvite>(decryptedPayloadJson) // Use decryptedPayloadJson
                        val userId = userRepository.getUserId() ?: return@launch
                        // Save the channel entity first
                        channelRepository.saveChannels(listOf(invite.channel))
                        // Then add the user as a member
                        channelRepository.addMemberToChannel(invite.channel.id, userId)
                        Log.d("NexaProtocol-Process", "Auto-accepted and joined channel ${invite.channel.name}.")
                    }
                    DtnMessageType.MESSAGE_REQUEST -> {
                        Log.d("NexaProtocol-Process", "Received a MessageRequest from ${senderPeer.name}")
                        val request = AppJson.decodeFromString<TransmissionPayload.MessageRequest>(decryptedPayloadJson)
                        val requestedMessages = dtnStore.getMessages(request.messageIds)
                        Log.d("NexaProtocol-Process", "Found ${requestedMessages.size} of ${request.messageIds.size} requested messages. Sending them to ${senderPeer.name}.")
                        requestedMessages.forEach { message ->
                            // Send each message directly to the peer that requested it.
                            sendToPeer(senderPeer, message)
                        }
                    }
                    DtnMessageType.DISCOVERY_HANDSHAKE -> {
                        val handshake = AppJson.decodeFromString<TransmissionPayload.Handshake>(decryptedPayloadJson)
                        val receivedStableId = handshake.stableId
                        val receivedPubKey = handshake.publicKeyString
                        val receivedName = handshake.name
                        val senderEndpointId = senderPeer.id

                        Log.d("NexaProtocol-Process", "HANDSHAKE RECEIVED from ${receivedName} (${receivedStableId}). Public Key Present: ${!receivedPubKey.isNullOrBlank()}.")

                        channelRepository.saveChannels(handshake.publicChannels)
                        Log.d("NexaProtocol-Process", "Saved public channels from handshake.")

                        // Update the master map of connected peers
                        connectedPeers.update { currentMap ->
                            val peerToUpdate = currentMap[senderEndpointId]
                            if (peerToUpdate != null) {
                                currentMap + (senderEndpointId to peerToUpdate.copy(stableId = receivedStableId, publicKeyString = receivedPubKey))
                            } else {
                                currentMap
                            }
                        }

                        _discoveredPeers.update { currentPeers ->
                            val peerIndex = currentPeers.indexOfFirst { it.stableId == receivedStableId }
                            val newPeer = Peer(
                                id = senderEndpointId,
                                name = receivedName,
                                stableId = receivedStableId,
                                publicKeyString = receivedPubKey
                            )
                            if (peerIndex != -1) {
                                // Peer exists, update it
                                currentPeers.toMutableList().apply { this[peerIndex] = newPeer }
                            } else {
                                // Peer does not exist, add it
                                currentPeers + newPeer
                            }
                        }

                        // Store the contact info with the new KNOWN status
                        val contact = contactRepository.getContactById(handshake.stableId)
                        if (contact == null) {
                            contactRepository.storeRequest(Contact(handshake.stableId, handshake.name, handshake.publicKeyString, ContactStatus.KNOWN))
                            Log.d("NexaProtocol-Process", "Stored new contact ${handshake.stableId} with KNOWN status.")
                        } else if (contact.publicKeyString != handshake.publicKeyString) {
                            contactRepository.storeRequest(contact.copy(publicKeyString = handshake.publicKeyString))
                            Log.d("NexaProtocol-Process", "Updated public key for contact ${handshake.stableId}.")
                        }

                        // --- DTN Message Sync ---
                        val remoteMessageIds = handshake.summaryVector.toSet()
                        val localMessageIds = dtnStore.getAllMessageIds().toSet()
                        val missingIds = remoteMessageIds - localMessageIds

                        if (missingIds.isNotEmpty()) {
                            Log.d("NexaProtocol-Sync", "Requesting ${missingIds.size} missing messages from ${senderPeer.name}.")
                            val request = TransmissionPayload.MessageRequest(missingIds.toList())
                            // Send the request back to the peer who sent the handshake
                            send(senderPeer.stableId ?: return@launch, request, senderPeer.publicKeyString)
                        }


                    }
                    DtnMessageType.PUBLIC_CHANNEL_ANNOUNCEMENT, DtnMessageType.PROFILE_UPDATE -> {
                        // These are public, payload is already unencrypted content JSON
                        // No E2E decryption needed here, just deserialize
                        // (Assuming content is directly deserializable for these types)
                        // For now, let's just log if not handled explicitly
                        Log.w("NexaProtocol-Process", "Unhandled public message type: ${dtnMessage.messageType}. Payload: $decryptedPayloadJson")
                    }
                    else -> Log.w("NexaProtocol-Process", "Unknown message type: ${dtnMessage.messageType}. Payload: $decryptedPayloadJson")
                }
            } catch (e: Exception) {
                Log.e("NexaProtocol-Process", "Failed to process payload for message ${dtnMessage.id}", e)
            }
        }
    }

    private fun sendToPeer(peer: Peer, dtnMessage: DtnMessage) {
        serviceScope.launch {
            try {
                val peerContact = peer.stableId?.let { contactRepository.getContactById(it) }
                Log.d("NexaProtocol-SendToPeer", "Preparing to send DtnMessage ID=${dtnMessage.id} to peer ${peer.name} (StableID: ${peer.stableId}).")
                val dtnMessageJson = AppJson.encodeToString(dtnMessage)
                val payloadBytes: ByteArray

                // If it's a handshake and we don't have their key, send the DtnMessage unencrypted.
                if (dtnMessage.messageType == DtnMessageType.DISCOVERY_HANDSHAKE && peerContact?.publicKeyString == null) {
                    Log.w("NexaProtocol-SendToPeer", "Sending initial DISCOVERY_HANDSHAKE as unencrypted DtnMessage to ${peer.name}.")
                    payloadBytes = dtnMessageJson.toByteArray(StandardCharsets.UTF_8)
                } else {
                    // For all other messages, or subsequent DISCOVERY_HANDSHAKE, encrypt hop-by-hop
                    val publicKeyString = peerContact?.publicKeyString
                    if (publicKeyString == null) {
                        Log.e("NexaProtocol-SendToPeer", "Failed to get public key for peer ${peer.name} (StableID: ${peer.stableId}) for hop-by-hop encryption of DtnMessage ID=${dtnMessage.id}. Public key is null.")
                        return@launch
                    }
                    val theirPublicKeyBytes = Base64.decode(publicKeyString, Base64.DEFAULT)
                    Log.d("NexaProtocol-SendToPeer", "Attempting hop-by-hop encryption for DtnMessage ID=${dtnMessage.id} to ${peer.name}.")
                    val encryptedBytes = securityRepository.encrypt(dtnMessageJson, theirPublicKeyBytes)
                    if (encryptedBytes == null) {
                        Log.e("NexaProtocol-SendToPeer", "Failed to encrypt DtnMessage ID=${dtnMessage.id} for hop-by-hop to ${peer.name}.")
                        return@launch
                    }
                    val transmission = TransmissionPayload.EncryptedMessage(Base64.encodeToString(encryptedBytes, Base64.DEFAULT))
                    payloadBytes = AppJson.encodeToString(transmission).toByteArray(StandardCharsets.UTF_8)
                    Log.d("NexaProtocol-SendToPeer", "DtnMessage ID=${dtnMessage.id} hop-by-hop encrypted successfully for ${peer.name}.")
                }

                if (payloadBytes != null) {
                    connectionsClient.sendPayload(peer.id, Payload.fromBytes(payloadBytes))
                    Log.d("NexaProtocol-SendToPeer", "Successfully sent payload for DtnMessage ID=${dtnMessage.id} to ${peer.name}.")
                } else {
                    Log.e("NexaProtocol-SendToPeer", "Failed to prepare payload for DtnMessage ID=${dtnMessage.id} to ${peer.name}. PayloadBytes was null.")
                }
            } catch (e: Exception) {
                Log.e("NexaProtocol-SendToPeer", "Failed to send DtnMessage ID=${dtnMessage.id} to ${peer.name}. Error: ${e.message}", e)
            }
        }
    }

    // --- PUBLIC API FOR VIEWMODELS ---

    fun sendMessage(conversationId: String, message: Message) {
        serviceScope.launch {
            val contact = contactRepository.getContactById(conversationId)
            send(conversationId, message, contact?.publicKeyString)
        }
    }
    fun sendChannelInvite(friendId: String, channel: Channel) {
        serviceScope.launch {
            val contact = contactRepository.getContactById(friendId)
            val invitePayload = TransmissionPayload.ChannelInvite(channel.copy(members = emptyList()), userRepository.getUsername())
            send(friendId, invitePayload, contact?.publicKeyString)
        }
    }
    fun sendFriendRequest(peer: Peer) {
        serviceScope.launch {
            val handshake = TransmissionPayload.Handshake(
                stableId = userRepository.getUserId() ?: return@launch,
                name = userRepository.getUsername(),
                publicKeyString = Base64.encodeToString(securityRepository.getMyPublicKeyBytes(), Base64.DEFAULT),
                handshakeType = HandshakeType.FRIEND_REQUEST
            )
            // Pass the peer.publicKeyString directly to send() for E2E encryption
            send(peer.stableId ?: return@launch, handshake, peer.publicKeyString)
        }
    }
    fun acceptFriendRequest(contact: Contact) {
        serviceScope.launch {
            contactRepository.updateContactStatusToFriend(contact.stableId)
            val handshake = TransmissionPayload.Handshake(
                stableId = userRepository.getUserId() ?: return@launch,
                name = userRepository.getUsername(),
                publicKeyString = Base64.encodeToString(securityRepository.getMyPublicKeyBytes(), Base64.DEFAULT),
                handshakeType = HandshakeType.FRIEND_ACCEPT
            )
            // Pass the contact.publicKeyString directly to send() for E2E encryption
            send(contact.stableId, handshake, contact.publicKeyString)
        }
    }

    fun disconnect() {
        connectionsClient.stopAllEndpoints()
        connectedPeers.update { emptyMap() }
    }

    fun disconnect(stableId: String) {
        val peer = connectedPeers.value[stableId]
        if (peer != null) {
            connectionsClient.disconnectFromEndpoint(peer.id)
            connectedPeers.update { it - stableId }
        }
    }

    fun requestConnectionTo(stableId: String) {
        serviceScope.launch {
            Log.d("NexaProtocol-Connect", "Attempting to request connection to stableId: $stableId.")
            val peerToConnect = _discoveredPeers.value.find { it.stableId == stableId }
            if (peerToConnect != null) {
                connectionsClient.requestConnection(userRepository.getUsername(), peerToConnect.id, connectionLifecycleCallback)
                Log.d("NexaProtocol-Connect", "Requested connection to discovered peer ${peerToConnect.name} (Endpoint ID: ${peerToConnect.id}).")
            } else {
                Log.w("NexaProtocol-Connect", "Peer with stableId $stableId not currently discovered, cannot request connection.")
            }
        }
    }


    // --- NEARBY CONNECTIONS BOILERPLATE ---

    private fun startTtlPruning() {
        serviceScope.launch {
            while (true) {
                dtnStore.pruneExpiredMessages()
                delay(60 * 60 * 1000) // Prune every hour
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            serviceScope.launch {
                try {
                    if (payload.type != Payload.Type.BYTES) return@launch
                    val receivedBytes = payload.asBytes() ?: return@launch
                    val receivedString = String(receivedBytes, StandardCharsets.UTF_8)

                    var senderPeer = connectedPeers.value[endpointId]
                    if (senderPeer == null) {
                        // Peer not in the connected map yet, maybe the connection is pending?
                        val connectionInfo = pendingConnections[endpointId]
                        if (connectionInfo != null) {
                            // Create a temporary Peer object from the pending info
                            val endpointNameParts = connectionInfo.endpointName.split("|")
                            val peerName = endpointNameParts.getOrNull(0) ?: "Unknown"
                            val peerStableId = endpointNameParts.getOrNull(1)
                            senderPeer = Peer(id = endpointId, name = peerName, stableId = peerStableId)
                            Log.w("NexaProtocol-Payload", "Processing payload from a peer who is not fully connected yet: ${senderPeer.name}")
                        } else {
                            // We don't know this endpoint at all. Abort.
                            Log.e("NexaProtocol-Payload", "Received payload from unknown endpointId: $endpointId. Dropping.")
                            return@launch
                        }
                    }

                    var dtnMessage: DtnMessage? = null

                    // Try to parse as hop-by-hop encrypted message first
                    try {
                        val transmission = AppJson.decodeFromString<TransmissionPayload.EncryptedMessage>(receivedString)
                        val ciphertext = Base64.decode(transmission.ciphertextString, Base64.DEFAULT)
                        val decryptedJson = securityRepository.decrypt(ciphertext, "nexa_handshake".toByteArray(StandardCharsets.UTF_8))
                        if (decryptedJson != null) {
                            dtnMessage = AppJson.decodeFromString<DtnMessage>(decryptedJson)
                        } else {
                            Log.e("NexaProtocol-Payload", "Failed to decrypt hop-by-hop message from ${senderPeer.name}")
                            return@launch
                        }
                    } catch (e: Exception) {
                        // If it fails, it might be an unencrypted bootstrap message
                        Log.w("NexaProtocol-Payload", "Could not parse as EncryptedMessage, trying as raw DtnMessage. Error: ${e.message}")
                        try {
                            dtnMessage = AppJson.decodeFromString<DtnMessage>(receivedString)
                        } catch (e2: Exception) {
                            Log.e("NexaProtocol-Payload", "Failed to parse payload as either EncryptedMessage or DtnMessage.", e2)
                        }
                    }

                    if (dtnMessage != null) {
                        onReceive(dtnMessage, senderPeer)
                    }

                } catch (e: Exception) {
                    Log.e("NexaProtocol-Payload", "Payload processing failed", e)
                }
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d("NexaProtocol-Connect", "Connection initiated with endpoint: $endpointId, name: ${info.endpointName}.")
            pendingConnections[endpointId] = info
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            Log.d("NexaProtocol-Connect", "Accepted connection with endpoint: $endpointId.")
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            val connectionInfo = pendingConnections.remove(endpointId)
            if (result.status.isSuccess && connectionInfo != null) {
                // stableId might not be reliably available in connectionInfo.endpointName
                // We will get the stableId from the DISCOVERY_HANDSHAKE
                Log.d("NexaProtocol-Connect", "Connection established to $endpointId (name: ${connectionInfo.endpointName}). Status: ${result.status.statusCode}.")
                val endpointNameParts = connectionInfo.endpointName.split("|")
                val peerName = endpointNameParts.getOrNull(0) ?: "Unknown"
                val peerStableId = endpointNameParts.getOrNull(1)
                val peer = Peer(id = endpointId, name = peerName, stableId = peerStableId)
                connectedPeers.update { it + (endpointId to peer) } // Key by endpointId, but Peer object has stableId
                Log.d("NexaProtocol-Connect", "Added peer ${peer.name} (Endpoint ID: ${peer.id}, StableID: ${peer.stableId}) to connected peers. Sending discovery handshake.")
                sendDiscoveryHandshake(peer)
            } else {
                Log.w("NexaProtocol-Connect", "Connection failed to $endpointId. Status: ${result.status.statusCode}. ConnectionInfo was null: ${connectionInfo == null}.")
            }
        }

        override fun onDisconnected(endpointId: String) {
            val peer = connectedPeers.value[endpointId]
            if (peer != null) {
                connectedPeers.update { it - endpointId }
                _discoveredPeers.update { it.filterNot { p -> p.id == endpointId } }
                Log.d("NexaProtocol-Connect", "Disconnected from peer ${peer.name} (EndpointId: $endpointId).")
            } else {
                Log.d("NexaProtocol-Connect", "Disconnected from unknown endpoint: $endpointId.")
            }
        }
    }

    private fun sendDiscoveryHandshake(peer: Peer) {
        serviceScope.launch {
            val handshake = TransmissionPayload.Handshake(
                stableId = userRepository.getUserId() ?: return@launch,
                name = userRepository.getUsername(),
                publicKeyString = Base64.encodeToString(securityRepository.getMyPublicKeyBytes(), Base64.DEFAULT),
                handshakeType = HandshakeType.DISCOVERY,
                summaryVector = dtnStore.getAllMessageIds(),
                publicChannels = channelRepository.getPublicChannels().first()
            )
            // Pass the peer.publicKeyString to send() for E2E encryption of the handshake payload
            send(peer.stableId ?: return@launch, handshake, peer.publicKeyString)
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d("NexaProtocol-Discovery", "Endpoint found: ID=$endpointId, Name=${info.endpointName}.")
            val parts = info.endpointName.split("|")
            val displayName = parts.getOrNull(0)
            val discoveredStableId = parts.getOrNull(1)

            // Prevent connecting to self
            if (displayName == null || discoveredStableId == null || discoveredStableId == userRepository.getUserId()) {
                Log.d("NexaProtocol-Discovery", "Skipping endpoint $endpointId: Invalid name format or is self.")
                return
            }

            // Prevent reconnecting if already connected or connecting
            if (connectedPeers.value.values.any { it.stableId == discoveredStableId } || pendingConnections.containsKey(endpointId)) {
                Log.d("NexaProtocol-Discovery", "Ignoring endpoint $endpointId ($displayName): Already connected or connection is pending.")
                return
            }

            // Tie-breaker: Only request connection if our endpoint ID is lexicographically smaller
            // This prevents both devices from simultaneously requesting connection.
            val myStableId = userRepository.getUserId()
            if (myStableId != null && myStableId < discoveredStableId) {
                Log.d("NexaProtocol-Discovery", "Our stable ID ($myStableId) is lexicographically smaller than discovered peer's ($discoveredStableId). Requesting connection.")
                val myName = "${userRepository.getUsername()}|${userRepository.getUserId()}"
                connectionsClient.requestConnection(myName, endpointId, connectionLifecycleCallback)
                    .addOnSuccessListener { Log.d("NexaProtocol-Connect", "Successfully requested connection to endpoint: $endpointId.") }
                    .addOnFailureListener { e -> Log.e("NexaProtocol-Connect", "Failed to request connection to endpoint: $endpointId. Error: ${e.message}", e) }
            } else {
                Log.d("NexaProtocol-Discovery", "Our stable ID (${myStableId}) is not lexicographically smaller than discovered peer's ($discoveredStableId). Waiting for other peer to request connection.")
            }
        }
        override fun onEndpointLost(endpointId: String) {
            val lostPeer = _discoveredPeers.value.find { it.id == endpointId }
            if (lostPeer != null){
                _discoveredPeers.update { it.filterNot { p -> p.id == endpointId } }
            }
            Log.d("NexaProtocol-Discovery", "Endpoint lost: ID=$endpointId, Name=${lostPeer?.name ?: "Unknown"}.")
        }
    }

    fun startAdvertising() {
        val myStableId = userRepository.getUserId() ?: return
        val advertisingName = "${userRepository.getUsername()}|$myStableId"
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startAdvertising(advertisingName, SERVICE_ID, connectionLifecycleCallback, options)
            .addOnSuccessListener {
                Log.d("NexaProtocol-Advertising", "Started advertising as $advertisingName.") 
            }
            .addOnFailureListener { e -> Log.e("NexaProtocol-Advertising", "Failed to start advertising: ${e.message}", e) }
    }

    fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnSuccessListener { Log.d("NexaProtocol-Discovery", "Started discovery.") }
            .addOnFailureListener { e -> Log.e("NexaProtocol-Discovery", "Failed to start discovery: ${e.message}", e) }
    }

    fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        Log.d("NexaProtocol-Advertising", "Stopped advertising.")
    }

    fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        Log.d("NexaProtocol-Discovery", "Stopped discovery.")
    }

    private fun startForegroundService() {
        val channelId = "nexa_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Nexa Connection Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Nexa Network")
            .setContentText("Running in background to discover peers and route messages.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
        startForeground(1, notification)
    }
}
