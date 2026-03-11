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
            correctIndex = options.indexOf(wordQuestion.correctSpelling),
            misspellings = wordQuestion.misspellings
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

    /**
     * Rögzíti a felhasználó válaszát és frissíti a pattern mastery-t.
     *
     * Logika:
     * - Helyes válasz  → az összes megjelenített hibás opció pattern-je +1 correct
     *                    (elkerülted őket, tehát "tudtad" őket)
     * - Helytelen válasz → a választott opció pattern-je -1 (rosszul csináltad)
     *                      az összes többi hibás opció pattern-je +1 correct
     *                      (ezeket legalább elkerülted)
     *
     * @param correctWord   a helyes szó
     * @param userAnswer    amit a felhasználó választott
     * @param isCorrect     helyes volt-e a válasz
     * @param allOptions    az összes megjelenített opció (csak Practice/Adaptive módban)
     *                      ha null → Typing mód, csak helyes/helytelen számít
     */
    suspend fun checkAnswer(
        correctWord: String,
        userAnswer: String,
        isCorrect: Boolean,
        allOptions: List<String>? = null
    ): String? {

        val optionPatterns: Map<String, String> = if (allOptions != null) {
            allOptions
                .filter { it != correctWord }
                .mapNotNull { option ->
                    val word = datasetLoader.getWordQuestion(correctWord)
                    val pattern = word?.misspellings
                        ?.firstOrNull { it.text == option }
                        ?.pattern
                    if (pattern != null) option to pattern else null
                }
                .toMap()
        } else {
            emptyMap()
        }

        val wrongPattern: String? = if (!isCorrect) {
            mlApi.predictPattern(correctWord, userAnswer)?.pattern
                ?: optionPatterns[userAnswer]
        } else {
            null
        }

        if (isCorrect) {
            optionPatterns.values.distinct().forEach { pattern ->
                updatePatternMastery(pattern, isCorrect = true)
            }
        } else {
            if (wrongPattern != null) {
                updatePatternMastery(wrongPattern, isCorrect = false)
            }
            optionPatterns.values
                .distinct()
                .filter { it != wrongPattern }
                .forEach { pattern ->
                    updatePatternMastery(pattern, isCorrect = true)
                }
        }

        attemptDao.insertAttempt(
            Attempt(
                correctWord    = correctWord,
                userAnswer     = userAnswer,
                isCorrect      = isCorrect,
                mistakePattern = wrongPattern
            )
        )

        updateUserStats(isCorrect)
        updateDailyStats(isCorrect)

        return wrongPattern
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
    val correctIndex: Int,
    val misspellings: List<Misspelling> = emptyList()
)