package com.example.jarvis.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jarvis.data.db.entities.NetworkTrafficEntity
import com.example.jarvis.ui.JarvisMainViewModel
import com.example.jarvis.ui.components.JarvisTopBar
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorderSubtle
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisGreenSecure
import com.example.ui.theme.JarvisRedCritical
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisWarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FirewallScreen(viewModel: JarvisMainViewModel) {
    val isFirewallActive by viewModel.isFirewallActive.collectAsStateWithLifecycle()
    val isGlobalKillSwitch by viewModel.isGlobalKillSwitch.collectAsStateWithLifecycle()
    val firewallRules by viewModel.firewallRules.collectAsStateWithLifecycle()
    val recentTraffic by viewModel.recentTraffic.collectAsStateWithLifecycle()
    val scannedApps by viewModel.scannedApps.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Live Traffic, 1: App Blocker
    var appSearchQuery by remember { mutableStateOf("") }
    var trafficFilter by remember { mutableStateOf("ALL") } // ALL, TRACKERS, BROWSER

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startFirewall()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) {
        JarvisTopBar(
            title = "MobiArmour Firewall",
            subtitle = "Zero-Trust Local Packet Inspection",
            onBackClick = { viewModel.navigateBack() },
            onSettingsClick = { viewModel.navigateTo("settings") }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header Card: Master Firewall Switch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
                .border(1.5.dp, if (isFirewallActive) JarvisGreenSecure.copy(alpha = 0.6f) else JarvisBorderSubtle, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isFirewallActive) JarvisGreenSecure.copy(alpha = 0.15f) else Color(0x33445566)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFirewallActive) Icons.Default.Shield else Icons.Default.Block,
                            contentDescription = "Firewall Status",
                            tint = if (isFirewallActive) JarvisGreenSecure else JarvisTextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "MOBIARMOUR FIREWALL",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isFirewallActive) "Active • Zero-Trust Tunnel" else "Inactive • Tap to enable filter",
                            color = if (isFirewallActive) JarvisGreenSecure else JarvisTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = isFirewallActive,
                    onCheckedChange = {
                        val prepareIntent = viewModel.getVpnPrepareIntent()
                        if (prepareIntent != null) {
                            vpnLauncher.launch(prepareIntent)
                        } else {
                            viewModel.toggleFirewall()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = JarvisGreenSecure,
                        checkedTrackColor = JarvisGreenSecure.copy(alpha = 0.3f),
                        uncheckedThumbColor = JarvisTextMuted,
                        uncheckedTrackColor = Color(0x33223344)
                    ),
                    modifier = Modifier.testTag("firewall_master_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Global Air-Gap / Kill-Switch Card (Block Whole Net)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(if (isGlobalKillSwitch) JarvisRedCritical.copy(alpha = 0.15f) else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    1.5.dp,
                    if (isGlobalKillSwitch) JarvisRedCritical.copy(alpha = 0.8f) else JarvisBorderSubtle,
                    RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isGlobalKillSwitch) JarvisRedCritical.copy(alpha = 0.25f) else Color(0x22FFB300)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Global Kill Switch",
                            tint = if (isGlobalKillSwitch) JarvisRedCritical else JarvisWarningAmber,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AIR-GAP KILL SWITCH",
                            color = if (isGlobalKillSwitch) JarvisRedCritical else androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (isGlobalKillSwitch) "WHOLE NET BLOCKED • All IP packets dropped" else "Standard mode • Selectively filter apps",
                            color = if (isGlobalKillSwitch) JarvisRedCritical else JarvisTextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isGlobalKillSwitch) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                Switch(
                    checked = isGlobalKillSwitch,
                    onCheckedChange = {
                        val prepareIntent = viewModel.getVpnPrepareIntent()
                        if (prepareIntent != null && !isFirewallActive) {
                            vpnLauncher.launch(prepareIntent)
                        } else {
                            viewModel.toggleGlobalKillSwitch()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = JarvisRedCritical,
                        checkedTrackColor = JarvisRedCritical.copy(alpha = 0.4f),
                        uncheckedThumbColor = JarvisTextMuted,
                        uncheckedTrackColor = Color(0x33223344)
                    ),
                    modifier = Modifier.testTag("global_kill_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs: "Where is Data Going" vs "App Blocker"
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = JarvisCyanPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = JarvisCyanPrimary,
                    height = 2.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        "Where Data Goes",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        "App Blocker (${firewallRules.count { it.isBlocked }})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 0) {
            // Live Traffic Destination Inspector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = trafficFilter == "ALL",
                    onClick = { trafficFilter = "ALL" },
                    label = { Text("All Endpoints", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JarvisCyanPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = JarvisCyanPrimary
                    )
                )
                FilterChip(
                    selected = trafficFilter == "TRACKERS",
                    onClick = { trafficFilter = "TRACKERS" },
                    label = { Text("Trackers & Ads", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JarvisRedCritical.copy(alpha = 0.2f),
                        selectedLabelColor = JarvisRedCritical
                    )
                )
                FilterChip(
                    selected = trafficFilter == "BROWSER",
                    onClick = { trafficFilter = "BROWSER" },
                    label = { Text("Browser", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JarvisGreenSecure.copy(alpha = 0.2f),
                        selectedLabelColor = JarvisGreenSecure
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val filteredTraffic = recentTraffic.filter { item ->
                when (trafficFilter) {
                    "TRACKERS" -> item.category == "TRACKER" || item.category == "AD_NETWORK"
                    "BROWSER" -> item.category == "BROWSER" || item.port == 80 || item.port == 443
                    else -> true
                }
            }

            if (filteredTraffic.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = JarvisTextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isFirewallActive) "Awaiting outbound app network requests..." else "Enable Firewall above to capture live outbound data destinations.",
                            color = JarvisTextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTraffic, key = { it.id }) { traffic ->
                        TrafficDestinationItem(
                            traffic = traffic,
                            onBlockApp = {
                                viewModel.toggleAppBlocked(traffic.packageName, traffic.appName, true)
                            }
                        )
                    }
                }
            }
        } else {
            // App Blocker list
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = appSearchQuery,
                    onValueChange = { appSearchQuery = it },
                    placeholder = { Text("Filter apps...", color = JarvisTextMuted, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = JarvisTextMuted, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyanPrimary,
                        unfocusedBorderColor = JarvisBorderSubtle,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.blockAllBackgroundApps() },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisRedCritical.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Block All BG", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val blockedMap = firewallRules.associate { it.packageName to it.isBlocked }
            val displayApps = scannedApps.filter {
                appSearchQuery.isEmpty() ||
                        it.appName.contains(appSearchQuery, ignoreCase = true) ||
                        it.packageName.contains(appSearchQuery, ignoreCase = true)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayApps, key = { it.packageName }) { app ->
                    val isBlocked = blockedMap[app.packageName] == true
                    AppBlockerRow(
                        app = app,
                        isBlocked = isBlocked,
                        onToggle = { block ->
                            viewModel.toggleAppBlocked(app.packageName, app.appName, block)
                        }
                    )
                }
            }
        }
    }
}
}

