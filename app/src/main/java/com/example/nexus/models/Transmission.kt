package com.example.nexus.models

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// The enum for handshake types remains the same.
enum class HandshakeType {
    DISCOVERY,       // For automatic, public data exchange with any peer.
    FRIEND_REQUEST,  // An explicit, user-initiated request to form a private link.
    FRIEND_ACCEPT    // A confirmation that a friend request has been accepted.
}

@OptIn(InternalSerializationApi::class)
@Serializable
sealed class TransmissionPayload {

    @Serializable
    @SerialName("Handshake")
    data class Handshake(
        val stableId: String,
        val name: String,
        val publicKeyString: String,


        @SerialName("handshake_type")
        val handshakeType: HandshakeType,
        val summaryVector: List<String> = emptyList(),
        val publicChannels: List<Channel> = emptyList()
    ) : TransmissionPayload()

    @Serializable
    @SerialName("EncryptedMessage")
    data class EncryptedMessage(val ciphertextString: String) : TransmissionPayload()

    @Serializable
    @SerialName("MessageRequest")
    data class MessageRequest(val messageIds: List<String>) : TransmissionPayload()

    @Serializable
    @SerialName("ChannelInvite")
    data class ChannelInvite(
        val channel: Channel,
        val inviterName: String
    ) : TransmissionPayload()
}