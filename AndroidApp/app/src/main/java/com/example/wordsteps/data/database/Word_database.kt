package com.example.wordsteps.data.database

import android.content.Context
import androidx.room.*
import com.example.wordsteps.data.models.*

@Dao
interface AttemptDao {
    @Query("SELECT * FROM attempts ORDER BY timestamp DESC LIMIT 100")
    suspend fun getRecentAttempts(): List<Attempt>

    @Query("SELECT * FROM attempts WHERE isCorrect = 0 ORDER BY timestamp DESC LIMIT 20")
    suspend fun getRecentMistakes(): List<Attempt>

    @Insert
    suspend fun insertAttempt(attempt: Attempt)

    @Query("DELETE FROM attempts")
    suspend fun deleteAll()
}

@Dao
interface StatsDao {
    @Query("SELECT * FROM stats WHERE id = 1")
    suspend fun getStats(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateStats(stats: UserStats)

    @Query("UPDATE stats SET currentStreak = 0 WHERE id = 1")
    suspend fun resetStreak()

    @Query("DELETE FROM stats")
    suspend fun deleteAll()
}

@Dao
interface PatternMasteryDao {
    @Query("SELECT * FROM pattern_mastery ORDER BY CAST(correctAttempts AS REAL) / totalAttempts ASC")
    suspend fun getAllPatterns(): List<PatternMastery>

    @Query("SELECT * FROM pattern_mastery WHERE pattern = :pattern")
    suspend fun getPattern(pattern: String): PatternMastery?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePattern(patternMastery: PatternMastery)

    @Query("SELECT * FROM pattern_mastery ORDER BY CAST(correctAttempts AS REAL) / totalAttempts ASC LIMIT 1")
    suspend fun getWeakestPattern(): PatternMastery?

    @Query("DELETE FROM pattern_mastery")
    suspend fun deleteAll()
}

@Database(
    entities = [Attempt::class, UserStats::class, PatternMastery::class],
    version = 1,
    exportSchema = false
)
abstract class SpellDatabase : RoomDatabase() {
    abstract fun attemptDao(): AttemptDao
    abstract fun statsDao(): StatsDao
    abstract fun patternMasteryDao(): PatternMasteryDao

    companion object {
        @Volatile
        private var INSTANCE: SpellDatabase? = null

        fun getDatabase(context: Context): SpellDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpellDatabase::class.java,
                    "spell_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}