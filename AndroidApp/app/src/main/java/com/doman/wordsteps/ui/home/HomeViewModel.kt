package com.doman.wordsteps.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.doman.wordsteps.data.models.PatternMastery
import com.doman.wordsteps.data.models.UserStats
import com.doman.wordsteps.data.preferences.UserPreferences
import com.doman.wordsteps.data.repository.SpellRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class HomeUiState(
    val stats: UserStats = UserStats(),
    val weakestPattern: PatternMastery? = null,
    val patternMasteryList: List<PatternMastery> = emptyList(),
    val isLoading: Boolean = true,
    val serverStatus: ServerStatus = ServerStatus.CHECKING
)

class HomeViewModel(private val repository: SpellRepository,private val prefs: UserPreferences) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val stats = repository.getUserStats()
            val patterns = repository.getPatternMasteryStats()
            val weakest = patterns.firstOrNull()

            _uiState.value = HomeUiState(
                stats = stats,
                weakestPattern = weakest,
                patternMasteryList = patterns,
                isLoading = false,
                serverStatus = ServerStatus.CHECKING
            )
            checkServerStatus()

        }
    }

    private fun checkServerStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ip = prefs.serverIpFlow.first()
                val request = Request.Builder()
                    .url("http://$ip/health")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val isOnline = response.isSuccessful

                _uiState.value = _uiState.value.copy(
                    serverStatus = if (isOnline) ServerStatus.ONLINE else ServerStatus.OFFLINE
                )
            } catch (e: Exception) {
                // Timeout vagy nincs szerver → OFFLINE
                _uiState.value = _uiState.value.copy(
                    serverStatus = ServerStatus.OFFLINE
                )
            }
        }
    }
}

class HomeViewModelFactory(
    private val repository: SpellRepository,
    private val prefs: UserPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(repository, prefs) as T
    }
}
