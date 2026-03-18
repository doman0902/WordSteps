package com.doman.wordsteps.ui.patternhunt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.doman.wordsteps.data.repository.SpellRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch



sealed class PatternHuntUiState {
    object Loading : PatternHuntUiState()

    data class FindWrong(
        val question: PatternHuntQuestion,
        val questionNumber: Int,
        val totalQuestions: Int,
        val score: Int,
        val selectedOption: String? = null
    ) : PatternHuntUiState()

    data class FindWrongFeedback(
        val isCorrect: Boolean,
        val question: PatternHuntQuestion,
        val selectedOption: String,
        val questionNumber: Int,
        val totalQuestions: Int,
        val score: Int
    ) : PatternHuntUiState()

    data class NamePattern(
        val question: PatternHuntQuestion,
        val patternOptions: List<String>,
        val questionNumber: Int,
        val totalQuestions: Int,
        val score: Int,
        val chosenWrongWord: String,
        val selectedPattern: String? = null,
        val timeStarted: Long = System.currentTimeMillis()
    ) : PatternHuntUiState()

    data class NamePatternFeedback(
        val isCorrect: Boolean,
        val question: PatternHuntQuestion,
        val selectedPattern: String,
        val questionNumber: Int,
        val totalQuestions: Int,
        val score: Int,
        val roundPoints: Int,
        val gotSpeedBonus: Boolean = false
    ) : PatternHuntUiState()

    data class Finished(
        val summary: PatternHuntSummary
    ) : PatternHuntUiState()
}


class PatternHuntViewModel(
    private val repository: SpellRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PatternHuntUiState>(PatternHuntUiState.Loading)
    val uiState: StateFlow<PatternHuntUiState> = _uiState

    private val totalQuestions = 10
    private var currentIndex   = 0
    private var score          = 0
    private var foundWrongCount    = 0
    private var namedPatternCount  = 0
    private val mistakes = mutableListOf<PatternHuntMistake>()
    private var questions: List<PatternHuntQuestion> = emptyList()

    private var currentFoundWrong = false

    init { loadQuestions() }

    fun loadQuestions() {
        viewModelScope.launch {
            _uiState.value = PatternHuntUiState.Loading

            val allWords = repository.getPracticeQuestions(200)
                .map { q ->

                    q
                }

            val wordQuestions = repository.getAllWordQuestions()

            questions = (1..totalQuestions).mapNotNull {
                PatternHuntGenerator.generateQuestion(wordQuestions)
            }

            if (questions.isEmpty()) {
                _uiState.value = PatternHuntUiState.Loading
                return@launch
            }

            currentIndex       = 0
            score              = 0
            foundWrongCount    = 0
            namedPatternCount  = 0
            mistakes.clear()
            showFindWrong()
        }
    }

    fun selectWrongOption(selected: String) {
        val state = _uiState.value as? PatternHuntUiState.FindWrong ?: return

        val isCorrect = selected == state.question.wrongOption
        if (isCorrect) {
            score += 10
            foundWrongCount++
            currentFoundWrong = true
        } else {
            currentFoundWrong = false
        }

        _uiState.value = PatternHuntUiState.FindWrongFeedback(
            isCorrect      = isCorrect,
            question       = state.question,
            selectedOption = selected,
            questionNumber = state.questionNumber,
            totalQuestions = state.totalQuestions,
            score          = score
        )
    }

    fun proceedToNamePattern() {
        val state = _uiState.value as? PatternHuntUiState.FindWrongFeedback ?: return

        val patternOptions = PatternHuntGenerator.generatePatternOptions(state.question.pattern)

        _uiState.value = PatternHuntUiState.NamePattern(
            question         = state.question,
            patternOptions   = patternOptions,
            questionNumber   = state.questionNumber,
            totalQuestions   = state.totalQuestions,
            score            = score,
            chosenWrongWord  = state.selectedOption
        )
    }

    fun selectPattern(selected: String) {
        val state = _uiState.value as? PatternHuntUiState.NamePattern ?: return

        val isCorrect = selected == state.question.pattern

        val elapsedMs     = System.currentTimeMillis() - state.timeStarted
        val gotSpeedBonus = isCorrect && elapsedMs < 5_000L

        val roundPoints = when {
            !isCorrect       -> 0
            gotSpeedBonus    -> 30
            else             -> 20
        } + if (currentFoundWrong) 10 else 0

        if (isCorrect) {
            score += 20
            namedPatternCount++
        }
        if (gotSpeedBonus) {
            score += 10
        }

        mistakes.add(
            PatternHuntMistake(
                wrongOption    = state.question.wrongOption,
                correctWord    = state.question.correctWord,
                pattern        = state.question.pattern,
                foundWrong     = currentFoundWrong,
                namedPattern   = isCorrect
            )
        )

        _uiState.value = PatternHuntUiState.NamePatternFeedback(
            isCorrect       = isCorrect,
            question        = state.question,
            selectedPattern = selected,
            questionNumber  = state.questionNumber,
            totalQuestions  = state.totalQuestions,
            score           = score,
            roundPoints     = roundPoints,
            gotSpeedBonus   = gotSpeedBonus
        )
    }

    fun nextQuestion() {
        currentIndex++
        if (currentIndex >= questions.size) {
            finishSession()
        } else {
            showFindWrong()
        }
    }

    private fun showFindWrong() {
        val question = questions.getOrNull(currentIndex) ?: run {
            finishSession()
            return
        }
        currentFoundWrong = false
        _uiState.value = PatternHuntUiState.FindWrong(
            question       = question,
            questionNumber = currentIndex + 1,
            totalQuestions = questions.size,
            score          = score
        )
    }

    private fun finishSession() {
        _uiState.value = PatternHuntUiState.Finished(
            PatternHuntSummary(
                score             = score,
                total             = questions.size,
                foundWrongCount   = foundWrongCount,
                namedPatternCount = namedPatternCount,
                wrongWords        = mistakes.toList()
            )
        )
    }

    fun restartSession() { loadQuestions() }
}


class PatternHuntViewModelFactory(
    private val repository: SpellRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PatternHuntViewModel(repository) as T
}