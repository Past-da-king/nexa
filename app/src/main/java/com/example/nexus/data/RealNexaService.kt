package com.example.nexus.data

import android.util.Log
import com.example.nexus.data.repositories.*
import com.example.nexus.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The production implementation of our NexaService interface.
 */
class RealNexaService(
    private val userRepository: UserRepository,
    private val connectionRepository: ConnectionRepository,
    private val securityRepository: SecurityRepository,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val channelRepository: ChannelRepository
) : NexaService {

    // --- 1. Onboarding & Identity ---

    override suspend fun createUserProfile(name: String): Boolean {
        userRepository.createUser(name)
        securityRepository.generateKeyPair() // Ensure keys are generated on profile creation
        return true
    }

    override fun getMyProfile(): Flow<UserProfile> {
        return userRepository.getUsernameFlow().map { name ->
            UserProfile(userId = userRepository.getUserId() ?: "", userName = name)
        }
    }

    // --- 2. Peer Discovery & Connectivity ---

    override suspend fun startPeerDiscovery() {
        connectionRepository.startDiscovery()
    }

    override suspend fun stopPeerDiscovery() {
        connectionRepository.stopDiscovery()
    }


    override suspend fun startAdvertising() {
        connectionRepository.startAdvertising()
    }

    override suspend fun stopAdvertising() {
        connectionRepository.stopAdvertising()
    }

    override suspend fun stopScan() {
        connectionRepository.stopScan()
    }

    override fun getDiscoveredPeers(): Flow<List<Peer>> {
        return connectionRepository.discoveredPeers
    }



    override fun generateMyQRCode(): String = "TODO: QR Code Data"

    override suspend fun connectWithQRCode(qrData: String) {
        // TODO: Implement
    }

    override fun getPendingRequest(): Flow<Peer?> = connectionRepository.pendingConnectionRequest
    override fun getConnectedPeer(): Flow<Peer?> = connectionRepository.getConnectedPeer()
    override fun getIsConnectionSecure(): Flow<Boolean> = connectionRepository.getIsConnectionSecure()
    override fun getConnectionStatus(): Flow<String> = connectionRepository.connectionStatus

    override suspend fun disconnect() {
        connectionRepository.disconnect()
    }
    // --- 3. Conversations & Messaging ---

    override fun getConversations(): Flow<List<Conversation>> {
        return messageRepository.getConversations()
    }

    override fun getMessages(conversationId: String): Flow<List<Message>> {
        return messageRepository.getConversation(conversationId)
    }

    override suspend fun sendMessage(conversationId: String, message: Message) {
        messageRepository.saveMessage(message, conversationId, userRepository.getUserId() ?: "")
        connectionRepository.sendMessage(conversationId, message)
    }

    override suspend fun requestConnectionTo(stableId: String) {
        connectionRepository.requestConnectionTo(stableId)
    }

    override fun getAllFriends(): Flow<List<Contact>> {
        return contactRepository.getAllFriends()
    }

    override fun getFriendRequests(): Flow<List<Contact>> {
        return contactRepository.getFriendRequests()
    }

    override suspend fun sendFriendRequest(peer: Peer) {
        connectionRepository.sendFriendRequest(peer)
    }

    override suspend fun acceptFriendRequest(contact: Contact) {
        connectionRepository.acceptFriendRequest(contact)
    }


    override suspend fun rejectFriendRequest(stableId: String) {
        contactRepository.deleteContact(stableId)
        // Removed: connectionRepository.disconnect(stableId) to align with DTN philosophy
    }

    // --- 4. Group Channels ---

    override fun getAllChannels(): Flow<List<Channel>> {
        return channelRepository.getAllChannels()
    }

    override fun getPublicChannels(): Flow<List<Channel>> {
        return channelRepository.getPublicChannels()
    }

    override suspend fun getChannelById(channelId: String): Channel? {
        return channelRepository.getChannelById(channelId)
    }

    override suspend fun createChannel(name: String, description: String, isPublic: Boolean): String {
        val userId = userRepository.getUserId() ?: throw IllegalStateException("User not onboarded")
        return channelRepository.createChannel(name, description, isPublic, userId)
    }

    override suspend fun joinChannel(channelId: String) {
        val userId = userRepository.getUserId() ?: throw IllegalStateException("User not onboarded")
        channelRepository.addMemberToChannel(channelId, userId)
    }

    override suspend fun sendChannelInvite(friendId: String, channel: Channel) {
        connectionRepository.sendChannelInvite(friendId, channel)
    }

    override suspend fun acceptChannelInvite(channelId: String) {
        val userId = userRepository.getUserId() ?: throw IllegalStateException("User not onboarded")
        channelRepository.addMemberToChannel(channelId, userId)
    }

    // --- 5. Data Management ---

    override suspend fun backupDataToCloud() {
        // TODO: Implement
    }

    override suspend fun restoreDataFromCloud() {
        // TODO: Implement
    }
}