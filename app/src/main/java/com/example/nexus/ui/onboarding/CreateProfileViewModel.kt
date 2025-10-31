package com.example.nexus.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.NexaService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateProfileViewModel @Inject constructor(
    private val nexaService: NexaService
) : ViewModel() {

    // This holds the state of the text field. The UI will observe this.
    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    fun onUsernameChange(newName: String) {
        _username.value = newName
    }

    fun onCreateProfileClicked() {
        if (_username.value.isNotBlank()) {
            viewModelScope.launch {
                nexaService.createUserProfile(_username.value)
            }
        }
    }
}