package com.example.nexus.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DtnMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: DtnMessageEntity)

    @Query("SELECT * FROM dtn_messages WHERE id = :id")
    suspend fun getById(id: String): DtnMessageEntity?

    @Query("SELECT id FROM dtn_messages")
    suspend fun getAllIds(): List<String>

    @Query("DELETE FROM dtn_messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM dtn_messages WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<DtnMessageEntity>

    @Query("DELETE FROM dtn_messages WHERE ttl < :currentTime")
    suspend fun pruneExpired(currentTime: Long)

    @Query("SELECT * FROM dtn_messages ORDER BY timestamp ASC LIMIT 1")
    suspend fun getOldest(): DtnMessageEntity?

    @Query("SELECT COUNT(*) FROM dtn_messages")
    suspend fun getCount(): Int
}
