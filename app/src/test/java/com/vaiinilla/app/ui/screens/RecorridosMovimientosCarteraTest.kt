package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.domain.model.WalletMovement
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/**
 * Cartera y movimientos (W01): lo que entró y lo que gastaste en el mes, los filtros por tipo y el estado vacío.
 * Corre en la JVM con Robolectric y Compose; no cuenta como recorrido en teléfono.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class RecorridosMovimientosCarteraTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val now = Instant.parse("2026-11-07T18:00:00Z")

    private fun movement(
        id: String,
        type: String,
        amount: String,
        description: String,
        folio: Int? = null,
    ) = WalletMovement(
        id = id,
        type = type,
        description = description,
        amount = amount,
        balanceAfter = "100.00",
        orderId = null,
        createdAt = "2026-11-06T18:00:00Z",
        orderFolio = folio,
    )

    private val movements =
        listOf(
            movement("1", "recarga_efectivo", "200.00", "Recarga"),
            movement("2", "compra", "-45.00", "Compra con saldo", folio = 12),
            movement("3", "cashback", "5.00", "Cashback"),
        )

    private fun show(list: List<WalletMovement>) {
        composeTestRule.setContent {
            VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
                MovementsPageContent(movements = list, onBack = {}, now = now)
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun w01_laPaginaMuestraLoQueEntroYLoQueGastasteEnElMes() {
        show(movements)
        composeTestRule.onNodeWithText("Movimientos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Entró en nov", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Gastaste", ignoreCase = true).assertExists()
    }

    @Test
    fun w01_traeLosCincoFiltros() {
        show(movements)
        MovementFilter.entries.forEach {
            assertTrue(composeTestRule.onAllNodesWithText(it.label).fetchSemanticsNodes().isNotEmpty())
        }
    }

    @Test
    fun w01_filtrarPorComprasEscondeLasRecargas() {
        show(movements)
        composeTestRule.onNodeWithText("Compra #12").assertExists()
        composeTestRule.onNodeWithText("Recarga en Caja").assertExists()
        composeTestRule.onNodeWithText("Compras").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Compra #12").assertExists()
        composeTestRule.onNodeWithText("Recarga en Caja").assertDoesNotExist()
    }

    @Test
    fun w01_unFiltroSinMovimientosDiceQueTodaviaNoHay() {
        show(movements)
        composeTestRule.onNodeWithText("Devoluciones").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Todavía no hay movimientos de este tipo.").assertIsDisplayed()
    }

    @Test
    fun w01_sinMovimientosExplicaQueAhiSeVeranLasRecargasYCompras() {
        show(emptyList())
        composeTestRule.onNodeWithText("Aquí verás tus recargas y lo que pagues con saldo.").assertIsDisplayed()
    }

    @Test
    fun w01_cadaFiltroDejaPasarSoloSusTipos() {
        assertTrue(MovementFilter.RELOADS.matches("recarga_efectivo"))
        assertTrue(MovementFilter.REFUNDS.matches("cancelacion"))
        assertEquals(false, MovementFilter.PURCHASES.matches("cashback"))
        assertTrue(MovementFilter.ALL.matches("cualquiera"))
    }
}
