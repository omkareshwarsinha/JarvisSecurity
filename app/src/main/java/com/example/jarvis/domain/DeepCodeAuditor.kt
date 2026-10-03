package com.example.jarvis.domain

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.example.jarvis.data.db.JarvisDatabase
import java.io.File
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.zip.ZipFile

class DeepCodeAuditor(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val database = JarvisDatabase.getInstance(context)

    data class DeepAnalysisResult(
        val detectedAdwareSdks: List<String>,
        val detectedMalwarePatterns: List<String>,
        val isPersonalDeveloperApp: Boolean,
        val isTrustedByUser: Boolean,
        val verdict: String,
        val signatureType: String,
        val dexClassesCount: Int,
        val nativeLibraries: List<String>,
        val riskScoreDeduction: Int,
        val additionalRiskFactors: List<String>,
        val isHiddenApp: Boolean = false,
        val isMalwareThreat: Boolean = false
    )

    private val knownAdwareSignatures = listOf(
        "com.airpush" to "Airpush Aggressive Adware",
        "com.leadbolt" to "Leadbolt Intrusive SDK",
        "com.startapp" to "StartApp Ad Network",
        "com.ironsource" to "IronSource Monetization",
        "com.unity3d.ads" to "Unity Ads SDK",
        "com.applovin" to "AppLovin Ad SDK",
        "com.vungle" to "Vungle Video Ads",
        "com.inmobi" to "InMobi Tracking & Ads",
        "com.chartboost" to "Chartboost Ads",
        "com.fyber" to "Fyber Offerwall SDK",
        "com.mintegral" to "Mintegral Ad Engine",
        "com.mobfox" to "MobFox Monetization",
        "com.senddroid" to "SendDroid Notification Spammer",
        "com.wooboo" to "Wooboo Overlay Ads",
        "com.batmobi" to "Batmobi Dropper Adware"
    )

    private val knownMalwareAndSpywareSignatures = listOf(
        "com.mspy" to "mSpy Commercial Stalkerware",
        "com.flexispy" to "FlexiSPY Surveillance Tool",
        "com.cerberus" to "Cerberus Trojan",
        "com.spybubble" to "SpyBubble Invisible Tracker",
        "com.hoverwatch" to "Hoverwatch Spyware",
        "com.thetruthspy" to "TheTruthSpy Stalkerware",
        "com.xspy" to "XSpy Surveillance Tool",
        "com.metasploit" to "Metasploit Meterpreter Payload",
        "com.androspy" to "AndroSpy Remote Access Trojan",
        "com.spyfone" to "SpyFone Surveillance Tool",
        "com.kidlogger" to "KidLogger Keylogger",
        "com.spappmonitoring" to "SPAPP Monitoring Spyware",
        "com.crying" to "Crying Trojan",
        "com.triada" to "Triada Banking Trojan",
        "com.joker" to "Joker SMS Billing Fraud",
        "com.anubis" to "Anubis Banking Trojan",
        "com.flubot" to "FluBot SMS Stealer",
        "com.sharkbot" to "SharkBot Financial Trojan",
        "com.teabot" to "TeaBot Credential Interceptor",
        "com.hydra" to "Hydra Banking Malware",
        "com.brata" to "BRATA Remote Access Trojan",
        "com.alien" to "Alien Banking Trojan",
        "com.pegasus" to "Pegasus Surveillance Tool",
        "com.chrysaor" to "Chrysaor Surveillance Framework",
        "com.fake.system" to "Deceptive Fake System Trojan",
        "com.hidden.spy" to "Stealth Spyware Payload"
    )

    suspend fun inspectApp(
        pkgInfo: PackageInfo,
        appName: String,
        isSystemApp: Boolean,
        isDebuggable: Boolean,
        requestedPermissions: List<String>,
        grantedDangerousPermissions: List<String>
    ): DeepAnalysisResult {
        val packageName = pkgInfo.packageName
        val appInfo = pkgInfo.applicationInfo

        // 1. Check if user explicitly marked as trusted
        val trustedEntities = database.trustedAppDao().getTrustedPackageNames()
        val isExplicitlyTrusted = trustedEntities.contains(packageName)

        // 2. Signature analysis
        val (isSignedWithDebugKey, signatureSubject) = analyzeSignature(pkgInfo)

        // 3. Inspect APK Zip archive entries (DEX files, Native libs, known Adware & Malware classes)
        val (dexCount, nativeLibs, detectedSdks, detectedMalwareClasses) = inspectApkArchive(appInfo?.sourceDir)

        // 4. Inspect registered components (Activities, Services, Receivers) for adware/telemetry/malware
        val (componentSdks, componentMalware) = inspectComponents(pkgInfo)
        val allDetectedSdks = (detectedSdks + componentSdks).distinct()
        val allDetectedMalwareClasses = (detectedMalwareClasses + componentMalware).distinct()

        // 5. Behavioral Malware & Trojan Pattern Detection
        val detectedPatterns = mutableListOf<String>()
        val additionalRisks = mutableListOf<String>()
        var calculatedScoreAdjustment = 0
        var isMalwareIdentified = false

        // Check against known malware signatures by package name
        for ((sig, label) in knownMalwareAndSpywareSignatures) {
            if (packageName.contains(sig, ignoreCase = true)) {
                detectedPatterns.add("Known Threat Signature: $label ($packageName)")
                additionalRisks.add("Definitive malware signature match: $label")
                calculatedScoreAdjustment += 60
                isMalwareIdentified = true
            }
        }
        if (allDetectedMalwareClasses.isNotEmpty()) {
            detectedPatterns.addAll(allDetectedMalwareClasses)
            additionalRisks.addAll(allDetectedMalwareClasses)
            calculatedScoreAdjustment += 50
            isMalwareIdentified = true
        }

        // Pattern 1: Hidden App / Stealth Mode (App with NO launcher activity, but has background presence)
        val isSelf = packageName == context.packageName
        var isHiddenApp = false
        if (!isSystemApp && !isSelf) {
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
            }
            val hasLauncher = try {
                packageManager.queryIntentActivities(launcherIntent, 0).isNotEmpty()
            } catch (e: Exception) {
                true
            }
            if (!hasLauncher) {
                // If it has no launcher icon but holds background services, receivers, or dangerous permissions
                val hasBackgroundPresence = (pkgInfo.services?.isNotEmpty() == true) ||
                        (pkgInfo.receivers?.isNotEmpty() == true) ||
                        requestedPermissions.any { it.contains("SMS") || it.contains("LOCATION") || it.contains("AUDIO") || it.contains("CAMERA") }

                if (hasBackgroundPresence) {
                    isHiddenApp = true
                    detectedPatterns.add("Elevated Risk Profile: No launcher activity registered in system drawer while executing background services/sensors.")
                    additionalRisks.add("Hidden Background Presence: App lacks a home screen launcher activity but retains background execution capabilities.")
                    calculatedScoreAdjustment += 35
                }
            }
        }

        // Pattern 2: Fake System App Impersonation
        if (!isSystemApp && !isSelf) {
            val systemKeywords = listOf("system update", "google play service", "android system", "device manager", "google service", "security service", "system helper")
            val isFakeName = systemKeywords.any { appName.contains(it, ignoreCase = true) } ||
                    packageName.startsWith("com.android.system.") ||
                    (packageName.startsWith("com.android.") && !signatureSubject.contains("Android", ignoreCase = true) && !signatureSubject.contains("Google", ignoreCase = true))

            if (isFakeName) {
                detectedPatterns.add("Deceptive Impersonation: Non-system third-party application disguising as '$appName'.")
                additionalRisks.add("Social Engineering Threat: Poses as core operating system component.")
                calculatedScoreAdjustment += 40
                isMalwareIdentified = true
            }
        }

        // Pattern 3: Banker Trojan / Tapjacking Pattern (Screen Overlay + Accessibility Service)
        val hasOverlay = requestedPermissions.contains("android.permission.SYSTEM_ALERT_WINDOW")
        val hasAccessibility = requestedPermissions.contains("android.permission.BIND_ACCESSIBILITY_SERVICE")
        if (hasOverlay && hasAccessibility && !isSystemApp && !isSelf) {
            detectedPatterns.add("Banker Trojan Pattern: Combines Screen Overlay + Accessibility Service (credential hijacking & automated taps).")
            additionalRisks.add("Critical Threat: Can spoof banking login interfaces and capture one-time passwords.")
            calculatedScoreAdjustment += 50
            isMalwareIdentified = true
        }

        // Pattern 4: Stalkerware / Persistent Sensor Harvester
        val hasMic = grantedDangerousPermissions.any { it.contains("RECORD_AUDIO") } || requestedPermissions.contains("android.permission.RECORD_AUDIO")
        val hasCamera = grantedDangerousPermissions.any { it.contains("CAMERA") } || requestedPermissions.contains("android.permission.CAMERA")
        val hasLocation = grantedDangerousPermissions.any { it.contains("LOCATION") } || requestedPermissions.any { it.contains("LOCATION") }
        val hasBoot = requestedPermissions.contains("android.permission.RECEIVE_BOOT_COMPLETED")
        val hasNet = requestedPermissions.contains("android.permission.INTERNET")

        if (hasMic && hasCamera && hasBoot && hasNet && !isSystemApp && !isSelf) {
            detectedPatterns.add("Background Surveillance / Stalkerware: Auto-starts on device boot with active microphone and camera recording.")
            additionalRisks.add("High Exposure: Remote audio and optical monitoring capability.")
            calculatedScoreAdjustment += 40
            isMalwareIdentified = true
        }

        // Pattern 5: SMS & OTP Interceptor
        val hasSmsReceive = requestedPermissions.contains("android.permission.RECEIVE_SMS")
        val hasSmsRead = requestedPermissions.contains("android.permission.READ_SMS")
        if ((hasSmsReceive || hasSmsRead) && hasNet && !isSystemApp && !isSelf) {
            detectedPatterns.add("SMS / 2FA Interceptor Pattern: Holds SMS interception authorization combined with network connectivity.")
            additionalRisks.add("Financial Exposure: Can capture two-factor authentication codes and banking OTPs.")
            calculatedScoreAdjustment += 35
        }

        // Pattern 6: Dropper / Secondary Payload Installer
        val hasInstall = requestedPermissions.contains("android.permission.REQUEST_INSTALL_PACKAGES")
        if (hasInstall && hasNet && !isSystemApp && !isSelf) {
            val installer = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    packageManager.getInstallSourceInfo(packageName).installingPackageName
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getInstallerPackageName(packageName)
                }
            } catch (e: Exception) {
                null
            }
            val isSideloaded = installer == null || installer !in listOf("com.android.vending", "com.google.android.feedback", "com.sec.android.app.samsungapps")
            if (isSideloaded) {
                detectedPatterns.add("Untrusted Dropper Vector: Sideloaded application with authorization to download & install external APKs.")
                additionalRisks.add("Payload Risk: Origin is unverified store and can prompt installation of unverified software.")
                calculatedScoreAdjustment += 30
            }
        }

        // Pattern 7: Invasive Adware Clusters
        if (allDetectedSdks.size >= 2 && !isSystemApp) {
            detectedPatterns.add("Aggressive Adware Cluster: Detected ${allDetectedSdks.size} third-party monetization & popup SDKs.")
            additionalRisks.add("Contains invasive advertising SDKs: ${allDetectedSdks.joinToString(", ")}")
            calculatedScoreAdjustment += 25
        } else if (allDetectedSdks.isNotEmpty()) {
            additionalRisks.add("Contains embedded ad network: ${allDetectedSdks.joinToString(", ")}")
            calculatedScoreAdjustment += 10
        }

        // Developer App Recognition: ONLY MobiArmour itself is strictly exempt
        val isPersonalDeveloperApp = isSelf

        // Verdict & adjustments
        val finalVerdict: String
        if (isExplicitlyTrusted) {
            finalVerdict = "USER_TRUSTED_SAFE"
            calculatedScoreAdjustment = -100
        } else if (isPersonalDeveloperApp) {
            finalVerdict = "PERSONAL_DEV_SAFE"
            calculatedScoreAdjustment = -100
            additionalRisks.clear()
            detectedPatterns.clear()
        } else if (isMalwareIdentified || calculatedScoreAdjustment >= 40) {
            finalVerdict = if (isHiddenApp) "HIDDEN_STEALTH_MALWARE" else "MALWARE_DETECTED"
        } else if (detectedPatterns.isNotEmpty()) {
            finalVerdict = "SUSPICIOUS_PAYLOAD"
        } else if (allDetectedSdks.isNotEmpty()) {
            finalVerdict = "ADWARE_DETECTED"
        } else {
            finalVerdict = "VERIFIED_CLEAN"
        }

        val sigDisplay = when {
            isSignedWithDebugKey -> "Android Debug Keystore (Local Build)"
            signatureSubject.contains("Google", ignoreCase = true) -> "Google Play Signed"
            signatureSubject.isNotEmpty() -> signatureSubject.take(40)
            else -> "Standard Package Signature"
        }

        return DeepAnalysisResult(
            detectedAdwareSdks = allDetectedSdks,
            detectedMalwarePatterns = detectedPatterns,
            isPersonalDeveloperApp = isPersonalDeveloperApp,
            isTrustedByUser = isExplicitlyTrusted,
            verdict = finalVerdict,
            signatureType = sigDisplay,
            dexClassesCount = dexCount,
            nativeLibraries = nativeLibs,
            riskScoreDeduction = calculatedScoreAdjustment,
            additionalRiskFactors = additionalRisks,
            isHiddenApp = isHiddenApp,
            isMalwareThreat = isMalwareIdentified || finalVerdict.contains("MALWARE")
        )
    }

    private fun analyzeSignature(pkgInfo: PackageInfo): Pair<Boolean, String> {
        return try {
            val signatures: Array<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.signatures
            }

            if (signatures.isNullOrEmpty()) {
                Pair(false, "")
            } else {
                val cf = CertificateFactory.getInstance("X509")
                val cert = cf.generateCertificate(signatures[0].toByteArray().inputStream()) as X509Certificate
                val subject = cert.subjectX500Principal.name
                val isDebug = subject.contains("Android Debug", ignoreCase = true) ||
                        (subject.contains("Android", ignoreCase = true) && subject.contains("Debug", ignoreCase = true))
                Pair(isDebug, subject)
            }
        } catch (e: Exception) {
            Pair(false, "")
        }
    }

    private fun inspectApkArchive(sourceDir: String?): Tuple4<Int, List<String>, List<String>, List<String>> {
        if (sourceDir == null) return Tuple4(1, emptyList(), emptyList(), emptyList())
        val apkFile = File(sourceDir)
        if (!apkFile.exists() || !apkFile.canRead()) return Tuple4(1, emptyList(), emptyList(), emptyList())

        var dexCount = 0
        val nativeLibs = mutableListOf<String>()
        val detectedSdks = mutableListOf<String>()
        val detectedMalware = mutableListOf<String>()

        try {
            ZipFile(apkFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name

                    if (name.endsWith(".dex")) {
                        dexCount++
                    }

                    if (name.startsWith("lib/") && name.endsWith(".so")) {
                        val libName = name.substringAfterLast("/")
                        if (nativeLibs.size < 10) {
                            nativeLibs.add(libName)
                        }
                    }

                    // Search for known adware classpaths
                    for ((sig, label) in knownAdwareSignatures) {
                        val path = sig.replace('.', '/')
                        if (name.contains(path) && !detectedSdks.contains(label)) {
                            detectedSdks.add(label)
                        }
                    }

                    // Search for known malware classpaths
                    for ((sig, label) in knownMalwareAndSpywareSignatures) {
                        val path = sig.replace('.', '/')
                        if (name.contains(path) && !detectedMalware.contains(label)) {
                            detectedMalware.add("Payload matched: $label")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Handled
        }

        return Tuple4(dexCount.coerceAtLeast(1), nativeLibs, detectedSdks, detectedMalware)
    }

    private fun inspectComponents(pkgInfo: PackageInfo): Pair<List<String>, List<String>> {
        val detectedAdware = mutableListOf<String>()
        val detectedMalware = mutableListOf<String>()

        pkgInfo.services?.forEach { service ->
            val name = service.name
            for ((sig, label) in knownAdwareSignatures) {
                if (name.contains(sig, ignoreCase = true) && !detectedAdware.contains(label)) {
                    detectedAdware.add(label)
                }
            }
            for ((sig, label) in knownMalwareAndSpywareSignatures) {
                if (name.contains(sig, ignoreCase = true) && !detectedMalware.contains(label)) {
                    detectedMalware.add("Malicious Service: $label")
                }
            }
        }

        pkgInfo.receivers?.forEach { receiver ->
            val name = receiver.name
            for ((sig, label) in knownAdwareSignatures) {
                if (name.contains(sig, ignoreCase = true) && !detectedAdware.contains(label)) {
                    detectedAdware.add(label)
                }
            }
            for ((sig, label) in knownMalwareAndSpywareSignatures) {
                if (name.contains(sig, ignoreCase = true) && !detectedMalware.contains(label)) {
                    detectedMalware.add("Malicious Receiver: $label")
                }
            }
        }

        return Pair(detectedAdware, detectedMalware)
    }

    private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
