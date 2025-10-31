package com.example.nexus.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MessageEntity::class, ContactEntity::class, DtnMessageEntity::class, ChannelEntity::class, ChannelMemberEntity::class], version = 6, exportSchema = false)
abstract class NexaDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao
    abstract fun dtnMessageDao(): DtnMessageDao
    abstract fun channelDao(): ChannelDao
}
