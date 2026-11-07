package com.vaiinilla.app.ui.hotel.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.vaiinilla.app.domain.hotel.HotelConfig
import com.vaiinilla.app.domain.hotel.HotelDeparture
import com.vaiinilla.app.domain.hotel.HotelRoom
import com.vaiinilla.app.domain.hotel.HotelSummary
import com.vaiinilla.app.domain.hotel.HotelTopItem
import com.vaiinilla.app.domain.hotel.RoomCleaning
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.admin.AdminDialogHost
import com.vaiinilla.app.ui.screens.admin.AdminHeroPage
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val SampleSummary =
    HotelSummary(
        occupied = 17,
        totalRooms = 24,
        roomServiceToday = "8420.00",
        roomOrdersToday = 23,
        inKitchenNow = 3,
        openRequests = 4,
        oldestRequestMinutes = 12,
        topItems =
            listOf(
                HotelTopItem("Chilaquiles verdes", 48),
                HotelTopItem("Hamburguesa de la casa", 41),
                HotelTopItem("Café americano", 39),
            ),
        departuresToday = 3,
        departures =
            listOf(
                HotelDeparture("s1", "Laura Ramos", "Habitación 304"),
                HotelDeparture("s2", "Iván Soto", "Habitación 112"),
                HotelDeparture("s3", "Marta Ibarra", "Habitación 408"),
            ),
    )

private fun room(
    number: Int,
    category: String,
    occupied: Boolean = false,
) = HotelRoom(
    id = number,
    name = "Habitación $number",
    floor = 3,
    category = category,
    cleaning = RoomCleaning.READY,
    occupied = occupied,
    qrUrl = "https://vaiinilla.app/q/h$number",
)

/** Las pantallas del admin del hotel dibujadas con datos armados para compararlas contra los marcos A01 y A03 de Doop. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp-normal-long-notround-any-xxhdpi", sdk = [33])
class HotelAdminDoopScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    private fun page(content: @Composable (OperationalColors) -> Unit) {
        rule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
                val colors = rememberOperationalColors()
                AdminDialogHost(
                    colors,
                ) { Box(Modifier.fillMaxSize().background(colors.background)) { content(colors) } }
            }
        }
        rule.mainClock.advanceTimeBy(1_500)
        rule.waitForIdle()
    }

    @Test fun a01_habitaciones() {
        page { colors ->
            HotelRoomsAdminPage(
                state =
                    HotelRoomsUiState(
                        rooms =
                            listOf(
                                room(301, "doble"),
                                room(302, "doble"),
                                room(303, "suite"),
                                room(304, "suite", occupied = true),
                            ),
                    ),
                draft = RoomBatchDraft(from = "301", to = "324", floor = "3", category = "Doble"),
                colors = colors,
                readOnly = false,
                onDraft = {},
                onCreate = {},
                onOpenRoom = {},
                onPrintAll = {},
                onRetry = {},
                onBack = {},
            )
        }
        captureScreenRoboImage("doop_hotel/doop_hotel_a01.png")
    }

    @Test fun a03_inicio() {
        page { colors ->
            AdminHeroPage(
                cover = null,
                caption = "Administración · Hotel Mirador del Valle",
                title = "Mi negocio",
                colors = colors,
                compactTitle = "Hotel Mirador del Valle",
            ) { hotelSummaryItems(HotelSummaryUiState(summary = SampleSummary), colors) {} }
        }
        captureScreenRoboImage("doop_hotel/doop_hotel_a03.png")
    }

    @Test fun a03_sin_solicitudes_ni_pedidos() {
        page { colors ->
            AdminHeroPage(
                cover = null,
                caption = "Administración · Hotel Mirador del Valle",
                title = "Mi negocio",
                colors = colors,
                compactTitle = "Hotel Mirador del Valle",
            ) {
                hotelSummaryItems(
                    HotelSummaryUiState(
                        summary = HotelSummary(occupied = 0, totalRooms = 12, roomServiceToday = null),
                    ),
                    colors,
                ) {}
            }
        }
        captureScreenRoboImage("doop_hotel/doop_hotel_a03_vacio.png")
    }

    @Test fun reglas_de_las_horas_y_del_precio() {
        assertTrue(isClock("07:00"))
        assertTrue(isClock("7:00"))
        assertFalse(isClock("24:00"))
        assertFalse(isClock("12:60"))
        assertFalse(isClock(""))
        assertEquals("", priceInput("abc"))
        assertEquals("250.5", priceInput("250.5"))
        assertEquals("250.55", priceInput("250.555"))
        assertEquals("12.3", priceInput("12..3"))
        assertEquals("07:0", clockInput("07:0x"))
        assertTrue(settingsValid(HotelConfig()))
        assertFalse(settingsValid(HotelConfig(kitchenFrom = "07:00")))
        assertFalse(settingsValid(HotelConfig(checkoutTime = "25:00")))
        assertEquals("Sin horario propio", kitchenLabel(HotelConfig()))
        assertEquals("07:00 a 23:00", kitchenLabel(HotelConfig(kitchenFrom = "07:00", kitchenTo = "23:00")))
    }

    @Test fun salidas_de_hoy_nombran_las_habitaciones() {
        assertEquals("304, 112 y 408 hacen check-out", departuresDetail(SampleSummary))
        assertEquals("Una habitación hace check-out", departuresDetail(HotelSummary(1, 2, departuresToday = 1)))
        assertEquals(
            "304 hace check-out",
            departuresDetail(HotelSummary(1, 2, departures = listOf(HotelDeparture("s", "A", "Habitación 304")))),
        )
    }

    @Test fun el_lote_cuenta_las_habitaciones() {
        assertEquals(24, RoomBatchDraft(from = "301", to = "324").count)
        assertEquals(1, RoomBatchDraft(from = "5", to = "5").count)
        assertEquals(0, RoomBatchDraft(from = "10", to = "5").count)
        assertEquals(0, RoomBatchDraft().count)
        assertEquals("Crear 24 habitaciones con QR", createLabel(24))
        assertEquals("Crear 1 habitación con QR", createLabel(1))
        assertEquals("Piso 3 · Doble · QR activo", roomDetail(room(301, "doble")))
        assertEquals("Piso 3 · Suite · ocupada", roomDetail(room(304, "suite", occupied = true)))
        assertTrue("Habitación 2".naturalKey() < "Habitación 10".naturalKey())
    }
}

/** A02 es más alta que una pantalla: el marco del lienzo mide 980. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h980dp-normal-long-notround-any-xxhdpi", sdk = [33])
class HotelSettingsDoopScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    @Test fun a02_plantilla_hotel() {
        rule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
                val colors = rememberOperationalColors()
                Box(Modifier.fillMaxSize().background(colors.background)) {
                    HotelSettingsPage(
                        saved =
                            HotelConfig(
                                chargeToRoom = true,
                                cardAtOrderEnabled = true,
                                deliveryCode = false,
                                kitchenFrom = "07:00",
                                kitchenTo = "23:00",
                                checkoutTime = "12:00",
                                lateCheckoutTime = "15:00",
                                lateCheckoutPrice = "250.00",
                            ),
                        colors = colors,
                        readOnly = false,
                        saving = false,
                        notice = null,
                        noticeIsError = false,
                        onSave = {},
                        onBack = {},
                    )
                }
            }
        }
        rule.mainClock.advanceTimeBy(1_500)
        rule.waitForIdle()
        captureScreenRoboImage("doop_hotel/doop_hotel_a02.png")
    }
}
