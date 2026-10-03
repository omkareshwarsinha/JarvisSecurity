package com.example.jarvis.domain

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.jarvis.data.model.FindingSeverity
import com.example.jarvis.data.model.RemediationAction
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AlertCategory(val displayName: String) {
    DEVICE_STATE("Device Posture"),
    PERMISSION_SENTINEL("Permission Abuse"),
    NETWORK_BEHAVIOR("Network & Traffic"),
    SUSPICIOUS_ACTIVITY("Suspicious Activity")
}

data class ExplainableAlert(
    val id: String,
    val title: String,
    val category: AlertCategory,
    val severity: FindingSeverity,
    val explanation: String,
    val threatImpact: String,
    val hardeningAdvice: String,
    val remediationAction: RemediationAction? = null,
    val targetPackageName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false
)

class ContinuousSecurityMonitor(
    private val context: Context,
    private val monitorScope: CoroutineScope = CoroutineScope(
        Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            Log.e("ContinuousSecurityMonitor", "Background monitor encountered non-fatal error", throwable)
        }
    )
) {

    private val _alerts = MutableStateFlow<List<ExplainableAlert>>(emptyList())
    val alerts: StateFlow<List<ExplainableAlert>> = _alerts.asStateFlow()

    private val _isMonitoringActive = MutableStateFlow(true)
    val isMonitoringActive: StateFlow<Boolean> = _isMonitoringActive.asStateFlow()

    private val _lastCheckTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastCheckTimestamp: StateFlow<Long> = _lastCheckTimestamp.asStateFlow()

    private var monitorJob: Job? = null

    init {
        startMonitoring()
    }

    fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        _isMonitoringActive.value = true
        monitorJob = monitorScope.launch {
            while (isActive) {
                try {
                    evaluateSecurityState()
                } catch (e: Throwable) {
                    Log.w("ContinuousSecurityMonitor", "Non-fatal monitoring pass error", e)
                }
                _lastCheckTimestamp.value = System.currentTimeMillis()
                delay(12_000) // sample every 12 seconds
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        _isMonitoringActive.value = false
    }

    fun toggleMonitoring(): Boolean {
        if (_isMonitoringActive.value) {
            stopMonitoring()
            return false
        } else {
            startMonitoring()
            return true
        }
    }

    fun evaluateSecurityState(): List<ExplainableAlert> {
        val detectedAlerts = mutableListOf<ExplainableAlert>()

        try {
            // 1. DEVICE STATE CHECKS
            checkDeviceLockState(detectedAlerts)
            checkUsbDebugging(detectedAlerts)
            checkDeveloperOptions(detectedAlerts)
            checkSecurityPatchFreshness(detectedAlerts)
            checkRootAndSuBinaries(detectedAlerts)
            checkUnknownAppSources(detectedAlerts)

            // 2. NETWORK BEHAVIOR CHECKS
            checkNetworkPosture(detectedAlerts)
            checkPrivateDns(detectedAlerts)

            // 3. PERMISSION & SUSPICIOUS ACTIVITY CHECKS
            checkInstalledPackagesSensitivities(detectedAlerts)
            checkHighMemoryRunawayProcesses(detectedAlerts)
        } catch (e: Throwable) {
            Log.e("ContinuousSecurityMonitor", "Error evaluating security state", e)
        }

        _alerts.value = detectedAlerts
        return detectedAlerts
    }

    private fun checkDeviceLockState(alerts: MutableList<ExplainableAlert>) {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isSecure = km?.isDeviceSecure ?: false
        if (!isSecure) {
            alerts.add(
                ExplainableAlert(
                    id = "ALERT_NO_LOCKSCREEN",
                    title = "Lock Screen Security Not Configured",
                    category = AlertCategory.DEVICE_STATE,
                    severity = FindingSeverity.CRITICAL,
                    explanation = "Your device has no PIN, password, or biometric lock. Anyone with physical access can open your device without restriction.",
                    threatImpact = "Android Keystore hardware-backed keys remain unlocked, app data can be dumped over USB, and local tokens are exposed to anyone holding the phone.",
                    hardeningAdvice = "Set up a strong PIN (at least 6 digits), passphrase, or biometric unlock in Android Security settings immediately.",
                    remediationAction = RemediationAction.OPEN_SECURITY_SETTINGS
                )
            )
        }
    }

    private fun checkUsbDebugging(alerts: MutableList<ExplainableAlert>) {
        val adbEnabled = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (e: Exception) {
            false
        }
        if (adbEnabled) {
            alerts.add(
                ExplainableAlert(
                    id = "ALERT_ADB_ENABLED",
                    title = "USB Debugging Bridge (ADB) Active",
                    category = AlertCategory.DEVICE_STATE,
                    severity = FindingSeverity.WARNING,
                    explanation = "Android Debug Bridge is currently enabled over USB or Wi-Fi.",
                    threatImpact = "Malicious charging kiosks ('juice jacking') or computers connected via USB can install background packages, dump app data, and execute shell commands.",
                    hardeningAdvice = "Turn off USB Debugging in Developer Options when not actively testing or compiling code.",
                    remediationAction = RemediationAction.OPEN_DEVELOPER_SETTINGS
                )
            )
        }
    }

    private fun checkDeveloperOptions(alerts: MutableList<ExplainableAlert>) {
        val devOptions = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        } catch (e: Exception) {
            false
        }
        if (devOptions) {
            alerts.add(
                ExplainableAlert(
                    id = "ALERT_DEV_OPTIONS",
                    title = "Developer Options Enabled",
                    category = AlertCategory.DEVICE_STATE,
                    severity = FindingSeverity.INFORMATIONAL,
                    explanation = "Developer Options menu is unlocked on this device.",
                    threatImpact = "Allows switching advanced flags like mock locations, OEM unlocking, and background process limits.",
                    hardeningAdvice = "If you are a regular user, disable Developer Options to harden Android's sandboxing baseline.",
                    remediationAction = RemediationAction.OPEN_DEVELOPER_SETTINGS
                )
            )
        }
    }

    private fun checkSecurityPatchFreshness(alerts: MutableList<ExplainableAlert>) {
        val patchDate = Build.VERSION.SECURITY_PATCH
        if (!patchDate.isNullOrEmpty()) {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val patch = format.parse(patchDate)
                if (patch != null) {
                    val ageDays = (System.currentTimeMillis() - patch.time) / (1000L * 60 * 60 * 24)
                    if (ageDays > 120) {
                        alerts.add(
                            ExplainableAlert(
                                id = "ALERT_OUTDATED_PATCH",
                                title = "Outdated Security Patch ($ageDays Days Old)",
                                category = AlertCategory.DEVICE_STATE,
                                severity = if (ageDays > 365) FindingSeverity.CRITICAL else FindingSeverity.WARNING,
                                explanation = "Your device security patch level is dated $patchDate, which is over ${ageDays / 30} months old.",
                                threatImpact = "Known Linux kernel CVEs and Android media framework vulnerabilities remain unpatched on this device.",
                                hardeningAdvice = "Check for system OTA updates or apply MobiArmour Legacy Hardening mitigations (Private DNS, disabling 2G, restricting background traffic).",
                                remediationAction = RemediationAction.OPEN_SYSTEM_UPDATE_SETTINGS
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
    }

    private fun checkRootAndSuBinaries(alerts: MutableList<ExplainableAlert>) {
        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su"
        )
        val suFound = suPaths.any { File(it).exists() }
        if (suFound) {
            alerts.add(
                ExplainableAlert(
                    id = "ALERT_SU_BINARY",
                    title = "Superuser Binary Present (Rooted)",
                    category = AlertCategory.DEVICE_STATE,
                    severity = FindingSeverity.CRITICAL,
                    explanation = "A superuser 'su' binary was detected in system partitions. The OS sandboxing guarantees have been bypassed.",
                    threatImpact = "Any application with root permissions can read private databases, bypass Android Keystore encryption, and intercept screen touches.",
                    hardeningAdvice = "Ensure root access is restricted and only granted to audited management utilities.",
                    remediationAction = RemediationAction.OPEN_SECURITY_SETTINGS
                )
            )
        }
    }

    private fun checkUnknownAppSources(alerts: MutableList<ExplainableAlert>) {
        try {
            val pm = context.packageManager
            val packages = try {
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            } catch (e: Throwable) {
                emptyList()
            }
            val canInstallPackages = packages.filter { pkg ->
                pkg.requestedPermissions?.contains("android.permission.REQUEST_INSTALL_PACKAGES") == true
            }
            if (canInstallPackages.isNotEmpty()) {
                alerts.add(
                    ExplainableAlert(
                        id = "unknown_sources",
                        title = "Apps with Installer Authorization",
                        category = AlertCategory.SUSPICIOUS_ACTIVITY,
                        severity = FindingSeverity.WARNING,
                        explanation = "${canInstallPackages.size} installed third-party apps hold REQUEST_INSTALL_PACKAGES authorization to trigger external APK installations.",
                        threatImpact = "Compromised apps with dropper permissions can download secondary payloads without user awareness.",
                        hardeningAdvice = "Review unknown sources permission in Settings > Apps > Special App Access > Install unknown apps.",
                        remediationAction = RemediationAction.OPEN_MANAGE_UNKNOWN_APP_SOURCES
                    )
                )
            }
        } catch (e: Throwable) {
            // Safe guard against restricted environment
        }
    }

    private fun checkNetworkPosture(alerts: MutableList<ExplainableAlert>) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNet = cm?.activeNetwork
            val caps = if (activeNet != null) cm.getNetworkCapabilities(activeNet) else null

            val isVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false
            val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false

            if (!isVpn && isWifi) {
                alerts.add(
                    ExplainableAlert(
                        id = "ALERT_NO_VPN_ACTIVE",
                        title = "Unshielded Network Connection",
                        category = AlertCategory.NETWORK_BEHAVIOR,
                        severity = FindingSeverity.INFORMATIONAL,
                        explanation = "Your Wi-Fi traffic is currently passing directly through the local router without an active firewall or VPN tunnel.",
                        threatImpact = "Unencrypted DNS requests and insecure HTTP metadata can be inspected by network operators, public Wi-Fi hotspots, or upstream routers.",
                        hardeningAdvice = "Turn on the in-built MobiArmour Firewall in the Firewall tab to shield outbound app traffic and track destination hosts.",
                        remediationAction = null
                    )
                )
            }
        } catch (e: Exception) {
            // Ignore network telemetry errors
        }
    }

    private fun checkPrivateDns(alerts: MutableList<ExplainableAlert>) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val privateDnsMode = try {
                    Settings.Global.getString(context.contentResolver, "private_dns_mode")
                } catch (e: Exception) {
                    null
                }
                if (privateDnsMode == "off" || privateDnsMode.isNullOrEmpty()) {
                    alerts.add(
                        ExplainableAlert(
                            id = "ALERT_NO_PRIVATE_DNS",
                            title = "Encrypted Private DNS (DoT) Disabled",
                            category = AlertCategory.NETWORK_BEHAVIOR,
                            severity = FindingSeverity.WARNING,
                            explanation = "Private DNS (DNS-over-TLS) is not enabled in Android Network settings.",
                            threatImpact = "Every domain name your apps and browser request is sent in cleartext (UDP port 53), allowing ISPs and local Wi-Fi eavesdroppers to track your browsing history.",
                            hardeningAdvice = "Open Network Settings -> Private DNS -> Set to 'dns.google' or 'one.one.one.one' (Cloudflare) for encrypted queries.",
                            remediationAction = RemediationAction.OPEN_WIRELESS_SETTINGS
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore settings read errors
        }
    }

    private fun checkInstalledPackagesSensitivities(alerts: MutableList<ExplainableAlert>) {
        try {
            val pm = context.packageManager
            val packages = try {
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            } catch (e: Exception) {
                // In case of TransactionTooLargeException on Android 9
                try {
                    pm.getInstalledPackages(0)
                } catch (e2: Exception) {
                    emptyList()
                }
            }

            var overlayAndAccessibilityCount = 0
            var smsAndInternetCount = 0

            for (pkg in packages) {
                val isSystem = (pkg.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
                if (isSystem || pkg.packageName == context.packageName) continue

                val reqPerms = pkg.requestedPermissions?.toList() ?: emptyList()

                // Check dangerous combination: Accessibility + Overlay (Screen hijack vector)
                val hasAccessibility = reqPerms.contains("android.permission.BIND_ACCESSIBILITY_SERVICE")
                val hasOverlay = reqPerms.contains("android.permission.SYSTEM_ALERT_WINDOW")

                if (hasAccessibility && hasOverlay) {
                    overlayAndAccessibilityCount++
                    val appLabel = try {
                        pkg.applicationInfo?.loadLabel(pm)?.toString() ?: pkg.packageName
                    } catch (e: Exception) {
                        pkg.packageName
                    }
                    alerts.add(
                        ExplainableAlert(
                            id = "ALERT_COMBO_${pkg.packageName}",
                            title = "High-Risk Vector: $appLabel",
                            category = AlertCategory.PERMISSION_SENTINEL,
                            severity = FindingSeverity.CRITICAL,
                            explanation = "'$appLabel' requests both Accessibility and System Overlay permissions simultaneously.",
                            threatImpact = "This combination allows applications to draw invisible windows over banking or login apps (Tapjacking/Cloaking) and intercept typed keystrokes or PINs.",
                            hardeningAdvice = "Audit '$appLabel'. If this is not an essential accessibility utility, revoke its permissions in Application Settings.",
                            remediationAction = RemediationAction.OPEN_APPLICATION_SETTINGS,
                            targetPackageName = pkg.packageName
                        )
                    )
                }

                // Check SMS + Internet (OTP stealer vector)
                val hasSms = reqPerms.any { it.contains("SMS") || it.contains("RECEIVE_MMS") }
                val hasNet = reqPerms.contains("android.permission.INTERNET")
                if (hasSms && hasNet) {
                    smsAndInternetCount++
                }
            }

            if (smsAndInternetCount > 5) {
                alerts.add(
                    ExplainableAlert(
                        id = "ALERT_MANY_SMS_APPS",
                        title = "Multiple Apps Requesting SMS & Internet ($smsAndInternetCount Apps)",
                        category = AlertCategory.PERMISSION_SENTINEL,
                        severity = FindingSeverity.WARNING,
                        explanation = "$smsAndInternetCount installed third-party apps have requested both SMS access and Internet connectivity.",
                        threatImpact = "Applications with SMS and Internet permissions can intercept one-time two-factor authentication codes and transmit them to external servers.",
                        hardeningAdvice = "Review granted SMS permissions in Privacy -> Permission Manager -> SMS, and grant access only to your default messaging app.",
                        remediationAction = RemediationAction.OPEN_APPLICATION_SETTINGS
                    )
                )
            }
        } catch (e: Exception) {
            // Protect against any IPC or package manager crash
        }
    }

    private fun checkHighMemoryRunawayProcesses(alerts: MutableList<ExplainableAlert>) {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)

            if (memInfo.totalMem > 0) {
                val usedPercent = ((memInfo.totalMem - memInfo.availMem).toDouble() / memInfo.totalMem.toDouble()) * 100
                if (!usedPercent.isNaN() && (usedPercent > 88.0 || memInfo.lowMemory)) {
                    alerts.add(
                        ExplainableAlert(
                            id = "ALERT_MEMORY_PRESSURE",
                            title = "High RAM Pressure (${usedPercent.toInt()}% Used)",
                            category = AlertCategory.SUSPICIOUS_ACTIVITY,
                            severity = FindingSeverity.WARNING,
                            explanation = "Available RAM is critically low. Background processes are competing for heap allocations.",
                            threatImpact = "Causes severe gaming frame drops, UI lag, and unexpected terminations of foreground security services.",
                            hardeningAdvice = "Use JARVIS Turbo RAM Optimizer in the Performance tab to flush standby process heaps and reclaim memory.",
                            remediationAction = null
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore memory telemetry errors
        }
    }
}
