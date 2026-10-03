package com.example.jarvis.data.model

data class AppSecurityInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val targetSdk: Int,
    val minSdk: Int,
    val isSystemApp: Boolean,
    val isDebuggable: Boolean,
    val requestedPermissions: List<String>,
    val grantedDangerousPermissions: List<String>,
    val specialPermissions: List<String>,
    val riskScore: Int, // 0 (safest) to 100 (highest risk)
    val riskLevel: FindingSeverity,
    val riskFactors: List<String>,
    val detectedAdwareSdks: List<String> = emptyList(),
    val isPersonalDeveloperApp: Boolean = false,
    val isTrustedByUser: Boolean = false,
    val codeVerificationVerdict: String = "VERIFIED_CLEAN", // VERIFIED_CLEAN, PERSONAL_DEV_SAFE, ADWARE_DETECTED, SUSPICIOUS_PAYLOAD
    val detectedMalwarePatterns: List<String> = emptyList(),
    val signatureType: String = "Standard Package",
    val dexClassesCount: Int = 1,
    val nativeLibraries: List<String> = emptyList(),
    val isHiddenApp: Boolean = false,
    val isMalwareThreat: Boolean = false
) {
    val dexClassCount: Int get() = dexClassesCount
}
