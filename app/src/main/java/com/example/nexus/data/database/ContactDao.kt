package com.example.nexus.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Query("SELECT * FROM contacts WHERE stableId = :stableId")
    suspend fun getContactById(stableId: String): ContactEntity?

    @Query("UPDATE contacts SET status = :newStatus WHERE stableId = :stableId")
    suspend fun updateContactStatus(stableId: String, newStatus: String)

    @Query("DELETE FROM contacts WHERE stableId = :stableId")
    suspend fun deleteContact(stableId: String)

    @Query("SELECT * FROM contacts WHERE status = 'FRIEND'")
    fun getAllFriends(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE status = 'REQUEST_RECEIVED'")
    fun getFriendRequests(): Flow<List<ContactEntity>>
}