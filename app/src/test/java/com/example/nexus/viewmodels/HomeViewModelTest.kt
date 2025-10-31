package com.example.nexus.viewmodels

import com.example.nexus.data.NexaService
import com.example.nexus.models.Channel
import com.example.nexus.models.Contact
import com.example.nexus.models.ContactStatus
import com.example.nexus.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockNexaService: NexaService
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockNexaService = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `conversations flow combines friends and channels`() = runTest {
        // Arrange
        val friends = listOf(Contact("friend1", "Friend 1", "pk1", ContactStatus.FRIEND))
        val channels = listOf(Channel("channel1", "Channel 1", emptyList()))
        whenever(mockNexaService.getAllFriends()).thenReturn(flowOf(friends))
        whenever(mockNexaService.getAllChannels()).thenReturn(flowOf(channels))

        // Act
        viewModel = HomeViewModel(mockNexaService)
        val conversations = viewModel.conversations.first()

        // Assert
        assertEquals(2, conversations.size)
        assertEquals(true, conversations.any { it.isGroup && it.name == "Channel 1" })
        assertEquals(true, conversations.any { !it.isGroup && it.name == "Friend 1" })
    }
}