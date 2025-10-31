package com.example.nexus.data.repositories

import com.example.nexus.models.Peer
import com.example.nexus.models.TransmissionPayload
import com.example.nexus.models.Contact
import com.example.nexus.models.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ConnectionRepository {
    // --- Live Data Streams ---
    val connectedPeer: StateFlow<Peer?>
    val discoveredPeers: StateFlow<List<Peer>>
    val connectionStatus: StateFlow<String>
    val pendingConnectionRequest: StateFlow<Peer?>
    val isConnectionSecure: StateFlow<Boolean>
    val permissionsGranted: StateFlow<Boolean>
    fun setPermissionsGranted(granted: Boolean)
    fun getConnectedPeer(): Flow<Peer?>
    fun getIsConnectionSecure(): Flow<Boolean>

    // --- Connection Management ---
    fun disconnect()
    fun disconnect(stableId: String)
    suspend fun sendFriendRequest(peer: Peer)
    suspend fun acceptFriendRequest(contact: Contact)

    // --- Core Actions ---
    suspend fun startAdvertising()
    suspend fun startDiscovery()
    suspend fun stopDiscovery()
    suspend fun stopAdvertising()
    suspend fun stopScan()

    // --- Messaging ---
    fun sendMessage(conversationId: String, message: com.example.nexus.models.Message)
    fun sendDtnMessage(recipientStableId: String, message: com.example.nexus.models.Message)
    fun sendChannelInvite(friendId: String, channel: Channel)


    fun requestConnectionTo(stableId: String)

}