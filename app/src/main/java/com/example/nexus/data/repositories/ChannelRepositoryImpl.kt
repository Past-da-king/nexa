package com.example.nexus.data.repositories

import com.example.nexus.data.database.ChannelDao
import com.example.nexus.data.database.ChannelEntity
import com.example.nexus.data.database.ChannelMemberEntity
import com.example.nexus.models.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class ChannelRepositoryImpl @Inject constructor(
    private val channelDao: ChannelDao
) : ChannelRepository {

    override fun getAllChannels(): Flow<List<Channel>> {
        return channelDao.getAllChannels().map { entities ->
            entities.map { it.toModel() }
        }
    }

    override fun getPublicChannels(): Flow<List<Channel>> {
        return channelDao.getPublicChannels().map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun getChannelById(channelId: String): Channel? {
        val entity = channelDao.getChannelById(channelId)
        val memberIds = channelDao.getMemberIdsForChannel(channelId)
        return entity?.toModel(memberIds)
    }

    override suspend fun createChannel(name: String, description: String, isPublic: Boolean, creatorId: String): String {
        val channelId = "GROUP_${UUID.randomUUID()}"
        val newChannel = ChannelEntity(id = channelId, name = name, description = description, isPublic = isPublic)
        channelDao.insertChannel(newChannel)
        addMemberToChannel(channelId, creatorId)
        return channelId
    }

    override suspend fun addMemberToChannel(channelId: String, userId: String) {
        val member = ChannelMemberEntity(channelId = channelId, userId = userId)
        channelDao.addMember(member)
    }

    override suspend fun removeMemberFromChannel(channelId: String, userId: String) {
        channelDao.removeMember(channelId, userId)
    }

    override suspend fun saveChannels(channels: List<Channel>) {
        for (channel in channels) {
            channelDao.insertChannel(channel.toEntity())
            for (memberId in channel.members) {
                addMemberToChannel(channel.id, memberId)
            }
        }
    }

    private suspend fun ChannelEntity.toModel(): Channel {
        val memberIds = channelDao.getMemberIdsForChannel(this.id)
        return Channel(
            id = this.id,
            name = this.name,
            description = this.description,
            isPublic = this.isPublic,
            members = memberIds
        )
    }

    private fun Channel.toEntity() = ChannelEntity(
        id = this.id,
        name = this.name,
        description = this.description,
        isPublic = this.isPublic
    )

    private fun ChannelEntity.toModel(memberIds: List<String>): Channel {
        return Channel(
            id = this.id,
            name = this.name,
            description = this.description,
            isPublic = this.isPublic,
            members = memberIds
        )
    }
}