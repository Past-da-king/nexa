package com.example.nexus.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY messageId ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    /**
     * Gets a list of the most recent message from each conversation.
     * This is the data source for the Home Screen.
     */    @Query("""
        SELECT * FROM messages 
        WHERE messageId IN (
            SELECT MAX(messageId) FROM messages GROUP BY conversationId
        )
        ORDER BY messageId DESC
    """)
    fun getConversations(): Flow<List<MessageEntity>>
}