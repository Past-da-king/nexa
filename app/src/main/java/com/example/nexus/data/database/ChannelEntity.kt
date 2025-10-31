package com.example.nexus.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val isPublic: Boolean
)