package com.example.nexus.models

/**
 * Represents the status of a sent message.
 */
enum class MessageStatus {
    SENDING, SENT, DELIVERED, FAILED, QUEUED_DTN, FORWARDING_DTN
}

/**
 * Represents whether a message is destined for a single user or a group channel.
 */
enum class RecipientType {
    USER, CHANNEL
}


// Represents the connection status of a peer
enum class ConnectionStatus {
    CONNECTED, DISCONNECTED, CONNECTING
}