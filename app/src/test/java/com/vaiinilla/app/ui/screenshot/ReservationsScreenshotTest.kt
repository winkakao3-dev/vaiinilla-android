package com.vaiinilla.app.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.model.BusyInterval
import com.vaiinilla.app.domain.model.CourtDay
import com.vaiinilla.app.domain.model.CourtProfile
import com.vaiinilla.app.domain.model.CourtSchedule
import com.vaiinilla.app.domain.model.CustomerPrice
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.ui.reservations.ReservationPaymentContent
import com.vaiinilla.app.ui.reservations.ReservationsScreen
import com.vaiinilla.app.ui.reservations.ReservationsUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
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

/** Capturas de la renta de canchas del cliente. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class ReservationsScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val zone = ZoneId.of("America/Chihuahua")
    private val today: LocalDate = LocalDate.now(zone)

    /** Una hora local de hoy como instante. */
    private fun at(
        hour: Int,
        minute: Int = 0,
    ): Instant = today.atTime(hour, minute).atZone(zone).toInstant()

    private val now: Instant = at(16, 10)

    private val day =
        CourtDay(
            date = today.toString(),
            today = today.toString(),
            timeZone = zone.id,
            opensAt = at(8),
            closesAt = at(22),
            blockMinutes = 30,
            durations = listOf(60, 90, 120, 150, 180),
            daysAhead = 14,
            holdMinutes = 10,
            now = now,
            courts =
                listOf(
                    CourtSchedule(
                        id = 7,
                        name = "Cancha 1",
                        pricePerHour = "300.00",
                        customerPricePerHour = CustomerPrice(card = "326.95", cashOrBalance = "330.00"),
                        rentable = true,
                        busy =
                            listOf(
                                BusyInterval(at(15, 30), at(16, 30), "turno"),
                                BusyInterval(at(19), at(20, 30), "reserva"),
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

    private fun reservation(
        state: ReservationState,
        start: Instant,
        minutes: Long,
        hold: Instant? = null,
    ) = Reservation(
        id = "r-${state.wire}",
        courtId = 7,
        courtName = "Cancha 1",
        start = start,
        end = start.plus(minutes, ChronoUnit.MINUTES),
        durationMinutes = minutes.toInt(),
        amount = "450.00",
        customerPrice = CustomerPrice(card = "490.43", cashOrBalance = "495.00"),
        state = state,
        holdExpiresAt = hold,
        orderId = null,
        channel = "cliente",
        customerName = "Ana",
        version = 1,
    )

    private fun capture(
        name: String,
        mode: VaiinillaThemeMode = VaiinillaThemeMode.Light,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { ScreenshotTheme(mode = mode, content = content) }
        composeTestRule.mainClock.advanceTimeBy(1_500)
        composeTestRule.onRoot().captureRoboImage(name)
    }

    @Composable
    private fun screen(state: ReservationsUiState) {
        ReservationsScreen(
            state = state,
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

    @Test
    fun reservations_day() =
        capture("reservations_day.png") {
            screen(ReservationsUiState(loading = false, day = day, selectedCourtId = 7))
        }

    @Test
    fun reservations_selected() =
        capture("reservations_selected.png") {
            screen(
                ReservationsUiState(
                    loading = false,
                    day = day,
                    selectedCourtId = 7,
                    selectedStart = at(17),
                    selectedDuration = 90,
                ),
            )
        }

    @Test
    fun reservations_rent_now() =
        capture("reservations_rent_now.png") {
            screen(
                ReservationsUiState(
                    loading = false,
                    day = day,
                    selectedCourtId = 8,
                    rentNow = true,
                    selectedDuration = 60,
                ),
            )
        }

    @Test
    fun reservations_selected_dark() =
        capture("reservations_selected_dark.png", VaiinillaThemeMode.Dark) {
            screen(
                ReservationsUiState(
                    loading = false,
                    day = day,
                    selectedCourtId = 7,
                    selectedStart = at(17),
                    selectedDuration = 90,
                ),
            )
        }

    @Test
    fun reservations_mine() =
        capture("reservations_mine.png") {
            screen(
                ReservationsUiState(
                    loading = false,
                    day = day.copy(courts = day.courts.map { it.copy(busy = emptyList()) }),
                    selectedCourtId = 7,
                    mine =
                        listOf(
                            reservation(
                                ReservationState.PENDING_PAYMENT,
                                at(20),
                                90,
                                hold = Instant.now().plusSeconds(480),
                            ),
                            reservation(ReservationState.CONFIRMED, at(21), 60),
                        ),
                    notice = "Paga en caja antes de que venza el apartado.",
                ),
            )
        }

    @Test
    fun l08_paga_cancha() =
        capture("lugar/l08_paga_cancha.png", VaiinillaThemeMode.Dark) {
            val held = Instant.now()
            val padel =
                day.copy(
                    now = held,
                    opensAt = held.minus(2, ChronoUnit.HOURS),
                    closesAt = held.plus(6, ChronoUnit.HOURS),
                    durations = listOf(60, 90, 120),
                    courts =
                        listOf(
                            day.courts.first().copy(
                                customerPricePerHour = CustomerPrice(card = "350.00", cashOrBalance = "350.00"),
                                busy = listOf(BusyInterval(held, held.plus(90, ChronoUnit.MINUTES), "reserva")),
                                profile = CourtProfile(sport = "Pádel"),
                            ),
                        ),
                )
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0E0F0D)), contentAlignment = Alignment.BottomCenter) {
                Box(modifier = Modifier.background(LocalVaiinillaColors.current.paper, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))) {
                    ReservationPaymentContent(
                        reservation =
                            reservation(ReservationState.PENDING_PAYMENT, held, 90, hold = held.plusSeconds(292))
                                .copy(customerPrice = CustomerPrice(card = "525.00", cashOrBalance = "525.00")),
                        zone = zone,
                        working = false,
                        onPay = {},
                        colors = LocalVaiinillaColors.current,
                        cardAvailable = true,
                        day = padel,
                        startsNow = true,
                    )
                }
            }
        }

    @Test
    fun reservations_payment() =        capture("reservations_payment.png") {
            Box(modifier = Modifier.fillMaxSize().background(LocalVaiinillaColors.current.paper)) {
                ReservationPaymentContent(
                    reservation =
                        reservation(
                            ReservationState.PENDING_PAYMENT,
                            at(17),
                            90,
                            hold = Instant.now().plusSeconds(540),
                        ),
                    zone = zone,
                    working = false,
                    onPay = {},
                    colors = LocalVaiinillaColors.current,
                    cardAvailable = true,
                )
            }
        }
}
