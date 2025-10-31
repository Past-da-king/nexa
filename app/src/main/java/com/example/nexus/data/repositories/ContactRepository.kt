package com.example.nexus.data.repositories

import com.example.nexus.models.Contact
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    suspend fun storeRequest(contact: Contact)
    suspend fun getContactById(stableId: String): Contact?
    suspend fun updateContactStatusToFriend(stableId: String)
    suspend fun deleteContact(stableId: String)
    fun getAllFriends(): Flow<List<Contact>>
    fun getFriendRequests(): Flow<List<Contact>>
}