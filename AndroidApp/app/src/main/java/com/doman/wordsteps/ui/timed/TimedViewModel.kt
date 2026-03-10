package com.doman.wordsteps.ui.timed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.doman.wordsteps.data.database.SpellDatabase
import com.doman.wordsteps.data.database.TimedScore
import com.doman.wordsteps.data.repository.QuizQuestion
import com.doman.wordsteps.data.repository.SpellRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Which option the user tapped and whether it was right
data class AnswerFlash(
    val selectedIndex: Int,
    val correctIndex: Int
)

sealed class TimedUiState {
    object Loading : TimedUiState()

    data class DurationPicker(
        val options: List<Int> = listOf(30, 60, 90)
    ) : TimedUiState()

    data class Playing(
        val question: QuizQuestion,
        val timeRemaining: Int,
        val totalDuration: Int,
        val score: Int,
        val wordsAnswered: Int,
        val flash: AnswerFlash? = null
    ) : TimedUiState()

    data class Finished(
        val score: Int,
        val wordsAnswered: Int,
        val totalDuration: Int,
        val topScores: List<TimedScore>
    ) : TimedUiState()
}

class TimedViewModel(
    private val repository: SpellRepository,
    private val database: SpellDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow<TimedUiState>(TimedUiState.DurationPicker())
    val uiState: StateFlow<TimedUiState> = _uiState

    private var questionPool: List<QuizQuestion> = emptyList()
    private var poolIndex     = 0
    private var score         = 0
    private var wordsAnswered = 0
    private var totalDuration = 60
    private var timeRemaining = 60
    private var timerJob: Job? = null
    private var flashJob: Job? = null

    fun startGame(durationSeconds: Int) {
        totalDuration  = durationSeconds
        timeRemaining  = durationSeconds
        score          = 0
        wordsAnswered  = 0
        poolIndex      = 0
        viewModelScope.launch {
            _uiState.value = TimedUiState.Loading
            questionPool = repository.getPracticeQuestions(200).shuffled()
            if (questionPool.isEmpty()) questionPool = repository.getPracticeQuestions(10)
            showQuestion()
            startTimer()
        }
    }

    fun onAnswerSelected(selectedIndex: Int) {
        val state = _uiState.value as? TimedUiState.Playing ?: return
        if (state.flash != null) return

        val isCorrect = selectedIndex == state.question.correctIndex
        if (isCorrect) score++
        wordsAnswered++

        _uiState.value = state.copy(
            score         = score,
            wordsAnswered = wordsAnswered,
            flash         = AnswerFlash(
                selectedIndex = selectedIndex,
                correctIndex  = state.question.correctIndex
            )
        )

        flashJob?.cancel()
        flashJob = viewModelScope.launch {
            delay(800L)
            if (timeRemaining > 0) showQuestion() else finishGame()
        }
    }

    fun restartGame() {
        timerJob?.cancel()
        flashJob?.cancel()
        _uiState.value = TimedUiState.DurationPicker()
    }

    private fun showQuestion() {
        val question = questionPool[poolIndex % questionPool.size]
        poolIndex++
        _uiState.value = TimedUiState.Playing(
            question      = question,
            timeRemaining = timeRemaining,
            totalDuration = totalDuration,
            score         = score,
            wordsAnswered = wordsAnswered,
            flash         = null
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (timeRemaining > 0) {
                delay(1000L)
                timeRemaining--
                val current = _uiState.value
                if (current is TimedUiState.Playing) {
                    _uiState.value = current.copy(timeRemaining = timeRemaining)
                }
            }
            flashJob?.cancel()
            finishGame()
        }
    }

    private fun finishGame() {
        timerJob?.cancel()
        viewModelScope.launch {
            database.timedScoreDao().insertScore(
                TimedScore(
                    score         = score,
                    wordsAnswered = wordsAnswered,
                    duration      = totalDuration
                )
            )
            val topScores = database.timedScoreDao().getTopScores()
            _uiState.value = TimedUiState.Finished(
                score         = score,
                wordsAnswered = wordsAnswered,
                totalDuration = totalDuration,
                topScores     = topScores
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        flashJob?.cancel()
    }
}

class TimedViewModelFactory(
    private val repository: SpellRepository,
    private val database: SpellDatabase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TimedViewModel(repository, database) as T
}