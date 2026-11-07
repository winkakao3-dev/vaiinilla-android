package com.vaiinilla.app.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.operational.PendingReady
import com.vaiinilla.app.ui.operational.QueueSync
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * B04/N02: la pantalla solo avisa que se tocó «Marcar lista» y pinta la espera que le da el ViewModel; ya no tiene
 * temporizador propio, así que girar o cambiar de pestaña no pierde la marca.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class KitchenReadyDelayTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val order = ScreenshotFixtures.sampleOrder(state = OrderState.PREPARING)

    @Test
    fun `tocar Marcar lista avisa una vez con la version del pedido`() {
        val marked = mutableListOf<Pair<String, Int>>()
        composeTestRule.setContent {
            ScreenshotTheme {
                KitchenOperationalScreen(
                    state = OperationalUiState(orders = listOf(order), queue = QueueSync.Live),
                    onBack = {},
                    onStart = { _, _ -> },
                    onMarkReady = { id, version -> marked += id to version },
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.onNodeWithText("Marcar lista").performScrollTo().performClick()
        composeTestRule.mainClock.advanceTimeBy(KITCHEN_UNDO_MS * 2)

        assertEquals(listOf(order.summary.id to order.summary.version), marked)
    }

    @Test
    fun `con una marca en espera la pantalla ofrece Deshacer y lo avisa`() {
        val undone = mutableListOf<String>()
        composeTestRule.setContent {
            ScreenshotTheme {
                KitchenOperationalScreen(
                    state =
                        OperationalUiState(
                            orders = listOf(order),
                            queue = QueueSync.Live,
                            pendingReady =
                                mapOf(
                                    order.summary.id to
                                        PendingReady(
                                            orderId = order.summary.id,
                                            version = order.summary.version,
                                            deadlineMs = System.currentTimeMillis() + KITCHEN_UNDO_MS,
                                        ),
                                ),
                        ),
                    onBack = {},
                    onStart = { _, _ -> },
                    onMarkReady = { _, _ -> },
                    onUndoReady = { undone += it },
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.onNodeWithText("Deshacer").performScrollTo().performClick()

        assertEquals(listOf(order.summary.id), undone)
    }
}
