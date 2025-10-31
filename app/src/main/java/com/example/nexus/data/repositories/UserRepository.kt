package com.example.nexus.data.repositories

import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun isUserOnboarded(): Boolean
    fun getUsername(): String
    fun createUser(name: String)
    fun getUsernameFlow(): Flow<String>
    fun getUserId(): String?

}