package com.vaiinilla.app.ui.hotel.guest

import com.vaiinilla.app.domain.model.VenueContext
import androidx.compose.runtime.CompositionLocalProvider
import com.vaiinilla.app.core.venue.LocalVenueContext
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.vaiinilla.app.domain.hotel.HotelConfig
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.PublicSpace
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.screens.CartScreen
import com.vaiinilla.app.ui.screens.CatalogScreen
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** H02, H03 y H04 de Doop con las pantallas reales del pedido, en oscuro y a 390 x 844. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w390dp-h844dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class HotelRoomOrderScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val hotel = VenueContext.of("hotel")

    private fun dark(content: @Composable () -> Unit) =
        composeTestRule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
                CompositionLocalProvider(LocalVenueContext provides hotel, content = content)
            }
        }

    private fun settle(ms: Long = 1_000) {
        composeTestRule.mainClock.advanceTimeBy(ms)
        composeTestRule.waitForIdle()
    }

    private fun roomState(): OrderFlowUiState {
        val base = ScreenshotFixtures.catalogLoadedState().copy(venue = hotel)
        val products = base.catalog!!.products
        return base.copy(
            guestVenue =
                GuestVenueContext(
                    establishment =
                        PublicEstablishment(
                            id = "e1",
                            name = "Hotel Mirador del Valle",
                            slug = "mirador",
                            clientIdLabel = "",
                            clientIdRequired = false,
                        ),
                    space = PublicSpace(id = 304, name = "Habitación 304", type = "mesa"),
                ),
            checkoutDestination = OrderDestination.IN_SPACE,
            hotelConfig = HotelConfig(chargeToRoom = true, cardAtOrderEnabled = true),
            cartLines =
                listOf(
                    CartLine(products[0], 1, emptySet()),
                    CartLine(products[1], 1, emptySet(), notes = "sin cebolla"),
                ),
        )
    }

    @Test
    fun doop_hotel_h02() {
        val state = roomState()
        dark {
            CatalogScreen(
                state = state,
                onRetry = {},
                onSearchChange = {},
                onCategorySelected = {},
                onProductSelected = {},
                onDismissProduct = {},
                onToggleOption = { _, _ -> },
                onClearOptionalGroup = {},
                onQuantityChange = {},
                onAddProduct = {},
                onOpenCart = {},
            )
        }
        settle()
        composeTestRule.onNodeWithText("Room service").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hotel_cart_bar").assertIsDisplayed()
        captureScreenRoboImage("hotel/doop_hotel_h02.png")
    }

    @Test
    fun doop_hotel_h03() {
        val state = roomState()
        dark {
            CartScreen(
                state = state,
                onMenu = {},
                onQuantityChange = { _, _ -> },
                onNotesChange = {},
                onDestinationChange = {},
                onPaymentChange = {},
                onConfirm = {},
            )
        }
        settle()
        composeTestRule.onNodeWithText("Entrega a tu cuarto").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cargar a la habitación 304").assertIsDisplayed()
        captureScreenRoboImage("hotel/doop_hotel_h03.png")
    }

    @Test
    fun pedir_carga_a_la_habitacion_y_confirma() {
        val state = roomState()
        val calls = mutableListOf<String>()
        dark {
            CartScreen(
                state = state,
                onMenu = {},
                onQuantityChange = { _, _ -> },
                onNotesChange = {},
                onDestinationChange = {},
                onPaymentChange = { calls += "pago:$it" },
                onPayAtEnd = { calls += "cuenta" },
                onConfirm = { calls += "confirmar" },
            )
        }
        settle()
        composeTestRule.onNodeWithTag("hotel_checkout_confirm").performClick()
        assertEquals(listOf("cuenta", "confirmar"), calls)

        calls.clear()
        composeTestRule.onNodeWithTag("hotel_pay_card").performClick()
        assertEquals(listOf("pago:${PaymentMethod.STRIPE}"), calls)
    }

    @Test
    fun doop_hotel_h04() {
        val base =
            ScreenshotFixtures.sampleOrder(
                state = OrderState.PREPARING,
                destination = OrderDestination.IN_SPACE,
                spaceId = 304,
            )
        val order =
            base.copy(
                summary =
                    base.summary.copy(
                        folio = 412,
                        space = OrderSpace(id = 304, name = "Habitación 304", type = "mesa"),
                        payAtEnd = true,
                        total = "236.50",
                    ),
            )
        dark { HotelTrackingContent(order = order, onCallReception = {}) }
        settle()
        composeTestRule.onNodeWithText("Pedido #412 · Habitación 304").assertIsDisplayed()
        composeTestRule.onNodeWithText("Llamar a recepción").assertIsDisplayed()
        captureScreenRoboImage("hotel/doop_hotel_h04.png")
    }
}
