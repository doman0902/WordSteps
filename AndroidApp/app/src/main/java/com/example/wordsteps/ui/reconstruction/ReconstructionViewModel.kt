package com.example.wordsteps.ui.reconstruction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wordsteps.data.database.ReconstructionScore
import com.example.wordsteps.data.database.SpellDatabase
import com.example.wordsteps.data.models.PatternResult
import com.example.wordsteps.data.models.SessionSummary
import com.example.wordsteps.data.models.WrongWord
import com.example.wordsteps.data.repository.SpellRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class LetterTile(
    val id: Int,              // = position in word, always unique
    val char: Char,
    val isFlashingWrong: Boolean = false,
    val isLocked: Boolean = false
)

object Scoring {
    const val BASE_PTS_PER_LETTER = 10
    const val COMBO_STEP          = 0.1
    const val COMBO_START         = 1.0
    const val COMBO_MAX           = 5.0
    const val PERFECT_WORD_BONUS  = 50
}

sealed class ReconstructionUiState {
    object Loading : ReconstructionUiState()
    data class Question(
        val wordToReconstruct: String,
        // slots: word-position index -> tile (locked or player-placed). absent = empty.
        val slots: Map<Int, LetterTile>,
        val scrambledTiles: List<LetterTile>,
        val combo: Double,
        val totalPoints: Int,
        val pointsJustEarned: Int?,
        val questionNumber: Int,
        val totalQuestions: Int,
        val wordHadMistake: Boolean,
        val scoredTileIds: Set<Int>
    ) : ReconstructionUiState()
    data class WordFailed(
        val correctWord: String,
        val questionNumber: Int,
        val totalQuestions: Int,
        val totalPoints: Int,
        val combo: Double
    ) : ReconstructionUiState()
    data class Feedback(
        val correctWord: String,
        val pointsEarned: Int,
        val gotPerfectBonus: Boolean,
        val combo: Double,
        val totalPoints: Int,
        val questionNumber: Int,
        val totalQuestions: Int
    ) : ReconstructionUiState()
    data class Finished(
        val summary: SessionSummary,
        val totalPoints: Int,
        val bestCombo: Double,
        val topScores: List<ReconstructionScore>
    ) : ReconstructionUiState()
}

