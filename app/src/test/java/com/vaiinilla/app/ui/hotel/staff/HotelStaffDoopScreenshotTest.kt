package com.vaiinilla.app.ui.hotel.staff

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.hotel.ChargeCategory
import com.vaiinilla.app.domain.hotel.CheckoutMethod
import com.vaiinilla.app.domain.hotel.HotelRequest
import com.vaiinilla.app.domain.hotel.HotelRoom
import com.vaiinilla.app.domain.hotel.RequestStatus
import com.vaiinilla.app.domain.hotel.RequestType
import com.vaiinilla.app.domain.hotel.RoomCleaning
import com.vaiinilla.app.domain.hotel.Stay
import com.vaiinilla.app.domain.hotel.StayCharge
import com.vaiinilla.app.domain.hotel.StayOrder
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.Venue
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import com.vaiinilla.app.ui.screens.KitchenOperationalScreen
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.LocalDate

/**
 * Las pantallas del personal del hotel en el tema oscuro del dibujo, para ponerlas junto a los marcos de Doop
 * (página «Hotel · huésped, staff y admin»): S01 cocina, S02 entrega, S03 ama de llaves y S04 check-out, más
 * Recepción, check-in y cargo manual, que no tienen marco propio. Solo capturan; el parecido se revisa a la vista.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w390dp-h844dp-normal-long-notround-any-xxhdpi",
    sdk = [33],
)
class HotelStaffDoopScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun hotel() = Venue.update(Venue.HOTEL)

    @After
    fun noHotel() = Venue.update(null)

    private fun capture(
        name: String,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.setContent { ScreenshotTheme(mode = VaiinillaThemeMode.Dark, content = content) }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(name)
    }

    private val now = Instant.parse("2026-11-06T20:06:00Z")
    private val today = LocalDate.parse("2026-11-08")

    private fun room(
        number: Int,
        cleaning: RoomCleaning = RoomCleaning.READY,
        occupied: Boolean = false,
        dnd: Boolean = false,
    ) = HotelRoom(id = number, name = "Habitación $number", floor = 3, cleaning = cleaning, occupied = occupied, doNotDisturb = dnd)

    private val floor3 =
        listOf(
            room(301, occupied = true),
            room(302, RoomCleaning.DIRTY),
            room(303),
            room(304, occupied = true),
            room(305, RoomCleaning.DIRTY),
            room(306),
            room(307, occupied = true, dnd = true),
            room(308),
            room(309, occupied = true),
            room(310, RoomCleaning.DIRTY),
            room(311),
            room(312, occupied = true),
        )

    private val stay =
        Stay(
            id = "S1",
            guestName = "Laura Ramos",
            code = "482916",
            roomId = 304,
            roomName = "Habitación 304",
            floor = 3,
            checkIn = "2026-11-06",
            checkOut = "2026-11-09",
            nightlyRate = "1650.00",
            nights = 3,
            orders = listOf(StayOrder("a", 1, "236.50", "entregado"), StayOrder("b", 2, "548.00", "entregado")),
            charges =
                listOf(
                    StayCharge("c1", "Minibar", "90.00", ChargeCategory.MINIBAR),
                    StayCharge("c2", "Spa", "310.00", ChargeCategory.SPA),
                ),
        )

    @Test
    fun s01_cocina() {
        val base = ScreenshotFixtures.sampleOrder(state = OrderState.PAID, destination = OrderDestination.IN_SPACE, spaceId = 304)
        val order =
            base.copy(
                summary = base.summary.copy(space = OrderSpace(304, "Habitación 304", "mesa")),
                items =
                    listOf(
                        base.items.first().copy(productName = "Chilaquiles verdes", quantity = 1, rejection = null),
                        base.items.first().copy(id = 99, productName = "Café americano", quantity = 1, rejection = null),
                    ),
            )
        capture("doop_hotel_s01_cocina.png") {
            KitchenOperationalScreen(
                state = OperationalUiState(orders = listOf(order), lastRefreshOk = true),
                onBack = {},
                onStart = { _, _ -> },
                onReady = { _, _ -> },
                placeName = "Hotel Casa Vainilla",
            )
        }
    }

    @Test
    fun s02_entrega() {
        capture("doop_hotel_s02_entrega.png") {
            HotelDeliverContent(
                room = "Hab. 304",
                floor = 3,
                lines = deliveryLines("1× Chilaquiles verdes, 1× Café americano"),
                charge = "236.50",
                acting = false,
                onConfirm = {},
                onBack = null,
                initialCode = "48",
            )
        }
    }

    @Test
    fun s02_entrega_sin_codigo() {
        capture("doop_hotel_s02_entrega_sin_codigo.png") {
            HotelDeliverContent(
                room = "Hab. 304",
                floor = 3,
                lines = deliveryLines("1× Chilaquiles verdes, 1× Café americano"),
                charge = "236.50",
                acting = false,
                onConfirm = {},
                onBack = null,
            )
        }
    }

    @Test
    fun s03_ama_de_llaves() {
        val requests =
            listOf(
                HotelRequest("1", roomName = "Habitación 304", type = RequestType.TOWELS, quantity = 2, createdAt = "2026-11-06T20:00:00Z"),
                HotelRequest("2", roomName = "Habitación 309", type = RequestType.CLEANING, note = "Después de las 11:00", createdAt = "2026-11-06T19:40:00Z"),
                HotelRequest(
                    "3",
                    roomName = "Habitación 312",
                    type = RequestType.PILLOWS,
                    status = RequestStatus.TAKEN,
                    takenBy = "Rosa",
                    createdAt = "2026-11-06T19:30:00Z",
                ),
            )
        capture("doop_hotel_s03_ama_de_llaves.png") {
            HotelRoomsContent(
                state = HotelRoomsUiState(rooms = floor3, requests = requests, loaded = true),
                now = now,
                onFilter = {},
                onFloor = {},
                onRoom = {},
                onTake = {},
                onResolve = {},
                onBack = null,
            )
        }
    }

    @Test
    fun s04_checkout() {
        capture("doop_hotel_s04_checkout.png") {
            HotelCheckoutContent(
                stay = stay,
                loading = false,
                method = CheckoutMethod.CARD,
                busy = false,
                error = null,
                onMethod = {},
                onConfirm = {},
                onBack = null,
            )
        }
    }

    @Test
    fun recepcion() {
        val second = stay.copy(id = "S2", guestName = "Marcos Peña", roomName = "Habitación 112", checkIn = "2026-11-06", checkOut = "2026-11-08")
        val third = stay.copy(id = "S3", guestName = "Ana Torres", roomName = "Habitación 408", checkOut = "2026-11-11")
        val arriving = stay.copy(id = "S4", guestName = "Iván Soto", roomName = "Habitación 210", checkIn = "2026-11-09", checkOut = "2026-11-12", status = com.vaiinilla.app.domain.hotel.StayStatus.RESERVED)
        capture("doop_hotel_recepcion.png") {
            HotelReceptionContent(
                state = HotelReceptionUiState(stays = listOf(second, third, arriving), loaded = true),
                today = today,
                onCheckIn = {},
                onCheckout = {},
                onCharge = {},
                onBack = null,
            )
        }
    }

    @Test
    fun checkin() {
        capture("doop_hotel_checkin.png") {
            HotelCheckInContent(
                rooms = floor3.filter { !it.occupied },
                created = null,
                busy = false,
                error = null,
                today = today,
                onSubmit = {},
                onBack = {},
                initialName = "Laura Ramos",
                initialRate = "1650",
            )
        }
    }

    @Test
    fun checkin_codigo() {
        capture("doop_hotel_checkin_codigo.png") {
            HotelCheckInContent(rooms = emptyList(), created = stay, busy = false, error = null, today = today, onSubmit = {}, onBack = {})
        }
    }

    @Test
    fun cargo_manual() {
        capture("doop_hotel_cargo_manual.png") {
            HotelManualChargeContent(stay = stay, busy = false, error = null, onSave = { _, _, _ -> })
        }
    }
}
