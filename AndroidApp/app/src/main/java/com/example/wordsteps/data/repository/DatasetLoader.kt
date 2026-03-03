package com.example.wordsteps.data.repository

import android.content.Context
import com.example.wordsteps.data.models.*
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Loads the 439 words × 3 misspellings dataset from assets/words.csv
 *
 * CSV Format (semicolon separated):
 * word;misspelling_1;rule_1;misspelling_2;rule_2;misspelling_3;rule_3
 */
class DatasetLoader(private val context: Context) {

    private var allWords: List<WordQuestion> = emptyList()

    // ── NEW: word → list of its patterns (from CSV rules columns) ────────────
    // e.g. "receive" → ["ie_ei_swap", "vowel_drop", "vowel_drop"]
    private var wordPatternMap: Map<String, List<String>> = emptyMap()

    fun loadDataset(): List<WordQuestion> {
        if (allWords.isNotEmpty()) return allWords

        val words      = mutableListOf<WordQuestion>()
        val patternMap = mutableMapOf<String, List<String>>()

        try {
            val reader = BufferedReader(
                InputStreamReader(context.assets.open("words.csv"))
            )
            reader.readLine() // skip header

            reader.useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split(";")
                    if (parts.size >= 7) {
                        val word = parts[0].trim()

                        val misspellings = listOf(
                            Misspelling(parts[1].trim(), parts[2].trim()),
                            Misspelling(parts[3].trim(), parts[4].trim()),
                            Misspelling(parts[5].trim(), parts[6].trim())
                        ).filter { it.text.isNotEmpty() && it.pattern.isNotEmpty() }

                        words.add(WordQuestion(word, word, misspellings))

                        // Store all patterns associated with this word
                        patternMap[word.lowercase()] = misspellings.map { it.pattern }
                    }
                }
            }

            allWords      = words
            wordPatternMap = patternMap

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return allWords
    }

    /**
     * Returns the most common pattern for a given word.
     * e.g. "receive" → "ie_ei_swap"
     * Falls back to the first pattern if no majority.
     */
    fun getPrimaryPattern(word: String): String? {
        val patterns = wordPatternMap[word.lowercase()] ?: return null
        return patterns
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    fun getRandomWords(count: Int): List<WordQuestion> =
        allWords.shuffled().take(count)

    fun getWordsByPattern(pattern: String, count: Int): List<WordQuestion> =
        allWords
            .filter { word -> word.misspellings.any { it.pattern == pattern } }
            .shuffled()
            .take(count)

    fun getWordsByDifficulty(difficulty: Difficulty, count: Int): List<WordQuestion> {
        val filtered = when (difficulty) {
            Difficulty.EASY   -> allWords.filter { it.word.length <= 7 }
            Difficulty.MEDIUM -> allWords.filter { it.word.length in 8..10 }
            Difficulty.HARD   -> allWords.filter { it.word.length > 10 }
        }
        return filtered.shuffled().take(count)
    }
}

enum class Difficulty { EASY, MEDIUM, HARD }