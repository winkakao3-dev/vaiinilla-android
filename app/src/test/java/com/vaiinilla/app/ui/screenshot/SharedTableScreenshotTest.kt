package com.vaiinilla.app.ui.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.SharedTable
import com.vaiinilla.app.domain.model.SharedTableAmounts
import com.vaiinilla.app.domain.model.SharedTableGroup
import com.vaiinilla.app.domain.model.SharedTableOrder
import com.vaiinilla.app.domain.model.SharedTableParticipant
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.screens.SharedTableCard
import com.vaiinilla.app.ui.screens.StudentTrackingScreen
import com.vaiinilla.app.ui.sharedtable.SharedTableUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal

/** Mesa compartida en Mis pedidos, junto a una tarjeta de pedido real. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h1600dp-normal-long-notround-any-xxhdpi", sdk = [33])
class SharedTableScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun money(value: String) = BigDecimal(value)

    private fun amounts(
        total: String,
        paid: String,
        pending: String,
    ) = SharedTableAmounts(money(total), money(paid), money(pending))

    private fun tableOrder(
        folio: Int,
        items: String,
        total: String,
        state: OrderState,
        pending: Boolean,
        payer: String? = null,
        iPayIt: Boolean = false,
    ) = SharedTableOrder(null, folio, state, items, money(total), pending, payer, iPayIt)

    private val table =
        SharedTable(
            spaceId = 3,
            spaceName = "Mesa 3",
            spaceType = "mesa",
            myAlias = "Ana",
            accountOpen = true,
            participants =
                listOf(
                    SharedTableParticipant("Ana", true),
                    SharedTableParticipant("Luis", false),
                    SharedTableParticipant("Sofi", false),
                ),
            groups =
                listOf(
                    SharedTableGroup(
                        "Luis",
                        false,
                        listOf(
                            tableOrder(
                                42,
                                "3× Tacos al pastor",
                                "85.00",
                                OrderState.READY,
                                true,
                                payer = "Ana",
                                iPayIt = true,
                            ),
                            tableOrder(43, "2× Refresco", "72.00", OrderState.DELIVERED, false),
                        ),
                        amounts("157.00", "72.00", "85.00"),
                    ),
                    SharedTableGroup(
                        "Ana",
                        true,
                        listOf(tableOrder(41, "1× Hamburguesa", "133.00", OrderState.PREPARING, true)),
                        amounts("133.00", "0.00", "133.00"),
                    ),
                    SharedTableGroup(
                        "Sofi",
                        false,
                        listOf(tableOrder(44, "1× Agua", "45.00", OrderState.PAID, true, payer = "Sofi")),
                        amounts("45.00", "0.00", "45.00"),
                    ),
                ),
            totals = amounts("335.00", "72.00", "263.00"),
            myShare = amounts("218.00", "0.00", "218.00"),
        )

    @Test
    fun `mesa_compartida_en_mis_pedidos`() {
        val order =
            ScreenshotFixtures.sampleOrder(
                state = OrderState.PREPARING,
                destination = OrderDestination.IN_SPACE,
                spaceId = 3,
            )
        composeTestRule.setContent {
            ScreenshotTheme {
                ScreenshotWithStudentNav(activeTab = StudentTab.ORDERS, cartCount = 0) {
                    StudentTrackingScreen(
                        state = ScreenshotFixtures.trackingState(order, selected = false),
                        orderState = ScreenshotFixtures.catalogLoadedState(),
                        onMenu = {},
                        onAssistant = {},
                        onWallet = {},
                        onCart = {},
                        onOpenCatalog = {},
                        onSelectOrder = {},
                        sharedTable = {
                            SharedTableCard(
                                state = SharedTableUiState(loaded = true, table = table),
                                qrToken = "qr",
                                onAliasChange = {},
                                onJoin = {},
                                onClaim = { _, _ -> },
                                onLeave = {},
                            )
                        },
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(2_000)
        composeTestRule.onRoot().captureRoboImage("mesa/mesa_compartida.png")
    }

    @Test
    fun `mesa_compartida_unirse`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                ScreenshotWithStudentNav(activeTab = StudentTab.ORDERS, cartCount = 0) {
                    StudentTrackingScreen(
                        state = ScreenshotFixtures.emptyTrackingState(),
                        orderState = ScreenshotFixtures.catalogLoadedState(),
                        onMenu = {},
                        onAssistant = {},
                        onWallet = {},
                        onCart = {},
                        onOpenCatalog = {},
                        onSelectOrder = {},
                        sharedTable = {
                            SharedTableCard(
                                state = SharedTableUiState(loaded = true, table = null, aliasInput = "Ana"),
                                qrToken = "qr",
                                onAliasChange = {},
                                onJoin = {},
                                onClaim = { _, _ -> },
                                onLeave = {},
                            )
                        },
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(2_000)
        composeTestRule.onRoot().captureRoboImage("mesa/mesa_unirse.png")
    }
}
