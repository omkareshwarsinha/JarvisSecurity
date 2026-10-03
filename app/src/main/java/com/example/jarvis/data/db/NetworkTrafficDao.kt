package com.example.jarvis.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.jarvis.data.db.entities.NetworkTrafficEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkTrafficDao {
    @Query("SELECT * FROM network_traffic ORDER BY timestamp DESC LIMIT 200")
    fun getRecentTraffic(): Flow<List<NetworkTrafficEntity>>

    @Query("SELECT * FROM network_traffic WHERE packageName = :packageName ORDER BY timestamp DESC LIMIT 100")
    fun getTrafficForPackage(packageName: String): Flow<List<NetworkTrafficEntity>>

    @Query("SELECT * FROM network_traffic WHERE category = 'TRACKER' OR category = 'AD_NETWORK' ORDER BY timestamp DESC LIMIT 100")
    fun getTrackingTraffic(): Flow<List<NetworkTrafficEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTraffic(item: NetworkTrafficEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<NetworkTrafficEntity>)

    @Query("DELETE FROM network_traffic")
    suspend fun clearAll()

    @Query("DELETE FROM network_traffic WHERE id NOT IN (SELECT id FROM network_traffic ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun pruneOldTraffic(keepCount: Int = 500)
}
