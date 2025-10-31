package com.example.nexus.data.repositories

import android.util.Log
import com.example.nexus.data.database.MessageDao
import com.example.nexus.data.database.MessageEntity
import com.example.nexus.models.Conversation
import com.example.nexus.models.Message
import com.example.nexus.models.MessageStatus
import com.example.nexus.models.MessageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessageRepositoryImpl(
    private val messageDao: MessageDao,
    private val contactRepository: ContactRepository // Add this parameter
) : MessageRepository {

    override fun getConversation(peerId: String): Flow<List<Message>> {
        // Get the entities from the DB and map them to our app's Message model
        return messageDao.getMessagesForConversation(peerId).map { entities ->
            entities.map { entity ->
                Message(
                    id = entity.messageId.toString(),
                    text = entity.content,
                    data = entity.data,
                    isSentByMe = entity.isSentByMe,
                    messageType = MessageType.valueOf(entity.messageType),
                    status = MessageStatus.valueOf(entity.status)
                )
            }
        }
    }

    override suspend fun saveMessage(message: Message, conversationId: String, senderId: String) {
        val entity = MessageEntity(
            messageId = message.id.toLong(),
            conversationId = conversationId,
            senderId = senderId,
            content = message.text,
            data = message.data,
            messageType = message.messageType.name,
            isSentByMe = message.isSentByMe,
            status = message.status.name
        )
        messageDao.insertMessage(entity)
        Log.d("SendMessage", "MessageRepo: Saving message to DB.")
    }

    override fun getConversations(): Flow<List<Conversation>> {
        return messageDao.getConversations().map { entities ->
            entities.map { entity ->
                // Look up the contact's real name using their stableId
                val contact = contactRepository.getContactById(entity.conversationId)

                Conversation(
                    conversationId = entity.conversationId, // This is the stableId
                    name = contact?.name ?: "Unknown Contact", // Show real name instead of UUID
                    lastMessage = entity.content,
                    timestamp = entity.messageId,
                    isGroup = false
                )
            }
        }
    }
}