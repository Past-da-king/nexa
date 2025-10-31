package com.example.nexus.data.repositories

import com.example.nexus.models.Channel
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun getAllChannels(): Flow<List<Channel>>
    fun getPublicChannels(): Flow<List<Channel>>
    suspend fun getChannelById(channelId: String): Channel?
    suspend fun createChannel(name: String, description: String, isPublic: Boolean, creatorId: String): String
    suspend fun addMemberToChannel(channelId: String, userId: String)
    suspend fun removeMemberFromChannel(channelId: String, userId: String)
    suspend fun saveChannels(channels: List<Channel>)
}
