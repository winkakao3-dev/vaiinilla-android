package com.vaiinilla.app.counter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.vaiinilla.app.domain.counter.OfflineStudentPurchase
import com.vaiinilla.app.domain.counter.SchoolPolicy
import com.vaiinilla.app.domain.counter.StudentCard
import com.vaiinilla.app.domain.counter.StudentPurchaseItem
import com.vaiinilla.app.domain.counter.StudentTodayOrder
import com.vaiinilla.app.ui.counter.CashierStudentsEntry
import com.vaiinilla.app.ui.counter.ChargePhase
import com.vaiinilla.app.ui.counter.OfflineCounterSection
import com.vaiinilla.app.ui.counter.OfflineCounterState
import com.vaiinilla.app.ui.counter.StudentCardPane
import com.vaiinilla.app.ui.counter.StudentCounterActions
import com.vaiinilla.app.ui.counter.StudentCounterUiState
import com.vaiinilla.app.ui.counter.TicketAllergyBand
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal
import java.time.Instant

/** La ficha del alumno en Caja (PIN y alergias), la sección Por resolver y la banda del ticket. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi", sdk = [33])
class StudentCounterUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val tilin =
        StudentCard(
            userId = "u-tilin",
            name = "Tilin Ramírez",
            studentId = "A100",
            group = "3B",
            hasOwnAccount = false,
            balance = BigDecimal("390.00"),
            spentToday = BigDecimal("40.00"),
            dailyLimit = BigDecimal("100.00"),
            allergens = listOf("cacahuate", "nueces"),
            todayOrders =
                listOf(StudentTodayOrder("p1", 12, "listo", 3, BigDecimal("45.00"), "1× Torta")),
            requiresPin = true,
        )

    private fun setCard(
        state: StudentCounterUiState,
        requiresCredential: Boolean = true,
        actions: StudentCounterActions = StudentCounterActions(),
    ) {
        composeTestRule.setContent {
            ScreenshotTheme {
                StudentCardPane(
                    card = state.selected!!,
                    state = state,
                    catalog = null,
                    canCharge = true,
                    requiresCredential = requiresCredential,
                    colors = rememberOperationalColors(),
                    actions = actions,
                )
            }
        }
    }

    @Test
    fun `la ficha dice lo que puede gastar, sus alergias en la banda y pide el PIN antes de cobrar`() {
        val typed = mutableListOf<String>()
        setCard(
            StudentCounterUiState(selected = tilin, cart = mapOf(4 to 1)),
            actions = StudentCounterActions(onPinChange = { typed += it }),
        )
        composeTestRule.onNodeWithText("$60").assertIsDisplayed()
        composeTestRule.onNodeWithText("Saldo $390 · lleva $40 de $100 hoy").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Alérgico a cacahuate, nueces").assertIsDisplayed()
        composeTestRule.onNodeWithText("Su tutor le puso PIN: pídeselo.").assertIsDisplayed()
        composeTestRule.onNodeWithText("de su saldo", substring = true).assertIsNotEnabled()

        composeTestRule.onNodeWithContentDescription("PIN de Tilin").performTextInput("12a34")
        assertEquals("1234", typed.last())
    }

    @Test
    fun `con el PIN completo se cobra, y un PIN equivocado le habla a la cajera`() {
        var charged: BigDecimal? = null
        setCard(
            StudentCounterUiState(
                selected = tilin,
                cart = mapOf(4 to 1),
                pin = "1234",
                chargePhase = ChargePhase.Failed("El PIN no coincide; quedan 3 intentos."),
            ),
            actions = StudentCounterActions(onCharge = { charged = it }),
        )
        composeTestRule.onNodeWithText("El PIN no coincide; quedan 3 intentos.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cobrar", substring = true).assertIsEnabled().performClick()
        assertEquals(BigDecimal.ZERO, charged)
    }

    @Test
    fun `con QR obligatorio lo listo se escanea antes de entregar`() {
        setCard(StudentCounterUiState(selected = tilin, policy = SchoolPolicy(true, true)))
        composeTestRule.onNodeWithText("Escanear").assertIsDisplayed()
        composeTestRule.onNodeWithText("Entregar").assertDoesNotExist()
    }

    @Test
    fun `con la credencial leida se entrega`() {
        val delivered = mutableListOf<String>()
        setCard(
            StudentCounterUiState(selected = tilin, scannedCredential = "vaiinilla:alumno:abc"),
            actions = StudentCounterActions(onDeliver = { delivered += it.id }),
        )
        composeTestRule.onNodeWithText("Entregar").performClick()
        assertEquals(listOf("p1"), delivered)
    }

    @Test
    fun `por resolver dice el motivo y se quita despues de confirmar`() {
        val problem =
            OfflineStudentPurchase(
                id = "k",
                studentId = "u-tilin",
                studentName = "Tilin Ramírez",
                items = listOf(StudentPurchaseItem(4, 1)),
                total = BigDecimal("20.00"),
                createdAt = Instant.parse("2026-10-21T15:00:00Z"),
                problem = "Tilin ya no tiene saldo suficiente.",
            )
        val pending = problem.copy(id = "p", problem = null, total = BigDecimal("35.50"))
        val settled = mutableListOf<String>()
        val state = OfflineCounterState(pending = listOf(pending), problems = listOf(problem))
        composeTestRule.setContent {
            ScreenshotTheme {
                val colors = rememberOperationalColors()
                androidx.compose.foundation.layout.Column {
                    CashierStudentsEntry(offline = state, colors = colors, onClick = {})
                    OfflineCounterSection(state = state, colors = colors, onSettle = { settled += it.id })
                }
            }
        }
        composeTestRule.onNodeWithText("1 por resolver").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 venta por mandar · $35.50").assertIsDisplayed()
        composeTestRule.onNodeWithText("Por resolver · Tilin Ramírez").assertIsDisplayed()
        composeTestRule.onNodeWithText("$20 no se cobró: Tilin ya no tiene saldo suficiente.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ya se lo cobré de otra forma").performClick()
        composeTestRule.onNodeWithText("¿Ya le cobraste $20 a Tilin Ramírez de otra forma?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sí, quitarlo").performClick()
        assertEquals(listOf("k"), settled)
    }

    @Test
    fun `el ticket lleva la banda de alergias y nada si no tiene`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                androidx.compose.foundation.layout.Column {
                    TicketAllergyBand(listOf("huevo", "lactosa"))
                    TicketAllergyBand(emptyList())
                }
            }
        }
        composeTestRule.onNodeWithContentDescription("Alergia: Huevo, Lactosa").assertIsDisplayed()
    }
}
