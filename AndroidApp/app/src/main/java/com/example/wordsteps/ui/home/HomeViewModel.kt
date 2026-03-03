package com.example.wordsteps.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wordsteps.data.models.PatternMastery
import com.example.wordsteps.data.models.UserStats
import com.example.wordsteps.data.repository.SpellRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val stats: UserStats = UserStats(),
    val weakestPattern: PatternMastery? = null,
    val patternMasteryList: List<PatternMastery> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel(private val repository: SpellRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val stats          = repository.getUserStats()
            val patterns       = repository.getPatternMasteryStats()
            val weakest        = patterns.firstOrNull()

            _uiState.value = HomeUiState(
                stats              = stats,
                weakestPattern     = weakest,
                patternMasteryList = patterns,
                isLoading          = false
            )
        }
    }
}

class HomeViewModelFactory(
    private val repository: SpellRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(repository) as T
    }
}