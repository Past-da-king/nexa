package com.example.nexus.ui.channel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.NexaService
import com.example.nexus.models.Channel
import com.example.nexus.models.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChannelDetailsState(
    val channel: Channel? = null,
    val members: List<Contact> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ChannelDetailsViewModel @Inject constructor(
    private val nexaService: NexaService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val channelId: String = savedStateHandle.get<String>("channelId") ?: ""

    private val _channel = MutableStateFlow<Channel?>(null)
    private val _members = MutableStateFlow<List<Contact>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val state: StateFlow<ChannelDetailsState> = combine(
        _channel,
        _members,
        _isLoading,
        _error
    ) { channel, members, isLoading, error ->
        ChannelDetailsState(channel, members, isLoading, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChannelDetailsState())

    init {
        loadChannelDetails()
    }

    private fun loadChannelDetails() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val channel = nexaService.getAllChannels().first().find { it.id == channelId }
                _channel.value = channel

                if (channel != null) {
                    val allFriends = nexaService.getAllFriends().first()
                    val channelMembers = allFriends.filter { friend ->
                        channel.members.contains(friend.stableId)
                    }
                    _members.value = channelMembers
                }
            } catch (e: Exception) {
                _error.value = "Failed to load channel details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendChannelInvite(friendId: String) {
        viewModelScope.launch {
            val channel = state.value.channel ?: return@launch
            try {
                nexaService.sendChannelInvite(friendId, channel)
                // Optionally, update UI to show invite sent
            } catch (e: Exception) {
                _error.value = "Failed to send invite: ${e.message}"
            }
        }
    }
}