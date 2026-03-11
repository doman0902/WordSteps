package com.doman.wordsteps.data.database

import androidx.room.*

@Entity(tableName = "reconstruction_scores")
data class ReconstructionScore(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val totalPoints: Int,
    val wordsCompleted: Int,
    val totalWords: Int,
    val bestCombo: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ReconstructionScoreDao {

    @Insert
    suspend fun insertScore(score: ReconstructionScore)

    /** Top 3 all-time scores */
    @Query("SELECT * FROM reconstruction_scores ORDER BY totalPoints DESC LIMIT 3")
    suspend fun getTopScores(): List<ReconstructionScore>

    @Query("DELETE FROM reconstruction_scores")
    suspend fun deleteAll()
}