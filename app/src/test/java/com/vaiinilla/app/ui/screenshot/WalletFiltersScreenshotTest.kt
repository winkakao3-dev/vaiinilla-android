package com.vaiinilla.app.ui.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertCountEquals
import com.vaiinilla.app.domain.guardianship.MyGuardianControls
import com.vaiinilla.app.domain.model.WalletMovement
import com.vaiinilla.app.domain.school.SchoolAllergen
import com.vaiinilla.app.ui.screens.MovementFilter
import com.vaiinilla.app.ui.screens.MovementsPageContent
import com.vaiinilla.app.ui.screens.WalletLedgerStats
import com.vaiinilla.app.ui.screens.lockedLines
import com.vaiinilla.app.ui.screens.movementDayLabel
import com.vaiinilla.app.ui.screens.studentBalanceLine
import com.vaiinilla.app.ui.screens.walletMonthLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** Cartera (Doop E02), Movimientos (E04) y la cartera del alumno (E14): sus cifras y sus textos. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi", sdk = [33])
class WalletFiltersScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val now = Instant.parse("2026-11-03T18:00:00Z")

    private fun movement(id: String, type: String, amount: String, at: String = "2026-11-03T10:00:00-06:00") =
        WalletMovement(id, type, "", amount, "100.00", null, at)

    @Test
    fun `cada filtro deja pasar solo su tipo`() {
        assertEquals(true, MovementFilter.PURCHASES.matches("compra"))
        assertEquals(true, MovementFilter.RELOADS.matches("recarga_stripe"))
        assertEquals(true, MovementFilter.REFUNDS.matches("devolucion_articulo"))
        assertEquals(false, MovementFilter.REFUNDS.matches("cashback"))
        assertEquals(true, MovementFilter.CASHBACK.matches("cashback"))
        assertEquals(true, MovementFilter.ALL.matches("cashback"))
    }

    @Test
    fun `suma el cashback y las devoluciones del mes`() {
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
        assertEquals("\$98.40", stats.incomeThisMonth)
        assertEquals("Este mes gastaste \$68 y te regresaron \$98.40", walletMonthLine(stats)?.text)
        assertNull(WalletLedgerStats.from(listOf(movement("1", "compra", "-68.00")), now = now).returnedThisMonth)
    }

    @Test
    fun `sin gastos el mes no dice gastaste cero`() {
        val stats = WalletLedgerStats.from(listOf(movement("1", "recarga_efectivo", "200.00")), now = now)
        assertEquals("Este mes no has gastado", walletMonthLine(stats)?.text)
        assertNull(walletMonthLine(WalletLedgerStats.from(emptyList(), now = now)))
    }

    @Test
    fun `cada dia de Movimientos dice hoy, ayer o la fecha corta`() {
        val today = LocalDate.of(2026, 11, 3)
        assertEquals("Hoy", movementDayLabel(today, today))
        assertEquals("Ayer", movementDayLabel(today.minusDays(1), today))
        assertEquals("mar 27 oct", movementDayLabel(LocalDate.of(2026, 10, 27), today))
    }

    @Test
    fun `el alumno ve su saldo, quien lo cuida y lo que tiene candado`() {
        val controls =
            MyGuardianControls(
                tutorNames = listOf("Kao Arreola"),
                dailyLimit = BigDecimal("60"),
                availableToday = BigDecimal("18"),
                blockedProductIds = setOf(1, 2),
                allergenProductIds = setOf(3),
                allergens = listOf(SchoolAllergen.CACAHUATE, SchoolAllergen.HUEVO),
            )
        assertEquals("Tu saldo: \$42 · te cuida Kao Arreola", studentBalanceLine("42.00", controls.tutorNames).text)
        val lines = lockedLines(controls)
        assertEquals("Cacahuate y huevo", lines[0].title)
        assertEquals("Por tu alergia", lines[0].reason)
        assertEquals("2 productos", lines[1].title)
        assertEquals("Lo decidió Kao", lines[1].reason)
    }

    @Test
    fun movimientos_filtros() {
        composeTestRule.setContent {
            ScreenshotTheme {
                MovementsPageContent(
                    movements =
                        listOf(
                            movement("1", "compra", "-68.00"),
                            movement("2", "recarga_efectivo", "200.00", at = "2026-11-02T08:12:00-06:00"),
                            movement("3", "compra", "-95.00", at = "2026-10-20T10:00:00-06:00"),
                        ),
                    onBack = {},
                    now = now,
                )
            }
        }
        composeTestRule.onNodeWithText("HOY").assertExists()
        composeTestRule.onNodeWithText("AYER").assertExists()
        composeTestRule.onNodeWithText("Ver octubre").assertExists()
        composeTestRule.onNodeWithText("Compras").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Compra").assertCountEquals(1)
        composeTestRule.onNodeWithText("Recarga en Caja").assertDoesNotExist()
        composeTestRule.onNodeWithText("Ver octubre").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Compra").assertCountEquals(2)
    }
}