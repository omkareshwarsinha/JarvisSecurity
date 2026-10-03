package com.example.jarvis.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jarvis.data.db.entities.FirewallRuleEntity
import com.example.jarvis.data.db.entities.NetworkTrafficEntity
import com.example.jarvis.data.db.entities.PhishingHistoryEntity
import com.example.jarvis.data.db.entities.SecurityEventEntity
import com.example.jarvis.data.model.AppSecurityInfo
import com.example.jarvis.data.model.DeviceCheckItem
import com.example.jarvis.data.model.PermissionRiskInfo
import com.example.jarvis.data.model.RemediationAction
import com.example.jarvis.data.model.ScanSummary
import com.example.jarvis.data.repository.SecurityRepository
import com.example.jarvis.domain.AlertCategory
import com.example.jarvis.domain.ExplainableAlert
import com.example.jarvis.domain.PerformanceOptimizer
import com.example.jarvis.domain.PhishingDetector
import com.example.jarvis.domain.ScoringEngine
import com.example.jarvis.domain.SystemLogReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JarvisMainViewModel(
    private val repository: SecurityRepository
) : ViewModel() {

    // Current navigation route
    private val _currentRoute = MutableStateFlow("dashboard")
    val currentRoute: StateFlow<String> = _currentRoute.asStateFlow()

    // Light / Dark Theme State (Glassmorphic)
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    // Scan Execution State
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _scanStepMessage = MutableStateFlow("Scanner Ready")
    val scanStepMessage: StateFlow<String> = _scanStepMessage.asStateFlow()

    // Data Collections
    private val _deviceChecks = MutableStateFlow<List<DeviceCheckItem>>(emptyList())
    val deviceChecks: StateFlow<List<DeviceCheckItem>> = _deviceChecks.asStateFlow()

    private val _scannedApps = MutableStateFlow<List<AppSecurityInfo>>(emptyList())
    val scannedApps: StateFlow<List<AppSecurityInfo>> = _scannedApps.asStateFlow()

    private val _permissionMatrix = MutableStateFlow<List<PermissionRiskInfo>>(emptyList())
    val permissionMatrix: StateFlow<List<PermissionRiskInfo>> = _permissionMatrix.asStateFlow()

    private val _deductions = MutableStateFlow<List<ScoringEngine.DeductionItem>>(emptyList())
    val deductions: StateFlow<List<ScoringEngine.DeductionItem>> = _deductions.asStateFlow()

    // 4 Dimension Security Scores for MobiArmour
    private val _deviceScore = MutableStateFlow(95)
    val deviceScore: StateFlow<Int> = _deviceScore.asStateFlow()

    private val _applicationScore = MutableStateFlow(90)
    val applicationScore: StateFlow<Int> = _applicationScore.asStateFlow()

    private val _networkScore = MutableStateFlow(95)
    val networkScore: StateFlow<Int> = _networkScore.asStateFlow()

    private val _behaviorScore = MutableStateFlow(98)
    val behaviorScore: StateFlow<Int> = _behaviorScore.asStateFlow()

    // Lost Device Manager State (Layer 6)
    val isAlarmSounding: StateFlow<Boolean> = repository.lostDeviceManager.isAlarmSounding
    val isDeviceAdminActive: StateFlow<Boolean> = repository.lostDeviceManager.isDeviceAdminActive

    // Database flows
    val latestScan: StateFlow<ScanSummary?> = repository.latestScan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val securityEvents: StateFlow<List<SecurityEventEntity>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val firewallRules: StateFlow<List<FirewallRuleEntity>> = repository.firewallRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTraffic: StateFlow<List<NetworkTrafficEntity>> = repository.recentTraffic
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val phishingHistory: StateFlow<List<PhishingHistoryEntity>> = repository.phishingHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter states for Apps Screen
    private val _appSearchQuery = MutableStateFlow("")
    val appSearchQuery: StateFlow<String> = _appSearchQuery.asStateFlow()

    private val _appFilterType = MutableStateFlow("ALL") // ALL, HIGH_RISK, USER, SYSTEM, ADWARE, PERSONAL_DEV
    val appFilterType: StateFlow<String> = _appFilterType.asStateFlow()

    // Selected app for deep code inspection modal/drawer
    private val _selectedAppForInspection = MutableStateFlow<AppSecurityInfo?>(null)
    val selectedAppForInspection: StateFlow<AppSecurityInfo?> = _selectedAppForInspection.asStateFlow()

    // Firewall State
    private val _isFirewallActive = MutableStateFlow<Boolean>(repository.firewallManager.isFirewallActive())
    val isFirewallActive: StateFlow<Boolean> = _isFirewallActive.asStateFlow()

    private val _isGlobalKillSwitch = MutableStateFlow<Boolean>(repository.firewallManager.isGlobalKillSwitchActive())
    val isGlobalKillSwitch: StateFlow<Boolean> = _isGlobalKillSwitch.asStateFlow()

    // Trusted Devices for Remote SMS Lockdown
    val trustedDevices: StateFlow<List<com.example.jarvis.data.db.entities.TrustedDeviceEntity>> = repository.trustedDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Performance & Game Boost State
    private val _memoryStatus = MutableStateFlow<PerformanceOptimizer.MemoryStatus>(repository.performanceOptimizer.getMemoryStatus())
    val memoryStatus: StateFlow<PerformanceOptimizer.MemoryStatus> = _memoryStatus.asStateFlow()

    private val _isBoosting = MutableStateFlow(false)
    val isBoosting: StateFlow<Boolean> = _isBoosting.asStateFlow()

    private val _lastBoostResult = MutableStateFlow<PerformanceOptimizer.BoostResult?>(null)
    val lastBoostResult: StateFlow<PerformanceOptimizer.BoostResult?> = _lastBoostResult.asStateFlow()

    val gamingTips: List<PerformanceOptimizer.GamingTuningTip> = repository.performanceOptimizer.getGamingOptimizationTips()

    // Phishing Guard State
    private val _phishingInputUrl = MutableStateFlow("")
    val phishingInputUrl: StateFlow<String> = _phishingInputUrl.asStateFlow()

    private val _phishingEvaluationResult = MutableStateFlow<PhishingDetector.PhishingEvaluation?>(null)
    val phishingEvaluationResult: StateFlow<PhishingDetector.PhishingEvaluation?> = _phishingEvaluationResult.asStateFlow()

    // System Log Reader State
    private val _isSystemLoggingEnabled = MutableStateFlow(true)
    val isSystemLoggingEnabled: StateFlow<Boolean> = _isSystemLoggingEnabled.asStateFlow()

    private val _systemLogs = MutableStateFlow<List<SystemLogReader.LogEntry>>(emptyList())
    val systemLogs: StateFlow<List<SystemLogReader.LogEntry>> = _systemLogs.asStateFlow()

    private val _logFilterLevel = MutableStateFlow("ALL")
    val logFilterLevel: StateFlow<String> = _logFilterLevel.asStateFlow()

    private val _logSearchQuery = MutableStateFlow("")
    val logSearchQuery: StateFlow<String> = _logSearchQuery.asStateFlow()

    // Continuous Security Layer State
    val continuousAlerts: StateFlow<List<ExplainableAlert>> = repository.continuousMonitor.alerts
    val isMonitoringActive: StateFlow<Boolean> = repository.continuousMonitor.isMonitoringActive
    val lastMonitorTimestamp: StateFlow<Long> = repository.continuousMonitor.lastCheckTimestamp

    init {
        // Initial automatic startup and continuous layer activation
        viewModelScope.launch {
            repository.initializeStartupDataIfEmpty()
            refreshCachedData()
            refreshMemoryStatus()
            refreshSystemLogs()
            refreshContinuousMonitor()
        }
    }

    fun toggleContinuousMonitor() {
        repository.continuousMonitor.toggleMonitoring()
    }

    fun refreshContinuousMonitor() {
        repository.continuousMonitor.evaluateSecurityState()
    }

    private val navigationBackStack = mutableListOf<String>()

    fun navigateTo(route: String) {
        if (_currentRoute.value != route) {
            navigationBackStack.add(_currentRoute.value)
            _currentRoute.value = route
        }
    }

    fun navigateBack(): Boolean {
        if (navigationBackStack.isNotEmpty()) {
            val previousRoute = navigationBackStack.removeAt(navigationBackStack.lastIndex)
            _currentRoute.value = previousRoute
            return true
        } else if (_currentRoute.value != "dashboard") {
            _currentRoute.value = "dashboard"
            return true
        }
        return false
    }

    fun canNavigateBack(): Boolean {
        return navigationBackStack.isNotEmpty() || _currentRoute.value != "dashboard"
    }

    fun setAppSearchQuery(query: String) {
        _appSearchQuery.value = query
    }

    fun setAppFilterType(type: String) {
        _appFilterType.value = type
    }

    fun selectAppForInspection(app: AppSecurityInfo?) {
        _selectedAppForInspection.value = app
    }

    fun refreshCachedData() {
        viewModelScope.launch {
            _deviceChecks.value = repository.getDeviceChecks()
            _scannedApps.value = repository.getScannedApps()
            _permissionMatrix.value = repository.getPermissionMatrix()

            val cachedScoring = repository.getCachedScoringResult()
            if (cachedScoring != null) {
                _deductions.value = cachedScoring.deductions
                _deviceScore.value = cachedScoring.deviceScore
                _applicationScore.value = cachedScoring.applicationScore
                _networkScore.value = cachedScoring.networkScore
                _behaviorScore.value = cachedScoring.behaviorScore
            }
        }
    }

    fun triggerScan() {
        if (_isScanning.value) return

        viewModelScope.launch {
            _isScanning.value = true
            _scanProgress.value = 0f
            _scanStepMessage.value = "Initializing Scanner..."

            val result = repository.performFullScan { step, progress ->
                _scanStepMessage.value = step
                _scanProgress.value = progress
            }

            _deviceChecks.value = repository.getDeviceChecks()
            _scannedApps.value = repository.getScannedApps()
            _permissionMatrix.value = repository.getPermissionMatrix()
            _deductions.value = result.deductions
            _deviceScore.value = result.deviceScore
            _applicationScore.value = result.applicationScore
            _networkScore.value = result.networkScore
            _behaviorScore.value = result.behaviorScore

            _isScanning.value = false
        }
    }

    // Trust/Whitelist a personal project or assistant app
    fun toggleTrustApp(app: AppSecurityInfo, isTrusted: Boolean) {
        viewModelScope.launch {
            repository.toggleAppTrusted(app.packageName, app.appName, isTrusted)
            repository.logUserEvent(
                title = if (isTrusted) "App Marked as Trusted" else "App Trust Revoked",
                category = "TRUST_MANAGER",
                severity = "INFO",
                description = "${app.appName} (${app.packageName}) trust state updated: $isTrusted",
                source = "App Inspector"
            )
            refreshCachedData()
            // Update selected app if open
            _selectedAppForInspection.value = _selectedAppForInspection.value?.copy(
                isTrustedByUser = isTrusted,
                riskScore = if (isTrusted) 0 else _selectedAppForInspection.value?.riskScore ?: 0
            )
        }
    }

    // Firewall Controls
    fun getVpnPrepareIntent(): Intent? {
        return repository.firewallManager.getVpnPrepareIntent()
    }

    fun startFirewall() {
        repository.firewallManager.startFirewall()
        _isFirewallActive.value = true
        viewModelScope.launch {
            repository.logUserEvent(
                title = "Firewall Activated",
                category = "FIREWALL",
                severity = "SUCCESS",
                description = "MobiArmour Firewall active. Zero-trust selective filtering enabled.",
                source = "Firewall Controller"
            )
        }
    }

    fun stopFirewall() {
        repository.firewallManager.stopFirewall()
        _isFirewallActive.value = false
        viewModelScope.launch {
            repository.logUserEvent(
                title = "Firewall Disabled",
                category = "FIREWALL",
                severity = "WARNING",
                description = "MobiArmour Firewall stopped by user.",
                source = "Firewall Controller"
            )
        }
    }

    fun toggleFirewall() {
        if (_isFirewallActive.value) {
            stopFirewall()
        } else {
            startFirewall()
        }
    }

    fun setFirewallActiveState(active: Boolean) {
        _isFirewallActive.value = active
    }

    fun handlePackageRemoved(packageName: String) {
        viewModelScope.launch {
            val appName = _scannedApps.value.find { it.packageName == packageName }?.appName ?: packageName
            _scannedApps.value = _scannedApps.value.filter { it.packageName != packageName }
            if (_selectedAppForInspection.value?.packageName == packageName) {
                _selectedAppForInspection.value = null
            }
            repository.logUserEvent(
                title = "Threat / App Purged",
                category = "APP_AUDIT",
                severity = "SUCCESS",
                description = "$appName ($packageName) was completely removed from the system.",
                source = "Package Monitor"
            )
            refreshCachedData()
        }
    }

    fun onAppResumed(context: android.content.Context) {
        viewModelScope.launch {
            val pm = context.packageManager
            val currentList = _scannedApps.value
            if (currentList.isNotEmpty()) {
                val removed = currentList.filter { app ->
                    try {
                        pm.getPackageInfo(app.packageName, 0)
                        false
                    } catch (e: Exception) {
                        true // No longer installed!
                    }
                }
                for (rem in removed) {
                    handlePackageRemoved(rem.packageName)
                }
            }
        }
    }

    fun toggleAppBlocked(packageName: String, appName: String, isBlocked: Boolean) {
        viewModelScope.launch {
            repository.firewallManager.setAppBlocked(packageName, appName, isBlocked)
            repository.logUserEvent(
                title = if (isBlocked) "App Traffic Blocked" else "App Traffic Allowed",
                category = "FIREWALL",
                severity = "INFO",
                description = "Firewall rule updated for $appName ($packageName). Blocked: $isBlocked",
                source = "Firewall Rules"
            )
        }
    }

    fun blockAllBackgroundApps() {
        viewModelScope.launch {
            val apps = _scannedApps.value.map { it.packageName to it.appName }
            repository.firewallManager.blockAllBackgroundApps(apps)
            repository.logUserEvent(
                title = "All Background Apps Blocked",
                category = "FIREWALL",
                severity = "WARNING",
                description = "Enforced strict zero-trust background firewall isolation.",
                source = "Firewall Presets"
            )
        }
    }

    fun unblockAllApps() {
        viewModelScope.launch {
            repository.firewallManager.unblockAllApps()
        }
    }

    // Performance / Gaming RAM Boost
    fun refreshMemoryStatus() {
        _memoryStatus.value = repository.performanceOptimizer.getMemoryStatus()
    }

    fun triggerGameBoost() {
        if (_isBoosting.value) return
        viewModelScope.launch {
            _isBoosting.value = true
            val boost = repository.performanceOptimizer.optimizeForGaming()
            _lastBoostResult.value = boost
            refreshMemoryStatus()
            _isBoosting.value = false

            repository.logUserEvent(
                title = "Gaming Performance Boost Applied",
                category = "PERFORMANCE",
                severity = "SUCCESS",
                description = "Freed ${boost.freedMemMb} MB of RAM across ${boost.killedProcessesCount} background processes.",
                source = "Performance Engine"
            )
        }
    }

    // Phishing URL Inspector
    fun setPhishingInputUrl(url: String) {
        _phishingInputUrl.value = url
    }

    fun evaluateUrl(url: String) {
        viewModelScope.launch {
            val eval = repository.phishingDetector.evaluateUrl(url)
            _phishingEvaluationResult.value = eval
            repository.logUserEvent(
                title = if (eval.isPhishing) "Phishing Threat Evaluated" else "URL Evaluated Safe",
                category = "PHISHING_SHIELD",
                severity = if (eval.isPhishing) "CRITICAL" else "INFO",
                description = "${eval.domain} (Score: ${eval.riskScore}%). Threat: ${eval.threatReason}",
                source = "Phishing URL Inspector"
            )
        }
    }

    // System Logs
    fun toggleSystemLogging(enabled: Boolean) {
        _isSystemLoggingEnabled.value = enabled
        if (enabled) {
            refreshSystemLogs()
        }
    }

    fun setLogFilterLevel(level: String) {
        _logFilterLevel.value = level
        refreshSystemLogs()
    }

    fun setLogSearchQuery(query: String) {
        _logSearchQuery.value = query
        refreshSystemLogs()
    }

    fun refreshSystemLogs() {
        viewModelScope.launch {
            if (_isSystemLoggingEnabled.value) {
                _systemLogs.value = repository.systemLogReader.readSystemLogs(
                    filterLevel = _logFilterLevel.value,
                    searchQuery = _logSearchQuery.value
                )
            }
        }
    }

    fun launchRemediation(action: RemediationAction) {
        viewModelScope.launch {
            repository.logUserEvent(
                title = "Hardening Action Triggered",
                category = "HARDENING",
                severity = "INFO",
                description = "User opened system settings for: ${action.buttonLabel}",
                source = "Hardening Center"
            )
            repository.launchRemediationIntent(action)
        }
    }

    fun openAppDetails(packageName: String) {
        viewModelScope.launch {
            repository.logUserEvent(
                title = "App Inspector Opened",
                category = "APP_ANALYSIS",
                severity = "INFO",
                description = "Inspecting package settings for: $packageName",
                source = "Apps Analyzer"
            )
            repository.openAppDetailsSettings(packageName)
        }
    }

    fun purgeAuditHistory() {
        viewModelScope.launch {
            repository.clearAllAuditHistory()
            _deviceChecks.value = emptyList()
            _scannedApps.value = emptyList()
            _permissionMatrix.value = emptyList()
            _deductions.value = emptyList()
            repository.logUserEvent(
                title = "Audit History Purged",
                category = "PRIVACY",
                severity = "INFO",
                description = "User wiped local database logs and cache.",
                source = "Privacy Controls"
            )
        }
    }

    // Lost Device Manager Methods (Layer 6)
    fun triggerEmergencyAlarm() {
        repository.lostDeviceManager.startEmergencyAlarm()
        viewModelScope.launch {
            repository.logUserEvent(
                title = "Emergency Siren Activated",
                category = "RESPONSE",
                severity = "CRITICAL",
                description = "High-decibel audible locating alarm triggered.",
                source = "Lost Device Manager"
            )
        }
    }

    fun stopEmergencyAlarm() {
        repository.lostDeviceManager.stopEmergencyAlarm()
        viewModelScope.launch {
            repository.logUserEvent(
                title = "Emergency Siren Silenced",
                category = "RESPONSE",
                severity = "INFO",
                description = "Audible locating alarm silenced.",
                source = "Lost Device Manager"
            )
        }
    }

    fun triggerInstantLock(): Boolean {
        val success = repository.lostDeviceManager.triggerInstantScreenLock()
        viewModelScope.launch {
            repository.logUserEvent(
                title = if (success) "Instant Screen Lock Executed" else "Instant Screen Lock Failed",
                category = "RESPONSE",
                severity = if (success) "WARNING" else "ERROR",
                description = if (success) "Device screen locked via Device Administration policy." else "Failed: Device Admin policy not active.",
                source = "Lost Device Manager"
            )
        }
        return success
    }

    fun refreshDeviceAdminStatus() {
        repository.lostDeviceManager.refreshAdminStatus()
    }

    fun toggleGlobalKillSwitch() {
        val newState = !_isGlobalKillSwitch.value
        _isGlobalKillSwitch.value = newState
        repository.firewallManager.setGlobalKillSwitch(newState)
        _isFirewallActive.value = repository.firewallManager.isFirewallActive()
        viewModelScope.launch {
            repository.logUserEvent(
                title = if (newState) "Global Network Air-Gap Activated" else "Global Network Air-Gap Restored",
                category = "FIREWALL",
                severity = if (newState) "WARNING" else "INFO",
                description = if (newState) "Total device internet traffic dropped & air-gapped." else "Standard per-app firewall filtering restored.",
                source = "Global Kill-Switch"
            )
        }
    }

    // Trusted Devices Management
    fun addTrustedDevice(phoneNumber: String, contactName: String) {
        viewModelScope.launch {
            repository.addTrustedDevice(phoneNumber, contactName)
            repository.logUserEvent(
                title = "Trusted Device Enrolled",
                category = "RESPONSE",
                severity = "INFO",
                description = "Enrolled $contactName ($phoneNumber) for remote SMS lockdown.",
                source = "Lost Device Manager"
            )
        }
    }

    fun deleteTrustedDevice(device: com.example.jarvis.data.db.entities.TrustedDeviceEntity) {
        viewModelScope.launch {
            repository.deleteTrustedDevice(device)
            repository.logUserEvent(
                title = "Trusted Device Removed",
                category = "RESPONSE",
                severity = "INFO",
                description = "Revoked remote control authorization for ${device.contactName}.",
                source = "Lost Device Manager"
            )
        }
    }

    fun toggleTrustedDevice(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleTrustedDevice(id, enabled)
        }
    }

    fun simulateRemoteLockdown() {
        triggerEmergencyAlarm()
        triggerInstantLock()
    }

    fun getDeviceAdminActivationIntent() = repository.lostDeviceManager.getDeviceAdminActivationIntent()
    fun getFindMyDeviceIntent() = repository.lostDeviceManager.getFindMyDeviceIntent()

    class Factory(private val repository: SecurityRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return JarvisMainViewModel(repository) as T
        }
    }
}
