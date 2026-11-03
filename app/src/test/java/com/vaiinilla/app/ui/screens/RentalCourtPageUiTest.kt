package com.vaiinilla.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderReservation
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.temporal.ChronoUnit

/** La cancha en juego (Doop L09): la tarjeta del menú abre el menú y «Mi código» enseña el QR personal. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi", sdk = [33])
class RentalCourtPageUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun playing(): OrderDetail {
        val base = ScreenshotFixtures.sampleOrder(state = OrderState.DELIVERED, paymentMethod = PaymentMethod.CASH)
        val start = Instant.now().minus(20, ChronoUnit.MINUTES)
        return base.copy(
            items = base.items.take(1).map { it.copy(productName = "Renta Cancha 1 · 60 min", quantity = 1) },
            reservation =
                OrderReservation(
                    id = "r-1",
                    space = OrderSpace(id = 7, name = "Cancha 1", type = "cancha"),
                    start = start,
                    end = start.plus(60, ChronoUnit.MINUTES),
                    durationMinutes = 60,
                    state = ReservationState.IN_PROGRESS,
                ),
        )
    }

    @Test
    fun `la tarjeta del menu abre el menu y mi codigo abre el QR`() {
        var menuOpened = 0
        composeTestRule.setContent {
            ScreenshotTheme {
                RentalCourtPage(
                    order = playing(),
                    catalog = ScreenshotFixtures.catalog(),
                    userId = "u-1",
                    onOpenMenu = { menuOpened++ },
                    onViewRental = {},
                    onCurrentWaiterCall = null,
                    onCallWaiter = null,
                    onCancelWaiter = null,
                )
            }
        }

        composeTestRule.onNodeWithTag(RENTAL_COURT_MENU_TAG).performScrollTo().performClick()
        assertEquals(1, menuOpened)

        composeTestRule.onNodeWithTag(RENTAL_COURT_CODE_TAG).performScrollTo().performClick()
        composeTestRule.onNodeWithText("Muéstralo en Caja para que encuentren tu cuenta.").assertIsDisplayed()
    }

    @Test
    fun `sin usuario no se ofrece mi codigo`() {
        composeTestRule.setContent {
            ScreenshotTheme {
                RentalCourtPage(
                    order = playing(),
                    catalog = null,
                    userId = null,
                    onOpenMenu = {},
                    onViewRental = {},
                    onCurrentWaiterCall = null,
                    onCallWaiter = null,
                    onCancelWaiter = null,
                )
            }
        }

        composeTestRule.onNodeWithTag(RENTAL_COURT_CODE_TAG).assertDoesNotExist()
    }
}