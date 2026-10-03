package com.example.jarvis.domain

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.jarvis.data.db.JarvisDatabase
import com.example.jarvis.data.db.entities.FirewallRuleEntity
import com.example.jarvis.data.db.entities.NetworkTrafficEntity
import com.example.jarvis.data.db.entities.SecurityEventEntity
import com.example.jarvis.data.db.entities.TrustedDeviceEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomDatabaseTest {

    private lateinit var db: JarvisDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, JarvisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testFirewallRuleInsertAndQuery() = runBlocking {
        val rule = FirewallRuleEntity(
            packageName = "com.untrusted.sample",
            appName = "Sample Untrusted",
            isBlocked = true
        )
        db.firewallRuleDao().setRule(rule)

        val blockedRules = db.firewallRuleDao().getBlockedRules().first()
        assertEquals(1, blockedRules.size)
        assertEquals("com.untrusted.sample", blockedRules[0].packageName)
        assertTrue(blockedRules[0].isBlocked)
    }

    @Test
    fun testSecurityEventOrderAndPersistence() = runBlocking {
        val event1 = SecurityEventEntity(
            timestamp = 1000L,
            title = "Scan Completed",
            category = "AUDIT",
            severity = "INFO",
            description = "Clean baseline",
            source = "SCANNER"
        )
        val event2 = SecurityEventEntity(
            timestamp = 2000L,
            title = "Alert Triggered",
            category = "DEFENSE",
            severity = "CRITICAL",
            description = "High threat detected",
            source = "MONITOR"
        )
        db.securityEventDao().insertEvent(event1)
        db.securityEventDao().insertEvent(event2)

        val recent = db.securityEventDao().getRecentEvents(10).first()
        assertEquals(2, recent.size)
        // Descending timestamp: event2 (2000L) should appear before event1 (1000L)
        assertEquals("Alert Triggered", recent[0].title)
        assertEquals("Scan Completed", recent[1].title)
    }

    @Test
    fun testTrustedDeviceInsertAndQuery() = runBlocking {
        val device = TrustedDeviceEntity(
            contactName = "Security Officer",
            phoneNumber = "+15550001111",
            isEnabled = true
        )
        val id = db.trustedDeviceDao().insert(device)
        assertTrue(id > 0)

        val enabled = db.trustedDeviceDao().getEnabledDevices()
        assertEquals(1, enabled.size)
        assertEquals("+15550001111", enabled[0].phoneNumber)
    }

    @Test
    fun testNetworkTrafficRetentionPruning() = runBlocking {
        // Insert 10 traffic records
        val items = (1..10).map { i ->
            NetworkTrafficEntity(
                timestamp = i * 1000L,
                packageName = "firewall.blocked_app",
                appName = "Blocked App",
                destinationHost = "test.host.$i",
                destinationIp = "10.0.0.$i",
                port = 443,
                protocol = "TCP",
                isBlocked = true,
                category = "BROWSER"
            )
        }
        db.networkTrafficDao().insertAll(items)

        // Prune to keep only latest 5
        db.networkTrafficDao().pruneOldTraffic(keepCount = 5)

        val remaining = db.networkTrafficDao().getRecentTraffic().first()
        assertEquals(5, remaining.size)
        // Ensure newest timestamp (10000L) is retained
        assertEquals(10000L, remaining[0].timestamp)
    }
}
