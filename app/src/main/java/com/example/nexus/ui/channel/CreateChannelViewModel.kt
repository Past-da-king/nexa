package com.example.nexus.ui.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.NexaService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateChannelViewModel @Inject constructor(
    private val nexaService: NexaService
) : ViewModel() {

    private val _channelName = MutableStateFlow("")
    val channelName = _channelName.asStateFlow()

    private val _channelDescription = MutableStateFlow("")
    val channelDescription = _channelDescription.asStateFlow()

    private val _isPublic = MutableStateFlow(true)
    val isPublic = _isPublic.asStateFlow()

    fun onChannelNameChange(newName: String) {
        _channelName.value = newName
    }

    fun onChannelDescriptionChange(newDescription: String) {
        _channelDescription.value = newDescription
    }

    fun onIsPublicChange(public: Boolean) {
        _isPublic.value = public
    }

    fun createChannel(onSuccess: (String) -> Unit) {
        if (_channelName.value.isNotBlank()) {
            viewModelScope.launch {
                val newChannelId = nexaService.createChannel(
                    _channelName.value,
                    _channelDescription.value,
                    _isPublic.value
                )
                onSuccess(newChannelId)
            }
        }
    }
}