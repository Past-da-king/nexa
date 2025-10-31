package com.example.nexus.data.repositories

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.example.nexus.models.Peer
import com.example.nexus.models.TransmissionPayload
import com.example.nexus.services.ConnectionService
import com.example.nexus.models.Channel
import com.example.nexus.models.Contact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class ConnectionRepositoryImpl @Inject constructor(
    private val context: Context
) : ConnectionRepository {

    private var connectionService: ConnectionService? = null
    private var serviceScope = CoroutineScope(Dispatchers.Main + Job()) // Use Main dispatcher for UI-related updates

    // Mirrored StateFlows
    private val _discoveredPeers = MutableStateFlow<List<Peer>>(emptyList())
    override val discoveredPeers: StateFlow<List<Peer>> = _discoveredPeers.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Not Connected")
    override val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _pendingConnectionRequest = MutableStateFlow<Peer?>(null)
    override val pendingConnectionRequest: StateFlow<Peer?> = _pendingConnectionRequest.asStateFlow()

    private val _connectedPeer = MutableStateFlow<Peer?>(null)
    override val connectedPeer: StateFlow<Peer?> = _connectedPeer.asStateFlow()

    private val _isConnectionSecure = MutableStateFlow(false)
    override val isConnectionSecure: StateFlow<Boolean> = _isConnectionSecure.asStateFlow()

    private val _permissionsGranted = MutableStateFlow(false)
    override val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()

    private var flowCollectionJob: Job? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ConnectionService.LocalBinder
            connectionService = binder.getService()
            // Start collecting flows from the service
            flowCollectionJob = serviceScope.launch {
                connectionService?.discoveredPeers?.collect { _discoveredPeers.value = it }
            }
            serviceScope.launch {
                connectionService?.connectionStatus?.collect { _connectionStatus.value = it }
            }
            serviceScope.launch {
                connectionService?.pendingConnectionRequest?.collect { _pendingConnectionRequest.value = it }
            }
            serviceScope.launch {
                connectionService?.connectedPeer?.collect { peer -> _connectedPeer.value = peer }
            }
            serviceScope.launch {
                connectionService?.isConnectionSecure?.collect { isSecure -> _isConnectionSecure.value = isSecure }
            }
            serviceScope.launch {
                connectionService?.permissionsGranted?.collect { granted -> _permissionsGranted.value = granted }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            connectionService = null
            flowCollectionJob?.cancel() // Cancel collection when service disconnects
            // Reset mirrored StateFlows
            _discoveredPeers.value = emptyList()
            _connectionStatus.value = "Not Connected"
            _pendingConnectionRequest.value = null
            _connectedPeer.value = null
            _isConnectionSecure.value = false
            _permissionsGranted.value = false
        }
    }

    init {
        Intent(context, ConnectionService::class.java).also { intent ->
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun setPermissionsGranted(granted: Boolean) {
        _permissionsGranted.value = granted
        connectionService?.setPermissionsGranted(granted)
    }

    // Delegate calls to the service
    override suspend fun startAdvertising() {
        connectionService?.startAdvertising()
    }

    override suspend fun startDiscovery() {
        connectionService?.startDiscovery()
    }

    override suspend fun stopAdvertising() {
        connectionService?.stopAdvertising()
    }

    override suspend fun stopDiscovery() {
        connectionService?.stopDiscovery()
    }

    override suspend fun stopScan() {
        connectionService?.stopDiscovery()
    }

    override fun sendMessage(conversationId: String, message: com.example.nexus.models.Message) {
        connectionService?.sendMessage(conversationId, message)
    }

    override fun sendDtnMessage(recipientStableId: String, message: com.example.nexus.models.Message) {
        // This method is not directly implemented in ConnectionService as the DTN logic is internal to send()
        // However, to satisfy the interface, we can delegate to sendMessage if the intent is similar
        // or leave it as a no-op if it's meant for internal DTN handling only.
        // For now, let's delegate to sendMessage, assuming it handles the DTN wrapping.
        connectionService?.sendMessage(recipientStableId, message)
    }

    override fun sendChannelInvite(friendId: String, channel: Channel) {
        connectionService?.sendChannelInvite(friendId, channel)
    }

    override suspend fun sendFriendRequest(peer: Peer) {
        connectionService?.sendFriendRequest(peer)
    }

    override suspend fun acceptFriendRequest(contact: Contact) {
        connectionService?.acceptFriendRequest(contact)
    }

    override fun disconnect() {
        connectionService?.disconnect()
    }

    override fun disconnect(stableId: String) {
        connectionService?.disconnect(stableId)
    }

    override fun requestConnectionTo(stableId: String) {
        connectionService?.requestConnectionTo(stableId)
    }

    override fun getConnectedPeer(): Flow<Peer?> = connectedPeer
    override fun getIsConnectionSecure(): Flow<Boolean> = isConnectionSecure
}