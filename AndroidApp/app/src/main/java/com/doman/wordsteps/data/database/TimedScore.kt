package com.doman.wordsteps.data.database

import androidx.room.*

@Entity(tableName = "timed_scores")
data class TimedScore(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val score: Int,
    val wordsAnswered: Int,
    val duration: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface TimedScoreDao {

    @Insert
    suspend fun insertScore(score: TimedScore)

    @Query("SELECT * FROM timed_scores ORDER BY score DESC LIMIT 3")
    suspend fun getTopScores(): List<TimedScore>

    @Query("SELECT * FROM timed_scores WHERE duration = :duration ORDER BY score DESC LIMIT 3")
    suspend fun getTopScoresForDuration(duration: Int): List<TimedScore>

    @Query("DELETE FROM timed_scores")
    suspend fun deleteAll()
}