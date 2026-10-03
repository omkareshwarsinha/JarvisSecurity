package com.example.jarvis.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.jarvis.data.db.entities.FirewallRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FirewallRuleDao {
    @Query("SELECT * FROM firewall_rules ORDER BY appName ASC")
    fun getAllRules(): Flow<List<FirewallRuleEntity>>

    @Query("SELECT * FROM firewall_rules")
    suspend fun getAllRulesList(): List<FirewallRuleEntity>

    @Query("SELECT * FROM firewall_rules WHERE isBlocked = 1")
    fun getBlockedRules(): Flow<List<FirewallRuleEntity>>

    @Query("SELECT * FROM firewall_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getRuleForPackage(packageName: String): FirewallRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setRule(rule: FirewallRuleEntity)

    @Query("UPDATE firewall_rules SET isBlocked = :isBlocked, updatedAtTimestamp = :timestamp WHERE packageName = :packageName")
    suspend fun updateRuleStatus(packageName: String, isBlocked: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM firewall_rules")
    suspend fun clearAll()
}
