package com.example.jarvis.domain

import android.Manifest
import com.example.jarvis.data.model.FindingSeverity
import com.example.jarvis.data.model.PermissionRiskInfo

object PermissionCatalog {

    data class PermissionDetail(
        val permission: String,
        val readableName: String,
        val category: String,
        val defaultSeverity: FindingSeverity,
        val implication: String,
        val recommendation: String
    )

    private val CATALOG = listOf(
        PermissionDetail(
            permission = Manifest.permission.RECORD_AUDIO,
            readableName = "Microphone & Audio Recording",
            category = "Sensors & Privacy",
            defaultSeverity = FindingSeverity.WARNING,
            implication = "Allows the application to record audio from device microphones at any time while running.",
            recommendation = "Review if the app genuinely needs voice input. Consider revoking or switching to 'Only while in use'."
        ),
        PermissionDetail(
            permission = Manifest.permission.CAMERA,
            readableName = "Camera Optical Sensor",
            category = "Sensors & Privacy",
            defaultSeverity = FindingSeverity.WARNING,
            implication = "Allows capturing photos and video streams from front and rear camera sensors.",
            recommendation = "Verify if the application requires continuous camera access. Revoke for flashlight or simple utility tools."
        ),
        PermissionDetail(
            permission = Manifest.permission.ACCESS_FINE_LOCATION,
            readableName = "Precise GPS Location",
            category = "Physical Location",
            defaultSeverity = FindingSeverity.WARNING,
            implication = "Determines your exact physical coordinates down to within a few meters using GPS, Wi-Fi, and cell towers.",
            recommendation = "Use 'Approximate Location' instead of Precise if the app only requires general weather or city data."
        ),
        PermissionDetail(
            permission = Manifest.permission.ACCESS_COARSE_LOCATION,
            readableName = "Coarse (Cell/Wi-Fi) Location",
            category = "Physical Location",
            defaultSeverity = FindingSeverity.INFORMATIONAL,
            implication = "Determines approximate city-block level coordinates based on network infrastructure.",
            recommendation = "Verify if location is required for the app's primary advertised function."
        ),
        PermissionDetail(
            permission = Manifest.permission.READ_CONTACTS,
            readableName = "Read Personal Contacts",
            category = "Personal Identity",
            defaultSeverity = FindingSeverity.WARNING,
            implication = "Grants full access to read your contacts database, phone numbers, email addresses, and relationship notes.",
            recommendation = "Unscrupulous apps often exfiltrate contact graphs to remote marketing brokers. Grant only to trusted communicators."
        ),
        PermissionDetail(
            permission = Manifest.permission.WRITE_CONTACTS,
            readableName = "Modify Personal Contacts",
            category = "Personal Identity",
            defaultSeverity = FindingSeverity.WARNING,
            implication = "Enables modifying, creating, or deleting contacts stored on your device accounts.",
            recommendation = "Only grant to verified contact management tools or dialers."
        ),
        PermissionDetail(
            permission = Manifest.permission.READ_CALL_LOG,
            readableName = "Read Telephone Call Logs",
            category = "Communications",
            defaultSeverity = FindingSeverity.CRITICAL,
            implication = "Exposes private incoming/outgoing telephone numbers, call durations, and caller timestamps.",
            recommendation = "Extremely sensitive. Google Play restricts this to default dialer apps. Revoke immediately for utilities."
        ),
        PermissionDetail(
            permission = Manifest.permission.READ_SMS,
            readableName = "Read SMS / Text Messages",
            category = "Communications & Financial",
            defaultSeverity = FindingSeverity.CRITICAL,
            implication = "Allows reading all SMS messages, including two-factor authentication (2FA) codes and bank transaction alerts.",
            recommendation = "High risk for OTP theft. Only the default SMS handler should hold this permission."
        ),
        PermissionDetail(
            permission = Manifest.permission.SEND_SMS,
            readableName = "Send SMS / Text Messages",
            category = "Communications & Financial",
            defaultSeverity = FindingSeverity.CRITICAL,
            implication = "Allows transmitting SMS messages silently, which can incur premium cellular rate charges or propagate spam.",
            recommendation = "Only the default SMS app should send texts."
        ),
        PermissionDetail(
            permission = Manifest.permission.READ_PHONE_STATE,
            readableName = "Read Phone State & Identifiers",
            category = "Device Identity",
            defaultSeverity = FindingSeverity.INFORMATIONAL,
            implication = "Exposes phone call status, network cellular carrier name, and on older OS versions, hardware SIM identifiers.",
            recommendation = "Usually needed by media players to pause audio during phone calls, but can be used for ad-fingerprinting."
        ),
        PermissionDetail(
            permission = Manifest.permission.READ_EXTERNAL_STORAGE,
            readableName = "Read Shared Media & Storage",
            category = "Storage & Files",
            defaultSeverity = FindingSeverity.INFORMATIONAL,
            implication = "Grants broad access to shared device photos, documents, and downloaded files on legacy Android versions.",
            recommendation = "Modern Android uses Photo Picker. Apps targeting recent Android do not require broad storage access."
        ),
        PermissionDetail(
            permission = Manifest.permission.BODY_SENSORS,
            readableName = "Body Sensors & Biometrics",
            category = "Health & Sensors",
            defaultSeverity = FindingSeverity.WARNING,
            implication = "Enables real-time heart rate, step count, and physiological sensor telemetry collection.",
            recommendation = "Restrict strictly to health and fitness trackers."
        ),
        PermissionDetail(
            permission = "android.permission.SYSTEM_ALERT_WINDOW",
            readableName = "Draw Over Other Apps (Overlay)",
            category = "System Control",
            defaultSeverity = FindingSeverity.CRITICAL,
            implication = "Allows rendering windows over other running apps. Frequently exploited in tapjacking and credential phishing attacks.",
            recommendation = "Verify strictly. Revoke in System Settings for any application that does not strictly require floating bubbles."
        ),
        PermissionDetail(
            permission = "android.permission.REQUEST_INSTALL_PACKAGES",
            readableName = "Install Unknown Apps / APKs",
            category = "System Control",
            defaultSeverity = FindingSeverity.CRITICAL,
            implication = "Allows downloading and launching installation intents for external third-party APK packages.",
            recommendation = "Only reputable web browsers or store clients should have this capability. Revoke for games or utilities."
        ),
        PermissionDetail(
            permission = "android.permission.BIND_ACCESSIBILITY_SERVICE",
            readableName = "Accessibility Service Engine",
            category = "Deep System Control",
            defaultSeverity = FindingSeverity.CRITICAL,
            implication = "Allows reading all on-screen contents, detecting keystrokes, and executing automated touch gestures.",
            recommendation = "Massive security surface. If enabled by an untrusted app, it can act as a full device controller. Audit carefully."
        ),
        PermissionDetail(
            permission = "android.permission.PACKAGE_USAGE_STATS",
            readableName = "App Usage Statistics Access",
            category = "Behavioral Privacy",
            defaultSeverity = FindingSeverity.WARNING,
            implication = "Tracks which applications you open, how frequently you use them, and daily screen time metrics.",
            recommendation = "Restrict to digital wellbeing or launcher applications."
        )
    )

    private val CATALOG_MAP = CATALOG.associateBy { it.permission }

    fun getDetail(permission: String): PermissionDetail {
        return CATALOG_MAP[permission] ?: PermissionDetail(
            permission = permission,
            readableName = permission.substringAfterLast("."),
            category = "General Permissions",
            defaultSeverity = FindingSeverity.INFORMATIONAL,
            implication = "Standard Android permission requested by the application manifest.",
            recommendation = "Review application source and reputation if unexpected."
        )
    }

    fun getAllCatalogItems(): List<PermissionDetail> = CATALOG
}
