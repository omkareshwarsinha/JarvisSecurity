package com.example.jarvis.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_events")
data class SecurityEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val category: String,
    val severity: String, // CRITICAL, WARNING, INFO, SUCCESS
    val description: String,
    val source: String,
    val details: String = ""
)
