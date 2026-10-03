package com.example.jarvis.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persists trusted emergency contact devices authorized to trigger
 * remote emergency SMS commands (Lockdown, High-Decibel Siren, Live GPS Location).
 */
@Entity(tableName = "trusted_devices")
data class TrustedDeviceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val contactName: String,
    val isEnabled: Boolean = true,
    val addedTimestamp: Long = System.currentTimeMillis()
)
