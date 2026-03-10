package com.doman.wordsteps.data.database

import androidx.room.*

@Entity(tableName = "daily_stats")
data class DailyStats(
    @PrimaryKey
    val date: String,           // "2024-03-10" format
    val totalAttempts: Int,
    val correctAttempts: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    val accuracy: Float
        get() = if (totalAttempts > 0) correctAttempts.toFloat() / totalAttempts else 0f
}

@Dao
interface DailyStatsDao {

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT 14")
    suspend fun getLast14Days(): List<DailyStats>

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getForDate(date: String): DailyStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dailyStats: DailyStats)

    @Query("DELETE FROM daily_stats")
    suspend fun deleteAll()
}