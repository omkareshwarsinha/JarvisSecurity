package com.example.jarvis.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.jarvis.data.db.entities.PhishingHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhishingHistoryDao {
    @Query("SELECT * FROM phishing_history ORDER BY timestamp DESC LIMIT 100")
    fun getRecentHistory(): Flow<List<PhishingHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PhishingHistoryEntity)

    @Query("DELETE FROM phishing_history")
    suspend fun clearAll()
}
