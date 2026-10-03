package com.example.jarvis.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBorderSubtle
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisTextMuted

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "HUD", Icons.Default.Security)
    object Toolkit : Screen("toolkit", "Toolkit", Icons.Default.Build)
    object Firewall : Screen("firewall", "Firewall", Icons.Default.Shield)
    object Performance : Screen("performance", "Turbo RAM", Icons.Default.Bolt)
    object Phishing : Screen("phishing", "Anti-Phish", Icons.Default.Link)
    object Apps : Screen("apps", "Auditor", Icons.Default.Apps)
    object Harden : Screen("harden", "Harden", Icons.Default.Policy)
    object LostDevice : Screen("lost_device", "Lost Device", Icons.Default.Security)
    object Logs : Screen("logs", "Logs", Icons.Default.Terminal)
    object Scan : Screen("scan", "Scan", Icons.Default.Search)
    object Permissions : Screen("permissions", "Perms", Icons.Default.Key)
    object Events : Screen("events", "Events", Icons.Default.Policy)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val BottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.Toolkit,
    Screen.Firewall,
    Screen.Phishing,
    Screen.Apps
)

@Composable
fun JarvisBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background == JarvisBackground
    val navBg = if (isDark) Color(0xE60A101D) else Color(0xF2FFFFFF)
    val navBorder = if (isDark) JarvisBorderSubtle else Color(0x260284C7)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(navBg)
            .border(1.dp, navBorder, RoundedCornerShape(24.dp))
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            BottomNavScreens.forEach { screen ->
                val isSelected = currentRoute == screen.route
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onNavigate(screen.route) },
                    icon = {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = screen.title,
                            fontSize = 9.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_item_${screen.route}")
                )
            }
        }
    }
}