@Composable
fun TrafficDestinationItem(
    traffic: NetworkTrafficEntity,
    onBlockApp: () -> Unit
) {
    val isRisk = traffic.category == "TRACKER" || traffic.category == "AD_NETWORK"
    val timeStr = remember(traffic.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(traffic.timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x990A101E))
            .border(1.dp, if (isRisk) JarvisRedCritical.copy(alpha = 0.4f) else JarvisBorderSubtle, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeStr,
                        color = JarvisTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = traffic.appName,
                        color = JarvisCyanPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isRisk) JarvisRedCritical.copy(alpha = 0.2f)
                                else Color(0x33445566)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = traffic.category,
                            color = if (isRisk) JarvisRedCritical else JarvisTextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Destination: ${traffic.destinationHost}",
                    color = JarvisTextPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "IP: ${traffic.destinationIp}:${traffic.port} (${traffic.protocol})",
                    color = JarvisTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (!traffic.isBlocked) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisRedCritical.copy(alpha = 0.15f))
                        .border(1.dp, JarvisRedCritical.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable { onBlockApp() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Block App",
                        color = JarvisRedCritical,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33445566))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "BLOCKED",
                        color = JarvisTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun AppBlockerRow(
    app: com.example.jarvis.data.model.AppSecurityInfo,
    isBlocked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x990A101E))
            .border(1.dp, if (isBlocked) JarvisRedCritical.copy(alpha = 0.5f) else JarvisBorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        color = JarvisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isBlocked) JarvisRedCritical.copy(alpha = 0.2f) else JarvisGreenSecure.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isBlocked) "BLOCKED" else "PERMITTED",
                            color = if (isBlocked) JarvisRedCritical else JarvisGreenSecure,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = app.packageName,
                    color = JarvisTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = isBlocked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = JarvisRedCritical,
                    checkedTrackColor = JarvisRedCritical.copy(alpha = 0.4f),
                    uncheckedThumbColor = JarvisTextMuted,
                    uncheckedTrackColor = Color(0x33223344)
                )
            )
        }
    }
}
