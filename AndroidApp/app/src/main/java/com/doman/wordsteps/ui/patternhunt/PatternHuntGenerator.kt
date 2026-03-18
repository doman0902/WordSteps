package com.doman.wordsteps.ui.patternhunt

import com.doman.wordsteps.data.models.WordQuestion

data class PatternHuntQuestion(
    val options: List<String>,
    val wrongOption: String,
    val correctWord: String,
    val pattern: String
)

data class PatternHuntSummary(
    val score: Int,
    val total: Int,
    val foundWrongCount: Int,
    val namedPatternCount: Int,
    val wrongWords: List<PatternHuntMistake>
)

data class PatternHuntMistake(
    val wrongOption: String,
    val correctWord: String,
    val pattern: String,
    val foundWrong: Boolean,
    val namedPattern: Boolean
)

object PatternHuntGenerator {

    private val allPatterns = listOf(
        "vowel_swap", "ie_ei_swap", "vowel_drop",
        "double_to_single", "insertion", "transposition",
        "consonant_drop", "consonant_change",
        "single_to_double", "y_to_ie_ending", "i_y_swap"
    )

    fun generateQuestion(allWords: List<WordQuestion>): PatternHuntQuestion? {
        if (allWords.size < 4) return null

        val wrongWord    = allWords.random()
        val misspelling  = wrongWord.misspellings
            .filter { it.text.isNotEmpty() && it.pattern.isNotEmpty() }
            .randomOrNull() ?: return null

        val correctWords = allWords
            .filter { it.word != wrongWord.word }
            .shuffled()
            .take(3)
            .map { it.word }

        if (correctWords.size < 3) return null

        val options = (correctWords + misspelling.text).shuffled()

        return PatternHuntQuestion(
            options     = options,
            wrongOption = misspelling.text,
            correctWord = wrongWord.word,
            pattern     = misspelling.pattern
        )
    }

    fun generatePatternOptions(correctPattern: String): List<String> {
        val wrong = allPatterns
            .filter { it != correctPattern }
            .shuffled()
            .take(3)
        return (wrong + correctPattern).shuffled()
    }
}