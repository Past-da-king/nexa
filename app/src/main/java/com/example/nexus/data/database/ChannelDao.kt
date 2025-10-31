package com.example.nexus.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Query("SELECT * FROM channels")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isPublic = 1")
    fun getPublicChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :channelId")
    suspend fun getChannelById(channelId: String): ChannelEntity?

    @Query("DELETE FROM channels WHERE id = :channelId")
    suspend fun deleteChannel(channelId: String)

    // Member management
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMember(member: ChannelMemberEntity)

    @Query("DELETE FROM channel_members WHERE channelId = :channelId AND userId = :userId")
    suspend fun removeMember(channelId: String, userId: String)

    @Query("SELECT userId FROM channel_members WHERE channelId = :channelId")
    suspend fun getMemberIdsForChannel(channelId: String): List<String>
}