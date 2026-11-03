package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.SharedTable
import com.vaiinilla.app.domain.model.SharedTableAmounts
import com.vaiinilla.app.domain.model.SharedTableGroup
import com.vaiinilla.app.domain.model.SharedTableOrder
import com.vaiinilla.app.domain.model.SharedTableParticipant
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.ui.order.MenuWaiterCallState
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal

/** La cuenta de la mesa compartida (Doop L04): tu parte, los pedidos de todos, tu mesero y el pago. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class SharedTablePageUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun amounts(
        total: String,
        paid: String,
        pending: String,
    ) = SharedTableAmounts(BigDecimal(total), BigDecimal(paid), BigDecimal(pending))

    private fun order(
        folio: Int,
        items: String,
        total: String,
        state: OrderState,
        pending: Boolean,
        payer: String? = null,
        iPayIt: Boolean = false,
    ) = SharedTableOrder(null, folio, state, items, BigDecimal(total), pending, payer, iPayIt)

    private val table =
        SharedTable(
            spaceId = 4,
            spaceName = "Mesa 4",
            spaceType = "mesa",
            myAlias = "Kao",
            accountOpen = true,
            participants =
                listOf(
                    SharedTableParticipant("Kao", true),
                    SharedTableParticipant("Luis", false),
                    SharedTableParticipant("Mariana", false),
                    SharedTableParticipant("Andrés", false),
                ),
            groups =
                listOf(
                    SharedTableGroup(
                        "Kao",
                        true,
                        listOf(order(221, "Pan francés, capuchino", "137.00", OrderState.PREPARING, true)),
                        amounts("137.00", "0.00", "137.00"),
                    ),
                    SharedTableGroup(
                        "Luis",
                        false,
                        listOf(order(218, "Chilaquiles, jugo", "118.00", OrderState.DELIVERED, false)),
                        amounts("118.00", "118.00", "0.00"),
                    ),
                    SharedTableGroup(
                        "Mariana",
                        false,
                        listOf(order(219, "Molletes, té", "92.00", OrderState.PREPARING, true, payer = "Kao", iPayIt = true)),
                        amounts("92.00", "0.00", "92.00"),
                    ),
                    SharedTableGroup(
                        "Andrés",
                        false,
                        listOf(order(220, "Concha, café", "65.00", OrderState.PREPARING, true)),
                        amounts("65.00", "0.00", "65.00"),
                    ),
                ),
            totals = amounts("412.00", "118.00", "294.00"),
            myShare = amounts("229.00", "0.00", "229.00"),
        )

    private val claims = mutableListOf<Pair<Int, Boolean>>()
    private val sent = mutableListOf<CallReason>()

    private fun page() {
        val waiter =
            MenuWaiterCallState(
                spaceId = 4,
                onCurrent = { Result.success(null) },
                onCall = { _, reason, _ ->
                    sent += reason
                    Result.success(
                        TableCall(
                            id = "call-1",
                            space = TableSpace(id = 4, name = "Mesa 4", type = "mesa"),
                            orderId = null,
                            reason = reason,
                            status = CallStatus.PENDIENTE,
                            clientName = "Kao",
                            takenBy = null,
                            createdAt = "2026-11-03T16:54:08.777Z",
                            takenAt = null,
                            closedAt = null,
                            version = 1,
                        ),
                    )
                },
                onCancel = { Result.success(it) },
                onJoinTable = null,
            )
        composeTestRule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
                SharedTablePage(
                    table = table,
                    busy = false,
                    error = null,
                    errorFolio = null,
                    onClaim = { folio, payIt -> claims += folio to payIt },
                    onLeave = {},
                    waiter = waiter,
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_500)
    }

    @Test
    fun `cuenta de la mesa como el dibujo de Doop`() {
        page()
        composeTestRule.onRoot().captureRoboImage("lugar/l04_mesa_compartida.png")
    }

    @Test
    fun `lo que paga suma lo tuyo y lo de quien dijiste que pagas`() {
        assertEquals("Tu parte + lo de Mariana", tablePayLine(table))
        assertEquals("Tu parte", tablePayLine(table.copy(groups = table.groups.filter { it.alias != "Mariana" })))
    }

    @Test
    fun `esto lo pago yo y pagar con tu mesero`() {
        page()
        composeTestRule.onNodeWithText("Mesa compartida · 4 personas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tu parte + lo de Mariana").assertIsDisplayed()
        // Al cliente nunca se le dice el nombre del mesero.
        composeTestRule.onNodeWithText("Tu mesero").assertIsDisplayed()

        composeTestRule.onNodeWithText("Esto lo pago yo").performClick()
        composeTestRule.onNodeWithText("Lo pago yo").performClick()
        assertEquals(listOf(220 to true, 219 to false), claims)

        composeTestRule.onNodeWithTag(SHARED_TABLE_PAY_TAG).performClick()
        composeTestRule.waitForIdle()
        assertEquals(listOf(CallReason.CUENTA), sent)
    }
}
