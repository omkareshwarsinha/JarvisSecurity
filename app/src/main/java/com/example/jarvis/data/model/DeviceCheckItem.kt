package com.example.jarvis.data.model

data class DeviceCheckItem(
    val id: String,
    val title: String,
    val category: CheckCategory,
    val severity: FindingSeverity,
    val detectedValue: String,
    val description: String,
    val technicalExplanation: String,
    val recommendation: String,
    val remediationAction: RemediationAction? = null,
    val isAvailableOnDevice: Boolean = true
)
