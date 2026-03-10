package com.doman.wordsteps.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.doman.wordsteps.data.database.SpellDatabase
import com.doman.wordsteps.data.preferences.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverIp: String = "",
    val isSaved: Boolean = false,       // brief confirmation after saving IP
    val resetDone: Boolean = false      // brief confirmation after reset
)

class SettingsViewModel(
    private val prefs: UserPreferences,
    private val database: SpellDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            val saved = prefs.serverIpFlow.first()
            _uiState.value = _uiState.value.copy(serverIp = saved)
        }
    }

    fun onIpChanged(value: String) {
        _uiState.value = _uiState.value.copy(serverIp = value, isSaved = false)
    }

    fun saveIp() {
        viewModelScope.launch {
            prefs.saveServerIp(_uiState.value.serverIp.trim())
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            database.attemptDao().deleteAll()
            database.statsDao().deleteAll()
            database.patternMasteryDao().deleteAll()
            _uiState.value = _uiState.value.copy(resetDone = true)
        }
    }

    fun clearConfirmations() {
        _uiState.value = _uiState.value.copy(isSaved = false, resetDone = false)
    }
}

class SettingsViewModelFactory(
    private val prefs: UserPreferences,
    private val database: SpellDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(prefs, database) as T
    }
}