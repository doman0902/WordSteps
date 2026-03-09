package com.example.wordsteps.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wordsteps.data.database.ReconstructionScore
import com.example.wordsteps.data.models.Attempt
import com.example.wordsteps.data.models.PatternMastery
import com.example.wordsteps.data.models.UserStats
import com.example.wordsteps.data.repository.SpellRepository
import com.example.wordsteps.data.database.SpellDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class StatsUiState(
    val stats: UserStats = UserStats(),
    val patterns: List<PatternMastery> = emptyList(),
    val recentMistakes: List<Attempt> = emptyList(),
    val reconstructionTopScores: List<ReconstructionScore> = emptyList(),  // ← NEW
    val isLoading: Boolean = true
)

class StatsViewModel(
    private val repository: SpellRepository,
    private val database: SpellDatabase          // ← NEW
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState
    init { load() }
    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            _uiState.value = StatsUiState(
                stats                   = repository.getUserStats(),
                patterns                = repository.getPatternMasteryStats(),
                recentMistakes          = repository.getRecentAttempts().filter { !it.isCorrect },
                reconstructionTopScores = database.reconstructionScoreDao().getTopScores(),  // ← NEW
                isLoading               = false
            )
        }
    }
}

class StatsViewModelFactory(
    private val repository: SpellRepository,
    private val database: SpellDatabase          // ← NEW
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return StatsViewModel(repository, database) as T
    }
}