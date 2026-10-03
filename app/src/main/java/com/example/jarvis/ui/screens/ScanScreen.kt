package com.example.jarvis.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jarvis.data.model.CheckCategory
import com.example.jarvis.data.model.DeviceCheckItem
import com.example.jarvis.data.model.FindingSeverity
import com.example.jarvis.ui.JarvisMainViewModel
import com.example.jarvis.ui.components.GlassCard
import com.example.jarvis.ui.components.JarvisTopBar
import com.example.jarvis.ui.components.SeverityBadge
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorderSubtle
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisSecureGreen
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.JarvisWarningAmber

@Composable
fun ScanScreen(
    viewModel: JarvisMainViewModel,
    modifier: Modifier = Modifier
) {
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val scanMessage by viewModel.scanStepMessage.collectAsStateWithLifecycle()
    val deviceChecks by viewModel.deviceChecks.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredChecks = remember(deviceChecks, selectedFilter) {
        when (selectedFilter) {
            "EXPOSURES" -> deviceChecks.filter { it.severity == FindingSeverity.CRITICAL || it.severity == FindingSeverity.WARNING }
            "SECURE" -> deviceChecks.filter { it.severity == FindingSeverity.VERIFIED_SECURE }
            "INTEGRITY" -> deviceChecks.filter { it.category == CheckCategory.PLATFORM_INTEGRITY }
            "NETWORK" -> deviceChecks.filter { it.category == CheckCategory.NETWORK_TELEMETRY }
            "LOCK & STORAGE" -> deviceChecks.filter { it.category == CheckCategory.DEVICE_LOCK || it.category == CheckCategory.STORAGE_ENCRYPTION }
            else -> deviceChecks
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
    ) {
        JarvisTopBar(
            title = "Security Check & Audit",
            subtitle = "${deviceChecks.size} Hardware, OS & Policy Checks",
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
            // Scanner Control Header
            item {
                GlassCard(borderGlow = isScanning) {
                    Column(modifier = Modifier.fillMaxWidth()) {
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
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = JarvisCyanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isScanning) "ACTIVE AUDIT IN PROGRESS" else "SECURITY CHECK OPERATIONAL",
                                    color = JarvisTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = scanMessage,
                                    color = if (isScanning) JarvisCyanPrimary else JarvisTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (isScanning) {
                            Spacer(modifier = Modifier.height(14.dp))
                            LinearProgressIndicator(
                                progress = { scanProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = JarvisCyanPrimary,
                                trackColor = Color(0x3300E5FF)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${(scanProgress * 100).toInt()}% completed",
                                color = JarvisTextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.End)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.triggerScan() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("scan_trigger_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JarvisCyanPrimary,
                                    contentColor = Color(0xFF070B14)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "RE-RUN FULL SECURITY CHECK",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // Quick Category Filter Row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filterOptions = listOf(
                        "ALL" to "All (${deviceChecks.size})",
                        "EXPOSURES" to "Exposures (${deviceChecks.count { it.severity == FindingSeverity.CRITICAL || it.severity == FindingSeverity.WARNING }})",
                        "SECURE" to "Verified (${deviceChecks.count { it.severity == FindingSeverity.VERIFIED_SECURE }})",
                        "INTEGRITY" to "Platform Integrity",
                        "LOCK & STORAGE" to "Lock & Storage",
                        "NETWORK" to "Network & DNS"
                    )
                    items(filterOptions.size) { index ->
                        val (filterKey, filterLabel) = filterOptions[index]
                        val isSelected = selectedFilter == filterKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filterKey },
                            label = {
                                Text(
                                    text = filterLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JarvisCyanPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = JarvisCyanPrimary,
                                containerColor = Color(0x330D1527),
                                labelColor = JarvisTextMuted
                            )
                        )
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "AUDIT FINDINGS (${filteredChecks.size})",
                        color = JarvisCyanPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "${filteredChecks.count { it.severity == FindingSeverity.VERIFIED_SECURE }} Verified Secure",
                        color = JarvisSecureGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Checks List
            items(filteredChecks, key = { it.id }) { checkItem ->
                DeviceCheckCard(
                    check = checkItem,
                    onRemediate = { action -> viewModel.launchRemediation(action) }
                )
            }
        }
    }
}

@Composable
fun DeviceCheckCard(
    check: DeviceCheckItem,
    onRemediate: (com.example.jarvis.data.model.RemediationAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = modifier.testTag("check_item_${check.id}"),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = check.title,
                        color = JarvisTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = check.detectedValue,
                        color = when (check.severity) {
                            FindingSeverity.CRITICAL -> com.example.ui.theme.JarvisCriticalRed
                            FindingSeverity.WARNING -> JarvisWarningAmber
                            FindingSeverity.VERIFIED_SECURE -> JarvisSecureGreen
                            else -> JarvisTextSecondary
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                SeverityBadge(severity = check.severity)

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = JarvisTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(JarvisBorderSubtle)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "DESCRIPTION",
                        color = JarvisTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = check.description,
                        color = JarvisTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "TECHNICAL EXPLANATION",
                        color = JarvisTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = check.technicalExplanation,
                        color = JarvisTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "RECOMMENDATION",
                        color = JarvisTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = check.recommendation,
                        color = JarvisTextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (check.remediationAction != null && check.severity != FindingSeverity.VERIFIED_SECURE) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onRemediate(check.remediationAction) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("remediate_button_${check.id}"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyanPrimary.copy(alpha = 0.2f),
                                contentColor = JarvisCyanPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = check.remediationAction.buttonLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
