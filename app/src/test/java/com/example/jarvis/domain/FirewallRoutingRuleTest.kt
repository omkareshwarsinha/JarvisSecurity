package com.example.jarvis.domain

import com.example.jarvis.data.db.entities.FirewallRuleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirewallRoutingRuleTest {

    @Test
    fun testFirewallRuleEntityDefaults() {
        val rule = FirewallRuleEntity(
            packageName = "com.example.untrusted",
            appName = "Untrusted App",
            isBlocked = true
        )
        assertEquals("com.example.untrusted", rule.packageName)
        assertEquals("Untrusted App", rule.appName)
        assertTrue(rule.isBlocked)
        assertTrue(rule.updatedAtTimestamp > 0)
    }

    @Test
    fun testSelectiveRoutingExcludesSelfAndOnlyRoutesBlocked() {
        val selfPackage = "com.jarvisservices.r"
        val rules = listOf(
            FirewallRuleEntity(packageName = "com.telemetry.adware", appName = "Adware", isBlocked = true),
            FirewallRuleEntity(packageName = "com.safe.browser", appName = "Safe Browser", isBlocked = false),
            FirewallRuleEntity(packageName = selfPackage, appName = "MobiArmour", isBlocked = true) // Even if marked blocked
        )

        // Simulate selective routing logic in JarvisFirewallService:
        // Exclude self package and only route apps where isBlocked == true
        val targetVpnApps = rules
            .filter { it.isBlocked && it.packageName != selfPackage }
            .map { it.packageName }
            .toSet()

        assertTrue(targetVpnApps.contains("com.telemetry.adware"))
        assertFalse(targetVpnApps.contains("com.safe.browser"))
        assertFalse("MobiArmour must never route itself to the drop sink VPN interface", targetVpnApps.contains(selfPackage))
        assertEquals(1, targetVpnApps.size)
    }

    @Test
    fun testDestinationClassificationHeuristic() {
        // Test port and IP classification patterns
        fun classifyPort(port: Int): String {
            return when (port) {
                53 -> "SYSTEM"
                80, 443 -> "BROWSER"
                else -> "UNKNOWN"
            }
        }

        assertEquals("SYSTEM", classifyPort(53))
        assertEquals("BROWSER", classifyPort(443))
        assertEquals("BROWSER", classifyPort(80))
        assertEquals("UNKNOWN", classifyPort(8080))
    }
}
