package com.example.jarvis.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phishing_history")
data class PhishingHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val url: String,
    val domain: String,
    val isPhishing: Boolean,
    val threatReason: String,
    val brandImpersonated: String = ""
)
