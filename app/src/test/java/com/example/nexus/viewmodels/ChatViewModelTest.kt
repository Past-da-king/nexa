package com.example.nexus.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.example.nexus.data.NexaService
import com.example.nexus.models.Message
import com.example.nexus.models.MessageType
import com.example.nexus.models.RecipientType
import com.example.nexus.ui.chat.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockNexaService: NexaService
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockNexaService = mock()
        val savedStateHandle = SavedStateHandle(mapOf("conversationId" to "test_id"))
        viewModel = ChatViewModel(mockNexaService, savedStateHandle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sendMessage constructs correct text message and calls service`() = runTest {
        // Arrange
        val text = "Hello, World!"

        // Act
        viewModel.sendMessage(text, null, MessageType.TEXT)

        // Assert
        verify(mockNexaService).sendMessage(
            any<Message>(),
            eq("test_id"),
            eq(RecipientType.USER)
        )
    }

    @Test
    fun `sendMessage constructs correct image message and calls service`() = runTest {
        // Arrange
        val imageData = "base64_image_string"

        // Act
        viewModel.sendMessage("", imageData, MessageType.IMAGE)

        // Assert
        verify(mockNexaService).sendMessage(
            any<Message>(),
            eq("test_id"),
            eq(RecipientType.USER)
        )
    }
}