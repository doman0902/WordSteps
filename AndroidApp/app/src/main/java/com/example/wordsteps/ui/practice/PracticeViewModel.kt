package com.example.wordsteps.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordsteps.data.models.PatternResult
import com.example.wordsteps.data.models.SessionSummary
import com.example.wordsteps.data.models.WrongWord
import com.example.wordsteps.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PracticeViewModel(private val repository: SpellRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<PracticeUiState>(PracticeUiState.Loading)
    val uiState: StateFlow<PracticeUiState> = _uiState

    private var questions: List<QuizQuestion> = emptyList()
    private var currentQuestionIndex = 0
    private var score = 0
    private var streak = 0
    private val correctWords = mutableListOf<String>()
    private val wrongWords   = mutableListOf<WrongWord>()
    private val patternMap   = mutableMapOf<String, PatternResult>()

    init { loadQuestions() }

    fun loadQuestions(mode: PracticeMode = PracticeMode.NORMAL) {
        viewModelScope.launch {
            _uiState.value = PracticeUiState.Loading
            questions = when (mode) {
                PracticeMode.NORMAL   -> repository.getPracticeQuestions(10)
                PracticeMode.ADAPTIVE -> repository.getAdaptiveQuestions(10)
            }
            currentQuestionIndex = 0
            score  = 0
            streak = 0
            correctWords.clear()
            wrongWords.clear()
            patternMap.clear()
            showNextQuestion()
        }
    }

    private fun showNextQuestion() {
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            _uiState.value = PracticeUiState.Question(
                question       = question,
                questionNumber = currentQuestionIndex + 1,
                totalQuestions = questions.size,
                currentScore   = score,
                currentStreak  = streak
            )
        } else {
            finishSession()
        }
    }

    private fun finishSession() {
        _uiState.value = PracticeUiState.Finished(
            SessionSummary(
                score            = score,
                total            = questions.size,
                accuracy         = if (questions.isEmpty()) 0 else (score.toFloat() / questions.size * 100).toInt(),
                correctWords     = correctWords.toList(),
                wrongWords       = wrongWords.toList(),
                patternBreakdown = patternMap.values.sortedBy { it.accuracy }
            )
        )
    }

    fun submitAnswer(selectedIndex: Int) {
        val currentState = _uiState.value
        if (currentState !is PracticeUiState.Question) return

        val question   = currentState.question
        val isCorrect  = selectedIndex == question.correctIndex
        val userAnswer = question.options[selectedIndex]

        // Track correct words immediately — question and isCorrect are defined here
        if (isCorrect) correctWords.add(question.correctWord)

        _uiState.value = PracticeUiState.Feedback(
            isCorrect      = isCorrect,
            correctAnswer  = question.correctWord,
            userAnswer     = userAnswer,
            correctIndex   = question.correctIndex,
            questionNumber = currentState.questionNumber,
            totalQuestions = currentState.totalQuestions,
            currentScore   = if (isCorrect) score + 1 else score,
            currentStreak  = if (isCorrect) streak + 1 else 0
        )

        if (isCorrect) { score++; streak++ } else { streak = 0 }

        viewModelScope.launch {
            val pattern = repository.checkAnswer(
                correctWord = question.correctWord,
                userAnswer  = userAnswer,
                isCorrect   = isCorrect
            )
            if (!isCorrect) {
                wrongWords.add(WrongWord(question.correctWord, userAnswer, pattern))
            }
            if (pattern != null) {
                val existing = patternMap[pattern] ?: PatternResult(pattern, 0, 0)
                patternMap[pattern] = existing.copy(
                    correct = existing.correct + if (isCorrect) 1 else 0,
                    total   = existing.total + 1
                )
            }
        }
    }

    fun nextQuestion() {
        currentQuestionIndex++
        showNextQuestion()
    }

    fun restartQuiz() { loadQuestions() }
}

sealed class PracticeUiState {
    object Loading : PracticeUiState()
    data class Question(
        val question: QuizQuestion,
        val questionNumber: Int,
        val totalQuestions: Int,
        val currentScore: Int,
        val currentStreak: Int
    ) : PracticeUiState()
    data class Feedback(
        val isCorrect: Boolean,
        val correctAnswer: String,
        val userAnswer: String,
        val correctIndex: Int,
        val questionNumber: Int,
        val totalQuestions: Int,
        val currentScore: Int,
        val currentStreak: Int
    ) : PracticeUiState()
    data class Finished(val summary: SessionSummary) : PracticeUiState()
}

enum class PracticeMode { NORMAL, ADAPTIVE }