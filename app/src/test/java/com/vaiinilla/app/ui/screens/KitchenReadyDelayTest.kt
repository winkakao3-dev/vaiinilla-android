package com.vaiinilla.app.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
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

/** B04: tocar «Marcar lista» y salir antes de que acabe la espera no puede perder la marca. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class KitchenReadyDelayTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val ready = mutableListOf<String>()
    private var onScreen by mutableStateOf(true)

    private fun markReady() {
        val order = ScreenshotFixtures.sampleOrder(state = OrderState.PREPARING)
        composeTestRule.setContent {
            ScreenshotTheme {
                if (onScreen) {
                    KitchenOperationalScreen(
                        state = OperationalUiState(orders = listOf(order), queue = QueueSync.Live),
                        onBack = {},
                        onStart = { _, _ -> },
                        onReady = { id, _ -> ready += id },
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.onNodeWithText("Marcar lista").performScrollTo().performClick()
        composeTestRule.mainClock.advanceTimeBy(100)
    }

    @Test
    fun `se manda una vez al acabar la espera`() {
        markReady()
        composeTestRule.mainClock.advanceTimeBy(KITCHEN_UNDO_MS - 2_000)
        assertEquals(0, ready.size)

        composeTestRule.mainClock.advanceTimeBy(3_000)
        assertEquals(1, ready.size)

        composeTestRule.mainClock.advanceTimeBy(KITCHEN_UNDO_MS)
        assertEquals(1, ready.size)
    }

    @Test
    fun `salir antes de que acabe la espera la manda una sola vez`() {
        markReady()
        composeTestRule.mainClock.advanceTimeBy(1_000)

        onScreen = false
        composeTestRule.waitForIdle()
        assertEquals(1, ready.size)

        composeTestRule.mainClock.advanceTimeBy(KITCHEN_UNDO_MS)
        assertEquals(1, ready.size)
    }
}
