package com.example.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jarvis.data.model.FindingSeverity
import com.example.jarvis.data.model.RemediationAction
import com.example.jarvis.ui.JarvisMainViewModel
import com.example.jarvis.ui.components.GlassCard
import com.example.jarvis.ui.components.JarvisTopBar
import com.example.jarvis.ui.components.SeverityBadge
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorderSubtle
import com.example.ui.theme.JarvisCriticalRed
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisSecureGreen
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.JarvisWarningAmber

data class HardeningItem(
    val id: String,
    val title: String,
    val description: String,
    val impact: String,
    val severity: FindingSeverity,
    val action: RemediationAction,
    val isHardened: Boolean
)

@Composable
fun HardeningScreen(
    viewModel: JarvisMainViewModel,
    modifier: Modifier = Modifier
) {
    val deviceChecks by viewModel.deviceChecks.collectAsStateWithLifecycle()
    val apps by viewModel.scannedApps.collectAsStateWithLifecycle()

    val screenLockCheck = deviceChecks.find { it.id == "screen_lock" }
    val adbCheck = deviceChecks.find { it.id == "adb_debugging" }
    val devCheck = deviceChecks.find { it.id == "developer_options" }
    val patchCheck = deviceChecks.find { it.id == "os_security_patch" }

    val hasHighRiskApps = apps.any { it.riskLevel == FindingSeverity.CRITICAL }

    val hardeningList = listOf(
        HardeningItem(
            id = "lock_screen",
            title = "Enforce Strong Screen Lock",
            description = "Biometrics, PIN, or passphrase safeguard device encryption keys and prevent unauthorized physical access.",
            impact = "Without a screen lock, Keystore hardware keys are exposed to physical compromise.",
            severity = if (screenLockCheck?.severity == FindingSeverity.CRITICAL) FindingSeverity.CRITICAL else FindingSeverity.VERIFIED_SECURE,
            action = RemediationAction.OPEN_SECURITY_SETTINGS,
            isHardened = screenLockCheck?.severity == FindingSeverity.VERIFIED_SECURE
        ),
        HardeningItem(
            id = "disable_adb",
            title = "Disable USB Debugging (ADB)",
            description = "USB Debugging allows connected computers to run shell commands, install APKs, and extract backups.",
            impact = "Protects against juice jacking, malicious charging kiosks, and stolen device exploitation.",
            severity = if (adbCheck?.severity == FindingSeverity.WARNING) FindingSeverity.WARNING else FindingSeverity.VERIFIED_SECURE,
            action = RemediationAction.OPEN_DEVELOPER_SETTINGS,
            isHardened = adbCheck?.severity == FindingSeverity.VERIFIED_SECURE
        ),
        HardeningItem(
            id = "unknown_sources",
            title = "Audit Unknown App Installers",
            description = "Inspect applications with authorization to download and trigger third-party APK installations.",
            impact = "Restricting dropper capabilities limits secondary payload installations.",
            severity = FindingSeverity.INFORMATIONAL,
            action = RemediationAction.OPEN_MANAGE_UNKNOWN_APP_SOURCES,
            isHardened = true
        ),
        HardeningItem(
            id = "system_update",
            title = "Check Google Security Bulletins",
            description = "Verify that your device is running the latest available monthly Android security update.",
            impact = "Patches known Linux kernel, Android framework, and hardware microcode vulnerabilities.",
            severity = if (patchCheck?.severity == FindingSeverity.WARNING) FindingSeverity.WARNING else FindingSeverity.VERIFIED_SECURE,
            action = RemediationAction.OPEN_DEVICE_INFO,
            isHardened = patchCheck?.severity == FindingSeverity.VERIFIED_SECURE
        ),
        HardeningItem(
            id = "audit_applications",
            title = "Review High-Risk Permissions",
            description = "Examine applications with Accessibility, Overlay, and SMS permissions.",
            impact = "Revoking unnecessary privileges minimizes spyware and tapjacking surfaces.",
            severity = if (hasHighRiskApps) FindingSeverity.WARNING else FindingSeverity.VERIFIED_SECURE,
            action = RemediationAction.OPEN_APPLICATION_SETTINGS,
            isHardened = !hasHighRiskApps
        ),
        HardeningItem(
            id = "accessibility_audit",
            title = "Audit Accessibility Services",
            description = "Accessibility services hold complete read and touch automation capabilities over your screen.",
            impact = "Ensure only assistive technologies you intentionally installed have active service bindings.",
            severity = FindingSeverity.INFORMATIONAL,
            action = RemediationAction.OPEN_ACCESSIBILITY_SETTINGS,
            isHardened = true
        ),
        HardeningItem(
            id = "private_dns_enforce",
            title = "Enforce Private DNS (DoT / DoH)",
            description = "Encrypt all DNS queries to block ISP domain surveillance, tracker resolution, and DNS spoofing.",
            impact = "Protects web history and blocks malicious domains at the DNS protocol layer.",
            severity = FindingSeverity.INFORMATIONAL,
            action = RemediationAction.OPEN_WIRELESS_SETTINGS,
            isHardened = true
        ),
        HardeningItem(
            id = "legacy_patch_mitigation",
            title = "Outdated OS Mitigation & Hardening",
            description = "For legacy Android versions without sandboxed storage or auto-reset permissions, revoke background network access and isolate APK droppers.",
            impact = "Compensates for missing OEM kernel patches through proactive application boundary enforcement.",
            severity = FindingSeverity.WARNING,
            action = RemediationAction.OPEN_SYSTEM_UPDATE_SETTINGS,
            isHardened = false
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
    ) {
        JarvisTopBar(
            title = "Hardening Center",
            subtitle = "Actionable Defensive Posture",
            onBackClick = { viewModel.navigateBack() },
            onRefreshClick = { viewModel.triggerScan() },
            onSettingsClick = { viewModel.navigateTo("settings") }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(JarvisCyanPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = JarvisCyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SYSTEM HARDENING PROTOCOLS",
                                color = JarvisTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Directly launch official system settings intents to resolve security exposures.",
                                color = JarvisTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            items(hardeningList, key = { it.id }) { item ->
                HardeningActionCard(
                    item = item,
                    onLaunch = { viewModel.launchRemediation(item.action) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun HardeningActionCard(
    item: HardeningItem,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.testTag("hardening_card_${item.id}")) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = JarvisTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (item.isHardened) "Verified Hardened" else "Remediation Recommended",
                        color = if (item.isHardened) JarvisSecureGreen else JarvisWarningAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                SeverityBadge(severity = item.severity)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.description,
                color = JarvisTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Defense Impact: ${item.impact}",
                color = JarvisTextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onLaunch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("hardening_button_${item.id}"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!item.isHardened) JarvisCyanPrimary else Color(0xFF1E2D4A),
                    contentColor = if (!item.isHardened) Color(0xFF070B14) else JarvisTextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.action.buttonLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
