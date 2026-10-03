package com.example.jarvis.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.jarvis.data.db.entities.TrustedDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedDeviceDao {

    @Query("SELECT * FROM trusted_devices ORDER BY addedTimestamp DESC")
    fun getAllDevicesFlow(): Flow<List<TrustedDeviceEntity>>

    @Query("SELECT * FROM trusted_devices WHERE isEnabled = 1")
    suspend fun getEnabledDevices(): List<TrustedDeviceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: TrustedDeviceEntity): Long

    @Delete
    suspend fun delete(device: TrustedDeviceEntity)

    @Query("UPDATE trusted_devices SET isEnabled = :enabled WHERE id = :id")
    suspend fun updateEnabled(id: Long, enabled: Boolean)

    @Query("SELECT COUNT(*) FROM trusted_devices")
    suspend fun getCount(): Int
}
