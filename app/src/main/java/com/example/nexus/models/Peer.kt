package com.example.nexus.models

/**
 * Represents another user discovered on the network.
 *
 * @param id The unique endpoint ID provided by the Nearby Connections API.
 * @param name The human-readable name that the peer is advertising.
 */
data class Peer(
    val id: String,
    val name: String,
    val stableId: String? = null,
    val publicKeyString: String? = null,
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED
)