package com.example.nexus.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.NexaService
import com.example.nexus.models.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val nexaService: NexaService
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> = combine(
        nexaService.getAllFriends(),
        nexaService.getAllChannels()
    ) { friends, channels ->
        val friendConversations = friends.map {
            Conversation(
                conversationId = it.stableId,
                name = it.name,
                lastMessage = "Tap to chat...", // Placeholder
                timestamp = 0L, // Placeholder
                isGroup = false
            )
        }

        val channelConversations = channels.map {
            Conversation(
                conversationId = it.id,
                name = it.name,
                lastMessage = "Channel", // Placeholder
                timestamp = 0L, // Placeholder
                isGroup = true
            )
        }

        (friendConversations + channelConversations).sortedByDescending { it.timestamp }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}