package com.vaiinilla.app.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.operational.OperationalUiState
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

/** B04: Atrás limpia el rol antes de que la pantalla se vaya; la marca de «lista» tiene que salir antes. */
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
    fun `Atras manda la marca pendiente antes de salir`() {
        val events = mutableListOf<String>()
        val order = ScreenshotFixtures.sampleOrder(state = OrderState.PREPARING)
        composeTestRule.setContent {
            ScreenshotTheme {
                KitchenOperationalScreen(
                    state = OperationalUiState(orders = listOf(order), queue = QueueSync.Live),
                    onBack = { events += "salir" },
                    onStart = { _, _ -> },
                    onReady = { _, _ -> events += "lista" },
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.onNodeWithText("Marcar lista").performScrollTo().performClick()
        composeTestRule.mainClock.advanceTimeBy(100)

        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf("lista", "salir"), events)
    }
}
