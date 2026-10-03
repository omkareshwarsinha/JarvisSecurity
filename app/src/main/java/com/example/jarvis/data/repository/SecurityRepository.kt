package com.example.jarvis.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.example.jarvis.data.db.JarvisDatabase
import com.example.jarvis.data.db.entities.ScanResultEntity
import com.example.jarvis.data.db.entities.SecurityEventEntity
import com.example.jarvis.data.model.AppSecurityInfo
import com.example.jarvis.data.model.DeviceCheckItem
import com.example.jarvis.data.model.FindingSeverity
import com.example.jarvis.data.model.PermissionRiskInfo
import com.example.jarvis.data.model.RemediationAction
import com.example.jarvis.data.model.ScanSummary
import com.example.jarvis.data.firewall.FirewallManager
import com.example.jarvis.domain.ContinuousSecurityMonitor
import com.example.jarvis.domain.DeviceAuditor
import com.example.jarvis.domain.ExplainableAlert
import com.example.jarvis.domain.PackageAuditor
import com.example.jarvis.domain.PerformanceOptimizer
import com.example.jarvis.domain.PermissionCatalog
import com.example.jarvis.domain.PhishingDetector
import com.example.jarvis.domain.ScoringEngine
import com.example.jarvis.domain.SystemLogReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SecurityRepository(
    private val context: Context,
    private val database: JarvisDatabase = JarvisDatabase.getInstance(context),
    private val deviceAuditor: DeviceAuditor = DeviceAuditor(context),
    private val packageAuditor: PackageAuditor = PackageAuditor(context)
) {

    private val scanResultDao = database.scanResultDao()
    private val securityEventDao = database.securityEventDao()
    private val trustedAppDao = database.trustedAppDao()
    private val phishingHistoryDao = database.phishingHistoryDao()
    private val trustedDeviceDao = database.trustedDeviceDao()

    val firewallManager = FirewallManager(context)
    val performanceOptimizer = PerformanceOptimizer(context)
    val phishingDetector = PhishingDetector()
    val systemLogReader = SystemLogReader(context)
    val continuousMonitor = ContinuousSecurityMonitor(context)
    val lostDeviceManager = com.example.jarvis.domain.LostDeviceManager(context)

    val trustedDevices: Flow<List<com.example.jarvis.data.db.entities.TrustedDeviceEntity>> = trustedDeviceDao.getAllDevicesFlow()

    suspend fun addTrustedDevice(phoneNumber: String, contactName: String) = withContext(Dispatchers.IO) {
        trustedDeviceDao.insert(
            com.example.jarvis.data.db.entities.TrustedDeviceEntity(
                phoneNumber = phoneNumber,
                contactName = contactName,
                isEnabled = true
            )
        )
    }

    suspend fun deleteTrustedDevice(device: com.example.jarvis.data.db.entities.TrustedDeviceEntity) = withContext(Dispatchers.IO) {
        trustedDeviceDao.delete(device)
    }

    suspend fun toggleTrustedDevice(id: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        trustedDeviceDao.updateEnabled(id, enabled)
    }

    // In-memory cache of latest execution
    @Volatile
    private var cachedDeviceChecks: List<DeviceCheckItem> = emptyList()

    @Volatile
    private var cachedApps: List<AppSecurityInfo> = emptyList()

    @Volatile
    private var cachedScoringResult: ScoringEngine.ScoringResult? = null

    val latestScan: Flow<ScanSummary?> = scanResultDao.getLatestScan().map { entity ->
        entity?.let {
            ScanSummary(
                id = it.id,
                timestamp = it.timestamp,
                overallScore = it.overallScore,
                criticalCount = it.criticalCount,
                warningCount = it.warningCount,
                infoCount = it.infoCount,
                verifiedCount = it.verifiedCount,
                totalAppsScanned = it.totalAppsScanned,
                summaryText = it.summaryText
            )
        }
    }

    val allEvents: Flow<List<SecurityEventEntity>> = securityEventDao.getAllEvents()
    val firewallRules = firewallManager.allRules
    val recentTraffic = firewallManager.recentTraffic
    val phishingHistory = phishingHistoryDao.getRecentHistory()

    fun getDeviceChecks(): List<DeviceCheckItem> {
        if (cachedDeviceChecks.isEmpty()) {
            cachedDeviceChecks = deviceAuditor.runAllDeviceChecks()
        }
        return cachedDeviceChecks
    }

    suspend fun getScannedApps(): List<AppSecurityInfo> {
        if (cachedApps.isEmpty()) {
            cachedApps = packageAuditor.auditInstalledApplications()
        }
        return cachedApps
    }

    fun getCachedScoringResult(): ScoringEngine.ScoringResult? = cachedScoringResult

    suspend fun toggleAppTrusted(packageName: String, appName: String, isTrusted: Boolean) {
        if (isTrusted) {
            trustedAppDao.setTrusted(
                com.example.jarvis.data.db.entities.TrustedAppEntity(
                    packageName = packageName,
                    appName = appName,
                    isTrusted = true
                )
            )
        } else {
            trustedAppDao.removeTrusted(packageName)
        }
        // Invalidate and re-scan apps
        cachedApps = packageAuditor.auditInstalledApplications()
        cachedScoringResult = ScoringEngine.evaluate(getDeviceChecks(), cachedApps)
    }

    suspend fun performFullScan(
        onProgress: (step: String, progress: Float) -> Unit = { _, _ -> }
    ): ScoringEngine.ScoringResult = withContext(Dispatchers.Default) {
        // Step 1: Initializing scanner
        onProgress("Initializing JARVIS Security Engine...", 0.1f)

        // Step 2: Querying platform & hardware security checks
        onProgress("Auditing Hardware Security, Keyguard & OS Hygiene...", 0.25f)
        val checks = deviceAuditor.runAllDeviceChecks()
        cachedDeviceChecks = checks

        // Step 3: Enumerating applications and permissions
        onProgress("Analyzing Installed Applications & Permissions...", 0.6f)
        val apps = packageAuditor.auditInstalledApplications()
        cachedApps = apps

        // Step 4: Computing threat model and risk scoring
        onProgress("Evaluating Risk Scores & Correlating Findings...", 0.85f)
        val scoringResult = ScoringEngine.evaluate(checks, apps)
        cachedScoringResult = scoringResult

        // Step 5: Persisting scan results and logging event
        onProgress("Finalizing & Storing Audit Report...", 0.95f)
        val scanEntity = ScanResultEntity(
            timestamp = System.currentTimeMillis(),
            overallScore = scoringResult.overallScore,
            criticalCount = scoringResult.criticalCount,
            warningCount = scoringResult.warningCount,
            infoCount = scoringResult.infoCount,
            verifiedCount = scoringResult.verifiedCount,
            totalAppsScanned = scoringResult.totalAppsScanned,
            summaryText = scoringResult.summaryText
        )
        scanResultDao.insertScan(scanEntity)

        val severityStr = when {
            scoringResult.overallScore >= 80 -> "SUCCESS"
            scoringResult.overallScore >= 50 -> "WARNING"
            else -> "CRITICAL"
        }

        securityEventDao.insertEvent(
            SecurityEventEntity(
                title = "Device Security Scan Completed",
                category = "AUDIT",
                severity = severityStr,
                description = "Score: ${scoringResult.overallScore}% • Critical: ${scoringResult.criticalCount} • Warnings: ${scoringResult.warningCount}",
                source = "JARVIS Scanner",
                details = scoringResult.summaryText
            )
        )

        onProgress("Scan Complete", 1.0f)
        scoringResult
    }

    suspend fun getPermissionMatrix(): List<PermissionRiskInfo> {
        val apps = getScannedApps()
        val catalogItems = PermissionCatalog.getAllCatalogItems()

        return catalogItems.map { detail ->
            val holdingApps = apps.filter { app ->
                app.requestedPermissions.contains(detail.permission) ||
                        app.grantedDangerousPermissions.contains(detail.permission)
            }.map { it.appName }

            PermissionRiskInfo(
                permission = detail.permission,
                readableName = detail.readableName,
                category = detail.category,
                riskLevel = detail.defaultSeverity,
                implication = detail.implication,
                recommendation = detail.recommendation,
                grantedAppsCount = holdingApps.size,
                holdingApps = holdingApps
            )
        }.sortedByDescending { it.riskLevel.weight * 100 + it.grantedAppsCount }
    }

    suspend fun logUserEvent(
        title: String,
        category: String,
        severity: String,
        description: String,
        source: String = "User Action",
        details: String = ""
    ) {
        withContext(Dispatchers.IO) {
            securityEventDao.insertEvent(
                SecurityEventEntity(
                    title = title,
                    category = category,
                    severity = severity,
                    description = description,
                    source = source,
                    details = details
                )
            )
        }
    }

    suspend fun clearAllAuditHistory() = withContext(Dispatchers.IO) {
        scanResultDao.clearAllScans()
        securityEventDao.clearAllEvents()
        cachedDeviceChecks = emptyList()
        cachedApps = emptyList()
        cachedScoringResult = null
    }

    fun launchRemediationIntent(action: RemediationAction): Boolean {
        return try {
            val intent = Intent(action.intentAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                // Fallback to main settings
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun openAppDetailsSettings(packageName: String): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun initializeStartupDataIfEmpty() = withContext(Dispatchers.IO) {
        try {
            // 1. Audit apps to seed firewall rules
            val apps = getScannedApps()
            val appPairs = apps.map { it.packageName to it.appName }
            firewallManager.seedInstalledAppRulesIfEmpty(appPairs)

            // 2. Seed initial traffic if empty
            firewallManager.seedInitialTrafficIfEmpty()

            // 3. Trigger continuous monitor initial evaluation
            continuousMonitor.evaluateSecurityState()

            // 4. If no scan exists in database, run initial full scan automatically
            val existingScan = scanResultDao.getLatestScanOnce()
            if (existingScan == null) {
                performFullScan()
            }
        } catch (e: Exception) {
            // Failsafe during startup
        }
    }
}
