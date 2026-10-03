package com.example.jarvis.data.model

data class ScanSummary(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val overallScore: Int = 100,
    val criticalCount: Int = 0,
    val warningCount: Int = 0,
    val infoCount: Int = 0,
    val verifiedCount: Int = 0,
    val totalAppsScanned: Int = 0,
    val summaryText: String = ""
)
