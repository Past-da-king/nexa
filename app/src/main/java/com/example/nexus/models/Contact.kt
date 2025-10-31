package com.example.nexus.models

// Enum to represent the state of a contact relationship
enum class ContactStatus {
    FRIEND, // An accepted, bidirectional contact
    REQUEST_SENT, // We sent a request, pending their approval
    REQUEST_RECEIVED, // We received a request, pending our approval
    KNOWN // A peer whose public key is known, but no friend request has been exchanged
}

data class Contact(
    val stableId: String,
    val name: String,
    val publicKeyString: String,
    val status: ContactStatus
)