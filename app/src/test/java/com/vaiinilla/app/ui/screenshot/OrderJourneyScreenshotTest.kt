package com.vaiinilla.app.ui.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.order.JourneyVenue
import com.vaiinilla.app.ui.screens.PickupJourneyScreen
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/** La hoja de seguimiento de cada tipo de pedido (diseño P1 a P13 de Doop). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class OrderJourneyScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun inSpace(
        state: OrderState,
        type: String,
        name: String,
    ): OrderDetail {
        val order =
            ScreenshotFixtures.sampleOrder(
                state = state,
                paymentMethod = PaymentMethod.BALANCE,
                destination = OrderDestination.IN_SPACE,
            )
        return order.copy(summary = order.summary.copy(space = OrderSpace(id = 2, name = name, type = type)))
    }

    private fun capture(
        name: String,
        order: OrderDetail,
        venue: JourneyVenue = JourneyVenue(),
    ) {
        composeTestRule.setContent {
            ScreenshotTheme(VaiinillaThemeMode.Light) {
                PickupJourneyScreen(
                    order = order,
                    qrPayload = null,
                    onBack = {},
                    onViewTracking = {},
                    venue = venue,
                    onAnnounceArrival = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(3_000)
        composeTestRule.onRoot().captureRoboImage("journey/$name.png")
    }

    @Test
    fun `journey_bar_preparing`() = capture("journey_bar_preparing", inSpace(OrderState.PREPARING, "barra", "Barra"))

    @Test
    fun `journey_court_food_ready`() =
        capture("journey_court_food_ready", inSpace(OrderState.READY, "cancha", "Cancha 2"))

    @Test
    fun `journey_drive_thru_waiting`() =
        capture(
            "journey_drive_thru_waiting",
            ScreenshotFixtures.sampleOrder(state = OrderState.PREPARING, paymentMethod = PaymentMethod.STRIPE),
            JourneyVenue(driveThru = true, whatsappUrl = "https://wa.me/5216140000000"),
        )

    @Test
    fun `journey_drive_thru_arrived`() =
        capture(
            "journey_drive_thru_arrived",
            ScreenshotFixtures
                .sampleOrder(state = OrderState.PREPARING, paymentMethod = PaymentMethod.STRIPE)
                .copy(arrivedAt = Instant.parse("2026-11-02T18:00:00Z")),
            JourneyVenue(driveThru = true),
        )

    @Test
    fun `journey_counter_cash_due`() = capture("journey_counter_cash_due", ScreenshotFixtures.sampleOrder())

    @Test
    fun `journey_preorder_awaiting`() {
        val order =
            ScreenshotFixtures.sampleOrder(
                state = OrderState.AWAITING_ACCEPTANCE,
                paymentMethod = PaymentMethod.STRIPE,
            )
        capture(
            "journey_preorder_awaiting",
            order.copy(
                summary = order.summary.copy(deliveryDate = "2026-11-09", deliverySlot = "12:00-13:00"),
            ),
        )
    }
}
