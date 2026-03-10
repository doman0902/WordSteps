package com.doman.wordsteps.data.repository

import com.doman.wordsteps.data.api.MLApiService
import com.doman.wordsteps.data.database.*
import com.doman.wordsteps.data.models.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SpellRepository(
    private val database: SpellDatabase,
    private val mlApi: MLApiService,
    private val datasetLoader: DatasetLoader
) {
    private val attemptDao   = database.attemptDao()
    private val statsDao     = database.statsDao()
    private val patternDao   = database.patternMasteryDao()
    private val dailyDao     = database.dailyStatsDao()

    private val dateFormat   = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private fun today(): String = dateFormat.format(Date())

    fun generateQuizQuestion(wordQuestion: WordQuestion): QuizQuestion {
        val options = mutableListOf(wordQuestion.correctSpelling)
        options.addAll(wordQuestion.misspellings.map { it.text }.take(3))
        options.shuffle()
        return QuizQuestion(
            correctWord  = wordQuestion.correctSpelling,
            options      = options,
            correctIndex = options.indexOf(wordQuestion.correctSpelling)
        )
    }

    fun getPracticeQuestions(count: Int = 10): List<QuizQuestion> =
        datasetLoader.getRandomWords(count).map { generateQuizQuestion(it) }

    fun getPatternFast(correctWord: String): String? =
        datasetLoader.getPrimaryPattern(correctWord)

    suspend fun getAdaptiveQuestions(count: Int = 10): List<QuizQuestion> {
        val weakPattern = patternDao.getWeakestPattern()?.pattern
        return if (weakPattern != null) {
            datasetLoader.getWordsByPattern(weakPattern, count).map { generateQuizQuestion(it) }
        } else {
            getPracticeQuestions(count)
        }
    }

    suspend fun checkAnswer(
        correctWord: String,
        userAnswer: String,
        isCorrect: Boolean
    ): String? {
        val pattern: String? = if (isCorrect) {
            datasetLoader.getPrimaryPattern(correctWord)
        } else {
            mlApi.predictPattern(correctWord, userAnswer)?.pattern
                ?: datasetLoader.getPrimaryPattern(correctWord)
        }

        attemptDao.insertAttempt(
            Attempt(
                correctWord    = correctWord,
                userAnswer     = userAnswer,
                isCorrect      = isCorrect,
                mistakePattern = if (!isCorrect) pattern else null
            )
        )

        updateUserStats(isCorrect)
        updateDailyStats(isCorrect)
        if (pattern != null) updatePatternMastery(pattern, isCorrect)

        return pattern
    }

    private suspend fun updateUserStats(isCorrect: Boolean) {
        val stats = statsDao.getStats() ?: UserStats()
        statsDao.updateStats(
            stats.copy(
                totalAttempts    = stats.totalAttempts + 1,
                correctAttempts  = if (isCorrect) stats.correctAttempts + 1 else stats.correctAttempts,
                currentStreak    = if (isCorrect) stats.currentStreak + 1 else 0,
                bestStreak       = maxOf(
                    stats.bestStreak,
                    if (isCorrect) stats.currentStreak + 1 else stats.bestStreak
                ),
                lastPracticeDate = System.currentTimeMillis()
            )
        )
    }

    private suspend fun updateDailyStats(isCorrect: Boolean) {
        val today   = today()
        val current = dailyDao.getForDate(today) ?: DailyStats(today, 0, 0)
        dailyDao.upsert(
            current.copy(
                totalAttempts   = current.totalAttempts + 1,
                correctAttempts = if (isCorrect) current.correctAttempts + 1 else current.correctAttempts
            )
        )
    }

    private suspend fun updatePatternMastery(pattern: String, isCorrect: Boolean) {
        val current = patternDao.getPattern(pattern) ?: PatternMastery(pattern = pattern)
        patternDao.updatePattern(
            current.copy(
                totalAttempts   = current.totalAttempts + 1,
                correctAttempts = if (isCorrect) current.correctAttempts + 1 else current.correctAttempts,
                lastPracticed   = System.currentTimeMillis()
            )
        )
    }

    suspend fun getPatternMasteryStats(): List<PatternMastery> = patternDao.getAllPatterns()
    suspend fun getUserStats(): UserStats = statsDao.getStats() ?: UserStats()
    suspend fun getRecentAttempts(): List<Attempt> = attemptDao.getRecentAttempts()
    suspend fun getDailyStats(): List<DailyStats> = dailyDao.getLast14Days()

    suspend fun analyzeWeakPattern(): WeakPatternAnalysis? {
        val recentMistakes = attemptDao.getRecentMistakes()
        if (recentMistakes.size < 5) return null
        return mlApi.analyzeUserWeakness(
            recentMistakes.map { it.correctWord to it.userAnswer }
        )
    }
}

data class QuizQuestion(
    val correctWord: String,
    val options: List<String>,
    val correctIndex: Int
)