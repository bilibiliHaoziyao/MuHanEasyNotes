package com.muhan.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.muhan.notes.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val uiScale: StateFlow<Float> = repository.uiScale.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 1f
    )

    val fontScale: StateFlow<Float> = repository.fontScale.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 1f
    )

    val autoSave: StateFlow<Boolean> = repository.autoSave.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    fun setUiScale(value: Float) {
        viewModelScope.launch { repository.setUiScale(value.coerceIn(MIN_SCALE, MAX_SCALE)) }
    }

    fun setFontScale(value: Float) {
        viewModelScope.launch { repository.setFontScale(value.coerceIn(MIN_SCALE, MAX_SCALE)) }
    }

    fun setAutoSave(value: Boolean) {
        viewModelScope.launch { repository.setAutoSave(value) }
    }

    companion object {
        const val MIN_SCALE = 0.8f
        const val MAX_SCALE = 1.5f

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                SettingsViewModel(SettingsRepository(app))
            }
        }
    }
}
