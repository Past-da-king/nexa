package com.example.nexus.data.repositories

import com.example.nexus.models.DtnMessage

interface DtnRepository {
    suspend fun addMessage(message: DtnMessage)
    suspend fun getMessage(id: String): DtnMessage?
    suspend fun getAllMessageIds(): List<String>
    suspend fun deleteMessage(id: String)
    suspend fun getMessages(ids: List<String>): List<DtnMessage>
    suspend fun pruneExpiredMessages()
    suspend fun getCount(): Int
    suspend fun getOldestMessage(): DtnMessage?
}
