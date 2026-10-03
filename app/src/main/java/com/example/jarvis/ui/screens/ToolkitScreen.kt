package com.example.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jarvis.data.model.FindingSeverity
import com.example.jarvis.domain.AlertCategory
import com.example.jarvis.domain.ExplainableAlert
import com.example.jarvis.ui.JarvisMainViewModel
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorderSubtle
import com.example.ui.theme.JarvisCriticalRed
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisInfoBlue
import com.example.ui.theme.JarvisSecureGreen
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisWarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ToolkitScreen(viewModel: JarvisMainViewModel) {
    val alerts by viewModel.continuousAlerts.collectAsStateWithLifecycle()
    val isMonitoringActive by viewModel.isMonitoringActive.collectAsStateWithLifecycle()
    val lastCheckTime by viewModel.lastMonitorTimestamp.collectAsStateWithLifecycle()

    val timeFormatted = remember(lastCheckTime) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastCheckTime))
    }

    val criticalAlerts = alerts.count { it.severity == FindingSeverity.CRITICAL }
    val warningAlerts = alerts.count { it.severity == FindingSeverity.WARNING }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "SECURITY TOOLKIT",
                        color = JarvisCyanPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Continuous No-Root Sentinel & Diagnostics",
                        color = JarvisTextMuted,
                        fontSize = 12.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.refreshContinuousMonitor() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x2200E5FF))
                            .size(38.dp)
                            .testTag("refresh_sentinel_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Sentinel", tint = JarvisCyanPrimary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { viewModel.toggleContinuousMonitor() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isMonitoringActive) Color(0x2200E676)
                                else Color(0x22FFB300)
                            )
                            .size(38.dp)
                            .testTag("toggle_sentinel_button")
                    ) {
                        Icon(
                            imageVector = if (isMonitoringActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isMonitoringActive) "Pause Sentinel" else "Resume Sentinel",
                            tint = if (isMonitoringActive) JarvisSecureGreen else JarvisWarningAmber
                        )
                    }
                }
            }
        }

        // Sentinel Live Status Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (isMonitoringActive) Color(0x3300E5FF) else Color(0x22FFB300),
                                JarvisSurfaceDark
                            )
                        )
                    )
                    .border(
                        1.dp,
                        if (isMonitoringActive) JarvisCyanPrimary.copy(alpha = 0.5f) else JarvisWarningAmber.copy(alpha = 0.4f),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isMonitoringActive) JarvisSecureGreen else JarvisWarningAmber)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isMonitoringActive) "SENTINEL LAYER ACTIVE" else "SENTINEL LAYER PAUSED",
                                color = if (isMonitoringActive) JarvisSecureGreen else JarvisWarningAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = "Last probe: $timeFormatted",
                            color = JarvisTextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SentinelStatPill(
                            label = "CRITICAL",
                            value = criticalAlerts.toString(),
                            color = if (criticalAlerts > 0) JarvisCriticalRed else JarvisTextMuted,
                            modifier = Modifier.weight(1f)
                        )
                        SentinelStatPill(
                            label = "WARNINGS",
                            value = warningAlerts.toString(),
                            color = if (warningAlerts > 0) JarvisWarningAmber else JarvisTextMuted,
                            modifier = Modifier.weight(1f)
                        )
                        SentinelStatPill(
                            label = "ACTIVE RULES",
                            value = (alerts.size).toString(),
                            color = JarvisCyanPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section Title: Explainable Alerts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "EXPLAINABLE ALERTS & HARDENING",
                    color = JarvisTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "${alerts.size} Findings",
                    color = JarvisCyanPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (alerts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x990A101E))
                        .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(14.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = JarvisSecureGreen,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "All Security Checks Passed",
                            color = JarvisTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "No suspicious permission abuses, ADB leaks, or network risks detected.",
                            color = JarvisTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(alerts, key = { it.id }) { alert ->
                ExplainableAlertCard(
                    alert = alert,
                    onRemediate = {
                        if (alert.targetPackageName != null) {
                            viewModel.openAppDetails(alert.targetPackageName)
                        } else if (alert.remediationAction != null) {
                            viewModel.launchRemediation(alert.remediationAction)
                        }
                    }
                )
            }
        }

        // Section Title: Complete Security Utilities Toolkit
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SECURITY UTILITY MODULES",
                color = JarvisTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        // Grid of All 8 Security Tools
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolkitModuleCard(
                        title = "Live Threat Scanner",
                        subtitle = "OS, DEX & Hygiene",
                        icon = Icons.Default.Search,
                        badge = "AUDIT",
                        color = JarvisCyanPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo("scan") }
                    )
                    ToolkitModuleCard(
                        title = "In-Built Firewall",
                        subtitle = "Block per-app traffic",
                        icon = Icons.Default.Shield,
                        badge = "VPN NET",
                        color = JarvisSecureGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo("firewall") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolkitModuleCard(
                        title = "Turbo RAM Gaming",
                        subtitle = "Heap flush & latency",
                        icon = Icons.Default.Bolt,
                        badge = "BOOST",
                        color = Color(0xFFFF9100),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo("performance") }
                    )
                    ToolkitModuleCard(
                        title = "Pre-Click Anti-Phish",
                        subtitle = "Homograph & Link Gate",
                        icon = Icons.Default.Link,
                        badge = "SHIELD",
                        color = JarvisInfoBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo("phishing") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolkitModuleCard(
                        title = "App & Adware Auditor",
                        subtitle = "Bytecode & AI whitelist",
                        icon = Icons.Default.Apps,
                        badge = "DEX AUDIT",
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo("apps") }
                    )
                    ToolkitModuleCard(
                        title = "Privacy Matrix",
                        subtitle = "Sensors & Revocation",
                        icon = Icons.Default.Key,
                        badge = "PERMS",
                        color = Color(0xFFE040FB),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo("permissions") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolkitModuleCard(
                        title = "OS Hardening Hub",
                        subtitle = "Legacy OS patches",
                        icon = Icons.Default.Policy,
                        badge = "PATCHES",
                        color = JarvisWarningAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo("harden") }
                    )
                    ToolkitModuleCard(
                        title = "System Logcat Hub",
                        subtitle = "Real-time security logs",
                        icon = Icons.Default.Terminal,
                        badge = "CONSOLE",
                        color = Color(0xFF76FF03),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo("logs") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolkitModuleCard(
                        title = "Lost-Device Actions",
                        subtitle = "Siren, lock & find guide",
                        icon = Icons.Default.Security,
                        badge = "LAYER 6",
                        color = Color(0xFFFF5252),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo("lost_device") }
                    )
                    ToolkitModuleCard(
                        title = "Firewall & Filtering",
                        subtitle = "App network protection",
                        icon = Icons.Default.Shield,
                        badge = "VPN SHIELD",
                        color = Color(0xFF00E676),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo("firewall") }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SentinelStatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x660A101E))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = label,
                color = JarvisTextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun ExplainableAlertCard(
    alert: ExplainableAlert,
    onRemediate: () -> Unit
) {
    val severityColor = when (alert.severity) {
        FindingSeverity.CRITICAL -> JarvisCriticalRed
        FindingSeverity.WARNING -> JarvisWarningAmber
        FindingSeverity.INFORMATIONAL -> JarvisInfoBlue
        FindingSeverity.VERIFIED_SECURE -> JarvisSecureGreen
        else -> JarvisInfoBlue
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x990A101E))
            .border(1.dp, severityColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            // Header Row: Category Badge + Severity + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(severityColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = alert.category.displayName.uppercase(),
                        color = severityColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = alert.severity.name,
                    color = severityColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = alert.title,
                color = JarvisTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Explanation Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33101826))
                    .padding(8.dp)
            ) {
                Text(
                    text = "WHY IT'S FLAGGED:",
                    color = JarvisCyanPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = alert.explanation,
                    color = JarvisTextPrimary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "THREAT VECTOR & IMPACT:",
                    color = JarvisWarningAmber,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = alert.threatImpact,
                    color = JarvisTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "RECOMMENDED HARDENING:",
                    color = JarvisSecureGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = alert.hardeningAdvice,
                    color = JarvisTextPrimary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            if (alert.remediationAction != null || alert.targetPackageName != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onRemediate,
                        colors = ButtonDefaults.buttonColors(containerColor = severityColor.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (alert.targetPackageName != null) "Manage App" else (alert.remediationAction?.buttonLabel ?: "Harden Now"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolkitModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x990A101E))
            .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = color,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                color = JarvisTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                color = JarvisTextMuted,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
