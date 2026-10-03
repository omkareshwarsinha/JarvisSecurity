package com.example.jarvis.data.firewall

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.jarvis.data.db.JarvisDatabase
import com.example.jarvis.data.db.entities.NetworkTrafficEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

class JarvisFirewallService : VpnService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var packetProcessingJob: Job? = null

    // In-memory set of blocked package names
    private val blockedPackages = ConcurrentHashMap.newKeySet<String>()
    // In-memory set of blocked UIDs
    private val blockedUids = ConcurrentHashMap.newKeySet<Int>()

    private lateinit var database: JarvisDatabase

    companion object {
        const val ACTION_START = "com.example.jarvis.firewall.START"
        const val ACTION_STOP = "com.example.jarvis.firewall.STOP"
        const val ACTION_UPDATE_RULES = "com.example.jarvis.firewall.UPDATE_RULES"
        const val ACTION_SET_KILL_SWITCH = "com.example.jarvis.firewall.SET_KILL_SWITCH"
        const val EXTRA_KILL_SWITCH = "extra_kill_switch"
        const val CHANNEL_ID = "jarvis_firewall_channel"
        const val NOTIFICATION_ID = 2001

        enum class FirewallState {
            DISABLED,
            STARTING,
            ACTIVE,
            ISOLATED,
            ERROR
        }

        @Volatile
        var currentState: FirewallState = FirewallState.DISABLED
            private set

        val isRunning: Boolean
            get() = currentState == FirewallState.ACTIVE || currentState == FirewallState.ISOLATED

        @Volatile
        var isGlobalKillSwitchActive: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, JarvisFirewallService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, JarvisFirewallService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun updateRules(context: Context) {
            val intent = Intent(context, JarvisFirewallService::class.java).apply {
                action = ACTION_UPDATE_RULES
            }
            context.startService(intent)
        }

        fun setGlobalKillSwitch(context: Context, active: Boolean) {
            isGlobalKillSwitchActive = active
            if (!isRunning && active) {
                // If kill switch requested and firewall not running, start it
                val intent = Intent(context, JarvisFirewallService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                val intent = Intent(context, JarvisFirewallService::class.java).apply {
                    action = ACTION_SET_KILL_SWITCH
                    putExtra(EXTRA_KILL_SWITCH, active)
                }
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        database = JarvisDatabase.getInstance(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification("MobiArmour Firewall Active", "Per-app zero-trust packet filtering & protection active")
                )
                startFirewall()
            }
            ACTION_STOP -> {
                stopFirewall()
                stopSelf()
            }
            ACTION_UPDATE_RULES -> {
                serviceScope.launch {
                    refreshBlockedRules()
                    rebuildVpn()
                }
            }
            ACTION_SET_KILL_SWITCH -> {
                val active = intent.getBooleanExtra(EXTRA_KILL_SWITCH, false)
                isGlobalKillSwitchActive = active
                val notifTitle = if (active) "MobiArmour Kill-Switch ACTIVE" else "MobiArmour Firewall Active"
                val notifText = if (active) "Air-Gap Enabled: All Device Traffic Blocked" else "Per-app zero-trust packet filtering & protection active"
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.notify(NOTIFICATION_ID, buildNotification(notifTitle, notifText))
                serviceScope.launch {
                    rebuildVpn()
                }
            }
        }
        return START_STICKY
    }

    private fun startFirewall() {
        if (isRunning) return
        currentState = FirewallState.STARTING

        serviceScope.launch {
            refreshBlockedRules()
            establishVpn()
        }
    }

    private suspend fun rebuildVpn() {
        if (!isRunning && currentState != FirewallState.STARTING) return
        try {
            packetProcessingJob?.cancel()
            packetProcessingJob = null
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e("JarvisFirewall", "Error tearing down previous VPN interface", e)
        }
        establishVpn()
    }

    private suspend fun refreshBlockedRules() {
        try {
            val blocked = database.firewallRuleDao().getBlockedRules().first()
            blockedPackages.clear()
            blockedUids.clear()
            val pm = packageManager
            for (rule in blocked) {
                blockedPackages.add(rule.packageName)
                try {
                    val uid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getPackageUid(rule.packageName, PackageManager.PackageInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getPackageUid(rule.packageName, 0)
                    }
                    blockedUids.add(uid)
                } catch (e: Exception) {
                    // App might be uninstalled
                }
            }
            Log.d("JarvisFirewall", "Loaded ${blockedPackages.size} blocked packages")
        } catch (e: Exception) {
            Log.e("JarvisFirewall", "Error loading firewall rules", e)
        }
    }

    private fun establishVpn() {
        try {
            val builder = Builder()
                .setMtu(1500)
                .addAddress("10.0.0.2", 32)

            if (isGlobalKillSwitchActive) {
                // Total device air-gap: capture all traffic and drop it
                builder.setSession("MobiArmour Air-Gap Kill Switch")
                builder.addRoute("0.0.0.0", 0)
                try {
                    builder.addRoute("::", 0)
                } catch (e: Exception) {
                    Log.w("JarvisFirewall", "IPv6 route could not be added", e)
                }
                // In full device air-gap mode, disallow our own package so local app lifecycle isn't isolated
                try {
                    builder.addDisallowedApplication(packageName)
                } catch (e: Exception) {
                    Log.w("JarvisFirewall", "Could not disallow self package in air-gap mode", e)
                }
            } else {
                // Per-app selective firewall:
                // CRITICAL ANDROID VPN RULE: Do NOT mix addAllowedApplication and addDisallowedApplication!
                // Only route the blocked packages into the tun interface to drop their packets.
                // Unblocked apps and our own app bypass the VPN completely!
                builder.setSession("MobiArmour Firewall")
                var addedApps = 0
                for (pkg in blockedPackages) {
                    if (pkg == packageName) continue
                    try {
                        builder.addAllowedApplication(pkg)
                        addedApps++
                    } catch (e: Exception) {
                        Log.w("JarvisFirewall", "Could not route package $pkg to VPN", e)
                    }
                }

                if (addedApps > 0) {
                    builder.addRoute("0.0.0.0", 0)
                } else {
                    // No apps currently blocked. Route internal dummy subnet so TUN is ready
                    builder.addRoute("10.0.0.0", 24)
                }
            }

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                Log.e("JarvisFirewall", "Failed to establish VPN interface (null descriptor)")
                currentState = FirewallState.ERROR
                return
            }

            currentState = if (isGlobalKillSwitchActive) FirewallState.ISOLATED else FirewallState.ACTIVE
            startPacketInspector(vpnInterface!!)
        } catch (e: Exception) {
            Log.e("JarvisFirewall", "Exception establishing VPN", e)
            currentState = FirewallState.ERROR
        }
    }

    private fun startPacketInspector(pfd: ParcelFileDescriptor) {
        packetProcessingJob = serviceScope.launch(Dispatchers.IO) {
            val inputStream = FileInputStream(pfd.fileDescriptor)
            val outputStream = FileOutputStream(pfd.fileDescriptor)
            val buffer = ByteBuffer.allocate(32767)

            try {
                while (isActive && isRunning) {
                    buffer.clear()
                    val length = inputStream.read(buffer.array())
                    if (length > 0) {
                        buffer.limit(length)
                        inspectAndProcessPacket(buffer, length)
                    }
                }
            } catch (e: Exception) {
                Log.d("JarvisFirewall", "Packet inspector terminated: ${e.message}")
            } finally {
                try { inputStream.close() } catch (_: Exception) {}
                try { outputStream.close() } catch (_: Exception) {}
            }
        }
    }

    private var packetCount = 0

    private suspend fun inspectAndProcessPacket(buffer: ByteBuffer, length: Int) {
        try {
            // Basic IPv4 header parsing
            if (length < 20) return
            val versionAndIhl = buffer.get(0).toInt()
            val version = (versionAndIhl shr 4) and 0x0F
            if (version != 4) return // Focus on IPv4 for deterministic analysis

            val protocolNum = buffer.get(9).toInt() and 0xFF
            val protocolStr = when (protocolNum) {
                6 -> "TCP"
                17 -> "UDP"
                1 -> "ICMP"
                else -> "IP-$protocolNum"
            }

            // Destination IP (bytes 16..19)
            val destIpBytes = ByteArray(4)
            destIpBytes[0] = buffer.get(16)
            destIpBytes[1] = buffer.get(17)
            destIpBytes[2] = buffer.get(18)
            destIpBytes[3] = buffer.get(19)
            val destIp = InetAddress.getByAddress(destIpBytes).hostAddress ?: "0.0.0.0"

            // Header length in 32-bit words
            val ihl = (versionAndIhl and 0x0F) * 4
            var destPort = 0
            if (length >= ihl + 4 && (protocolNum == 6 || protocolNum == 17)) {
                // Destination port is at offset ihl + 2
                destPort = ((buffer.get(ihl + 2).toInt() and 0xFF) shl 8) or
                        (buffer.get(ihl + 3).toInt() and 0xFF)
            }

            // Categorize host & destination
            val (hostName, category) = classifyDestination(destIp, destPort)

            // Honest attribution: packets in TUN were routed because their app was blocked or air-gapped
            val (pkgName, appDisplayName) = if (isGlobalKillSwitchActive) {
                Pair("airgap.isolated", "Device Air-Gap Isolated")
            } else {
                Pair("firewall.blocked_app", "Blocked App (L3/L4 Drop Sink)")
            }

            // Log network traffic item
            val trafficItem = NetworkTrafficEntity(
                timestamp = System.currentTimeMillis(),
                packageName = pkgName,
                appName = appDisplayName,
                destinationHost = hostName,
                destinationIp = destIp,
                port = destPort,
                protocol = protocolStr,
                isBlocked = true, // Dropped at TUN interface
                category = category
            )
            database.networkTrafficDao().insertTraffic(trafficItem)

            packetCount++
            if (packetCount % 50 == 0) {
                database.networkTrafficDao().pruneOldTraffic(500)
            }
        } catch (e: Exception) {
            // Silent catch to prevent loop crashes
        }
    }

    private fun isDestinationBlocked(ip: String, host: String): Boolean {
        return true // Traffic arriving on TUN interface is dropped by design
    }

    private fun classifyDestination(ip: String, port: Int): Pair<String, String> {
        return when {
            // DNS
            port == 53 -> Pair("DNS Resolver ($ip)", "SYSTEM")
            // Known tracking & analytics domains / IP blocks
            ip.startsWith("157.240.") || ip.startsWith("31.13.") -> Pair("Meta Graph / Telemetry ($ip)", "TRACKER")
            ip.startsWith("142.250.") || ip.startsWith("172.217.") -> Pair("Google Services ($ip)", if (port == 443) "CLOUD_API" else "BROWSER")
            ip.startsWith("104.16.") || ip.startsWith("104.17.") || ip.startsWith("172.67.") -> Pair("Cloudflare Edge ($ip)", "CLOUD_API")
            ip.startsWith("13.107.") || ip.startsWith("20.190.") -> Pair("Microsoft Azure Telemetry ($ip)", "TRACKER")
            ip.startsWith("52.") || ip.startsWith("54.") || ip.startsWith("3.") -> Pair("AWS Cloud Node ($ip)", "CLOUD_API")
            port == 443 -> Pair("HTTPS Endpoint ($ip)", "BROWSER")
            port == 80 -> Pair("HTTP Cleartext Endpoint ($ip)", "BROWSER")
            else -> Pair("Port $port ($ip)", "UNKNOWN")
        }
    }

    private fun stopFirewall() {
        currentState = FirewallState.DISABLED
        packetProcessingJob?.cancel()
        packetProcessingJob = null
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e("JarvisFirewall", "Error closing VPN interface", e)
        }
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopFirewall()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MobiArmour Firewall Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifies when MobiArmour Firewall is actively shielding device traffic."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
