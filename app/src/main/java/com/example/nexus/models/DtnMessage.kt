package com.example.nexus.models

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

enum class DtnMessageType {
    DIRECT_MESSAGE,
    GROUP_MESSAGE,
    FRIEND_REQUEST,
    FRIEND_ACCEPT,
    CHANNEL_INVITE,
    PUBLIC_CHANNEL_ANNOUNCEMENT,
    PROFILE_UPDATE,
    DISCOVERY_HANDSHAKE,
    MESSAGE_REQUEST
}

@OptIn(InternalSerializationApi::class)
@Serializable
data class DtnMessage(
    val id: String,
    val source: String,
    val destination: String,
    val payload: String,
    val ttl: Long,
    val hopCount: Int,
    val timestamp: Long,
    val messageType: DtnMessageType = DtnMessageType.DIRECT_MESSAGE
)
