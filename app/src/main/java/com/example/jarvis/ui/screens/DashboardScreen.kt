package com.example.jarvis.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jarvis.data.model.FindingSeverity
import com.example.jarvis.ui.JarvisMainViewModel
import com.example.jarvis.ui.components.GlassCard
import com.example.jarvis.ui.components.JarvisTopBar
import com.example.jarvis.ui.components.MetricPill
import com.example.jarvis.ui.components.ScoreGauge
import com.example.jarvis.ui.components.SeverityBadge
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorderGlow
import com.example.ui.theme.JarvisBorderSubtle
import com.example.ui.theme.JarvisCriticalRed
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisInfoBlue
import com.example.ui.theme.JarvisSecureGreen
import com.example.ui.theme.JarvisSurfaceCard
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.JarvisWarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: JarvisMainViewModel,
    modifier: Modifier = Modifier
) {
    val latestScan by viewModel.latestScan.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val scanMessage by viewModel.scanStepMessage.collectAsStateWithLifecycle()
    val deviceChecks by viewModel.deviceChecks.collectAsStateWithLifecycle()
    val deductions by viewModel.deductions.collectAsStateWithLifecycle()
    val events by viewModel.securityEvents.collectAsStateWithLifecycle()
    val isFirewallActive by viewModel.isFirewallActive.collectAsStateWithLifecycle()
    val memoryStatus by viewModel.memoryStatus.collectAsStateWithLifecycle()
    val firewallRules by viewModel.firewallRules.collectAsStateWithLifecycle()
    val alerts by viewModel.continuousAlerts.collectAsStateWithLifecycle()
    val isMonitoringActive by viewModel.isMonitoringActive.collectAsStateWithLifecycle()
    val deviceScore by viewModel.deviceScore.collectAsStateWithLifecycle()
    val appScore by viewModel.applicationScore.collectAsStateWithLifecycle()
    val netScore by viewModel.networkScore.collectAsStateWithLifecycle()
    val behavScore by viewModel.behaviorScore.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isGlobalKillSwitch by viewModel.isGlobalKillSwitch.collectAsStateWithLifecycle()
    val trustedDevices by viewModel.trustedDevices.collectAsStateWithLifecycle()

    val score = latestScan?.overallScore ?: if (deviceChecks.isNotEmpty()) {
        (100 - (deviceChecks.count { it.severity == FindingSeverity.CRITICAL } * 25) -
                (deviceChecks.count { it.severity == FindingSeverity.WARNING } * 10)).coerceIn(0, 100)
    } else 100

    val criticalCount = latestScan?.criticalCount ?: deviceChecks.count { it.severity == FindingSeverity.CRITICAL }
    val warningCount = latestScan?.warningCount ?: deviceChecks.count { it.severity == FindingSeverity.WARNING }
    val infoCount = latestScan?.infoCount ?: deviceChecks.count { it.severity == FindingSeverity.INFORMATIONAL }
    val secureCount = latestScan?.verifiedCount ?: deviceChecks.count { it.severity == FindingSeverity.VERIFIED_SECURE }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) {
        JarvisTopBar(
            title = "MOBIARMOUR",
            subtitle = "Android Security & Device Hardening",
            onSettingsClick = { viewModel.navigateTo("settings") },
            onRefreshClick = { viewModel.triggerScan() },
            onThemeToggle = { viewModel.toggleTheme() },
            isDarkTheme = isDarkMode
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Scanning progress banner (if active)
            if (isScanning) {
                item {
                    GlassCard(borderGlow = true) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(JarvisCyanPrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = scanMessage.uppercase(),
                                    color = JarvisCyanPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { scanProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = JarvisCyanPrimary,
                                trackColor = Color(0x3300E5FF)
                            )
                        }
                    }
                }
            }

            // Central Posture Card
            item {
                GlassCard(borderGlow = score >= 70) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ScoreGauge(score = score)

                        Spacer(modifier = Modifier.height(12.dp))

                        val summaryText = latestScan?.summaryText
                            ?: "Real-time hardware, operating system, and permission analysis active."
                        Text(
                            text = summaryText,
                            color = JarvisTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action buttons row: Scan Now & Security Check
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.triggerScan() },
                                enabled = !isScanning,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("dashboard_scan_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JarvisCyanPrimary,
                                    contentColor = Color(0xFF070B14)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isScanning) "SCANNING..." else "SCAN NOW",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            androidx.compose.material3.OutlinedButton(
                                onClick = { viewModel.navigateTo("scan") },
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(48.dp)
                                    .testTag("dashboard_view_checks_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = JarvisCyanPrimary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCyanPrimary.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SECURITY CHECK",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        if (latestScan != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
                                .format(Date(latestScan!!.timestamp))
                            Text(
                                text = "Last full scan: $dateStr",
                                color = JarvisTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Metric Counters Row (Clickable to open detailed checks audit)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill(
                        count = criticalCount,
                        label = "Critical",
                        accentColor = JarvisCriticalRed,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo("scan") }
                    )
                    MetricPill(
                        count = warningCount,
                        label = "Warnings",
                        accentColor = JarvisWarningAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo("scan") }
                    )
                    MetricPill(
                        count = infoCount,
                        label = "Info",
                        accentColor = JarvisInfoBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo("scan") }
                    )
                    MetricPill(
                        count = secureCount,
                        label = "Verified",
                        accentColor = JarvisSecureGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo("scan") }
                    )
                }
            }

            // MobiArmour 4 Security Sub-Scores Row
            item {
                GlassCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SECURITY POSTURE BREAKDOWN",
                                color = JarvisTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "4 DIMENSIONS",
                                color = JarvisCyanPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SubScoreBadge(title = "Device", score = deviceScore, modifier = Modifier.weight(1f))
                            SubScoreBadge(title = "Apps", score = appScore, modifier = Modifier.weight(1f))
                            SubScoreBadge(title = "Network", score = netScore, modifier = Modifier.weight(1f))
                            SubScoreBadge(title = "Behavior", score = behavScore, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Sentinel Continuous Security Monitor Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0x3300E5FF), Color(0x220D1527))
                            )
                        )
                        .border(1.dp, JarvisCyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .clickable { viewModel.navigateTo("toolkit") }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x2200E5FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = JarvisCyanPrimary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isMonitoringActive) JarvisSecureGreen else JarvisWarningAmber)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isMonitoringActive) "CONTINUOUS SENTINEL ACTIVE" else "SENTINEL PAUSED",
                                        color = if (isMonitoringActive) JarvisSecureGreen else JarvisWarningAmber,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (alerts.isNotEmpty()) "${alerts.size} Explainable Threat Findings Detected" else "Real-time device & permission posture protected",
                                    color = JarvisTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap to open Security Toolkit & Hardening",
                                    color = JarvisTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = JarvisCyanPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Legacy / Outdated Device Hardening Alert
            if (Build.VERSION.SDK_INT < 33) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x33FFAA00))
                            .border(1.dp, JarvisWarningAmber, RoundedCornerShape(16.dp))
                            .clickable { viewModel.navigateTo("harden") }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = JarvisWarningAmber, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "LEGACY ANDROID ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                                    color = JarvisWarningAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                Text(
                                    text = "Device lacks modern sandboxing. Tap to review recommended Outdated Device Hardening patches.",
                                    color = JarvisTextPrimary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = JarvisWarningAmber, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Interactive Defense Modules Hub (2x2 Grid)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Tile 1: Firewall
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x990D1527))
                                .border(1.dp, if (isFirewallActive) JarvisSecureGreen.copy(alpha = 0.5f) else JarvisBorderSubtle, RoundedCornerShape(16.dp))
                                .clickable { viewModel.navigateTo("firewall") }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (isFirewallActive) JarvisSecureGreen else JarvisTextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isFirewallActive) JarvisSecureGreen.copy(alpha = 0.2f) else Color(0x33445566))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isFirewallActive) "ON" else "OFF",
                                            color = if (isFirewallActive) JarvisSecureGreen else JarvisTextMuted,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "FIREWALL",
                                    color = JarvisTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                Text(
                                    text = "${firewallRules.count { it.isBlocked }} apps blocked",
                                    color = JarvisTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Tile 2: Turbo RAM
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x990D1527))
                                .border(1.dp, JarvisCyanPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .clickable { viewModel.navigateTo("performance") }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = JarvisCyanPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "${memoryStatus.usedPercentage}% RAM",
                                        color = JarvisCyanPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "TURBO RAM",
                                    color = JarvisTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                Text(
                                    text = "Gaming accelerator",
                                    color = JarvisTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Tile 3: Anti-Phish
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x990D1527))
                                .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(16.dp))
                                .clickable { viewModel.navigateTo("phishing") }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = JarvisSecureGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "PROTECTED",
                                        color = JarvisSecureGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "ANTI-PHISH",
                                    color = JarvisTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                Text(
                                    text = "Pre-click link guard",
                                    color = JarvisTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Tile 4: System Logs
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x990D1527))
                                .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(16.dp))
                                .clickable { viewModel.navigateTo("logs") }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = JarvisCyanPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "LIVE",
                                        color = JarvisCyanPrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "SYSTEM LOGS",
                                    color = JarvisTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                Text(
                                    text = "Logcat & Audit Stream",
                                    color = JarvisTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Device Profile & Telemetry Card
            item {
                GlassCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = JarvisCyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DEVICE HARDWARE & SYSTEM",
                                color = JarvisTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Model", color = JarvisTextMuted, fontSize = 12.sp)
                            Text(
                                text = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
                                color = JarvisTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Android Version", color = JarvisTextMuted, fontSize = 12.sp)
                            Text(
                                text = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                                color = JarvisTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Security Patch", color = JarvisTextMuted, fontSize = 12.sp)
                            Text(
                                text = Build.VERSION.SECURITY_PATCH ?: "Unavailable",
                                color = JarvisCyanPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Scoring Deductions Breakdown (if any)
            if (deductions.isNotEmpty()) {
                item {
                    GlassCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = JarvisWarningAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DEDUCTION AUDIT (${deductions.sumOf { it.pointsLost }} PTS DEDUCTED)",
                                    color = JarvisTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            deductions.forEach { deduction ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "-${deduction.pointsLost}",
                                        color = JarvisCriticalRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(32.dp)
                                    )
                                    Column {
                                        Text(
                                            text = deduction.title,
                                            color = JarvisTextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = deduction.reason,
                                            color = JarvisTextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions: Hardening Center & Security Events Shortcut
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo("harden") }
                    ) {
                        Column {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = JarvisCyanPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Hardening Center",
                                color = JarvisTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Actionable remediations",
                                color = JarvisTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    GlassCard(
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo("events") }
                    ) {
                        Column {
                            Icon(
                                imageVector = Icons.Default.Policy,
                                contentDescription = null,
                                tint = JarvisInfoBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Security Events",
                                color = JarvisTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${events.size} logged events",
                                color = JarvisTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Layer 6: Lost Device Emergency Action Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(Color(0x33FFAA00), Color(0x1A0D1527))
                            )
                        )
                        .border(1.dp, JarvisWarningAmber.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { viewModel.navigateTo("lost_device") }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(JarvisWarningAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = JarvisWarningAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "LOST-DEVICE ACTIONS (LAYER 6)",
                                    color = JarvisTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                Text(
                                    text = "Emergency siren, device lock & recovery guide",
                                    color = JarvisTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = JarvisWarningAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun SubScoreBadge(
    title: String,
    score: Int,
    modifier: Modifier = Modifier
) {
    val badgeColor = when {
        score >= 85 -> JarvisSecureGreen
        score >= 65 -> JarvisWarningAmber
        else -> JarvisCriticalRed
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x330D1527))
            .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score%",
                color = badgeColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title.uppercase(),
                color = JarvisTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp
            )
        }
    }
}
