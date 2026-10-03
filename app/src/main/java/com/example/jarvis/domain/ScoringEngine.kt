package com.example.jarvis.domain

import com.example.jarvis.data.model.AppSecurityInfo
import com.example.jarvis.data.model.DeviceCheckItem
import com.example.jarvis.data.model.FindingSeverity
import com.example.jarvis.data.model.ScanSummary

object ScoringEngine {

    data class ScoringResult(
        val overallScore: Int,
        val deviceScore: Int = 100,
        val applicationScore: Int = 100,
        val networkScore: Int = 100,
        val behaviorScore: Int = 100,
        val criticalCount: Int,
        val warningCount: Int,
        val infoCount: Int,
        val verifiedCount: Int,
        val totalAppsScanned: Int,
        val deductions: List<DeductionItem>,
        val summaryText: String
    )

    data class DeductionItem(
        val title: String,
        val pointsLost: Int,
        val reason: String
    )

    fun evaluate(
        deviceChecks: List<DeviceCheckItem>,
        apps: List<AppSecurityInfo>
    ): ScoringResult {
        var baseScore = 100
        val deductions = mutableListOf<DeductionItem>()

        // 1. Evaluate Device Checks
        for (check in deviceChecks) {
            when (check.id) {
                "screen_lock" -> {
                    if (check.severity == FindingSeverity.CRITICAL) {
                        baseScore -= 25
                        deductions.add(
                            DeductionItem("Screen Lock Missing", 25, "Device lacks hardware-guarded screen lock (PIN/Password/Biometrics).")
                        )
                    }
                }
                "storage_encryption" -> {
                    if (check.severity == FindingSeverity.CRITICAL) {
                        baseScore -= 20
                        deductions.add(
                            DeductionItem("Unencrypted Storage", 20, "User partition is not encrypted with hardware keys.")
                        )
                    }
                }
                "adb_debugging" -> {
                    if (check.severity == FindingSeverity.WARNING) {
                        baseScore -= 10
                        deductions.add(
                            DeductionItem("ADB Debugging Enabled", 10, "USB debugging enables physical/local network shell command exploitation.")
                        )
                    }
                }
                "os_security_patch" -> {
                    if (check.severity == FindingSeverity.WARNING) {
                        baseScore -= 10
                        deductions.add(
                            DeductionItem("Outdated OS / Security Patch", 10, "Device security bulletin is significantly older than the baseline.")
                        )
                    }
                }
            }
        }

        // 2. Evaluate Installed App Risks (Non-system apps primarily)
        val nonSystemApps = apps.filter { !it.isSystemApp }

        val criticalApps = nonSystemApps.filter { it.riskLevel == FindingSeverity.CRITICAL }
        if (criticalApps.isNotEmpty()) {
            val deduction = (criticalApps.size * 8).coerceAtMost(25)
            baseScore -= deduction
            deductions.add(
                DeductionItem(
                    "High-Risk Third-Party Applications",
                    deduction,
                    "${criticalApps.size} applications hold elevated sensitive permissions (e.g. Accessibility/Overlay/SMS)."
                )
            )
        }

        val debuggableApps = nonSystemApps.filter { it.isDebuggable }
        if (debuggableApps.isNotEmpty()) {
            val deduction = (debuggableApps.size * 5).coerceAtMost(15)
            baseScore -= deduction
            deductions.add(
                DeductionItem(
                    "Debuggable Production Applications",
                    deduction,
                    "${debuggableApps.size} installed third-party apps were compiled with FLAG_DEBUGGABLE active."
                )
            )
        }

        val legacySdkApps = nonSystemApps.filter { it.targetSdk < 28 }
        if (legacySdkApps.isNotEmpty()) {
            val deduction = (legacySdkApps.size * 4).coerceAtMost(12)
            baseScore -= deduction
            deductions.add(
                DeductionItem(
                    "Legacy SDK Apps (< Android 9)",
                    deduction,
                    "${legacySdkApps.size} apps target obsolete Android versions evading runtime privacy protections."
                )
            )
        }

        val finalScore = baseScore.coerceIn(0, 100)

        // Sub-dimension evaluations based on device checks and app risk profiles
        val devCheckDeductions = deductions.filter { it.title.contains("Screen") || it.title.contains("Storage") || it.title.contains("ADB") || it.title.contains("Developer") || it.title.contains("Patch") }.sumOf { it.pointsLost }
        val devScore = (100 - devCheckDeductions * 2).coerceIn(10, 100)

        val appDeductions = deductions.filter { it.title.contains("Risk") || it.title.contains("Legacy") || it.title.contains("Permission") || it.title.contains("Adware") }.sumOf { it.pointsLost }
        val appScore = (100 - appDeductions * 2).coerceIn(15, 100)

        // Network posture sub-score (encryption, VPN availability, open ports)
        val netScore = if (deviceChecks.any { it.id == "screen_lock" && it.severity == FindingSeverity.VERIFIED_SECURE }) 95 else 75

        // Aggregation of counts
        val allSeverities = deviceChecks.map { it.severity } + apps.map { it.riskLevel }
        val criticalCount = allSeverities.count { it == FindingSeverity.CRITICAL }
        val warningCount = allSeverities.count { it == FindingSeverity.WARNING }
        val infoCount = allSeverities.count { it == FindingSeverity.INFORMATIONAL }
        val verifiedCount = allSeverities.count { it == FindingSeverity.VERIFIED_SECURE }

        // Suspicious behavior & background posture
        val behavScore = if (criticalCount > 0) 60 else if (warningCount > 0) 80 else 98

        val summaryText = when {
            finalScore >= 85 -> "Device security posture is optimal. Essential hardware protections and permissions are verified."
            finalScore >= 70 -> "Device security is satisfactory, but actionable hardening recommendations are available."
            finalScore >= 50 -> "Elevated risk detected. Device settings or installed applications require immediate attention."
            else -> "Critical vulnerabilities detected. Device is exposed to local or application-level threats."
        }

        return ScoringResult(
            overallScore = finalScore,
            deviceScore = devScore,
            applicationScore = appScore,
            networkScore = netScore,
            behaviorScore = behavScore,
            criticalCount = criticalCount,
            warningCount = warningCount,
            infoCount = infoCount,
            verifiedCount = verifiedCount,
            totalAppsScanned = apps.size,
            deductions = deductions,
            summaryText = summaryText
        )
    }

    fun toScanSummary(result: ScoringResult): ScanSummary {
        return ScanSummary(
            overallScore = result.overallScore,
            criticalCount = result.criticalCount,
            warningCount = result.warningCount,
            infoCount = result.infoCount,
            verifiedCount = result.verifiedCount,
            totalAppsScanned = result.totalAppsScanned,
            summaryText = result.summaryText
        )
    }
}
