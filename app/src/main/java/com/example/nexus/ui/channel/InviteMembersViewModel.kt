package com.example.nexus.ui.channel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.NexaService
import com.example.nexus.models.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InviteMembersState(
    val friends: List<Contact> = emptyList(),
    val selectedFriends: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class InviteMembersViewModel @Inject constructor(
    private val nexaService: NexaService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val channelId: String = savedStateHandle.get<String>("channelId") ?: ""

    private val _channel = MutableStateFlow<com.example.nexus.models.Channel?>(null)
    private val _friends = MutableStateFlow<List<Contact>>(emptyList())
    private val _selectedFriends = MutableStateFlow<Set<String>>(emptySet())
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val state: StateFlow<InviteMembersState> = combine(
        _friends,
        _selectedFriends,
        _isLoading,
        _error
    ) { friends, selectedFriends, isLoading, error ->
        InviteMembersState(friends, selectedFriends, isLoading, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InviteMembersState())

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch both channel and friends
                _channel.value = nexaService.getChannelById(channelId)
                val allFriends = nexaService.getAllFriends().first()
                _friends.value = allFriends
            } catch (e: Exception) {
                _error.value = "Failed to load data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFriendSelection(friendId: String) {
        _selectedFriends.update {
            if (it.contains(friendId)) it - friendId else it + friendId
        }
    }

    fun sendInvites(onInvitesSent: () -> Unit) {
        viewModelScope.launch {
            val channel = _channel.value ?: run {
                _error.value = "Channel not found!"
                return@launch
            }
            _isLoading.value = true
            try {
                _selectedFriends.value.forEach {
                    nexaService.sendChannelInvite(it, channel)
                }
                onInvitesSent()
            } catch (e: Exception) {
                _error.value = "Failed to send invites: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
