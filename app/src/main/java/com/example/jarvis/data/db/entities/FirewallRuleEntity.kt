package com.example.jarvis.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "firewall_rules")
data class FirewallRuleEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isBlocked: Boolean = false,
    val updatedAtTimestamp: Long = System.currentTimeMillis()
)
