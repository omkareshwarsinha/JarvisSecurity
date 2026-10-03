package com.example.jarvis.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.jarvis.data.db.entities.TrustedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedAppDao {
    @Query("SELECT * FROM trusted_apps")
    fun getAllTrustedApps(): Flow<List<TrustedAppEntity>>

    @Query("SELECT * FROM trusted_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getTrustedApp(packageName: String): TrustedAppEntity?

    @Query("SELECT packageName FROM trusted_apps WHERE isTrusted = 1")
    suspend fun getTrustedPackageNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setTrusted(app: TrustedAppEntity)

    @Query("DELETE FROM trusted_apps WHERE packageName = :packageName")
    suspend fun removeTrusted(packageName: String)

    @Query("DELETE FROM trusted_apps")
    suspend fun clearAll()
}
