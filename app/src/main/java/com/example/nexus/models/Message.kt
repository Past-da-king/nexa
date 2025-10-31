package com.example.nexus.models

import kotlinx.serialization.Serializable

/**
 * Represents the type of content a message holds.
 */
@Serializable
enum class MessageType {
    TEXT,
    IMAGE,
    VOICE
}

/**
 * Represents a single message within a conversation.
 *
 * @param id A unique identifier for the message (e.g., a timestamp).
 * @param text The content of the message, or a caption for media.
 * @param data Holds the Base64 encoded string for IMAGE or VOICE messages.
 * @param isSentByMe True if the message was sent by the current user, false if it was received.
 * @param messageType The type of the message (TEXT, IMAGE, or VOICE).
 * @param status The delivery status of the message.
 */
@Serializable
data class Message(
    val id: String,
    val text: String, // Used for text content or media caption
    val data: String? = null, // Base64 for IMAGE/VOICE
    val isSentByMe: Boolean,
    val messageType: MessageType = MessageType.TEXT,
    val status: MessageStatus
)