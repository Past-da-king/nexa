package com.example.nexus.data

import com.example.nexus.models.*
import kotlinx.coroutines.flow.Flow

/**
 * The single source of truth for all interactions between the UI and the backend services.
 * The UI will call these methods, and the backend team will implement them.
 *
 * NOTE: 'Flow' is used for data that needs to be observed in real-time.
 * 'suspend' is used for one-shot actions that may take time.
 */
interface NexaService {

    // --- 1. Onboarding & Identity ---

    suspend fun createUserProfile(name: String): Boolean

    fun getMyProfile(): Flow<UserProfile>


    // --- 2. Peer Discovery & Connectivity ---

    suspend fun startPeerDiscovery()

    suspend fun stopPeerDiscovery()

    suspend fun startAdvertising()
    suspend fun stopAdvertising()
    suspend fun stopScan()

    fun getIsConnectionSecure(): Flow<Boolean>

    fun getDiscoveredPeers(): Flow<List<Peer>>



    fun generateMyQRCode(): String

    suspend fun connectWithQRCode(qrData: String)


    // --- 3. Conversations & Messaging ---

    fun getConversations(): Flow<List<Conversation>>

    fun getMessages(conversationId: String): Flow<List<Message>>

    suspend fun sendMessage(conversationId: String, message: Message)

    // --- 4. Group Channels ---

    fun getAllChannels(): Flow<List<Channel>>
    fun getPublicChannels(): Flow<List<Channel>>
    suspend fun getChannelById(channelId: String): Channel?

    suspend fun createChannel(name: String, description: String, isPublic: Boolean): String

    suspend fun joinChannel(channelId: String)
    suspend fun sendChannelInvite(friendId: String, channel: Channel)
    suspend fun acceptChannelInvite(channelId: String)


    // --- 5. Data Management ---

    suspend fun backupDataToCloud()

    suspend fun restoreDataFromCloud()


    fun getPendingRequest(): Flow<Peer?>
    fun getConnectedPeer(): Flow<Peer?>

    fun getConnectionStatus(): Flow<String>
    
    suspend fun disconnect()
    suspend fun requestConnectionTo(stableId: String)

    fun getAllFriends(): Flow<List<Contact>>

    fun getFriendRequests(): Flow<List<Contact>>

    suspend fun sendFriendRequest(peer: Peer)

    suspend fun acceptFriendRequest(contact: Contact)

    suspend fun rejectFriendRequest(stableId: String)
}