package com.vaiinilla.app

import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.checkoutStaffBlocker
import com.vaiinilla.app.ui.order.isOperationallyReady
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EnvironmentSeparationTest {
    @Test
    fun testFlavorsAndGoogleServicesFilesExist() {
        val rootDir = File(".").canonicalFile
        val projectDir = if (File(rootDir, "app").exists()) rootDir else rootDir.parentFile

        val devGoogleServices = File(projectDir, "app/src/dev/google-services.json")
        val prodGoogleServices = File(projectDir, "app/src/prod/google-services.json")
        val rootGoogleServices = File(projectDir, "app/google-services.json")

        assertTrue("app/src/dev/google-services.json must exist", devGoogleServices.exists())
        assertTrue("app/src/prod/google-services.json must exist", prodGoogleServices.exists())
        assertFalse(
            "app/google-services.json must NOT exist to prevent accidental fallback",
            rootGoogleServices.exists(),
        )
    }

    @Test
    fun testFirebaseProjectIdsDoNotCrossEnvironments() {
        val rootDir = File(".").canonicalFile
        val projectDir = if (File(rootDir, "app").exists()) rootDir else rootDir.parentFile

        val devContent = File(projectDir, "app/src/dev/google-services.json").readText()
        val prodContent = File(projectDir, "app/src/prod/google-services.json").readText()

        assertTrue("Development Firebase project must be vaiinilla-b3a70", devContent.contains("vaiinilla-b3a70"))
        assertFalse("Development Firebase must not reference vaiinilla-produc", devContent.contains("vaiinilla-produc"))

        assertTrue("Production Firebase project must be vaiinilla-produc", prodContent.contains("vaiinilla-produc"))
        assertFalse("Production Firebase must not reference vaiinilla-b3a70", prodContent.contains("vaiinilla-b3a70"))
    }

    @Test
    fun testDevelopmentHasApplicationIdSuffixClient() {
        val rootDir = File(".").canonicalFile
        val projectDir = if (File(rootDir, "app").exists()) rootDir else rootDir.parentFile

        val devContent = File(projectDir, "app/src/dev/google-services.json").readText()
        assertTrue(
            "Development google-services.json must support com.vaiinilla.app.dev",
            devContent.contains("com.vaiinilla.app.dev"),
        )
    }

    @Test
    fun testProductionTargetsStandardPackageName() {
        val rootDir = File(".").canonicalFile
        val projectDir = if (File(rootDir, "app").exists()) rootDir else rootDir.parentFile

        val prodContent = File(projectDir, "app/src/prod/google-services.json").readText()
        assertTrue(
            "Production google-services.json must support com.vaiinilla.app",
            prodContent.contains("com.vaiinilla.app"),
        )
        assertFalse(
            "Production google-services.json must not have dev suffix",
            prodContent.contains("com.vaiinilla.app.dev"),
        )
    }

    @Test
    fun testDevelopmentAppNameStringOverride() {
        val rootDir = File(".").canonicalFile
        val projectDir = if (File(rootDir, "app").exists()) rootDir else rootDir.parentFile

        val devStrings = File(projectDir, "app/src/dev/res/values/strings.xml")
        assertTrue("Development strings.xml must exist", devStrings.exists())
        val content = devStrings.readText()
        assertTrue("Development app_name must be 'Vaiinilla Dev'", content.contains("Vaiinilla Dev"))
    }

    @Test
    fun testOperationalReadyRequiresAllFourConditions() {
        val fullStatus =
            OperationalStatus(
                acceptingOrders = true,
                cashSessionOpen = true,
                cashierOnline = true,
                kitchenOnline = true,
                estimatedTimeMinutes = 10,
                consultedAt = "2026-08-12T00:00:00.000Z",
            )

        val fullState = OrderFlowUiState(operationalStatus = fullStatus)
        assertTrue("All 4 conditions met must mean ready", fullState.isOperationallyReady)
        assertNull("All 4 conditions met must have no blocker", fullStatus.checkoutStaffBlocker())

        // Cashier offline
        val noCashierState = OrderFlowUiState(operationalStatus = fullStatus.copy(cashierOnline = false))
        assertFalse(noCashierState.isOperationallyReady)
        assertTrue(fullStatus.copy(cashierOnline = false).checkoutStaffBlocker()!!.contains("Caja no está en línea"))

        // Kitchen offline
        val noKitchenState = OrderFlowUiState(operationalStatus = fullStatus.copy(kitchenOnline = false))
        assertFalse(noKitchenState.isOperationallyReady)
        assertTrue(fullStatus.copy(kitchenOnline = false).checkoutStaffBlocker()!!.contains("Cocina no está en línea"))

        // Both offline
        val bothOfflineState =
            OrderFlowUiState(operationalStatus = fullStatus.copy(cashierOnline = false, kitchenOnline = false))
        assertFalse(bothOfflineState.isOperationallyReady)
        assertTrue(
            fullStatus
                .copy(
                    cashierOnline = false,
                    kitchenOnline = false,
                ).checkoutStaffBlocker()!!
                .contains("Caja y Cocina no están en línea"),
        )

        // Cash session closed
        val closedCashState = OrderFlowUiState(operationalStatus = fullStatus.copy(cashSessionOpen = false))
        assertFalse(closedCashState.isOperationallyReady)
        assertTrue(
            fullStatus.copy(cashSessionOpen = false).checkoutStaffBlocker()!!.contains("Caja no tiene sesión abierta"),
        )

        // Not accepting orders
        val notAcceptingState = OrderFlowUiState(operationalStatus = fullStatus.copy(acceptingOrders = false))
        assertFalse(notAcceptingState.isOperationallyReady)
    }
}
