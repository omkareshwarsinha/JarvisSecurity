package com.example.jarvis.domain

import com.example.jarvis.data.model.AppSecurityInfo
import com.example.jarvis.data.model.CheckCategory
import com.example.jarvis.data.model.DeviceCheckItem
import com.example.jarvis.data.model.FindingSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringEngineTest {

    @Test
    fun testCleanBaselineScoreIs100() {
        val result = ScoringEngine.evaluate(emptyList(), emptyList())
        assertEquals(100, result.overallScore)
        assertEquals(100, result.deviceScore)
        assertEquals(100, result.applicationScore)
        assertEquals(0, result.criticalCount)
        assertEquals(0, result.warningCount)
        assertEquals(0, result.deductions.size)
        assertTrue(result.summaryText.contains("optimal", ignoreCase = true))
    }

    @Test
    fun testScreenLockMissingDeduction() {
        val deviceChecks = listOf(
            DeviceCheckItem(
                id = "screen_lock",
                title = "Screen Lock",
                category = CheckCategory.DEVICE_LOCK,
                severity = FindingSeverity.CRITICAL,
                detectedValue = "No Lock",
                description = "No PIN or biometric lock configured",
                technicalExplanation = "isDeviceSecure returned false",
                recommendation = "Configure PIN"
            )
        )

        val result = ScoringEngine.evaluate(deviceChecks, emptyList())
        assertEquals(75, result.overallScore) // 100 - 25
        assertEquals(1, result.criticalCount)
        assertEquals(1, result.deductions.size)
        assertEquals("Screen Lock Missing", result.deductions[0].title)
        assertEquals(25, result.deductions[0].pointsLost)
    }

    @Test
    fun testMultipleDeviceVulnerabilitiesAccumulate() {
        val deviceChecks = listOf(
            DeviceCheckItem(
                id = "screen_lock",
                title = "Screen Lock",
                category = CheckCategory.DEVICE_LOCK,
                severity = FindingSeverity.CRITICAL,
                detectedValue = "None",
                description = "Missing",
                technicalExplanation = "",
                recommendation = ""
            ),
            DeviceCheckItem(
                id = "storage_encryption",
                title = "Storage",
                category = CheckCategory.STORAGE_ENCRYPTION,
                severity = FindingSeverity.CRITICAL,
                detectedValue = "Unencrypted",
                description = "Unencrypted",
                technicalExplanation = "",
                recommendation = ""
            ),
            DeviceCheckItem(
                id = "adb_debugging",
                title = "ADB",
                category = CheckCategory.DEVELOPER_OPTIONS,
                severity = FindingSeverity.WARNING,
                detectedValue = "Enabled",
                description = "ADB on",
                technicalExplanation = "",
                recommendation = ""
            ),
            DeviceCheckItem(
                id = "os_security_patch",
                title = "OS Patch",
                category = CheckCategory.SYSTEM_HYGIENE,
                severity = FindingSeverity.WARNING,
                detectedValue = "Outdated",
                description = "Old patch",
                technicalExplanation = "",
                recommendation = ""
            )
        )

        val result = ScoringEngine.evaluate(deviceChecks, emptyList())
        // 100 - 25 (screen_lock) - 20 (storage) - 10 (adb) - 10 (patch) = 35
        assertEquals(35, result.overallScore)
        assertEquals(2, result.criticalCount)
        assertEquals(2, result.warningCount)
        assertEquals(4, result.deductions.size)
        assertTrue(result.summaryText.contains("Critical", ignoreCase = true))
    }

    @Test
    fun testHighRiskThirdPartyAppsDeduction() {
        val sampleCriticalApp = AppSecurityInfo(
            packageName = "com.suspicious.app",
            appName = "Suspicious App",
            versionName = "1.0",
            targetSdk = 34,
            minSdk = 26,
            isSystemApp = false,
            isDebuggable = false,
            requestedPermissions = listOf("android.permission.RECEIVE_SMS"),
            grantedDangerousPermissions = listOf("android.permission.RECEIVE_SMS"),
            specialPermissions = emptyList(),
            riskScore = 85,
            riskLevel = FindingSeverity.CRITICAL,
            riskFactors = listOf("Sensitive SMS Access")
        )

        val result = ScoringEngine.evaluate(emptyList(), listOf(sampleCriticalApp))
        // 100 - (1 * 8) = 92
        assertEquals(92, result.overallScore)
        assertEquals(1, result.criticalCount)
        assertEquals(1, result.totalAppsScanned)
        assertEquals("High-Risk Third-Party Applications", result.deductions[0].title)
    }

    @Test
    fun testScoreNeverDropsBelowZero() {
        // Build an extreme list of risk items
        val extremeChecks = listOf(
            DeviceCheckItem("screen_lock", "Screen", CheckCategory.DEVICE_LOCK, FindingSeverity.CRITICAL, "", "", "", ""),
            DeviceCheckItem("storage_encryption", "Storage", CheckCategory.STORAGE_ENCRYPTION, FindingSeverity.CRITICAL, "", "", "", ""),
            DeviceCheckItem("adb_debugging", "ADB", CheckCategory.DEVELOPER_OPTIONS, FindingSeverity.WARNING, "", "", "", ""),
            DeviceCheckItem("os_security_patch", "Patch", CheckCategory.SYSTEM_HYGIENE, FindingSeverity.WARNING, "", "", "", "")
        )

        val dangerousApps = (1..10).map { i ->
            AppSecurityInfo(
                packageName = "com.dangerous.app$i",
                appName = "Danger $i",
                versionName = "1.0",
                targetSdk = 21, // legacy (< 28)
                minSdk = 16,
                isSystemApp = false,
                isDebuggable = true, // debuggable
                requestedPermissions = emptyList(),
                grantedDangerousPermissions = emptyList(),
                specialPermissions = emptyList(),
                riskScore = 95,
                riskLevel = FindingSeverity.CRITICAL,
                riskFactors = listOf("Critical Risk")
            )
        }

        val result = ScoringEngine.evaluate(extremeChecks, dangerousApps)
        assertTrue("Score should be non-negative", result.overallScore >= 0)
        assertTrue("Score should not exceed 100", result.overallScore <= 100)
    }

    @Test
    fun testScanSummaryConversion() {
        val result = ScoringEngine.evaluate(emptyList(), emptyList())
        val summary = ScoringEngine.toScanSummary(result)
        assertEquals(100, summary.overallScore)
        assertEquals(0, summary.criticalCount)
        assertEquals(0, summary.warningCount)
        assertEquals(result.summaryText, summary.summaryText)
    }
}
