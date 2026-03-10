package com.doman.wordsteps.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.doman.wordsteps.data.database.DailyStats
import com.doman.wordsteps.data.database.ReconstructionScore
import com.doman.wordsteps.data.database.SpellDatabase
import com.doman.wordsteps.data.database.TimedScore
import com.doman.wordsteps.data.models.Attempt
import com.doman.wordsteps.data.models.PatternMastery
import com.doman.wordsteps.data.models.UserStats
import com.doman.wordsteps.data.repository.SpellRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class StatsUiState(
    val stats: UserStats = UserStats(),
    val patterns: List<PatternMastery> = emptyList(),
    val recentMistakes: List<Attempt> = emptyList(),
    val dailyStats: List<DailyStats> = emptyList(),
    val reconstructionTopScores: List<ReconstructionScore> = emptyList(),
    val timedScores30: List<TimedScore> = emptyList(),
    val timedScores60: List<TimedScore> = emptyList(),
    val timedScores90: List<TimedScore> = emptyList(),
    val isLoading: Boolean = true
)

class StatsViewModel(
    private val repository: SpellRepository,
    private val database: SpellDatabase
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
                dailyStats              = repository.getDailyStats(),
                reconstructionTopScores = database.reconstructionScoreDao().getTopScores(),
                timedScores30           = database.timedScoreDao().getTopScoresForDuration(30),
                timedScores60           = database.timedScoreDao().getTopScoresForDuration(60),
                timedScores90           = database.timedScoreDao().getTopScoresForDuration(90),
                isLoading               = false
            )
        }
    }
}

class StatsViewModelFactory(
    private val repository: SpellRepository,
    private val database: SpellDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return StatsViewModel(repository, database) as T
    }
}