package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.domain.repository.CashDrawerMovement
import com.vaiinilla.app.domain.repository.CashDrawerMovementType
import com.vaiinilla.app.domain.repository.CashDrawerReport
import com.vaiinilla.app.domain.repository.CashDrawerSummary
import com.vaiinilla.app.ui.operational.CashDrawerUi
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * El menú de la cuenta de Caja (N08): «Mi turno» y «Movimientos del cajón». Corre en la JVM con Robolectric y
 * Compose; no cuenta como recorrido en teléfono.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class RecorridosCajaPantallaTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun movement(
        id: String,
        type: String,
        outgoing: Boolean,
        createdAt: String,
        reason: String? = null,
        folio: Int? = null,
    ) = CashDrawerMovement(
        id = id,
        type = type,
        outgoing = outgoing,
        amount = "100.00",
        reason = reason,
        registeredBy = "Ana",
        orderFolio = folio,
        isRefund = false,
        createdAt = createdAt,
    )

    private fun report() =
        CashDrawerReport(
            summary = CashDrawerSummary("500.00", "300.00", "20.00", "0.00", "100.00", "50.00", "0.00", "870.00"),
            movements =
                listOf(
                    movement("a", "apertura", false, "2026-11-07T14:00:00Z"),
                    movement("b", "cobro", false, "2026-11-07T15:00:00Z", folio = 7),
                    movement("c", "ajuste_salida", true, "2026-11-07T16:00:00Z", reason = "Compra de hielo"),
                ),
        )

    private fun showShift() {
        composeTestRule.setContent {
            ScreenshotTheme {
                StaffShiftContent(
                    modeLabel = "Caja",
                    placeName = "Boliche Strike",
                    today = LocalDate.parse("2026-11-07"),
                    colors = rememberOperationalColors(),
                )
            }
        }
    }

    private fun showDrawer(
        drawer: CashDrawerUi = CashDrawerUi(report = report()),
        onSave: ((CashDrawerMovementType, String, String) -> Unit)? = { _, _, _ -> },
    ) {
        composeTestRule.setContent {
            ScreenshotTheme {
                CashDrawerContent(
                    expectedCash = "870.00",
                    colors = rememberOperationalColors(),
                    drawer = drawer,
                    onSave = onSave,
                )
            }
        }
    }

    @Test
    fun n08_miTurnoDiceQueNoHayDatosEnVezDeInventarCifras() {
        showShift()
        composeTestRule.onNodeWithText("Mi turno").assertIsDisplayed()
        composeTestRule.onNodeWithText("Caja · Boliche Strike").assertIsDisplayed()
        composeTestRule.onNodeWithText("Propinas de hoy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sin datos").assertIsDisplayed()
    }

    @Test
    fun n08_elCajonMuestraLoQueDebeHaberYLosMovimientosDelDia() {
        showDrawer()
        composeTestRule.onNodeWithText("Movimientos del cajón").assertIsDisplayed()
        composeTestRule.onNodeWithText("Debe haber en el cajón").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fondo al abrir").assertExists()
        composeTestRule.onNodeWithText("Cobro en efectivo #7").assertExists()
        composeTestRule.onNodeWithText("Salida del cajón").assertExists()
    }

    @Test
    fun n08_losMovimientosVanDelMasNuevoAlMasViejo() {
        val ordered = drawerMovementsNewestFirst(report().movements).map { it.id }
        assertEquals(listOf("c", "b", "a"), ordered)
    }

    @Test
    fun n08_meterOSacarDineroAbreSuFormularioYPideMontoYMotivo() {
        showDrawer()
        composeTestRule.onNodeWithText("Meter dinero").performClick()
        composeTestRule.onNodeWithText("CUÁNTO METES").assertIsDisplayed()
        composeTestRule.onNodeWithText("Escribe el monto").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sacar dinero").performClick()
        composeTestRule.onNodeWithText("CUÁNTO SACAS").assertIsDisplayed()
    }

    @Test
    fun n08_enSoloLecturaNoSeOfreceMeterNiSacar() {
        showDrawer(onSave = null)
        composeTestRule.onNodeWithText("Meter dinero").assertDoesNotExist()
        composeTestRule.onNodeWithText("Sacar dinero").assertDoesNotExist()
    }

    @Test
    fun n08_sinMovimientosYSinErrorDiceQueAunNoHay() {
        showDrawer(
            CashDrawerUi(
                report = CashDrawerReport(report().summary, emptyList()),
            ),
        )
        composeTestRule.onNodeWithText("Aún no hay movimientos en este turno.").assertIsDisplayed()
    }

    @Test
    fun n08_siLaLecturaFallaSeMuestraElErrorYReintentar() {
        showDrawer(CashDrawerUi(errorMessage = "No se pudieron cargar los movimientos."))
        composeTestRule.onNodeWithText("No se pudieron cargar los movimientos.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reintentar").assertIsDisplayed()
    }
}
