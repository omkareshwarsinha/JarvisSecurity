package com.example.jarvis.data.model

data class PermissionRiskInfo(
    val permission: String,
    val readableName: String,
    val category: String,
    val riskLevel: FindingSeverity,
    val implication: String,
    val recommendation: String,
    val grantedAppsCount: Int = 0,
    val holdingApps: List<String> = emptyList()
)
