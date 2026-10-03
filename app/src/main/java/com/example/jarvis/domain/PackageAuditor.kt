package com.example.jarvis.domain

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.jarvis.data.model.AppSecurityInfo
import com.example.jarvis.data.model.FindingSeverity

class PackageAuditor(private val context: Context) {

    private val pm: PackageManager = context.packageManager
    private val deepCodeAuditor = DeepCodeAuditor(context)

    suspend fun auditInstalledApplications(): List<AppSecurityInfo> {
        val appList = mutableListOf<AppSecurityInfo>()

        val queryFlags = PackageManager.GET_PERMISSIONS or
                PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS

        val packages: List<PackageInfo> = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(queryFlags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(queryFlags)
            }
        } catch (e: Exception) {
            emptyList()
        }

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val packageName = pkg.packageName

            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isDebuggable = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val targetSdk = appInfo.targetSdkVersion
            val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appInfo.minSdkVersion else 21
            val appName = try {
                appInfo.loadLabel(pm).toString()
            } catch (e: Exception) {
                packageName
            }
            val versionName = pkg.versionName ?: "1.0"

            val rawReqPerms = pkg.requestedPermissions
            val requestedPermissions = rawReqPerms?.toList() ?: emptyList()
            val flags = pkg.requestedPermissionsFlags

