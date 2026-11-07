package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.vaiinilla.app.domain.guardianship.ChildRestriction
import com.vaiinilla.app.domain.guardianship.MyGuardianControls
import com.vaiinilla.app.domain.guardianship.restrictionFor
import com.vaiinilla.app.ui.operational.sampleProduct
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal

/**
 * Cartera del alumno con tutor (W09): el anillo de lo que le queda hoy, su saldo, quién lo cuida y lo que está
 * con candado. Corre en la JVM con Robolectric y Compose; no cuenta como recorrido en teléfono.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class RecorridosCarteraAlumnoTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun controls(
        limit: String? = "60.00",
        left: String? = "42.00",
        blocked: Set<Int> = emptySet(),
        allergic: Set<Int> = emptySet(),
        tutors: List<String> = listOf("Kao Papá"),
    ) = MyGuardianControls(
        tutorNames = tutors,
        dailyLimit = limit?.let(::BigDecimal),
        availableToday = left?.let(::BigDecimal),
        blockedProductIds = blocked,
        allergenProductIds = allergic,
        allergens = emptyList(),
    )

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeTestRule.setContent { VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) { content() } }
        composeTestRule.waitForIdle()
    }

    @Test
    fun w09_elAnilloDiceCuantoLeQuedaHoyDeSuLimiteConSuSaldoYQuienLoCuida() {
        show { StudentAllowance(controls = controls(), balance = "120.00") }
        composeTestRule.onNodeWithText("HOY TE QUEDAN").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Hoy te quedan $42 de $60").assertExists()
        composeTestRule.onNodeWithText("de $60").assertIsDisplayed()
    }

    @Test
    fun w09_sinLimiteHoyElAnilloMuestraElSaldo() {
        show { StudentAllowance(controls = controls(limit = null, left = null), balance = "120.00") }
        composeTestRule.onNodeWithText("TU SALDO").assertIsDisplayed()
    }

    @Test
    fun w09_sinBloqueosNiAlergiasNoSaleConCandado() {
        show { StudentLockedCard(controls = controls()) }
        composeTestRule.onNodeWithText("Con candado", ignoreCase = true).assertDoesNotExist()
        assertTrue(lockedLines(controls()).isEmpty())
    }

    @Test
    fun w09_conBloqueosYAlergiasSaleConCandadoConSuPorQue() {
        val withLocks = controls(blocked = setOf(5, 6), allergic = setOf(9))
        val lines = lockedLines(withLocks)
        assertEquals(listOf("Por tu alergia", "Lo decidió Kao"), lines.map { it.reason })
        assertEquals("2 productos", lines[1].title)
        show { StudentLockedCard(controls = withLocks) }
        composeTestRule.onNodeWithText("Con candado", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Por tu alergia").assertExists()
    }

    @Test
    fun w09_laLineaDeQuienCuidaConcuerdaEnSingularYPlural() {
        assertTrue(studentBalanceLine("120.00", listOf("Kao")).text.endsWith("te cuida Kao"))
        assertTrue(studentBalanceLine(null, listOf("Kao", "Marta")).text.startsWith("Te cuidan"))
    }

    @Test
    fun f06_unProductoConAlergenoDelHijoNoSePuedeAgregar() {
        val product = sampleProduct(9, "Galleta de cacahuate")
        assertEquals(ChildRestriction.Allergen, controls(allergic = setOf(9)).restriction(product))
        assertNull(controls(allergic = setOf(9)).restriction(sampleProduct(10, "Galleta libre")))
    }

    @Test
    fun f06_loQueElTutorBloqueoGanaSobreLaAlergia() {
        val product = sampleProduct(9, "Galleta")
        assertEquals(ChildRestriction.Blocked, controls(blocked = setOf(9), allergic = setOf(9)).restriction(product))
    }

    @Test
    fun f06_unProductoAgotadoSeDiceAgotadoParaCualquiera() {
        assertEquals(
            ChildRestriction.SoldOut,
            controls().restrictionFor(sampleProduct(9, "Galleta").copy(available = false)),
        )
        assertNull(controls().restrictionFor(sampleProduct(9, "Galleta")))
    }
}
