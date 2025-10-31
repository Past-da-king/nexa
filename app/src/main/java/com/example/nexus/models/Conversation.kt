package com.example.nexus.models

/**
 * Represents a single conversation item on the home screen.
 * This can be either a 1-to-1 chat or a group channel.
 *
 * @param conversationId The unique ID of the conversation (can be a peerId or a channelId).
 * @param name The display name of the conversation (e.g., "Alice" or "Hiking Club").
 * @param lastMessage A preview of the most recent message in the conversation.
 * @param timestamp The time the last message was sent.
 * @param isGroup True if this conversation is a group channel, false if it is a direct message.
 */
data class Conversation(
    val conversationId: String,
    val name: String,
    val lastMessage: String,
    val timestamp: Long,
    val isGroup: Boolean
)