package com.example.nexus.data.repositories

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class UserRepositoryImpl(private val context: Context) : UserRepository {

    private val sharedPrefs = context.getSharedPreferences("NexaPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USERNAME = "USERNAME"
        private const val KEY_USER_ID = "USER_ID" // 2. Add a new constant for the key
    }

    private val _username = MutableStateFlow(getUsername())

    override fun getUsernameFlow(): Flow<String> = _username.asStateFlow()

    override fun isUserOnboarded(): Boolean {
        val username = sharedPrefs.getString("USERNAME", null)
        return !username.isNullOrBlank()
    }

    override fun getUsername(): String {
        return sharedPrefs.getString("USERNAME", "Guest") ?: "Guest"
    }

    override fun createUser(name: String) {
        if (name.isNotBlank()) {
            with(sharedPrefs.edit()) {
                // --- 3. GENERATE AND SAVE THE PERMANENT ID ---
                val newUserId = UUID.randomUUID().toString()
                putString(KEY_USERNAME, name)
                putString(KEY_USER_ID, newUserId)
                apply()
            }
            _username.value = name
        }
    }

    override fun getUserId(): String? {
        return sharedPrefs.getString(KEY_USER_ID, null)
    }
}