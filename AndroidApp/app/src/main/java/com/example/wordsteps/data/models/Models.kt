package com.example.wordsteps.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Word with its misspellings from your dataset
 */
data class WordQuestion(
    val word: String,
    val correctSpelling: String,
    val misspellings: List<Misspelling>
)

data class Misspelling(
    val text: String,
    val pattern: String  // vowel_swap, ie_ei_swap, etc.
)

/**
 * User's attempt at a word - stored in database
 */
@Entity(tableName = "attempts")
data class Attempt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val correctWord: String,
    val userAnswer: String,
    val isCorrect: Boolean,
    val mistakePattern: String?,  // if wrong, what pattern was it
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * User statistics
 */
@Entity(tableName = "stats")
data class UserStats(
    @PrimaryKey
    val id: Int = 1,  // Only one stats record
    val totalAttempts: Int = 0,
    val correctAttempts: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastPracticeDate: Long = 0
)

/**
 * Pattern mastery tracking
 */
@Entity(tableName = "pattern_mastery")
data class PatternMastery(
    @PrimaryKey
    val pattern: String,  // vowel_swap, ie_ei_swap, etc.
    val totalAttempts: Int = 0,
    val correctAttempts: Int = 0,
    val lastPracticed: Long = 0
) {
    val accuracy: Float
        get() = if (totalAttempts > 0) correctAttempts.toFloat() / totalAttempts else 0f
}

/**
 * ML prediction result
 */
data class PatternPrediction(
    val pattern: String,
    val confidence: Double
)

/**
 * User's weak pattern analysis
 */
data class WeakPatternAnalysis(
    val weakestPattern: String,
    val patternCounts: Map<String, Int>,
    val accuracy: Float
)