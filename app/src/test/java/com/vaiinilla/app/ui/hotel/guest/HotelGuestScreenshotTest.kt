package com.vaiinilla.app.ui.hotel.guest

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.vaiinilla.app.domain.hotel.ChargeCategory
import com.vaiinilla.app.domain.hotel.HotelConfig
import com.vaiinilla.app.domain.hotel.RequestType
import com.vaiinilla.app.domain.hotel.Stay
import com.vaiinilla.app.domain.hotel.StayCharge
import com.vaiinilla.app.domain.hotel.StayOrder
import com.vaiinilla.app.domain.hotel.StayStatus
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Las pantallas del huésped del hotel contra los marcos H01, H05 y H06 de Doop (oscuro, 390 x 844). Solo captura:
 * se mira junto al \`image_url\` del marco. Sin foto del hotel el encabezado de H01 queda en papel.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w390dp-h844dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class HotelGuestScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun dark(content: @Composable () -> Unit) =
        composeTestRule.setContent { ScreenshotTheme(mode = VaiinillaThemeMode.Dark, content = content) }

    private fun settle(ms: Long = 1_000) {
        composeTestRule.mainClock.advanceTimeBy(ms)
        composeTestRule.waitForIdle()
    }

    private val laura =
        Stay(
            id = "S1",
            guestName = "Laura Méndez",
            code = "482916",
            roomId = 304,
            roomName = "Habitación 304",
            floor = 3,
            checkIn = "2026-11-06",
            checkOut = "2026-11-09",
            nightlyRate = "1650.00",
            status = StayStatus.IN_HOUSE,
            nights = 3,
            orders =
                listOf(
                    StayOrder(
                        "P1",
                        "A-412",
                        "236.50",
                        "entregado",
                        "2026-11-06T14:02:00Z",
                        itemsSummary = "Chilaquiles y café",
                    ),
                    StayOrder(
                        "P0",
                        "A-398",
                        "548.00",
                        "entregado",
                        "2026-11-05T03:10:00Z",
                        itemsSummary = "Cena · pasta y vino",
                    ),
                ),
            charges =
                listOf(
                    StayCharge("C1", "Minibar", "90.00", ChargeCategory.MINIBAR, "2026-11-05T20:00:00Z"),
                    StayCharge("C2", "Spa · masaje 60 min", "310.00", ChargeCategory.SPA, "2026-11-05T23:00:00Z"),
                ),
        )

    @Test
    fun doop_hotel_h01() {
        dark {
            HotelWelcomeContent(
                venueName = "Hotel Mirador del Valle",
                photoUrl = null,
                stay = laura,
                config = HotelConfig(lateCheckoutTime = "14:00"),
                onOrder = {},
                onRequests = {},
                onAccount = {},
            )
        }
        settle()
        captureScreenRoboImage("hotel/doop_hotel_h01.png")
    }

    @Test
    fun doop_hotel_h05() {
        dark {
            HotelRequestsContent(
                roomName = "Habitación 304",
                state =
                    HotelRequestsUiState(
                        picked = mapOf(RequestType.TOWELS to 2, RequestType.CLEANING to 1),
                        doNotDisturb = true,
                        config = HotelConfig(lateCheckoutTime = "14:00", lateCheckoutPrice = "350.00"),
                    ),
                onTap = {},
                onToggleDoNotDisturb = {},
                onSubmit = {},
                onCancel = {},
            )
        }
        settle()
        captureScreenRoboImage("hotel/doop_hotel_h05.png")
    }

    @Test
    fun doop_hotel_h06() {
        dark { HotelAccountContent(stay = laura) }
        settle()
        captureScreenRoboImage("hotel/doop_hotel_h06.png")
    }

    @Test
    fun doop_hotel_codigo() {
        dark {
            HotelStayCodeContent(
                linking = false,
                error = null,
                onSubmit = {},
                initialCode = "4829",
                autoFocus = false,
            )
        }
        settle()
        captureScreenRoboImage("hotel/doop_hotel_codigo.png")
    }

    @Test
    fun doop_hotel_codigo_error() {
        dark {
            HotelStayCodeContent(
                linking = false,
                error = "Ese código no coincide. Revísalo con recepción.",
                onSubmit = {},
                initialCode = "482917",
                autoFocus = false,
            )
        }
        settle()
        captureScreenRoboImage("hotel/doop_hotel_codigo_error.png")
    }
}
