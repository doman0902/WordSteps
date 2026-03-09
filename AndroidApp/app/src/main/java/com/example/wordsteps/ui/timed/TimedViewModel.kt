package com.example.wordsteps.ui.timed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wordsteps.data.repository.SpellRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TimedLetterTile(
    val id: Int,
    val char: Char,
    val isLocked: Boolean = false
)

enum class WordResult { CORRECT, WRONG }

sealed class TimedUiState {
    object Loading : TimedUiState()
    data class DurationPicker(val options: List<Int> = listOf(30, 60, 90)) : TimedUiState()
    data class Playing(
        val word: String,
        val slots: Map<Int, TimedLetterTile>,
        val bankTiles: List<TimedLetterTile>,
        val timeRemaining: Int,
        val totalDuration: Int,
        val score: Int,
        val wordsAttempted: Int,
        val flashResult: WordResult? = null
    ) : TimedUiState()
    data class Finished(
        val score: Int,
        val wordsAttempted: Int,
        val totalDuration: Int
    ) : TimedUiState()
}

class TimedViewModel(
    private val repository: SpellRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TimedUiState>(TimedUiState.DurationPicker())
    val uiState: StateFlow<TimedUiState> = _uiState

    private var allWords       = emptyList<String>()
    private var wordIndex      = 0
    private var score          = 0
    private var wordsAttempted = 0
    private var totalDuration  = 60
    private var timeRemaining  = 60
    private var timerJob: Job? = null
    private var flashJob: Job? = null

    fun startGame(durationSeconds: Int) {
        totalDuration  = durationSeconds
        timeRemaining  = durationSeconds
        score          = 0
        wordsAttempted = 0
        wordIndex      = 0
        viewModelScope.launch {
            _uiState.value = TimedUiState.Loading
            val pool = repository.getPracticeQuestions(50).map { it.correctWord }.shuffled()
            allWords = if (pool.isEmpty()) listOf("example", "spelling", "practice") else pool
            showQuestion()
            startTimer()
        }
    }

    fun tapTile(tile: TimedLetterTile) {
        val state = _uiState.value as? TimedUiState.Playing ?: return
        if (state.flashResult != null) return

        val nextSlot = (0 until state.word.length).firstOrNull { state.slots[it] == null } ?: return
        val newSlots = state.slots + (nextSlot to tile)
        val newBank  = state.bankTiles - tile

        if (newSlots.size == state.word.length) {
            val built = (0 until state.word.length).joinToString("") { newSlots[it]!!.char.toString() }
            wordsAttempted++
            if (built.equals(state.word, ignoreCase = true)) {
                score++
                showFlash(state.copy(slots = newSlots, bankTiles = newBank), WordResult.CORRECT)
            } else {
                showFlash(state.copy(slots = newSlots, bankTiles = newBank), WordResult.WRONG)
            }
        } else {
            _uiState.value = state.copy(slots = newSlots, bankTiles = newBank)
        }
    }

    fun removePlacedTile(tile: TimedLetterTile) {
        val state = _uiState.value as? TimedUiState.Playing ?: return
        if (state.flashResult != null || tile.isLocked) return
        val slotIndex = (0 until state.word.length).reversed()
            .firstOrNull { state.slots[it]?.id == tile.id && state.slots[it]?.isLocked == false } ?: return
        _uiState.value = state.copy(
            slots     = state.slots - slotIndex,
            bankTiles = state.bankTiles + tile
        )
    }

    fun restartGame() {
        timerJob?.cancel()
        flashJob?.cancel()
        _uiState.value = TimedUiState.DurationPicker()
    }

    private fun showQuestion() {
        val word = allWords[wordIndex % allWords.size]
        wordIndex++
        val len = word.length
        val hintCount = when {
            len <= 5  -> 1
            len == 6  -> 2
            len == 7  -> 2
            len == 8  -> 3
            len <= 10 -> 3
            len <= 12 -> 4
            else      -> 5
        }.coerceIn(1, len - 1)

        val lockedIndices = setOf(0) + (1 until len).shuffled().take(hintCount - 1)
        val allTiles = word.lowercase().mapIndexed { i, c ->
            TimedLetterTile(id = i, char = c, isLocked = i in lockedIndices)
        }

        _uiState.value = TimedUiState.Playing(
            word           = word,
            slots          = allTiles.filter { it.isLocked }.associateBy { it.id },
            bankTiles      = allTiles.filter { !it.isLocked }.shuffled(),
            timeRemaining  = timeRemaining,
            totalDuration  = totalDuration,
            score          = score,
            wordsAttempted = wordsAttempted,
            flashResult    = null
        )
    }

    private fun showFlash(state: TimedUiState.Playing, result: WordResult) {
        _uiState.value = state.copy(score = score, wordsAttempted = wordsAttempted, flashResult = result)
        flashJob?.cancel()
        flashJob = viewModelScope.launch {
            delay(1000L)
            if (timeRemaining > 0) showQuestion()
        }
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
            _uiState.value = TimedUiState.Finished(
                score          = score,
                wordsAttempted = wordsAttempted,
                totalDuration  = totalDuration
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        flashJob?.cancel()
    }

    class Factory(private val repository: SpellRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TimedViewModel(repository) as T
    }
}