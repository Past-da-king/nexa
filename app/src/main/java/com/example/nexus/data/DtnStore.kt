package com.example.nexus.data

import android.util.Log
import com.example.nexus.data.repositories.DtnRepository
import com.example.nexus.data.repositories.DtnSettingsRepository
import com.example.nexus.models.DtnMessage
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DtnStore @Inject constructor(
    private val dtnRepository: DtnRepository,
    private val dtnSettingsRepository: DtnSettingsRepository
) {
    private val TAG = "DtnStore"

    suspend fun addMessage(message: DtnMessage) {
        val settings = dtnSettingsRepository.dtnSettings.first()
        val currentCount = dtnRepository.getCount()
        Log.d(
            TAG,
            "Attempting to add message ID=${message.id}. Current count: $currentCount, Storage limit: ${settings.storageLimit}."
        )

        if (currentCount >= settings.storageLimit) {
            Log.w(
                TAG,
                "Storage limit reached ($currentCount/${settings.storageLimit}). Dropping oldest message to make space."
            )
            val oldestMessage = dtnRepository.getOldestMessage()
            if (oldestMessage != null) {
                dtnRepository.deleteMessage(oldestMessage.id)
                Log.d(TAG, "Dropped oldest message ID=${oldestMessage.id}.")
            } else {
                Log.w(TAG, "Storage limit reached but no oldest message found to drop.")
            }
        }
        dtnRepository.addMessage(message)
        Log.d(TAG, "Successfully added message ID=${message.id}.")
    }

    suspend fun getMessage(id: String): DtnMessage? {
        Log.d(TAG, "Retrieving message ID=$id.")
        return dtnRepository.getMessage(id)
    }

    suspend fun getAllMessageIds(): List<String> {
        Log.d(TAG, "Retrieving all message IDs.")
        return dtnRepository.getAllMessageIds()
    }

    suspend fun deleteMessage(id: String) {
        Log.d(TAG, "Deleting message ID=$id.")
        dtnRepository.deleteMessage(id)
    }

    suspend fun getMessages(ids: List<String>): List<DtnMessage> {
        Log.d(TAG, "Retrieving messages for IDs: $ids.")
        return dtnRepository.getMessages(ids)
    }

    suspend fun pruneExpiredMessages() {
        Log.d(TAG, "Pruning expired messages.")
        dtnRepository.pruneExpiredMessages()
    }
}