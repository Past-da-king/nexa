package com.example.nexus.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dtn_messages")
data class DtnMessageEntity(
    @PrimaryKey
    val id: String,
    val source: String,
    val destination: String,
    val payload: String,
    val ttl: Long,
    val hopCount: Int,
    val timestamp: Long,
    val messageType: String
)
