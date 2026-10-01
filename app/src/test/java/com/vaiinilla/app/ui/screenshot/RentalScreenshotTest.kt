package com.vaiinilla.app.ui.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.model.CourtDay
import com.vaiinilla.app.domain.model.CourtProfile
import com.vaiinilla.app.domain.model.CourtSchedule
import com.vaiinilla.app.domain.model.CustomerPrice
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderReservation
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.ui.reservations.ReservationsScreen
import com.vaiinilla.app.ui.reservations.ReservationsUiState
import com.vaiinilla.app.ui.screens.StudentTrackingScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Capturas de la renta de cancha en Mis pedidos y de la ficha de la cancha. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class RentalScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val now: Instant = Instant.now()

    private fun rental(
        orderState: OrderState,
        reservationState: ReservationState,
        startsIn: Long,
    ): OrderDetail {
        val base = ScreenshotFixtures.sampleOrder(state = orderState, paymentMethod = PaymentMethod.CASH)
        val start = now.plus(startsIn, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.HOURS)
        return base.copy(
            summary = base.summary.copy(total = "275.00", folio = 2),
            items = base.items.take(1).map { it.copy(productName = "Renta Cancha 2 · 60 min", quantity = 1) },
            reservation =
                OrderReservation(
                    id = "r-1",
                    space = OrderSpace(id = 8, name = "Cancha 2", type = "cancha"),
                    start = start,
                    end = start.plus(60, ChronoUnit.MINUTES),
                    durationMinutes = 60,
                    state = reservationState,
                ),
        )
    }

    @Composable
    private fun tracking(
        order: OrderDetail,
        selected: Boolean,
    ) {
        StudentTrackingScreen(
            state = ScreenshotFixtures.trackingState(order, selected = selected),
            orderState = ScreenshotFixtures.catalogLoadedState(),
            onMenu = {},
            onAssistant = {},
            onWallet = {},
            onCart = {},
            onOpenCatalog = {},
            onSelectOrder = {},
        )
    }

    @Test
    fun rental_to_pay_expanded() {
        val order = rental(OrderState.PENDING_PAYMENT, ReservationState.PENDING_PAYMENT, startsIn = 120)
        composeTestRule.setContent { ScreenshotTheme { tracking(order, selected = false) } }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Ver seguimiento").performClick()
        composeTestRule.mainClock.advanceTimeBy(1_500)
        composeTestRule.onRoot().captureRoboImage("rental_to_pay_expanded.png")
    }

    @Test
    fun rental_booked_selected() {
        val order = rental(OrderState.DELIVERED, ReservationState.CONFIRMED, startsIn = 180)
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { ScreenshotTheme { tracking(order, selected = true) } }
        composeTestRule.mainClock.advanceTimeBy(1_500)
        composeTestRule.onRoot().captureRoboImage("rental_booked_selected.png")
    }

    @Test
    fun reservations_court_profile() {
        val zone = ZoneId.of("America/Chihuahua")
        val today = LocalDate.now(zone)

        fun at(hour: Int) = today.atTime(hour, 0).atZone(zone).toInstant()
        val day =
            CourtDay(
                date = today.plusDays(1).toString(),
                today = today.toString(),
                timeZone = zone.id,
                opensAt = at(8).plus(1, ChronoUnit.DAYS),
                closesAt = at(22).plus(1, ChronoUnit.DAYS),
                blockMinutes = 30,
                durations = listOf(60, 90, 120),
                daysAhead = 14,
                holdMinutes = 10,
                now = at(16),
                courts =
                    listOf(
                        CourtSchedule(
                            id = 7,
                            name = "Cancha 1",
                            pricePerHour = "300.00",
                            customerPricePerHour = CustomerPrice(card = "326.95", cashOrBalance = "330.00"),
                            rentable = true,
                            busy = emptyList(),
                            profile =
                                CourtProfile(
                                    description = "Cancha techada con paredes de vidrio, a un lado de la barra.",
                                    // Imagen local de muestra: en la app viene del bucket del panel.
                                    imageUrl = "fruta",
                                    features = listOf("Techada", "Con luz", "Vidrio panorámico"),
                                ),
                        ),
                        CourtSchedule(
                            id = 8,
                            name = "Cancha 2",
                            pricePerHour = "250.00",
                            customerPricePerHour = CustomerPrice(card = "274.13", cashOrBalance = "275.00"),
                            rentable = true,
                            busy = emptyList(),
                        ),
                    ),
            )
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            ScreenshotTheme {
                ReservationsScreen(
                    state = ReservationsUiState(loading = false, day = day, selectedCourtId = 7),
                    venueName = "padel prueba",
                    onBack = {},
                    onSelectDate = {},
                    onSelectCourt = {},
                    onRentNow = {},
                    onSelectStart = {},
                    onSelectDuration = {},
                    onReserve = {},
                    onPay = {},
                    onDismissPayment = {},
                    onResumePayment = {},
                    onCancel = {},
                    onDismissMessage = {},
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_500)
        composeTestRule.onRoot().captureRoboImage("reservations_court_profile.png")
    }
}
