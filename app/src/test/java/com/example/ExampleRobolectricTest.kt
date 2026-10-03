package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.jarvis.domain.PermissionCatalog
import com.example.jarvis.domain.ScoringEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MobiArmour", appName)
    }

    @Test
    fun testPermissionCatalogLookup() {
        val cameraPerm = PermissionCatalog.getDetail("android.permission.CAMERA")
        assertNotNull(cameraPerm)
        assertEquals("Camera Optical Sensor", cameraPerm.readableName)
    }

    @Test
    fun testScoringEngineCleanBaseline() {
        val result = ScoringEngine.evaluate(emptyList(), emptyList())
        assertEquals(100, result.overallScore)
        assertEquals(0, result.criticalCount)
        assertTrue(result.summaryText.contains("optimal", ignoreCase = true))
    }

    @Test
    @Config(sdk = [28])
    fun testActivityLaunchUnderSdk28() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        val app = ApplicationProvider.getApplicationContext<Context>() as JarvisApplication
        val repo = app.securityRepository
        
        // Execute all core auditor modules explicitly under SDK 28
        kotlinx.coroutines.runBlocking {
            repo.getDeviceChecks()
            repo.getScannedApps()
            repo.getPermissionMatrix()
            repo.performanceOptimizer.optimizeForGaming()
            repo.systemLogReader.readSystemLogs()
        }
        repo.continuousMonitor.evaluateSecurityState()
        repo.performanceOptimizer.getMemoryStatus()
        repo.lostDeviceManager.checkDeviceAdminActive()
    }
}

