package com.doman.wordsteps.data.models

data class SessionSummary(
    val score: Int,
    val total: Int,
    val accuracy: Int,
    val correctWords: List<String>,
    val wrongWords: List<WrongWord>,
    val patternBreakdown: List<PatternResult>
)

data class WrongWord(
    val correct: String,
    val userAnswer: String,
    val pattern: String?
)

data class PatternResult(
    val pattern: String,
    val correct: Int,
    val total: Int
) {
    val accuracy: Float get() = if (total == 0) 0f else correct.toFloat() / total
}