package com.example.wordsteps.data.repository

import android.content.Context
import com.example.wordsteps.data.models.*
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Loads your 439 words × 3 misspellings dataset from assets/words.csv
 *
 * CSV Format:
 * word,misspelling_1,rule_1,misspelling_2,rule_2,misspelling_3,rule_3
 */
class DatasetLoader(private val context: Context) {

    private var allWords: List<WordQuestion> = emptyList()

    /**
     * Load dataset from CSV in assets folder
     * Place your Munkafüzet1.xlsx exported as CSV at: app/src/main/assets/words.csv
     */
    fun loadDataset(): List<WordQuestion> {
        if (allWords.isNotEmpty()) return allWords

        val words = mutableListOf<WordQuestion>()

        try {
            val inputStream = context.assets.open("words.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))

            // Skip header
            reader.readLine()

            reader.useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split(";")
                    if (parts.size >= 7) {
                        val word = parts[0].trim()
                        val misspellings = listOf(
                            Misspelling(parts[1].trim(), parts[2].trim()),
                            Misspelling(parts[3].trim(), parts[4].trim()),
                            Misspelling(parts[5].trim(), parts[6].trim())
                        ).filter { it.text.isNotEmpty() }

                        words.add(WordQuestion(word, word, misspellings))
                    }
                }
            }

            allWords = words
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return allWords
    }

    /**
     * Get random words for practice mode
     */
    fun getRandomWords(count: Int): List<WordQuestion> {
        return allWords.shuffled().take(count)
    }

    /**
     * Get words filtered by specific mistake pattern
     */
    fun getWordsByPattern(pattern: String, count: Int): List<WordQuestion> {
        return allWords
            .filter { word ->
                word.misspellings.any { it.pattern == pattern }
            }
            .shuffled()
            .take(count)
    }

    /**
     * Get words by difficulty (based on word length)
     */
    fun getWordsByDifficulty(difficulty: Difficulty, count: Int): List<WordQuestion> {
        val filtered = when (difficulty) {
            Difficulty.EASY -> allWords.filter { it.word.length <= 7 }
            Difficulty.MEDIUM -> allWords.filter { it.word.length in 8..10 }
            Difficulty.HARD -> allWords.filter { it.word.length > 10 }
        }
        return filtered.shuffled().take(count)
    }
}

enum class Difficulty {
    EASY, MEDIUM, HARD
}