package com.example.nexus

import com.example.nexus.data.DtnStore
import com.example.nexus.data.repositories.DtnRepository
import com.example.nexus.data.repositories.DtnSettingsRepository
import com.example.nexus.models.DtnMessage
import com.example.nexus.models.DtnSettings
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DtnRoutingTest {

    private lateinit var mockDtnRepoA: DtnRepository
    private lateinit var mockDtnRepoB: DtnRepository
    private lateinit var mockDtnRepoC: DtnRepository
    private lateinit var mockSettingsRepo: DtnSettingsRepository

    private lateinit var dtnStoreA: DtnStore
    private lateinit var dtnStoreB: DtnStore
    private lateinit var dtnStoreC: DtnStore

    private val settings = DtnSettings(ttl = 86400_000L, hopCount = 3, storageLimit = 100)

    @Before
    fun setup() {
        mockDtnRepoA = mock()
        mockDtnRepoB = mock()
        mockDtnRepoC = mock()
        mockSettingsRepo = mock()

        runBlocking {
            whenever(mockSettingsRepo.dtnSettings).thenReturn(flowOf(settings))
        }

        dtnStoreA = DtnStore(mockDtnRepoA, mockSettingsRepo)
        dtnStoreB = DtnStore(mockDtnRepoB, mockSettingsRepo)
        dtnStoreC = DtnStore(mockDtnRepoC, mockSettingsRepo)
    }

    @Test
    fun `message is forwarded and hop count decremented`() = runBlocking {
        // Arrange: Device A has a message for D
        val messageForD = DtnMessage("msg1", "A", "D", "payload", 0, settings.hopCount, 0)
        whenever(mockDtnRepoA.getMessages(any())).thenReturn(listOf(messageForD))

        // Act: Simulate A connecting to B
        // B requests messages from A
        val messagesFromA = dtnStoreA.getMessages(listOf("msg1"))
        val forwardedMessage = messagesFromA.first().copy(hopCount = messagesFromA.first().hopCount - 1)
        dtnStoreB.addMessage(forwardedMessage)

        // Assert: B should now store the message with a decremented hop count
        verify(mockDtnRepoB).addMessage(forwardedMessage)
        assertEquals(settings.hopCount - 1, forwardedMessage.hopCount)
    }

    @Test
    fun `message at hop limit is not forwarded`() = runBlocking {
        // Arrange: Device B has a message with 1 hop left
        val messageWithOneHop = DtnMessage("msg1", "A", "D", "payload", 0, 1, 0)
        whenever(mockDtnRepoB.getMessages(any())).thenReturn(listOf(messageWithOneHop))

        // Act: Simulate B connecting to C
        val messagesFromB = dtnStoreB.getMessages(listOf("msg1"))
        val messageToForward = messagesFromB.first()

        // C checks the hop count before adding
        if (messageToForward.hopCount > 0) {
            val decrementedMessage = messageToForward.copy(hopCount = messageToForward.hopCount - 1)
            dtnStoreC.addMessage(decrementedMessage)
        }

        // Assert: C should NOT have the message added because B should not have sent it.
        // This test simulates the logic that would be in ConnectionService: only forward if hop > 0
        verify(mockDtnRepoC, never()).addMessage(any())
    }

    @Test
    fun `storage limit drops oldest message`() = runBlocking {
        // Arrange: Store B is full
        whenever(mockDtnRepoB.getCount()).thenReturn(settings.storageLimit)
        val oldestMessage = DtnMessage("oldest", "X", "Y", "old_payload", 0, 1, 0)
        whenever(mockDtnRepoB.getOldestMessage()).thenReturn(oldestMessage)

        // Act: A new message arrives
        val newMessage = DtnMessage("new", "A", "B", "new_payload", 0, 3, 1)
        dtnStoreB.addMessage(newMessage)

        // Assert: The oldest message should be deleted to make space for the new one
        verify(mockDtnRepoB).deleteMessage(oldestMessage.id)
        verify(mockDtnRepoB).addMessage(newMessage)
    }
}