class ReconstructionViewModel(
    private val repository: SpellRepository,
    private val database: SpellDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReconstructionUiState>(ReconstructionUiState.Loading)
    val uiState: StateFlow<ReconstructionUiState> = _uiState

    private var words            = emptyList<String>()
    private var currentIndex     = 0
    private var totalPoints      = 0
    private var combo            = Scoring.COMBO_START
    private var bestCombo        = Scoring.COMBO_START
    private var wordHadMistake   = false
    private var correctWordCount = 0
    private val correctWords     = mutableListOf<String>()
    private val wrongWords       = mutableListOf<WrongWord>()
    private val patternMap       = mutableMapOf<String, PatternResult>()

    init { loadWords() }

    fun loadWords() {
        viewModelScope.launch {
            _uiState.value = ReconstructionUiState.Loading
            val recentMistakes = repository.getRecentAttempts()
                .filter { !it.isCorrect }.map { it.correctWord }.distinct()
            val target = 10
            val randomWords = if (recentMistakes.size < target)
                repository.getPracticeQuestions(target - recentMistakes.size)
                    .map { it.correctWord }.filter { it !in recentMistakes }
            else emptyList()
            words = (recentMistakes + randomWords).take(target).let {
                if (it.isEmpty()) repository.getPracticeQuestions(target).map { q -> q.correctWord } else it
            }
            resetSession()
            showQuestion()
        }
    }

    private fun resetSession() {
        currentIndex = 0; totalPoints = 0; combo = Scoring.COMBO_START
        bestCombo = Scoring.COMBO_START; wordHadMistake = false; correctWordCount = 0
        correctWords.clear(); wrongWords.clear(); patternMap.clear()
    }

    private fun showQuestion() {
        val word = words.getOrNull(currentIndex) ?: run { finishSession(); return }
        wordHadMistake = false
        val len = word.length

        // How many positions to pre-fill (always includes index 0)
        val hintCount = when {
            len <= 5  -> 1
            len == 6  -> 2
            len == 7  -> 3
            len == 8  -> 3
            len <= 10 -> 4
            len <= 12 -> 5
            else      -> 6
        }.coerceAtMost(len)

        // Position 0 always locked; rest chosen randomly
        val lockedIndices: Set<Int> = setOf(0) +
                (1 until len).shuffled().take(hintCount - 1).toSet()

        val allTiles = word.lowercase().mapIndexed { i, c ->
            LetterTile(id = i, char = c, isLocked = i in lockedIndices)
        }

        // Slots pre-filled with locked tiles
        val initialSlots: Map<Int, LetterTile> = allTiles
            .filter { it.isLocked }
            .associateBy { it.id }

        // Bank: only unlocked tiles, shuffled
        val bankTiles = allTiles.filter { !it.isLocked }.shuffled()

        _uiState.value = ReconstructionUiState.Question(
            wordToReconstruct = word,
            slots             = initialSlots,
            scrambledTiles    = bankTiles,
            combo             = combo,
            totalPoints       = totalPoints,
            pointsJustEarned  = null,
            questionNumber    = currentIndex + 1,
            totalQuestions    = words.size,
            wordHadMistake    = false,
            scoredTileIds     = lockedIndices   // locked tiles never earn points
        )
    }

    fun tapTile(tile: LetterTile) {
        val state = _uiState.value as? ReconstructionUiState.Question ?: return
        // Next empty slot = lowest index not yet in slots map
        val nextEmpty = (0 until state.wordToReconstruct.length)
            .firstOrNull { !state.slots.containsKey(it) } ?: return
        val expectedChar = state.wordToReconstruct[nextEmpty].lowercaseChar()

        if (tile.char.lowercaseChar() == expectedChar) onCorrectTap(tile, state, nextEmpty)
        else onWrongTap(tile, state)
    }

    private fun onCorrectTap(tile: LetterTile, state: ReconstructionUiState.Question, slotIndex: Int) {
        val newSlots = state.slots + (slotIndex to tile)
        val newBank  = state.scrambledTiles.filter { it.id != tile.id }

        val alreadyScored = tile.id in state.scoredTileIds
        val pts = if (alreadyScored) 0
        else (Scoring.BASE_PTS_PER_LETTER * combo).roundToInt()

        if (!alreadyScored) {
            totalPoints += pts
            combo = (combo + Scoring.COMBO_STEP).coerceAtMost(Scoring.COMBO_MAX)
            if (combo > bestCombo) bestCombo = combo
        }

        val newScoredIds = state.scoredTileIds + tile.id

        if (newSlots.size == state.wordToReconstruct.length) {
            val perfect = !state.wordHadMistake
            if (perfect) totalPoints += Scoring.PERFECT_WORD_BONUS
            correctWordCount++
            correctWords.add(state.wordToReconstruct)
            _uiState.value = ReconstructionUiState.Feedback(
                correctWord     = state.wordToReconstruct,
                pointsEarned    = pts + if (perfect) Scoring.PERFECT_WORD_BONUS else 0,
                gotPerfectBonus = perfect,
                combo           = combo,
                totalPoints     = totalPoints,
                questionNumber  = state.questionNumber,
                totalQuestions  = state.totalQuestions
            )
            viewModelScope.launch {
                val pattern = repository.checkAnswer(state.wordToReconstruct, state.wordToReconstruct, true)
                if (pattern != null) {
                    val ex = patternMap[pattern] ?: PatternResult(pattern, 0, 0)
                    patternMap[pattern] = ex.copy(correct = ex.correct + 1, total = ex.total + 1)
                }
            }
        } else {
            _uiState.value = state.copy(
                slots            = newSlots,
                scrambledTiles   = newBank,
                combo            = combo,
                totalPoints      = totalPoints,
                pointsJustEarned = if (pts > 0) pts else null,
                scoredTileIds    = newScoredIds
            )
            if (pts > 0) viewModelScope.launch {
                delay(600)
                (_uiState.value as? ReconstructionUiState.Question)
                    ?.let { _uiState.value = it.copy(pointsJustEarned = null) }
            }
        }
    }

    private fun onWrongTap(tile: LetterTile, state: ReconstructionUiState.Question) {
        combo = Scoring.COMBO_START
        wordHadMistake = true
        _uiState.value = state.copy(
            scrambledTiles = state.scrambledTiles.map {
                if (it.id == tile.id) it.copy(isFlashingWrong = true) else it
            },
            combo          = combo,
            wordHadMistake = true
        )
        viewModelScope.launch {
            delay(400)
            (_uiState.value as? ReconstructionUiState.Question)?.let { cur ->
                _uiState.value = cur.copy(
                    scrambledTiles = cur.scrambledTiles.map { it.copy(isFlashingWrong = false) }
                )
            }
        }
    }

    fun removePlacedTile(tile: LetterTile) {
        val state = _uiState.value as? ReconstructionUiState.Question ?: return
        if (tile.isLocked) return
        // Highest filled non-locked slot
        val lastIndex = (state.wordToReconstruct.indices).reversed()
            .firstOrNull { state.slots[it]?.isLocked == false } ?: return
        if (state.slots[lastIndex]?.id != tile.id) return
        _uiState.value = state.copy(
            slots          = state.slots - lastIndex,
            scrambledTiles = state.scrambledTiles + tile.copy(isFlashingWrong = false)
        )
    }

    fun nextQuestion() {
        currentIndex++
        if (currentIndex >= words.size) finishSession() else showQuestion()
    }

    private fun finishSession() {
        viewModelScope.launch {
            database.reconstructionScoreDao().insertScore(
                ReconstructionScore(
                    totalPoints    = totalPoints,
                    wordsCompleted = correctWordCount,
                    totalWords     = words.size,
                    bestCombo      = bestCombo
                )
            )
            val topScores = database.reconstructionScoreDao().getTopScores()
            _uiState.value = ReconstructionUiState.Finished(
                summary = SessionSummary(
                    score            = correctWordCount,
                    total            = words.size,
                    accuracy         = if (words.isEmpty()) 0
                    else (correctWordCount.toFloat() / words.size * 100).toInt(),
                    correctWords     = correctWords.toList(),
                    wrongWords       = wrongWords.toList(),
                    patternBreakdown = patternMap.values.sortedBy { it.accuracy }
                ),
                totalPoints = totalPoints,
                bestCombo   = bestCombo,
                topScores   = topScores
            )
        }
    }

    fun restartSession() { loadWords() }
}

class ReconstructionViewModelFactory(
    private val repository: SpellRepository,
    private val database: SpellDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ReconstructionViewModel(repository, database) as T
    }
}