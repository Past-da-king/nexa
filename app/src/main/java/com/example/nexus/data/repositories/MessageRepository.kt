package com.example.nexus.data.repositories

import com.example.nexus.models.Conversation
import com.example.nexus.models.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getConversation(peerId: String): Flow<List<Message>>
    suspend fun saveMessage(message: Message, conversationId: String, senderId: String)
    fun getConversations(): Flow<List<Conversation>>
}