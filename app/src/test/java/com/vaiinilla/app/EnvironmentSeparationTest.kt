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
            "Production google-services.json must support vaiinilla.innovapro.app",
            prodContent.contains("vaiinilla.innovapro.app"),
        )
        assertFalse(
            "Production google-services.json must not have dev suffix",
            prodContent.contains("com.vaiinilla.app.dev"),
        )
    }

    @Test
    fun testDevelopmentRoleAppNameLabels() {
        val rootDir = File(".").canonicalFile
        val projectDir = if (File(rootDir, "app").exists()) rootDir else rootDir.parentFile

        val expectedLabels =
            mapOf(
                "devAlumno" to "Vaiinilla (Alumno)",
                "devCaja" to "Vaiinilla (Caja)",
                "devCocina" to "Vaiinilla (Cocina)",
            )
        expectedLabels.forEach { (sourceSet, label) ->
            val strings = File(projectDir, "app/src/$sourceSet/res/values/strings.xml")
            assertTrue("$sourceSet strings.xml must exist", strings.exists())
            assertTrue("$sourceSet app_name must be '$label'", strings.readText().contains(label))
        }
    }

    @Test
    fun testCheckoutBlockerDoesNotExposeStaffRoles() {
        val closed =
            OperationalStatus(
                acceptingOrders = false,
                cashSessionOpen = false,
                cashierOnline = false,
                kitchenOnline = false,
                estimatedTimeMinutes = 10,
                consultedAt = "2026-08-12T00:00:00.000Z",
            )
        val message = closed.checkoutStaffBlocker()!!
        assertFalse(message.contains("Caja") || message.contains("Cocina"))
        assertTrue(message.contains("no está abierto"))
    }

    @Test
    fun testOperationalReadyFollowsBackendAcceptingOrders() {
        val fullStatus =
            OperationalStatus(
                acceptingOrders = true,
                cashSessionOpen = true,
                cashierOnline = true,
                kitchenOnline = true,
                estimatedTimeMinutes = 10,
                consultedAt = "2026-08-12T00:00:00.000Z",
            )
        assertTrue(OrderFlowUiState(operationalStatus = fullStatus).isOperationallyReady)
        assertNull(fullStatus.checkoutStaffBlocker())

        // El backend acepta el pedido y lo deja en cola aunque Caja o Cocina estén fuera
        // de línea o la caja esté cerrada: la app no debe bloquear más que el backend.
        val degraded =
            listOf(
                fullStatus.copy(cashierOnline = false),
                fullStatus.copy(kitchenOnline = false),
                fullStatus.copy(cashSessionOpen = false),
                fullStatus.copy(cashSessionOpen = false, cashierOnline = false, kitchenOnline = false),
            )
        degraded.forEach { status ->
            assertTrue(OrderFlowUiState(operationalStatus = status).isOperationallyReady)
            assertNull(status.checkoutStaffBlocker())
        }

        // Solo bloquea cuando el backend dice que no recibe pedidos.
        val notAccepting = fullStatus.copy(acceptingOrders = false)
        assertFalse(OrderFlowUiState(operationalStatus = notAccepting).isOperationallyReady)
        assertTrue(notAccepting.checkoutStaffBlocker()!!.contains("no está abierto"))
    }
}
