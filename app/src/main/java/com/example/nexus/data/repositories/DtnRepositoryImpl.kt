package com.example.nexus.data.repositories

import com.example.nexus.data.database.DtnMessageDao
import com.example.nexus.data.database.DtnMessageEntity
import com.example.nexus.models.DtnMessage
import com.example.nexus.models.DtnMessageType
import javax.inject.Inject

class DtnRepositoryImpl @Inject constructor(
    private val dtnMessageDao: DtnMessageDao
) : DtnRepository {
    override suspend fun addMessage(message: DtnMessage) {
        dtnMessageDao.insert(message.toEntity())
    }

    override suspend fun getMessage(id: String): DtnMessage? {
        return dtnMessageDao.getById(id)?.toModel()
    }

    override suspend fun getAllMessageIds(): List<String> {
        return dtnMessageDao.getAllIds()
    }

    override suspend fun deleteMessage(id: String) {
        dtnMessageDao.deleteById(id)
    }

    override suspend fun getMessages(ids: List<String>): List<DtnMessage> {
        return dtnMessageDao.getByIds(ids).map { it.toModel() }
    }

    override suspend fun pruneExpiredMessages() {
        dtnMessageDao.pruneExpired(System.currentTimeMillis())
    }

    override suspend fun getCount(): Int {
        return dtnMessageDao.getCount()
    }

    override suspend fun getOldestMessage(): DtnMessage? {
        return dtnMessageDao.getOldest()?.toModel()
    }
}

private fun DtnMessage.toEntity() = DtnMessageEntity(
    id = id,
    source = source,
    destination = destination,
    payload = payload,
    ttl = ttl,
    hopCount = hopCount,
    timestamp = timestamp,
    messageType = messageType.name
)

private fun DtnMessageEntity.toModel() = DtnMessage(
    id = id,
    source = source,
    destination = destination,
    payload = payload,
    ttl = ttl,
    hopCount = hopCount,
    timestamp = timestamp,
    messageType = DtnMessageType.valueOf(messageType)
)
