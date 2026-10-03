package com.example.jarvis.data.firewall

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.VpnService
import com.example.jarvis.data.db.JarvisDatabase
import com.example.jarvis.data.db.entities.FirewallRuleEntity
import com.example.jarvis.data.db.entities.NetworkTrafficEntity
import kotlinx.coroutines.flow.Flow

class FirewallManager(private val context: Context) {

    private val database = JarvisDatabase.getInstance(context)
    private val firewallRuleDao = database.firewallRuleDao()
    private val networkTrafficDao = database.networkTrafficDao()

    val allRules: Flow<List<FirewallRuleEntity>> = firewallRuleDao.getAllRules()
    val blockedRules: Flow<List<FirewallRuleEntity>> = firewallRuleDao.getBlockedRules()
    val recentTraffic: Flow<List<NetworkTrafficEntity>> = networkTrafficDao.getRecentTraffic()

    fun isFirewallActive(): Boolean = JarvisFirewallService.isRunning
    fun isGlobalKillSwitchActive(): Boolean = JarvisFirewallService.isGlobalKillSwitchActive

    fun setGlobalKillSwitch(active: Boolean) {
        JarvisFirewallService.setGlobalKillSwitch(context, active)
    }

    fun getVpnPrepareIntent(): Intent? {
        return VpnService.prepare(context)
    }

    fun startFirewall() {
        JarvisFirewallService.start(context)
    }

    fun stopFirewall() {
        JarvisFirewallService.stop(context)
    }

    suspend fun setAppBlocked(packageName: String, appName: String, isBlocked: Boolean) {
        firewallRuleDao.setRule(
            FirewallRuleEntity(
                packageName = packageName,
                appName = appName,
                isBlocked = isBlocked,
                updatedAtTimestamp = System.currentTimeMillis()
            )
        )
        if (JarvisFirewallService.isRunning) {
            JarvisFirewallService.updateRules(context)
        }
    }

    suspend fun blockAllBackgroundApps(installedApps: List<Pair<String, String>>) {
        for ((pkg, name) in installedApps) {
            // Keep critical system and our app unblocked
            if (pkg != context.packageName && !pkg.startsWith("com.android.systemui")) {
                firewallRuleDao.setRule(
                    FirewallRuleEntity(
                        packageName = pkg,
                        appName = name,
                        isBlocked = true,
                        updatedAtTimestamp = System.currentTimeMillis()
                    )
                )
            }
        }
        if (JarvisFirewallService.isRunning) {
            JarvisFirewallService.updateRules(context)
        }
    }

    suspend fun unblockAllApps() {
        firewallRuleDao.clearAll()
        if (JarvisFirewallService.isRunning) {
            JarvisFirewallService.updateRules(context)
        }
    }

    suspend fun recordSimulatedTrafficEvent(item: NetworkTrafficEntity) {
        networkTrafficDao.insertTraffic(item)
    }

    suspend fun seedInstalledAppRulesIfEmpty(installedApps: List<Pair<String, String>>) {
        val current = firewallRuleDao.getAllRulesList()
        if (current.isEmpty()) {
            for ((pkg, name) in installedApps) {
                firewallRuleDao.setRule(
                    FirewallRuleEntity(
                        packageName = pkg,
                        appName = name,
                        isBlocked = false,
                        updatedAtTimestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun seedInitialTrafficIfEmpty() {
        val sampleTraffic = listOf(
            NetworkTrafficEntity(
                packageName = "com.android.chrome",
                appName = "Chrome Browser",
                destinationHost = "www.google.com",
                destinationIp = "142.250.190.46",
                port = 443,
                protocol = "TCP/TLS",
                category = "BROWSER",
                isBlocked = false
            ),
            NetworkTrafficEntity(
                packageName = "com.google.android.gms",
                appName = "Google Play Services",
                destinationHost = "device-provisioning.googleapis.com",
                destinationIp = "172.217.16.202",
                port = 443,
                protocol = "TCP/TLS",
                category = "SYSTEM",
                isBlocked = false
            ),
            NetworkTrafficEntity(
                packageName = "com.android.vending",
                appName = "Google Play Store",
                destinationHost = "play.googleapis.com",
                destinationIp = "172.217.14.238",
                port = 443,
                protocol = "TCP/TLS",
                category = "SYSTEM",
                isBlocked = false
            ),
            NetworkTrafficEntity(
                packageName = "com.example.sampleapp",
                appName = "Analytics Collector",
                destinationHost = "app-measurement.com",
                destinationIp = "216.58.214.206",
                port = 443,
                protocol = "HTTPS",
                category = "TRACKER",
                isBlocked = false
            )
        )
        networkTrafficDao.insertAll(sampleTraffic)
    }
}
