package com.vaiinilla.app.ui.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.screens.StudentTrackingScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/** Capturas del "Ya llegué" del drive-thru en Mis pedidos. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class ArrivalScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val driveThru =
        OperationalStatus(
            acceptingOrders = true,
            cashSessionOpen = true,
            cashierOnline = true,
            kitchenOnline = true,
            estimatedTimeMinutes = 8,
            consultedAt = Instant.now().toString(),
            businessType = "drive_thru",
        )

    @Composable
    private fun tracking(
        order: OrderDetail,
        selected: Boolean,
    ) {
        StudentTrackingScreen(
            state = ScreenshotFixtures.trackingState(order, selected = selected),
            orderState = ScreenshotFixtures.catalogLoadedState().copy(operationalStatus = driveThru),
            onMenu = {},
            onAssistant = {},
            onWallet = {},
            onCart = {},
            onOpenCatalog = {},
            onSelectOrder = {},
        )
    }

    private fun capture(
        name: String,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { ScreenshotTheme { content() } }
        composeTestRule.mainClock.advanceTimeBy(1_500)
        composeTestRule.onRoot().captureRoboImage(name)
    }

    @Test
    fun arrival_button() {
        val order = ScreenshotFixtures.sampleOrder(state = OrderState.PREPARING)
        capture("arrival_button.png") { tracking(order, selected = false) }
    }

    @Test
    fun arrival_announced() {
        val order = ScreenshotFixtures.sampleOrder(state = OrderState.PREPARING).copy(arrivedAt = Instant.now())
        capture("arrival_announced.png") { tracking(order, selected = false) }
    }
}
