package com.example.nexus.models

import kotlinx.serialization.Serializable

/**
 * Represents a group channel.
 *
 * @param id The unique ID for the channel.
 * @param name The display name of the channel.
 * @param description A short description of the channel's purpose.
 * @param isPublic True if the channel is discoverable by others, false if it is private and invite-only.
 * @param members A list of stableIds of the channel members.
 */
@Serializable
data class Channel(
    val id: String,
    val name: String,
    val description: String,
    val isPublic: Boolean,
    val members: List<String> = emptyList()
)