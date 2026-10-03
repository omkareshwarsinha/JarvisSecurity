package com.example.jarvis.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trusted_apps")
data class TrustedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isTrusted: Boolean = true,
    val addedTimestamp: Long = System.currentTimeMillis()
)
