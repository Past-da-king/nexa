package com.example.nexus.ui.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.NexaService
import com.example.nexus.models.Channel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PublicChannelsState(
    val publicChannels: List<Channel> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PublicChannelsViewModel @Inject constructor(
    private val nexaService: NexaService
) : ViewModel() {

    private val _publicChannels = MutableStateFlow<List<Channel>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val state: StateFlow<PublicChannelsState> = combine(
        _publicChannels,
        _isLoading,
        _error
    ) { channels, isLoading, error ->
        PublicChannelsState(channels, isLoading, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PublicChannelsState())

    init {
        loadPublicChannels()
    }

    fun loadPublicChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                nexaService.getPublicChannels().collect { channels ->
                    _publicChannels.value = channels
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Failed to load public channels: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun joinChannel(channelId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                nexaService.joinChannel(channelId)
                onSuccess()
            } catch (e: Exception) {
                _error.value = "Failed to join channel: ${e.message}"
            }
        }
    }
}