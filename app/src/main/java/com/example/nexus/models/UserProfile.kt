package com.example.nexus.models

/**
 * Represents the current user's own profile.
 *
 * @param userId A unique identifier for the current user.
 * @param userName The user's chosen display name.
 */
data class UserProfile(
    val userId: String,
    val userName: String
)