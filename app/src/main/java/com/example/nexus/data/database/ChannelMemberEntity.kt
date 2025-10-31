package com.example.nexus.data.database

import androidx.room.Entity

/**
 * Represents the many-to-many relationship between a Channel and a User (Contact).
 * Each entry signifies that a user is a member of a channel.
 */
@Entity(tableName = "channel_members", primaryKeys = ["channelId", "userId"])
data class ChannelMemberEntity(
    val channelId: String,
    val userId: String // This is the stableId of the user
)