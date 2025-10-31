package com.example.nexus.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a "Contact" or a "Peer" that we have a persistent relationship with.
 * This is our app's "address book" stored in the database.
 */
@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val stableId: String, // The user's permanent, unique ID.
    val name: String,
    val publicKeyString: String,
    val status: String // Will hold the value from the ContactStatus enum (e.g., "FRIEND")
)