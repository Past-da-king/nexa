package com.example.nexus.data.repositories

import android.util.Log
import com.example.nexus.data.database.ContactDao
import com.example.nexus.data.database.ContactEntity
import com.example.nexus.models.Contact
import com.example.nexus.models.ContactStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContactRepositoryImpl(
    private val contactDao: ContactDao
) : ContactRepository {

    override suspend fun storeRequest(contact: Contact) {
        Log.d("FriendRequestFlow", "[RECEIVER] Step 5: Saving request to database for stable ID: ${contact.stableId}")
        contactDao.insertContact(contact.toEntity())
    }

    override suspend fun getContactById(stableId: String): Contact? {
        return contactDao.getContactById(stableId)?.toModel()
    }

    override suspend fun updateContactStatusToFriend(stableId: String) {
        contactDao.updateContactStatus(stableId, ContactStatus.FRIEND.name)
    }

    override suspend fun deleteContact(stableId: String) {
        contactDao.deleteContact(stableId)
    }

    override fun getAllFriends(): Flow<List<Contact>> {
        return contactDao.getAllFriends().map { entities -> entities.map { it.toModel() } }
    }

    override fun getFriendRequests(): Flow<List<Contact>> {
        return contactDao.getFriendRequests().map { entities -> entities.map { it.toModel() } }
    }

    private fun ContactEntity.toModel() = Contact(stableId, name, publicKeyString, ContactStatus.valueOf(status))
    private fun Contact.toEntity() = ContactEntity(stableId, name, publicKeyString, status.name)
}