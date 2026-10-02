package com.vaiinilla.app.ui.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.ui.screens.StudentTrackingScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Un pedido sin cocina en listo: "Preparando" sale omitido, no hecho. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class SkippedPreparingScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tracking_skipped_preparing() {
        val base = ScreenshotFixtures.sampleOrder(state = OrderState.READY)
        val order = base.copy(items = base.items.map { it.copy(preparationStation = PreparationStation.CASHIER) })
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            ScreenshotTheme {
                StudentTrackingScreen(
                    state = ScreenshotFixtures.trackingState(order, selected = true),
                    orderState = ScreenshotFixtures.catalogLoadedState(),
                    onMenu = {},
                    onAssistant = {},
                    onWallet = {},
                    onCart = {},
                    onOpenCatalog = {},
                    onSelectOrder = {},
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_500)
        composeTestRule.onRoot().captureRoboImage("tracking_skipped_preparing.png")
    }
}
