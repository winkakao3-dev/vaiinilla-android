package com.vaiinilla.app.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

/** B04: Atrás limpia el rol antes de que la pantalla se vaya; las marcas de «lista» tienen que salir antes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class KitchenReadyBeforeLeavingTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `Atras con marcas en espera las manda antes de salir`() {
        val events = mutableListOf<String>()
        val order = ScreenshotFixtures.sampleOrder(state = OrderState.PREPARING)
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
                                        PendingReady(order.summary.id, order.summary.version, Long.MAX_VALUE),
                                ),
                        ),
                    onBack = { events += "salir" },
                    onStart = { _, _ -> },
                    onMarkReady = { _, _ -> },
                    onFlushReady = { events += "lista" },
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)

        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf("lista", "salir"), events)
    }
}
