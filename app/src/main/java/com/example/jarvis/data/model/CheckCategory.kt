package com.example.jarvis.data.model

enum class CheckCategory(val displayName: String) {
    DEVICE_LOCK("Device Lock & Keyguard"),
    STORAGE_ENCRYPTION("Storage & Encryption"),
    DEVELOPER_OPTIONS("Developer & Debugging"),
    SYSTEM_HYGIENE("Operating System Hygiene"),
    PACKAGE_INSTALLS("Package Installation Sources"),
    NETWORK_TELEMETRY("Network & VPN Posture"),
    APPLICATION_PERMISSIONS("Application Permissions"),
    PLATFORM_INTEGRITY("Platform Integrity & Hardening")
}