            val grantedPermissions = mutableListOf<String>()
            if (flags != null && rawReqPerms != null) {
                for (i in rawReqPerms.indices) {
                    val isGranted = (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                    if (isGranted) {
                        grantedPermissions.add(rawReqPerms[i])
                    }
                }
            }

            val specialPermissions = mutableListOf<String>()
            val riskFactors = mutableListOf<String>()
            var calculatedRisk = 0

            // Run deep code and component analysis
            val deepAnalysis = deepCodeAuditor.inspectApp(
                pkgInfo = pkg,
                appName = appName,
                isSystemApp = isSystemApp,
                isDebuggable = isDebuggable,
                requestedPermissions = requestedPermissions,
                grantedDangerousPermissions = grantedPermissions
            )

            // 1. Debuggable check (SKIP IF VERIFIED DEVELOPER APP OR TRUSTED)
            if (isDebuggable) {
                if (deepAnalysis.isPersonalDeveloperApp) {
                    riskFactors.add("✅ Personal Developer Project: Active debug flag verified as safe local development build.")
                } else if (deepAnalysis.isTrustedByUser) {
                    riskFactors.add("🛡️ Marked as Trusted Project by User.")
                } else {
                    calculatedRisk += 25
                    riskFactors.add("Application has FLAG_DEBUGGABLE enabled in manifest (allows memory attachment, log interception, and code injection).")
                }
            }

            // 2. Outdated Target SDK check
            if (targetSdk < 29 && !deepAnalysis.isPersonalDeveloperApp && !deepAnalysis.isTrustedByUser) {
                calculatedRisk += 25
                riskFactors.add("Targets legacy Android $targetSdk (bypasses Scoped Storage and modern background execution constraints).")
            } else if (targetSdk < 33 && !deepAnalysis.isPersonalDeveloperApp && !deepAnalysis.isTrustedByUser) {
                calculatedRisk += 10
                riskFactors.add("Targets Android $targetSdk (< Android 13: misses granular media and notification permission controls).")
            }

            // 3. Special Permissions audit
            if (requestedPermissions.contains("android.permission.SYSTEM_ALERT_WINDOW")) {
                specialPermissions.add("SYSTEM_ALERT_WINDOW")
                if (!deepAnalysis.isPersonalDeveloperApp && !deepAnalysis.isTrustedByUser) {
                    calculatedRisk += 20
                    riskFactors.add("Can draw over other applications (Overlay / potential tapjacking & UI spoofing surface).")
                }
            }

            if (requestedPermissions.contains("android.permission.REQUEST_INSTALL_PACKAGES")) {
                specialPermissions.add("REQUEST_INSTALL_PACKAGES")
                if (!deepAnalysis.isPersonalDeveloperApp && !deepAnalysis.isTrustedByUser) {
                    calculatedRisk += 15
                    riskFactors.add("Can request installation of external APK packages (dropper capability).")
                }
            }

            if (requestedPermissions.contains("android.permission.BIND_ACCESSIBILITY_SERVICE")) {
                specialPermissions.add("BIND_ACCESSIBILITY_SERVICE")
                if (!deepAnalysis.isPersonalDeveloperApp && !deepAnalysis.isTrustedByUser) {
                    calculatedRisk += 30
                    riskFactors.add("Requests Accessibility Service binding (capable of screen content reading and automated touches).")
                }
            }

            if (requestedPermissions.contains("android.permission.PACKAGE_USAGE_STATS")) {
                specialPermissions.add("PACKAGE_USAGE_STATS")
                if (!deepAnalysis.isPersonalDeveloperApp && !deepAnalysis.isTrustedByUser) {
                    calculatedRisk += 10
                    riskFactors.add("Can inspect application usage statistics and foreground launch timestamps.")
                }
            }

            // 4. Dangerous sensitive combinations (only penalize if not personal developer app)
            val hasSms = grantedPermissions.any { it.contains("SMS", ignoreCase = true) }
            val hasCallLog = grantedPermissions.any { it.contains("CALL_LOG", ignoreCase = true) }
            val hasContacts = grantedPermissions.any { it.contains("CONTACTS", ignoreCase = true) }
            val hasMic = grantedPermissions.any { it.contains("RECORD_AUDIO", ignoreCase = true) }
            val hasCamera = grantedPermissions.any { it.contains("CAMERA", ignoreCase = true) }
            val hasLocation = grantedPermissions.any { it.contains("LOCATION", ignoreCase = true) }

            if (hasSms) {
                calculatedRisk += 25
                riskFactors.add("Holds granted SMS permissions (high exposure for 2FA / OTP interception).")
            }
            if (hasCallLog) {
                calculatedRisk += 20
                riskFactors.add("Holds granted Call Log permission (exposes private calling metadata).")
            }
            if (hasContacts) {
                calculatedRisk += 15
                riskFactors.add("Holds granted Contacts permission (exposes user address book graph).")
            }
            if (hasMic && hasCamera) {
                calculatedRisk += 15
                riskFactors.add("Simultaneous optical and audio sensor recording capabilities.")
            } else if (hasMic || hasCamera) {
                calculatedRisk += 8
                riskFactors.add("Holds physical sensor recording permission (Microphone or Camera).")
            }
            if (hasLocation) {
                calculatedRisk += 8
                riskFactors.add("Holds device physical geolocation tracking permissions.")
            }

            // Add deep analysis adware/malware risk adjustments
            calculatedRisk += deepAnalysis.riskScoreDeduction
            riskFactors.addAll(deepAnalysis.additionalRiskFactors)

            // Trust modifiers: apply proportionate trust mitigation rather than complete exemption
            if (deepAnalysis.isPersonalDeveloperApp) {
                calculatedRisk = (calculatedRisk * 0.2).toInt()
                riskFactors.add("🛡️ Personal Developer Trust Modifier applied (-80% base risk).")
            } else if (deepAnalysis.isTrustedByUser) {
                calculatedRisk = (calculatedRisk * 0.25).toInt()
                riskFactors.add("🛡️ User Whitelist Trust Modifier applied (-75% base risk).")
            }

            // Adjust for system pre-installed apps (moderate discount, not total wipeout)
            if (isSystemApp) {
                calculatedRisk = (calculatedRisk * 0.65).toInt()
            }

            val calculatedFinal = if (deepAnalysis.isMalwareThreat) {
                calculatedRisk.coerceAtLeast(80).coerceIn(0, 100)
            } else {
                calculatedRisk.coerceIn(0, 100)
            }
            val finalRiskScore = calculatedFinal
            val severity = when {
                deepAnalysis.isMalwareThreat -> FindingSeverity.CRITICAL
                deepAnalysis.isPersonalDeveloperApp || deepAnalysis.isTrustedByUser -> {
                    if (finalRiskScore >= 40) FindingSeverity.WARNING else FindingSeverity.VERIFIED_SECURE
                }
                finalRiskScore >= 55 -> FindingSeverity.CRITICAL
                finalRiskScore >= 35 -> FindingSeverity.WARNING
                finalRiskScore >= 15 -> FindingSeverity.INFORMATIONAL
                else -> FindingSeverity.VERIFIED_SECURE
            }

            val dangerousGranted = grantedPermissions.filter { perm ->
                perm.contains("LOCATION") || perm.contains("CAMERA") || perm.contains("AUDIO") ||
                        perm.contains("CONTACTS") || perm.contains("SMS") || perm.contains("CALL_LOG") ||
                        perm.contains("STORAGE") || perm.contains("CALENDAR") || perm.contains("SENSORS")
            }

            appList.add(
                AppSecurityInfo(
                    packageName = packageName,
                    appName = appName,
                    versionName = versionName,
                    targetSdk = targetSdk,
                    minSdk = minSdk,
                    isSystemApp = isSystemApp,
                    isDebuggable = isDebuggable,
                    requestedPermissions = requestedPermissions,
                    grantedDangerousPermissions = dangerousGranted,
                    specialPermissions = specialPermissions,
                    riskScore = finalRiskScore,
                    riskLevel = severity,
                    riskFactors = riskFactors,
                    detectedAdwareSdks = deepAnalysis.detectedAdwareSdks,
                    isPersonalDeveloperApp = deepAnalysis.isPersonalDeveloperApp,
                    isTrustedByUser = deepAnalysis.isTrustedByUser,
                    codeVerificationVerdict = deepAnalysis.verdict,
                    detectedMalwarePatterns = deepAnalysis.detectedMalwarePatterns,
                    signatureType = deepAnalysis.signatureType,
                    dexClassesCount = deepAnalysis.dexClassesCount,
                    nativeLibraries = deepAnalysis.nativeLibraries,
                    isHiddenApp = deepAnalysis.isHiddenApp,
                    isMalwareThreat = deepAnalysis.isMalwareThreat
                )
            )
        }

        // Return sorted by highest risk first
        return appList.sortedByDescending { it.riskScore }
    }
}
