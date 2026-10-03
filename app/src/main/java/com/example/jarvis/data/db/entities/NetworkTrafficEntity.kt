package com.example.jarvis.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_traffic")
data class NetworkTrafficEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val appName: String,
    val destinationHost: String,
    val destinationIp: String,
    val port: Int,
    val protocol: String,
    val isBlocked: Boolean,
    val category: String // TRACKER, AD_NETWORK, CLOUD_API, BROWSER, SYSTEM, UNKNOWN
)
