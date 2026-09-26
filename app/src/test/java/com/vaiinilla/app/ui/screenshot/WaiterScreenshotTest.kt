package com.vaiinilla.app.ui.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.TableCallTaker
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.ui.screens.StudentTrackingScreen
import com.vaiinilla.app.ui.screens.WaiterOperationalScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class WaiterScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun capture(
        name: String,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.setContent { ScreenshotTheme(content = content) }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(name)
    }

    private fun openCall(status: CallStatus): TableCall =
        TableCall(
            id = "call-4",
            space = TableSpace(id = 4, name = "Mesa 4", type = "mesa"),
            orderId = null,
            reason = CallReason.UTENSILIOS,
            status = status,
            clientName = "Ana",
            takenBy = if (status == CallStatus.EN_CAMINO) TableCallTaker(userId = "u-2", name = "Luis") else null,
            createdAt =
                java.time.Instant
                    .now()
                    .minusSeconds(222)
                    .toString(),
            takenAt = null,
            closedAt = null,
            version = 1,
        )

    @Test
    fun `waiter_board`() {
        capture("waiter_board.png") {
            WaiterOperationalScreen(
                state = ScreenshotFixtures.waiterBoardState(),
                onBack = {},
                placeName = "Cafetería Central",
            )
        }
    }

    @Test
    fun `waiter_board_filter`() {
        capture("waiter_board_filter.png") {
            WaiterOperationalScreen(
                state = ScreenshotFixtures.waiterBoardState().copy(filterAttending = true),
                onBack = {},
                placeName = "Cafetería Central",
            )
        }
    }

    @Test
    fun `waiter_call_idle`() {
        val order =
            ScreenshotFixtures.sampleOrder(
                state = OrderState.READY,
                destination = OrderDestination.IN_SPACE,
                spaceId = 4,
            )
        capture("waiter_call_idle.png") {
            StudentTrackingScreen(
                state = ScreenshotFixtures.trackingState(order, selected = false),
                orderState = ScreenshotFixtures.catalogLoadedState(),
                onMenu = {},
                onAssistant = {},
                onWallet = {},
                onCart = {},
                onOpenCatalog = {},
                onSelectOrder = {},
                canCallWaiter = { true },
                onCurrentWaiterCall = { Result.success(null) },
                onCallWaiter = { _, _, _ -> Result.success(openCall(CallStatus.PENDIENTE)) },
                onCancelWaiter = { Result.success(it) },
            )
        }
    }

    @Test
    fun `waiter_call_reasons`() {
        val order =
            ScreenshotFixtures.sampleOrder(
                state = OrderState.READY,
                destination = OrderDestination.IN_SPACE,
                spaceId = 4,
            )
        composeTestRule.setContent {
            ScreenshotTheme {
                StudentTrackingScreen(
                    state = ScreenshotFixtures.trackingState(order, selected = false),
                    orderState = ScreenshotFixtures.catalogLoadedState(),
                    onMenu = {},
                    onAssistant = {},
                    onWallet = {},
                    onCart = {},
                    onOpenCatalog = {},
                    onSelectOrder = {},
                    canCallWaiter = { true },
                    onCurrentWaiterCall = { Result.success(null) },
                    onCallWaiter = { _, _, _ -> Result.success(openCall(CallStatus.PENDIENTE)) },
                    onCancelWaiter = { Result.success(it) },
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Llamar al mesero").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("waiter_call_reasons.png")
    }

    @Test
    fun `waiter_call_on_the_way`() {
        val order =
            ScreenshotFixtures.sampleOrder(
                state = OrderState.READY,
                destination = OrderDestination.IN_SPACE,
                spaceId = 4,
            )
        val call = openCall(CallStatus.EN_CAMINO)
        capture("waiter_call_on_the_way.png") {
            StudentTrackingScreen(
                state = ScreenshotFixtures.trackingState(order, selected = false),
                orderState = ScreenshotFixtures.catalogLoadedState(),
                onMenu = {},
                onAssistant = {},
                onWallet = {},
                onCart = {},
                onOpenCatalog = {},
                onSelectOrder = {},
                canCallWaiter = { true },
                onCurrentWaiterCall = { Result.success(call) },
                onCallWaiter = { _, _, _ -> Result.success(call) },
                onCancelWaiter = { Result.success(it) },
            )
        }
    }
}
