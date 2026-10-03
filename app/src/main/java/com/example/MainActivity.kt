package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jarvis.ui.JarvisMainViewModel
import com.example.jarvis.ui.navigation.JarvisBottomNav
import com.example.jarvis.ui.screens.AppsScreen
import com.example.jarvis.ui.screens.DashboardScreen
import com.example.jarvis.ui.screens.EventsScreen
import com.example.jarvis.ui.screens.FirewallScreen
import com.example.jarvis.ui.screens.HardeningScreen
import com.example.jarvis.ui.screens.LogsScreen
import com.example.jarvis.ui.screens.LostDeviceScreen
import com.example.jarvis.ui.screens.PerformanceScreen
import com.example.jarvis.ui.screens.PermissionsScreen
import com.example.jarvis.ui.screens.PhishingScreen
import com.example.jarvis.ui.screens.ScanScreen
import com.example.jarvis.ui.screens.SettingsScreen
import com.example.jarvis.ui.screens.ToolkitScreen
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisTheme

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisMainViewModel by viewModels {
        val app = application as JarvisApplication
        JarvisMainViewModel.Factory(app.securityRepository)
    }

    private val packageRemovedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val packageName = intent?.data?.schemeSpecificPart
            if (!packageName.isNullOrBlank()) {
                viewModel.handlePackageRemoved(packageName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            // Graceful fallback for custom OEM window implementations on Android 9
        }

        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
            addDataScheme("package")
        }
        registerReceiver(packageRemovedReceiver, packageFilter)

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

            JarvisTheme(darkTheme = isDarkMode) {
                val currentRoute by viewModel.currentRoute.collectAsStateWithLifecycle()

                // Intercept back button if can navigate back or not on dashboard
                BackHandler(enabled = viewModel.canNavigateBack()) {
                    viewModel.navigateBack()
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.background),
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
                    bottomBar = {
                        JarvisBottomNav(
                            currentRoute = currentRoute,
                            onNavigate = { route -> viewModel.navigateTo(route) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        when (currentRoute) {
                            "dashboard" -> DashboardScreen(viewModel = viewModel)
                            "toolkit" -> ToolkitScreen(viewModel = viewModel)
                            "firewall" -> FirewallScreen(viewModel = viewModel)
                            "performance" -> PerformanceScreen(viewModel = viewModel)
                            "phishing" -> PhishingScreen(viewModel = viewModel)
                            "apps" -> AppsScreen(viewModel = viewModel)
                            "harden" -> HardeningScreen(viewModel = viewModel)
                            "lost_device" -> LostDeviceScreen(viewModel = viewModel)
                            "logs" -> LogsScreen(viewModel = viewModel)
                            "scan" -> ScanScreen(viewModel = viewModel)
                            "permissions" -> PermissionsScreen(viewModel = viewModel)
                            "events" -> EventsScreen(viewModel = viewModel)
                            "settings" -> SettingsScreen(viewModel = viewModel)
                            else -> DashboardScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed(this)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(packageRemovedReceiver)
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
