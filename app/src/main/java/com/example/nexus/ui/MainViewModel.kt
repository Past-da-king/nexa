package com.example.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.NexaService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * A high-level, app-wide ViewModel.
 * Its primary job is to observe global state from the repositories (like who we
 * are currently connected to) and provide it to the MainActivity for high-level
 * navigation and state decisions.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val nexaService: NexaService
) : ViewModel() {

    // Observe the currently connected peer. This will be null if we are disconnected.
    val connectedPeer = nexaService.getConnectedPeer()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}