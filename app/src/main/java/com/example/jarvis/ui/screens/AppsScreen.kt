package com.example.jarvis.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jarvis.data.model.AppSecurityInfo
import com.example.jarvis.data.model.FindingSeverity
import com.example.jarvis.ui.JarvisMainViewModel
import com.example.jarvis.ui.components.GlassCard
import com.example.jarvis.ui.components.JarvisTopBar
import com.example.jarvis.ui.components.SeverityBadge
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorderSubtle
import com.example.ui.theme.JarvisCriticalRed
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisSecureGreen
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.JarvisWarningAmber

fun launchUninstallPackage(context: Context, packageName: String) {
    try {
        val deleteIntent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(deleteIntent)
    } catch (e: Exception) {
        try {
            val settingsIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Cannot open uninstaller for $packageName", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun AppsScreen(
    viewModel: JarvisMainViewModel,
    modifier: Modifier = Modifier
) {
    val apps by viewModel.scannedApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.appSearchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.appFilterType.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    val malwareCount = remember(apps) { apps.count { it.isMalwareThreat } }
    val hiddenCount = remember(apps) { apps.count { it.isHiddenApp } }

    val filteredApps = remember(apps, searchQuery, filterType) {
        apps.filter { app ->
            val matchesQuery = searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (filterType) {
                "MALWARE" -> app.isMalwareThreat || app.riskLevel == FindingSeverity.CRITICAL
                "HIDDEN" -> app.isHiddenApp
                "HIGH_RISK" -> app.riskLevel == FindingSeverity.CRITICAL || app.riskLevel == FindingSeverity.WARNING || app.isMalwareThreat
                "ADWARE" -> app.detectedAdwareSdks.isNotEmpty()
                "PERSONAL_DEV" -> app.isPersonalDeveloperApp || app.isTrustedByUser
                "USER" -> !app.isSystemApp
                "SYSTEM" -> app.isSystemApp
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        JarvisTopBar(
            title = "APPLICATION AUDITOR",
            subtitle = "Deep APK archive & DEX bytecode analysis (${apps.size} scanned)",
            onBackClick = { viewModel.navigateBack() },
            onSettingsClick = { viewModel.navigateTo("settings") },
            onThemeToggle = { viewModel.toggleTheme() },
            isDarkTheme = isDarkMode
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Critical Threat Action Center (if hidden apps or malware found)
        if (malwareCount > 0 || hiddenCount > 0) {
            val threats = remember(apps) { apps.filter { it.isMalwareThreat || it.isHiddenApp } }
            val context = LocalContext.current

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(JarvisCriticalRed.copy(alpha = 0.15f))
                    .border(1.5.dp, JarvisCriticalRed.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Threat Detected",
                                tint = JarvisCriticalRed,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "THREATS FOUND: $malwareCount MALWARE, $hiddenCount HIDDEN",
                                    color = JarvisCriticalRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Purge stealth packages or quarantine internet access immediately.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    threats.forEach { threat ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x33000000))
                                .border(1.dp, JarvisCriticalRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = threat.appName,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = threat.packageName,
                                            color = JarvisTextMuted,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(JarvisCriticalRed.copy(alpha = 0.3f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (threat.isHiddenApp) "STEALTH HIDDEN" else "TROJAN MALWARE",
                                            color = JarvisCriticalRed,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!threat.isSystemApp) {
                                        Button(
                                            onClick = { launchUninstallPackage(context, threat.packageName) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .testTag("banner_uninstall_${threat.packageName}"),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = JarvisCriticalRed,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("UNINSTALL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = { viewModel.openAppDetails(threat.packageName) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .testTag("banner_settings_${threat.packageName}"),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0x33FFB300),
                                                contentColor = JarvisWarningAmber
                                            )
                                        ) {
                                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("SETTINGS (SYSTEM)", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.toggleAppBlocked(threat.packageName, threat.appName, true) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .testTag("banner_block_${threat.packageName}"),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = JarvisCyanPrimary
                                        ),
                                        border = BorderStroke(1.dp, JarvisCyanPrimary.copy(alpha = 0.5f))
                                    ) {
                                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("BLOCK NET", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.toggleTrustApp(threat, true) },
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("banner_trust_${threat.packageName}"),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = JarvisSecureGreen
                                        ),
                                        border = BorderStroke(1.dp, JarvisSecureGreen.copy(alpha = 0.5f))
                                    ) {
                                        Text("TRUST", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setAppSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_search_input"),
            placeholder = {
                Text(
                    text = "Search by app name or package...",
                    color = JarvisTextMuted,
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setAppSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = JarvisTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = JarvisBorderSubtle,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chips = listOf(
                "ALL" to "All Apps (${apps.size})",
                "MALWARE" to "Malware ($malwareCount)",
                "HIDDEN" to "Hidden ($hiddenCount)",
                "HIGH_RISK" to "Risk Flags",
                "ADWARE" to "Adware SDKs",
                "PERSONAL_DEV" to "Dev / AI Apps",
                "USER" to "User Apps",
                "SYSTEM" to "System Apps"
            )

            items(chips) { (type, label) ->
                val isSelected = filterType == type
                val isMalwareChip = type == "MALWARE" && malwareCount > 0
                val isHiddenChip = type == "HIDDEN" && hiddenCount > 0

                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setAppFilterType(type) },
                    label = {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected || isMalwareChip) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isMalwareChip) JarvisCriticalRed.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = if (isMalwareChip) JarvisCriticalRed else MaterialTheme.colorScheme.primary,
                        containerColor = if (isMalwareChip) JarvisCriticalRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = if (isMalwareChip) JarvisCriticalRed else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isMalwareChip) JarvisCriticalRed else if (isSelected) MaterialTheme.colorScheme.primary else JarvisBorderSubtle
                    ),
                    modifier = Modifier.testTag("filter_chip_$type")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // App List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("scanned_apps_list"),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredApps.isEmpty()) {
                item {
                    GlassCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = JarvisTextMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Applications Found",
                                color = JarvisTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try adjusting your search query or filter tags.",
                                color = JarvisTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredApps, key = { it.packageName }) { appInfo ->
                    AppSecurityCard(
                        app = appInfo,
                        onOpenSettings = { viewModel.openAppDetails(appInfo.packageName) },
                        onToggleTrust = { isTrusted ->
                            viewModel.toggleTrustApp(appInfo, isTrusted)
                        },
                        onBlockInFirewall = { shouldBlock ->
                            viewModel.toggleAppBlocked(appInfo.packageName, appInfo.appName, shouldBlock)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun AppSecurityCard(
    app: AppSecurityInfo,
    onOpenSettings: () -> Unit,
    onToggleTrust: (Boolean) -> Unit,
    onBlockInFirewall: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = modifier.testTag("app_card_${app.packageName}"),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App initial avatar
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF132038))
                        .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.firstOrNull()?.uppercase() ?: "?",
                        color = JarvisCyanPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.appName,
                            color = JarvisTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (app.isTrustedByUser) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(JarvisSecureGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "TRUSTED DEV",
                                    color = JarvisSecureGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else if (app.isPersonalDeveloperApp) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(JarvisCyanPrimary.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PERSONAL / AI APP",
                                    color = JarvisCyanPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Text(
                        text = app.packageName,
                        color = JarvisTextMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                SeverityBadge(severity = if (app.isTrustedByUser) FindingSeverity.VERIFIED_SECURE else app.riskLevel)

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = JarvisTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (app.isMalwareThreat) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(JarvisCriticalRed.copy(alpha = 0.25f))
                            .border(1.dp, JarvisCriticalRed.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "🚨 MALWARE THREAT",
                            color = JarvisCriticalRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (app.isHiddenApp) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(JarvisWarningAmber.copy(alpha = 0.2f))
                            .border(1.dp, JarvisWarningAmber.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "👁️ HIDDEN APP",
                            color = JarvisWarningAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Code Verification verdict badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (app.detectedAdwareSdks.isNotEmpty()) JarvisCriticalRed.copy(alpha = 0.2f)
                            else if (app.isTrustedByUser) JarvisSecureGreen.copy(alpha = 0.2f)
                            else Color(0x33446688)
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (app.detectedAdwareSdks.isNotEmpty()) "ADWARE DETECTED" else app.codeVerificationVerdict,
                        color = if (app.detectedAdwareSdks.isNotEmpty()) JarvisCriticalRed else if (app.isTrustedByUser) JarvisSecureGreen else JarvisCyanPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (app.isDebuggable) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33FF3B5C))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "DEBUGGABLE",
                            color = JarvisCriticalRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Risk: ${if (app.isTrustedByUser) 0 else app.riskScore}/100",
                    color = when {
                        app.isTrustedByUser -> JarvisSecureGreen
                        app.riskScore >= 55 -> JarvisCriticalRed
                        app.riskScore >= 35 -> JarvisWarningAmber
                        else -> JarvisSecureGreen
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (app.isMalwareThreat || app.isHiddenApp) {
                Spacer(modifier = Modifier.height(8.dp))
                val cardContext = LocalContext.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!app.isSystemApp) {
                        Button(
                            onClick = { launchUninstallPackage(cardContext, app.packageName) },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("card_quick_uninstall_${app.packageName}"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCriticalRed,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("UNINSTALL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("card_quick_settings_${app.packageName}"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x33FFB300),
                                contentColor = JarvisWarningAmber
                            )
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SETTINGS (SYSTEM)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { onBlockInFirewall(true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("card_quick_block_${app.packageName}"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = JarvisCyanPrimary
                        ),
                        border = BorderStroke(1.dp, JarvisCyanPrimary.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BLOCK NET", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
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

                    // Deep Code Inspection Findings Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x80080D18))
                            .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = JarvisCyanPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DEEP APK ARCHIVE & CODE AUDIT",
                                    color = JarvisCyanPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "• Keystore Signature: ${app.signatureType}", color = JarvisTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(text = "• DEX Archive Classes: ${app.dexClassCount} enumerated", color = JarvisTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            if (app.nativeLibraries.isNotEmpty()) {
                                Text(text = "• Native Libs (.so): ${app.nativeLibraries.take(4).joinToString(", ")}", color = JarvisTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }

                            if (app.detectedAdwareSdks.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "🚨 ADWARE & TRACKER SDKS DETECTED:",
                                    color = JarvisCriticalRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                app.detectedAdwareSdks.forEach { sdk ->
                                    Text(text = "  - $sdk", color = JarvisCriticalRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }

                            if (app.detectedMalwarePatterns.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "⚠️ SUSPICIOUS ARCHITECTURE PATTERNS:",
                                    color = JarvisWarningAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                app.detectedMalwarePatterns.forEach { pat ->
                                    Text(text = "  - $pat", color = JarvisWarningAmber, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Personal Project Whitelist / Trust Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x550C182B))
                            .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Trust as Personal / AI Project",
                                color = JarvisTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Eliminates false positives and excludes from risk deductions.",
                                color = JarvisTextMuted,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = app.isTrustedByUser,
                            onCheckedChange = onToggleTrust,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisSecureGreen,
                                checkedTrackColor = JarvisSecureGreen.copy(alpha = 0.3f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (app.riskFactors.isNotEmpty()) {
                        Text(
                            text = "DETECTED BEHAVIORAL FACTORS",
                            color = JarvisWarningAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        app.riskFactors.forEach { factor ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "• ",
                                    color = JarvisWarningAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = factor,
                                    color = JarvisTextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (app.grantedDangerousPermissions.isNotEmpty()) {
                        Text(
                            text = "GRANTED SENSITIVE PERMISSIONS (${app.grantedDangerousPermissions.size})",
                            color = JarvisTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        app.grantedDangerousPermissions.take(6).forEach { perm ->
                            Text(
                                text = "✓ ${perm.substringAfterLast(".")}",
                                color = JarvisCyanPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (app.grantedDangerousPermissions.size > 6) {
                            Text(
                                text = "+${app.grantedDangerousPermissions.size - 6} more permissions",
                                color = JarvisTextMuted,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Button: Deep Link to Revoke Permissions
                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("open_settings_${app.packageName}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Manage & Revoke Permissions in Settings",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!app.isSystemApp) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val ctx = LocalContext.current
                        Button(
                            onClick = {
                                launchUninstallPackage(ctx, app.packageName)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("uninstall_app_${app.packageName}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (app.isMalwareThreat) JarvisCriticalRed else JarvisCriticalRed.copy(alpha = 0.2f),
                                contentColor = if (app.isMalwareThreat) Color.White else JarvisCriticalRed
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (app.isMalwareThreat) "QUARANTINE / UNINSTALL MALWARE" else "UNINSTALL APPLICATION",
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
