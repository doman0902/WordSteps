package com.example.wordsteps.data.repository

import com.example.wordsteps.data.api.MLApiService
import com.example.wordsteps.data.database.*
import com.example.wordsteps.data.models.*

/**
 * Main repository - coordinates database, ML API, and dataset
 */
class SpellRepository(
    private val database: SpellDatabase,
    private val mlApi: MLApiService,
    private val datasetLoader: DatasetLoader
) {

    private val attemptDao = database.attemptDao()
    private val statsDao = database.statsDao()
    private val patternDao = database.patternMasteryDao()

    // ========================================================================
    // QUIZ GENERATION
    // ========================================================================

    /**
     * Generate a quiz question with 4 options (1 correct + 3 wrong)
     */
    fun generateQuizQuestion(wordQuestion: WordQuestion): QuizQuestion {
        val options = mutableListOf(wordQuestion.correctSpelling)
        options.addAll(wordQuestion.misspellings.map { it.text }.take(3))
        options.shuffle()

        return QuizQuestion(
            correctWord = wordQuestion.correctSpelling,
            options = options,
            correctIndex = options.indexOf(wordQuestion.correctSpelling)
        )
    }

    /**
     * Get questions for practice mode
     */
    fun getPracticeQuestions(count: Int = 10): List<QuizQuestion> {
        val words = datasetLoader.getRandomWords(count)
        return words.map { generateQuizQuestion(it) }
    }

    /**
     * Get questions targeting user's weak pattern
     */
    suspend fun getAdaptiveQuestions(count: Int = 10): List<QuizQuestion> {
        val weakPattern = patternDao.getWeakestPattern()?.pattern

        return if (weakPattern != null) {
            val words = datasetLoader.getWordsByPattern(weakPattern, count)
            words.map { generateQuizQuestion(it) }
        } else {
            getPracticeQuestions(count)
        }
    }

    // ========================================================================
    // ANSWER CHECKING & TRACKING
    // ========================================================================

    /**
     * Check user's answer and record it
     */
    suspend fun checkAnswer(
        correctWord: String,
        userAnswer: String,
        isCorrect: Boolean
    ) {
        // Record attempt
        val mistakePattern = if (!isCorrect) {
            mlApi.predictPattern(correctWord, userAnswer)?.pattern
        } else null

        val attempt = Attempt(
            correctWord = correctWord,
            userAnswer = userAnswer,
            isCorrect = isCorrect,
            mistakePattern = mistakePattern
        )
        attemptDao.insertAttempt(attempt)

        // Update stats
        updateUserStats(isCorrect)

        // Update pattern mastery if wrong
        if (!isCorrect && mistakePattern != null) {
            updatePatternMastery(mistakePattern, isCorrect = false)
        }
    }

    private suspend fun updateUserStats(isCorrect: Boolean) {
        val stats = statsDao.getStats() ?: UserStats()

        val newStats = stats.copy(
            totalAttempts = stats.totalAttempts + 1,
            correctAttempts = if (isCorrect) stats.correctAttempts + 1 else stats.correctAttempts,
            currentStreak = if (isCorrect) stats.currentStreak + 1 else 0,
            bestStreak = maxOf(stats.bestStreak, if (isCorrect) stats.currentStreak + 1 else 0),
            lastPracticeDate = System.currentTimeMillis()
        )

        statsDao.updateStats(newStats)
    }

    private suspend fun updatePatternMastery(pattern: String, isCorrect: Boolean) {
        val current = patternDao.getPattern(pattern) ?: PatternMastery(pattern = pattern)

        val updated = current.copy(
            totalAttempts = current.totalAttempts + 1,
            correctAttempts = if (isCorrect) current.correctAttempts + 1 else current.correctAttempts,
            lastPracticed = System.currentTimeMillis()
        )

        patternDao.updatePattern(updated)
    }

    // ========================================================================
    // ANALYTICS & INSIGHTS
    // ========================================================================

    /**
     * Analyze user's weak pattern from recent mistakes
     */
    suspend fun analyzeWeakPattern(): WeakPatternAnalysis? {
        val recentMistakes = attemptDao.getRecentMistakes()
        if (recentMistakes.size < 5) return null

        val mistakes = recentMistakes.map { it.correctWord to it.userAnswer }
        return mlApi.analyzeUserWeakness(mistakes)
    }

    /**
     * Get all pattern mastery stats
     */
    suspend fun getPatternMasteryStats(): List<PatternMastery> {
        return patternDao.getAllPatterns()
    }

    /**
     * Get overall user stats
     */
    suspend fun getUserStats(): UserStats {
        return statsDao.getStats() ?: UserStats()
    }

    /**
     * Get recent attempts for history
     */
    suspend fun getRecentAttempts(): List<Attempt> {
        return attemptDao.getRecentAttempts()
    }
}

data class QuizQuestion(
    val correctWord: String,
    val options: List<String>,
    val correctIndex: Int
)