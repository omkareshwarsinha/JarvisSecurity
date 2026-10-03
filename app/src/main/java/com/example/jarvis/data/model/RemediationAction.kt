package com.example.jarvis.data.model

import android.content.Intent
import android.provider.Settings

enum class RemediationAction(val intentAction: String, val buttonLabel: String) {
    OPEN_SECURITY_SETTINGS(Settings.ACTION_SECURITY_SETTINGS, "Open Security Settings"),
    OPEN_DEVELOPER_SETTINGS(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, "Open Developer Options"),
    OPEN_DEVICE_INFO(Settings.ACTION_DEVICE_INFO_SETTINGS, "Check System Updates"),
    OPEN_WIRELESS_SETTINGS(Settings.ACTION_WIRELESS_SETTINGS, "Open Network Settings"),
    OPEN_APPLICATION_SETTINGS(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, "Manage Applications"),
    OPEN_NOTIFICATION_LISTENER_SETTINGS(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, "Notification Access"),
    OPEN_ACCESSIBILITY_SETTINGS(Settings.ACTION_ACCESSIBILITY_SETTINGS, "Accessibility Settings"),
    OPEN_SYSTEM_UPDATE_SETTINGS(Settings.ACTION_DEVICE_INFO_SETTINGS, "Check System Updates"),
    OPEN_NOTIFICATION_SETTINGS(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, "Notification Settings"),
    OPEN_BATTERY_OPTIMIZATION_SETTINGS(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, "Battery Optimization"),
    OPEN_MANAGE_UNKNOWN_APP_SOURCES(
        "android.settings.MANAGE_UNKNOWN_APP_SOURCES",
        "Install Unknown Apps"
    )
}
