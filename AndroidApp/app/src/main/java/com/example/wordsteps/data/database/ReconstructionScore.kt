package com.example.wordsteps.data.database

import androidx.room.*

// ── Entity ────────────────────────────────────────────────────────────────────
@Entity(tableName = "reconstruction_scores")
data class ReconstructionScore(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val totalPoints: Int,
    val wordsCompleted: Int,
    val totalWords: Int,
    val bestCombo: Double,       // highest multiplier reached in the session
    val timestamp: Long = System.currentTimeMillis()
)

// ── DAO ───────────────────────────────────────────────────────────────────────
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