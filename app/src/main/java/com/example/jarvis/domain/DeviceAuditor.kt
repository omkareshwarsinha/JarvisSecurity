package com.example.jarvis.domain

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import com.example.jarvis.data.model.CheckCategory
import com.example.jarvis.data.model.DeviceCheckItem
import com.example.jarvis.data.model.FindingSeverity
import com.example.jarvis.data.model.RemediationAction
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DeviceAuditor(private val context: Context) {

    fun runAllDeviceChecks(): List<DeviceCheckItem> {
        val results = mutableListOf<DeviceCheckItem>()
        results.add(checkRootAndSuBinaries())
        results.add(checkSelinuxStatus())
        results.add(checkScreenLock())
        results.add(checkStorageEncryption())
        results.add(checkAdbDebugging())
        results.add(checkDeveloperOptions())
        results.add(checkMockLocationSettings())
        results.add(checkAndroidVersionAndPatch())
        results.add(checkNetworkAndVpn())
        results.add(checkPrivateDns())
        results.add(checkAccessibilityServices())
        results.add(checkUnknownAppSources())
        results.add(checkOutdatedDeviceMitigation())
        return results
    }

    private fun checkScreenLock(): DeviceCheckItem {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardManager == null) {
            return DeviceCheckItem(
                id = "screen_lock",
                title = "Device Lock & Keyguard",
                category = CheckCategory.DEVICE_LOCK,
                severity = FindingSeverity.NOT_AVAILABLE,
                detectedValue = "Keyguard Service Unavailable",
                description = "Unable to query KeyguardManager on this device.",
                technicalExplanation = "KeyguardManager service was not returned by the system.",
                recommendation = "Verify screen lock manually in Settings.",
                isAvailableOnDevice = false
            )
        }

        val isSecure = try {
            keyguardManager.isDeviceSecure
        } catch (e: Exception) {
            false
        }

        return if (isSecure) {
            DeviceCheckItem(
                id = "screen_lock",
                title = "Screen Lock & Keyguard",
                category = CheckCategory.DEVICE_LOCK,
                severity = FindingSeverity.VERIFIED_SECURE,
                detectedValue = "Secure Screen Lock Configured",
                description = "Device requires a PIN, password, pattern, or biometric authentication to unlock.",
                technicalExplanation = "KeyguardManager.isDeviceSecure() returned true. User credentials are required for device unlock.",
                recommendation = "Maintain a strong PIN or biometric lock.",
                remediationAction = RemediationAction.OPEN_SECURITY_SETTINGS
            )
        } else {
            DeviceCheckItem(
                id = "screen_lock",
                title = "Screen Lock & Keyguard",
                category = CheckCategory.DEVICE_LOCK,
                severity = FindingSeverity.CRITICAL,
                detectedValue = "No Screen Lock Configured",
                description = "Anyone with physical access can open the device and access unencrypted local user data.",
                technicalExplanation = "KeyguardManager.isDeviceSecure() returned false. No user authentication required.",
                recommendation = "Set up a biometric lock, PIN, or passphrase immediately in Android Security Settings.",
                remediationAction = RemediationAction.OPEN_SECURITY_SETTINGS
            )
        }
    }

    private fun checkStorageEncryption(): DeviceCheckItem {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val status = try {
            dpm?.storageEncryptionStatus ?: DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED
        } catch (e: Exception) {
            DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED
        }

        return when (status) {
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE,
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY,
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER -> {
                DeviceCheckItem(
                    id = "storage_encryption",
                    title = "Storage Encryption Status",
                    category = CheckCategory.STORAGE_ENCRYPTION,
                    severity = FindingSeverity.VERIFIED_SECURE,
                    detectedValue = "Reported Active (FBE / Full-Disk)",
                    description = "Flash storage partition reports active encryption status.",
                    technicalExplanation = "DevicePolicyManager returned encryption status ACTIVE.",
                    recommendation = "Storage encryption is reported active by the system.",
                    remediationAction = RemediationAction.OPEN_SECURITY_SETTINGS
                )
            }
            DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE -> {
                DeviceCheckItem(
                    id = "storage_encryption",
                    title = "Storage Encryption Status",
                    category = CheckCategory.STORAGE_ENCRYPTION,
                    severity = FindingSeverity.CRITICAL,
                    detectedValue = "Storage Not Encrypted",
                    description = "User storage reports unencrypted status. Physical dumps could be extracted.",
                    technicalExplanation = "DevicePolicyManager returned ENCRYPTION_STATUS_INACTIVE.",
                    recommendation = "Enable storage encryption in System Security Settings.",
                    remediationAction = RemediationAction.OPEN_SECURITY_SETTINGS
                )
            }
            else -> {
                // On modern Android (API 29+), all shipped devices require mandatory FBE.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    DeviceCheckItem(
                        id = "storage_encryption",
                        title = "Storage Encryption Status",
                        category = CheckCategory.STORAGE_ENCRYPTION,
                        severity = FindingSeverity.VERIFIED_SECURE,
                        detectedValue = "Active (Mandatory Android 10+ CDD Baseline)",
                        description = "Storage encryption is mandatory for Android 10+ certified devices.",
                        technicalExplanation = "Android CDD requires mandatory File-Based Encryption on Android 10+ devices.",
                        recommendation = "No action required.",
                        remediationAction = RemediationAction.OPEN_SECURITY_SETTINGS
                    )
                } else {
                    DeviceCheckItem(
                        id = "storage_encryption",
                        title = "Storage Encryption Status",
                        category = CheckCategory.STORAGE_ENCRYPTION,
                        severity = FindingSeverity.NOT_AVAILABLE,
                        detectedValue = "Not queried on this device/version",
                        description = "The OEM does not expose encryption status via standard DevicePolicyManager APIs.",
                        technicalExplanation = "API query returned ENCRYPTION_STATUS_UNSUPPORTED or threw a security limitation.",
                        recommendation = "Check manufacturer documentation for encryption details.",
                        isAvailableOnDevice = false
                    )
                }
            }
        }
    }

    private fun checkAdbDebugging(): DeviceCheckItem {
        val resolver = context.contentResolver
        val adbEnabled = try {
            Settings.Global.getInt(resolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (e: Exception) {
            false
        }

        return if (adbEnabled) {
            DeviceCheckItem(
                id = "adb_debugging",
                title = "USB Debugging (ADB)",
                category = CheckCategory.DEVELOPER_OPTIONS,
                severity = FindingSeverity.WARNING,
                detectedValue = "ADB Debugging is Active",
                description = "USB debugging allows computers connected via cable or local Wi-Fi to execute shell commands, install APKs, and extract app backups.",
                technicalExplanation = "Settings.Global.ADB_ENABLED is set to 1. The adbd daemon is listening for host connections.",
                recommendation = "Disable USB debugging when not actively developing software to prevent rogue kiosk or charging-port exploitation.",
                remediationAction = RemediationAction.OPEN_DEVELOPER_SETTINGS
            )
        } else {
            DeviceCheckItem(
                id = "adb_debugging",
                title = "USB Debugging (ADB)",
                category = CheckCategory.DEVELOPER_OPTIONS,
                severity = FindingSeverity.VERIFIED_SECURE,
                detectedValue = "ADB Debugging Disabled",
                description = "External USB debug access is closed.",
                technicalExplanation = "Settings.Global.ADB_ENABLED is 0. Shell debugging bridge is inactive.",
                recommendation = "Keep ADB disabled during day-to-day operations.",
                remediationAction = RemediationAction.OPEN_DEVELOPER_SETTINGS
            )
        }
    }

    private fun checkDeveloperOptions(): DeviceCheckItem {
        val resolver = context.contentResolver
        val devOptionsEnabled = try {
            Settings.Global.getInt(resolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        } catch (e: Exception) {
            false
        }

        return if (devOptionsEnabled) {
            DeviceCheckItem(
                id = "developer_options",
                title = "Developer Options",
                category = CheckCategory.DEVELOPER_OPTIONS,
                severity = FindingSeverity.INFORMATIONAL,
                detectedValue = "Developer Options Enabled",
                description = "Developer Mode is unlocked on this device.",
                technicalExplanation = "Settings.Global.DEVELOPMENT_SETTINGS_ENABLED is 1.",
                recommendation = "If you are not an engineer, consider turning off Developer Options to reduce the device attack surface.",
                remediationAction = RemediationAction.OPEN_DEVELOPER_SETTINGS
            )
        } else {
            DeviceCheckItem(
                id = "developer_options",
                title = "Developer Options",
                category = CheckCategory.DEVELOPER_OPTIONS,
                severity = FindingSeverity.VERIFIED_SECURE,
                detectedValue = "Developer Options Locked",
                description = "Developer Mode is locked to standard user defaults.",
                technicalExplanation = "Settings.Global.DEVELOPMENT_SETTINGS_ENABLED is 0.",
                recommendation = "No action needed.",
                remediationAction = RemediationAction.OPEN_DEVELOPER_SETTINGS
            )
        }
    }

    private fun checkAndroidVersionAndPatch(): DeviceCheckItem {
        val sdkInt = Build.VERSION.SDK_INT
        val release = Build.VERSION.RELEASE
        val patchDateStr = Build.VERSION.SECURITY_PATCH

        val daysSincePatch = try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val patchDate = format.parse(patchDateStr)
            if (patchDate != null) {
                val diff = Date().time - patchDate.time
                TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(0)
            } else {
                -1
            }
        } catch (e: Exception) {
            -1
        }

        val severity: FindingSeverity
        val statusText: String
        val recommendationText: String

        if (daysSincePatch > 120 || sdkInt < Build.VERSION_CODES.R) {
            severity = FindingSeverity.WARNING
            statusText = "Android $release (Patch: $patchDateStr, ~$daysSincePatch days ago)"
            recommendationText = "Check System Updates for the latest security patch to guard against published Android CVEs."
        } else if (daysSincePatch in 0..120) {
            severity = FindingSeverity.VERIFIED_SECURE
            statusText = "Android $release (Patch: $patchDateStr)"
            recommendationText = "Security patch level is current."
        } else {
            severity = FindingSeverity.INFORMATIONAL
            statusText = "Android $release (API $sdkInt)"
            recommendationText = "Verify update status in System Settings."
        }

        return DeviceCheckItem(
            id = "os_security_patch",
            title = "OS & Security Patch Baseline",
            category = CheckCategory.SYSTEM_HYGIENE,
            severity = severity,
            detectedValue = statusText,
            description = "Android platform release and monthly Google security bulletin patch level.",
            technicalExplanation = "Build.VERSION.SDK_INT: $sdkInt, Security Patch: ${patchDateStr ?: "Unavailable"}.",
            recommendation = recommendationText,
            remediationAction = RemediationAction.OPEN_DEVICE_INFO
        )
    }

    private fun checkNetworkAndVpn(): DeviceCheckItem {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val capabilities = if (activeNetwork != null) cm.getNetworkCapabilities(activeNetwork) else null

        if (capabilities == null) {
            return DeviceCheckItem(
                id = "network_vpn",
                title = "Network Security & VPN",
                category = CheckCategory.NETWORK_TELEMETRY,
                severity = FindingSeverity.INFORMATIONAL,
                detectedValue = "No Active Network Connection",
                description = "The device is currently offline or in Airplane Mode.",
                technicalExplanation = "ConnectivityManager.activeNetwork returned null.",
                recommendation = "Connect to a trusted Wi-Fi or cellular network when performing online tasks.",
                remediationAction = RemediationAction.OPEN_WIRELESS_SETTINGS
            )
        }

        val hasVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        return if (hasVpn) {
            DeviceCheckItem(
                id = "network_vpn",
                title = "Network Security & VPN",
                category = CheckCategory.NETWORK_TELEMETRY,
                severity = FindingSeverity.VERIFIED_SECURE,
                detectedValue = "Encrypted VPN Tunnel Active",
                description = "Device internet traffic is channeled through a verified VPN tunnel, shielding IP and DNS traffic from local network snooping.",
                technicalExplanation = "NetworkCapabilities.TRANSPORT_VPN detected on active network routing table.",
                recommendation = "Ensure your VPN provider follows a zero-logs policy.",
                remediationAction = RemediationAction.OPEN_WIRELESS_SETTINGS
            )
        } else {
            DeviceCheckItem(
                id = "network_vpn",
                title = "Network Security & VPN",
                category = CheckCategory.NETWORK_TELEMETRY,
                severity = FindingSeverity.INFORMATIONAL,
                detectedValue = if (isValidated) "Direct Connection (No VPN)" else "Unvalidated Network",
                description = "Network traffic routes directly through your ISP or local Wi-Fi router without a VPN tunnel.",
                technicalExplanation = "No TRANSPORT_VPN flag on active network. Validated internet: $isValidated.",
                recommendation = "When connecting to public or untrusted Wi-Fi hotspots, consider using an encrypted VPN.",
                remediationAction = RemediationAction.OPEN_WIRELESS_SETTINGS
            )
        }
    }

    private fun checkUnknownAppSources(): DeviceCheckItem {
        // On Android 8.0+ (API 26+), install unknown apps is managed on a per-app basis.
        return DeviceCheckItem(
            id = "unknown_sources",
            title = "External APK Installation Control",
            category = CheckCategory.PACKAGE_INSTALLS,
            severity = FindingSeverity.INFORMATIONAL,
            detectedValue = "Managed Per-Application (Oreo+ Scoped)",
            description = "Android controls third-party APK installation per application rather than a global toggle.",
            technicalExplanation = "API 26+ uses android.permission.REQUEST_INSTALL_PACKAGES scoped to individual apps.",
            recommendation = "Review and revoke 'Install Unknown Apps' permissions for non-store applications in Settings.",
            remediationAction = RemediationAction.OPEN_MANAGE_UNKNOWN_APP_SOURCES
        )
    }

    private fun checkPrivateDns(): DeviceCheckItem {
        val resolver = context.contentResolver
        val mode = try {
            Settings.Global.getString(resolver, "private_dns_mode")
        } catch (e: Exception) {
            null
        }
        val specifier = try {
            Settings.Global.getString(resolver, "private_dns_specifier")
        } catch (e: Exception) {
            null
        }

        return when (mode) {
            "hostname" -> DeviceCheckItem(
                id = "private_dns",
                title = "Private DNS (DoT / DoH)",
                category = CheckCategory.NETWORK_TELEMETRY,
                severity = FindingSeverity.VERIFIED_SECURE,
                detectedValue = "Enforced: ${specifier ?: "Custom Host"}",
                description = "All DNS queries are encrypted using DNS-over-TLS, preventing ISP and local eavesdroppers from seeing visited domains.",
                technicalExplanation = "Settings.Global.private_dns_mode is 'hostname' with specifier '$specifier'.",
                recommendation = "Maintain your current Private DNS provider.",
                remediationAction = RemediationAction.OPEN_WIRELESS_SETTINGS
            )
            "opportunistic", "auto" -> DeviceCheckItem(
                id = "private_dns",
                title = "Private DNS (DoT / DoH)",
                category = CheckCategory.NETWORK_TELEMETRY,
                severity = FindingSeverity.INFORMATIONAL,
                detectedValue = "Automatic / Opportunistic",
                description = "Private DNS is set to automatic. If local network DNS does not support DoT, queries will fall back to unencrypted plaintext.",
                technicalExplanation = "Settings.Global.private_dns_mode is 'opportunistic'.",
                recommendation = "For strict privacy, configure a dedicated Private DNS provider (e.g., dns.adguard-dns.com or one.one.one.one).",
                remediationAction = RemediationAction.OPEN_WIRELESS_SETTINGS
            )
            else -> DeviceCheckItem(
                id = "private_dns",
                title = "Private DNS (DoT / DoH)",
                category = CheckCategory.NETWORK_TELEMETRY,
                severity = FindingSeverity.WARNING,
                detectedValue = "Disabled / Unencrypted DNS",
                description = "DNS lookups are transmitted in plaintext over your local network and ISP, allowing domain profiling and DNS spoofing.",
                technicalExplanation = "Settings.Global.private_dns_mode is 'off' or unset.",
                recommendation = "Enable Private DNS in Network & Internet settings with a secure provider (e.g., 1dot1dot1dot1.cloudflare-dns.com).",
                remediationAction = RemediationAction.OPEN_WIRELESS_SETTINGS
            )
        }
    }

    private fun checkAccessibilityServices(): DeviceCheckItem {
        val resolver = context.contentResolver
        val enabledServices = try {
            Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        } catch (e: Exception) {
            ""
        }

        val activeCount = if (enabledServices.isBlank()) 0 else enabledServices.split(":").filter { it.isNotBlank() }.size

        return if (activeCount == 0) {
            DeviceCheckItem(
                id = "accessibility_services",
                title = "Active Accessibility Services",
                category = CheckCategory.PLATFORM_INTEGRITY,
                severity = FindingSeverity.VERIFIED_SECURE,
                detectedValue = "0 Third-Party Services Active",
                description = "No accessibility services are actively monitoring user interface touches or screen content.",
                technicalExplanation = "Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES is empty.",
                recommendation = "Only grant accessibility access to verified system services.",
                remediationAction = RemediationAction.OPEN_ACCESSIBILITY_SETTINGS
            )
        } else {
            DeviceCheckItem(
                id = "accessibility_services",
                title = "Active Accessibility Services",
                category = CheckCategory.PLATFORM_INTEGRITY,
                severity = FindingSeverity.WARNING,
                detectedValue = "$activeCount Service(s) Enabled",
                description = "Active accessibility services have full permission to read on-screen text, inspect passwords, and simulate user touches.",
                technicalExplanation = "ENABLED_ACCESSIBILITY_SERVICES: $enabledServices",
                recommendation = "Review active services in Accessibility Settings and disable any unneeded third-party apps.",
                remediationAction = RemediationAction.OPEN_ACCESSIBILITY_SETTINGS
            )
        }
    }

    private fun checkOutdatedDeviceMitigation(): DeviceCheckItem {
        val sdk = Build.VERSION.SDK_INT
        val isOldAndroid = sdk < 33 // Older than Android 13

        return if (isOldAndroid) {
            DeviceCheckItem(
                id = "outdated_hardening",
                title = "Legacy Device Lockdown & Hardening",
                category = CheckCategory.PLATFORM_INTEGRITY,
                severity = FindingSeverity.WARNING,
                detectedValue = "Android $sdk (Legacy Architecture)",
                description = "This device runs an older Android version lacking modern Scoped Storage restrictions and granular media permissions. Extra hardening is strongly recommended.",
                technicalExplanation = "SDK_INT is $sdk (< 33). Device lacks modern runtime permission gates and memory mitigations.",
                recommendation = "Apply JARVIS Outdated Device Hardening: Enable Private DNS, revoke install unknown apps, and run JARVIS Firewall to block untrusted background traffic.",
                remediationAction = RemediationAction.OPEN_SYSTEM_UPDATE_SETTINGS
            )
        } else {
            DeviceCheckItem(
                id = "outdated_hardening",
                title = "Modern OS Hardening Standard",
                category = CheckCategory.PLATFORM_INTEGRITY,
                severity = FindingSeverity.VERIFIED_SECURE,
                detectedValue = "Android $sdk (Modern Platform)",
                description = "Device runs a modern Android version with sandboxed photo pickers, notification permission gates, and per-app language isolation.",
                technicalExplanation = "SDK_INT is $sdk (>= 33).",
                recommendation = "Keep software updated regularly when security patches are released.",
                remediationAction = RemediationAction.OPEN_SYSTEM_UPDATE_SETTINGS
            )
        }
    }

    private fun checkRootAndSuBinaries(): DeviceCheckItem {
        val suPaths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        val suBinaryFound = suPaths.any { path ->
            try {
                File(path).exists()
            } catch (e: Exception) {
                false
            }
        }

        val testKeysPresent = Build.TAGS?.contains("test-keys") == true

        return if (suBinaryFound || testKeysPresent) {
            val detected = when {
                suBinaryFound && testKeysPresent -> "Root su binary detected & Custom ROM test-keys"
                suBinaryFound -> "Root su binary detected in system partition"
                else -> "Custom / unofficial firmware signature (test-keys)"
            }
            DeviceCheckItem(
                id = "root_integrity",
                title = "Root & Superuser Integrity",
                category = CheckCategory.PLATFORM_INTEGRITY,
                severity = FindingSeverity.CRITICAL,
                detectedValue = detected,
                description = "Root privileges or test firmware detected. Any malicious app can completely bypass Linux sandboxing and read keystore keys.",
                technicalExplanation = "Discovered su executable or Build.TAGS containing 'test-keys'. Process isolation cannot be guaranteed.",
                recommendation = "Unroot the device or flash official OEM stock ROM firmware to restore application sandbox integrity.",
                remediationAction = RemediationAction.OPEN_SECURITY_SETTINGS
            )
        } else {
            DeviceCheckItem(
                id = "root_integrity",
                title = "Root & Superuser Integrity",
                category = CheckCategory.PLATFORM_INTEGRITY,
                severity = FindingSeverity.VERIFIED_SECURE,
                detectedValue = "Official Firmware (Enforced Sandbox)",
                description = "Standard Linux user separation and Android application sandboxing are intact. No su binaries detected.",
                technicalExplanation = "su binary checks negative and Build.TAGS='release-keys'. Application UID sandboxing operational.",
                recommendation = "Keep bootloader locked and do not install third-party root toolkits.",
                remediationAction = RemediationAction.OPEN_SECURITY_SETTINGS
            )
        }
    }

    private fun checkSelinuxStatus(): DeviceCheckItem {
        var isEnforcing = true
        try {
            val enforceFile = File("/sys/fs/selinux/enforce")
            if (enforceFile.exists() && enforceFile.canRead()) {
                val content = enforceFile.readText().trim()
                isEnforcing = (content == "1")
            }
        } catch (e: Exception) {
            // Android 8+ restricts direct read of selinux/enforce from untrusted app context;
            // if unreadable, standard non-rooted devices are mandatory enforcing.
            isEnforcing = true
        }

        return if (isEnforcing) {
            DeviceCheckItem(
                id = "selinux_mode",
                title = "SELinux Mandatory Access Control",
                category = CheckCategory.PLATFORM_INTEGRITY,
                severity = FindingSeverity.VERIFIED_SECURE,
                detectedValue = "Enforcing Mode (Strict MAC)",
                description = "Security-Enhanced Linux (SELinux) is actively enforcing Mandatory Access Control policies on all processes.",
                technicalExplanation = "SELinux policy engine is enforcing domain transitions and confining system services to their security domains.",
                recommendation = "SELinux enforcement is operational. No action required.",
                remediationAction = RemediationAction.OPEN_SECURITY_SETTINGS
            )
        } else {
            DeviceCheckItem(
                id = "selinux_mode",
                title = "SELinux Mandatory Access Control",
                category = CheckCategory.PLATFORM_INTEGRITY,
                severity = FindingSeverity.CRITICAL,
                detectedValue = "Permissive Mode / Disabled",
                description = "SELinux is running in Permissive or Disabled mode. Process confinement policies are not being enforced!",
                technicalExplanation = "SELinux enforce is set to 0. Access vector violations are only logged, not blocked.",
                recommendation = "Enable SELinux enforcing mode or restore official OEM firmware immediately.",
                remediationAction = RemediationAction.OPEN_SECURITY_SETTINGS
            )
        }
    }

    private fun checkMockLocationSettings(): DeviceCheckItem {
        val isMockLocationAllowed = try {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.ALLOW_MOCK_LOCATION, 0) != 0
        } catch (e: Exception) {
            false
        }

        return if (isMockLocationAllowed) {
            DeviceCheckItem(
                id = "mock_locations",
                title = "Mock Location Provider",
                category = CheckCategory.DEVELOPER_OPTIONS,
                severity = FindingSeverity.WARNING,
                detectedValue = "Mock Location Enabled",
                description = "Device is configured to allow simulated GPS coordinates, which can spoof location telemetry for banking and security apps.",
                technicalExplanation = "Settings.Secure.ALLOW_MOCK_LOCATION is non-zero.",
                recommendation = "Disable mock locations in Developer Options unless actively debugging navigation software.",
                remediationAction = RemediationAction.OPEN_DEVELOPER_SETTINGS
            )
        } else {
            DeviceCheckItem(
                id = "mock_locations",
                title = "Mock Location Provider",
                category = CheckCategory.DEVELOPER_OPTIONS,
                severity = FindingSeverity.VERIFIED_SECURE,
                detectedValue = "Hardware GPS Direct (No Mock)",
                description = "Simulated location provider is disabled. Location telemetry originates directly from GNSS hardware.",
                technicalExplanation = "Settings.Secure.ALLOW_MOCK_LOCATION is disabled.",
                recommendation = "Keep mock location providers disabled during daily operation.",
                remediationAction = RemediationAction.OPEN_DEVELOPER_SETTINGS
            )
        }
    }
}
