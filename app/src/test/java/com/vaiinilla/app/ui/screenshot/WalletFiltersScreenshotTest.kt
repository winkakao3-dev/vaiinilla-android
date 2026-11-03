package com.vaiinilla.app.ui.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.domain.model.WalletMovement
import com.vaiinilla.app.ui.screens.MovementFilter
import com.vaiinilla.app.ui.screens.WalletLedgerStats
import com.vaiinilla.app.ui.screens.WalletScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/** Filtros de Movimientos (Doop E04) y lo que te regresaron este mes (Doop E02). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi", sdk = [33])
class WalletFiltersScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun movement(id: String, type: String, amount: String, at: String = "2026-11-03T10:00:00-06:00") =
        WalletMovement(id, type, "", amount, "100.00", null, at)

    @Test
    fun `cada filtro deja pasar solo su tipo`() {
        assertEquals(true, MovementFilter.PURCHASES.matches("compra"))
        assertEquals(true, MovementFilter.RELOADS.matches("recarga_stripe"))
        assertEquals(true, MovementFilter.REFUNDS.matches("devolucion_articulo"))
        assertEquals(false, MovementFilter.REFUNDS.matches("cashback"))
        assertEquals(true, MovementFilter.ALL.matches("cashback"))
    }

    @Test
    fun `suma el cashback y las devoluciones del mes`() {
        val now = Instant.parse("2026-11-03T18:00:00Z")
        val stats =
            WalletLedgerStats.from(
                listOf(
                    movement("1", "compra", "-68.00"),
                    movement("2", "cashback", "3.40"),
                    movement("3", "cancelacion", "95.00"),
                    movement("4", "cashback", "10.00", at = "2026-10-20T10:00:00-06:00"),
                ),
                now = now,
            )
        assertEquals("\$98.40", stats.returnedThisMonth)
        assertNull(WalletLedgerStats.from(listOf(movement("1", "compra", "-68.00")), now = now).returnedThisMonth)
    }

    @Test
    fun cartera_filtros() {
        composeTestRule.setContent {
            ScreenshotTheme {
                WalletScreen(
                    remoteState = BreakUiFixtures.wallet(BreakUiData.DEMO),
                    userId = "u-1",
                    venueName = "Usagi (Pruebas)",
                    onMenu = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Compras").performClick()
        composeTestRule.waitForIdle()
        // La captura no se guarda: «Último movimiento» depende de la hora en que corre la prueba.
        composeTestRule.onNodeWithText("Compra").assertExists()
        composeTestRule.onNodeWithText("Recarga en Caja").assertDoesNotExist()
    }
}