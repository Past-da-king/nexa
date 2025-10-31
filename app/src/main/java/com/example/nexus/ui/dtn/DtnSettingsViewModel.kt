package com.example.nexus.ui.dtn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.repositories.DtnSettingsRepository
import com.example.nexus.models.DtnSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DtnSettingsViewModel @Inject constructor(
    private val dtnSettingsRepository: DtnSettingsRepository
) : ViewModel() {

    val dtnSettings: StateFlow<DtnSettings> = dtnSettingsRepository.dtnSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DtnSettings(0, 0, 0)
        )

    fun updateDtnSettings(dtnSettings: DtnSettings) {
        viewModelScope.launch {
            dtnSettingsRepository.updateDtnSettings(dtnSettings)
        }
    }
}
