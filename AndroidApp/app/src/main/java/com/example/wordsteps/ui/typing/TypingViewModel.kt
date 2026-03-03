package com.example.wordsteps.ui.typing

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wordsteps.data.repository.QuizQuestion
import com.example.wordsteps.data.repository.SpellRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

sealed class TypingUiState {
    object Setup : TypingUiState()
    object Loading : TypingUiState()
    data class Question(
        val questionNumber: Int, val totalQuestions: Int,
        val userInput: String, val isSpeaking: Boolean,
        val hasSpoken: Boolean, val score: Int, val streak: Int,
        val ttsReady: Boolean
    ) : TypingUiState()
    data class Feedback(
        val isCorrect: Boolean, val correctWord: String, val userAnswer: String,
        val questionNumber: Int, val totalQuestions: Int, val score: Int, val streak: Int
    ) : TypingUiState()
    data class Finished(val score: Int, val total: Int, val accuracy: Int) : TypingUiState()
}

class TypingViewModel(private val repository: SpellRepository, private val context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow<TypingUiState>(TypingUiState.Setup)
    val uiState: StateFlow<TypingUiState> = _uiState
    private var questions: List<QuizQuestion> = emptyList()
    private var currentIndex = 0
    private var score = 0
    private var streak = 0
    private var tts: TextToSpeech? = null
    private var lastWordCount = 10
    private var ttsReady = false

    init { initTts() }

    private fun initTts() {
        tts = TextToSpeech(context, { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.UK
                tts?.setSpeechRate(0.9f)
                ttsReady = true
                // refresh UI so the speaker button becomes tappable
                (_uiState.value as? TypingUiState.Question)?.let {
                    _uiState.value = it.copy(ttsReady = true)
                }
            } else {
                // Google TTS failed, fall back to default engine
                tts = TextToSpeech(context) { fallbackStatus ->
                    if (fallbackStatus == TextToSpeech.SUCCESS) {
                        tts?.language = Locale.UK
                        tts?.setSpeechRate(0.9f)
                        ttsReady = true
                        (_uiState.value as? TypingUiState.Question)?.let {
                            _uiState.value = it.copy(ttsReady = true)
                        }
                    }
                }
            }
        }, "com.google.android.tts")
    }

    fun startSession(wordCount: Int) {
        lastWordCount = wordCount
        viewModelScope.launch {
            _uiState.value = TypingUiState.Loading
            questions = repository.getPracticeQuestions(wordCount)
            currentIndex = 0; score = 0; streak = 0
            showQuestion()
        }
    }

    private fun showQuestion() {
        if (currentIndex >= questions.size) {
            _uiState.value = TypingUiState.Finished(
                score    = score,
                total    = questions.size,
                accuracy = if (questions.isEmpty()) 0 else (score.toFloat() / questions.size * 100).toInt()
            )
            return
        }
        _uiState.value = TypingUiState.Question(
            questionNumber = currentIndex + 1,
            totalQuestions = questions.size,
            userInput      = "",
            isSpeaking     = false,
            hasSpoken      = false,
            score          = score,
            streak         = streak,
            ttsReady       = ttsReady
        )
    }

    fun speakCurrentWord() {
        if (!ttsReady) return
        val word = questions.getOrNull(currentIndex)?.correctWord ?: return
        (_uiState.value as? TypingUiState.Question)?.let {
            _uiState.value = it.copy(isSpeaking = true)
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                (_uiState.value as? TypingUiState.Question)?.let {
                    _uiState.value = it.copy(isSpeaking = false, hasSpoken = true)
                }
            }
            override fun onError(id: String?) {
                (_uiState.value as? TypingUiState.Question)?.let {
                    _uiState.value = it.copy(isSpeaking = false)
                }
            }
        })
        tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "w$currentIndex")
    }

    fun onInputChanged(text: String) {
        (_uiState.value as? TypingUiState.Question)?.let {
            _uiState.value = it.copy(userInput = text)
        }
    }

    fun submitAnswer() {
        val state    = _uiState.value as? TypingUiState.Question ?: return
        val question = questions[currentIndex]
        val input    = state.userInput.trim()
        val isCorrect = input.equals(question.correctWord, ignoreCase = true)
        if (isCorrect) { score++; streak++ } else { streak = 0 }
        viewModelScope.launch { repository.checkAnswer(question.correctWord, input, isCorrect) }
        _uiState.value = TypingUiState.Feedback(
            isCorrect      = isCorrect,
            correctWord    = question.correctWord,
            userAnswer     = input,
            questionNumber = state.questionNumber,
            totalQuestions = state.totalQuestions,
            score          = score,
            streak         = streak
        )
    }

    fun nextQuestion() { currentIndex++; showQuestion() }
    fun restartSession() { startSession(lastWordCount) }

    override fun onCleared() { tts?.stop(); tts?.shutdown(); super.onCleared() }
}

class TypingViewModelFactory(
    private val repository: SpellRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TypingViewModel(repository, context) as T
    }
}