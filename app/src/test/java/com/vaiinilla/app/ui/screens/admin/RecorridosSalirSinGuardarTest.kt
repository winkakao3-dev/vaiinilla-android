package com.vaiinilla.app.ui.screens.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.vaiinilla.app.domain.admin.HomeDeliverySettings
import com.vaiinilla.app.domain.admin.PreorderSettings
import com.vaiinilla.app.domain.admin.StoreConfig
import com.vaiinilla.app.domain.model.OrderWindow
import com.vaiinilla.app.ui.admin.AdminStoreViewModel
import com.vaiinilla.app.ui.admin.FakeStore
import com.vaiinilla.app.ui.operational.FakeCatalog
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Salir del flujo de la tienda con cambios (Z05): pregunta «¿Salir sin guardar?»; Descartar no guarda, Seguir
 * editando se queda y Guardar manda el flujo. Corre en la JVM con Robolectric y Compose; no cuenta como teléfono.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w390dp-h2400dp-normal-long-notround-any-xxhdpi",
    sdk = [33],
)
class RecorridosSalirSinGuardarTest {
    @get:Rule
    val rule = createComposeRule()

    private val store = FakeStore()

    private val config =
        StoreConfig(
            typeWire = "cafeteria",
            deliveryRequiresQr = true,
            allowsPayAtEnd = false,
            allowsBalance = true,
            releaseGraceMinutes = 10,
            orderWindows = listOf(OrderWindow("08:00", "20:00")),
            videos = emptyList(),
            visibleInDirectory = true,
            storeUrl = "https://vaiinilla.app/p/negocio",
            imageUrl = null,
            logoUrl = null,
            homeDelivery = HomeDeliverySettings(false, "5", "35.00", "150.00", false, ""),
            preorders = PreorderSettings(7, "18:00", false, 50, 120),
        )

    private fun settle() {
        rule.mainClock.advanceTimeBy(1_500)
        rule.waitForIdle()
    }

    private fun tap(text: String) {
        val target = hasText(text, substring = true) or hasContentDescription(text, substring = true)
        val scrollers = rule.onAllNodes(hasScrollAction())
        for (i in scrollers.fetchSemanticsNodes().indices) {
            if (runCatching { scrollers[i].performScrollToNode(target) }.isSuccess) break
        }
        rule.onAllNodes(target).onFirst().performClick()
        settle()
    }

    /** Los botones del diálogo van en su propia ventana, después de los de la página: se toca el último. */
    private fun tapDialog(text: String) {
        rule.onAllNodes(hasText(text)).onLast().performClick()
        settle()
    }

    private fun openFlowAndEdit() {
        store.config = Result.success(config)
        val vm = AdminStoreViewModel(store, FakeCatalog())
        rule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
                val colors = rememberOperationalColors()
                AdminDialogHost(colors) {
                    Box(Modifier.fillMaxSize()) { AdminStoreSection(vm, colors, false, {}) }
                }
            }
        }
        settle()
        tap("Cómo funciona tu tienda")
        tap("Al final, con cuenta abierta")
    }

    @Test
    fun z05_salirConCambiosPreguntaAntesDeSalir() {
        openFlowAndEdit()
        tap("Regresar")
        rule.onNodeWithText("¿Salir sin guardar?").assertIsDisplayed()
        rule.onAllNodes(hasText("Descartar")).onLast().assertIsDisplayed()
        rule.onAllNodes(hasText("Seguir editando")).onLast().assertIsDisplayed()
    }

    @Test
    fun z05_descartarSaleYNoGuardaNada() {
        openFlowAndEdit()
        tap("Regresar")
        tapDialog("Descartar")
        assertTrue(store.flows.isEmpty())
        rule.onNodeWithText("¿Salir sin guardar?").assertDoesNotExist()
    }

    @Test
    fun z05_seguirEditandoSeQuedaEnLaPaginaSinGuardar() {
        openFlowAndEdit()
        tap("Regresar")
        tapDialog("Seguir editando")
        rule.onNodeWithText("¿Salir sin guardar?").assertDoesNotExist()
        rule.onNodeWithText("Cómo funciona tu tienda").assertExists()
        assertTrue(store.flows.isEmpty())
    }

    @Test
    fun z05_guardarMandaElFlujoCambiado() {
        openFlowAndEdit()
        tap("Regresar")
        tapDialog("Guardar")
        rule.waitUntil(5_000) { store.flows.isNotEmpty() }
        assertTrue(store.flows.single().allowsPayAtEnd)
        assertEquals("cafeteria", store.flows.single().typeWire)
    }

    @Test
    fun z05_sinCambiosSaleSinPreguntar() {
        store.config = Result.success(config)
        val vm = AdminStoreViewModel(store, FakeCatalog())
        rule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
                val colors = rememberOperationalColors()
                AdminDialogHost(colors) { Box(Modifier.fillMaxSize()) { AdminStoreSection(vm, colors, false, {}) } }
            }
        }
        settle()
        tap("Cómo funciona tu tienda")
        tap("Regresar")
        rule.onNodeWithText("¿Salir sin guardar?").assertDoesNotExist()
        assertFalse(store.flows.isNotEmpty())
    }
}
