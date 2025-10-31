package com.example.nexus.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = false) // We will use the timestamp as the ID
    val messageId: Long,
    val conversationId: String, // Will be the peer's ID
    val senderId: String,
    val content: String, // For text content or media caption
    val data: String?, // For Base64 encoded media
    val messageType: String,
    val isSentByMe: Boolean,
    val status: String
)