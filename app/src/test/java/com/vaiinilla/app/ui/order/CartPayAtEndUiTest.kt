package com.vaiinilla.app.ui.order

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.ui.screens.CartScreen
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** C01, C02, C06: el carrito de una mesa ofrece "Pagar al final" aunque el estado llegue después de entrar. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CartPayAtEndUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val status =
        OperationalStatus(
            acceptingOrders = true,
            cashSessionOpen = true,
            cashierOnline = true,
            kitchenOnline = true,
            estimatedTimeMinutes = 10,
            consultedAt = "2026-10-23T20:00:00.000Z",
            allowsPayAtEnd = true,
        )

    @Test
    fun `tapping pay asks for the status again and offers pay at end once it arrives`() {
        val state =
            mutableStateOf(
                ScreenshotFixtures.cartState(destination = OrderDestination.IN_SPACE).copy(operationalStatus = null),
            )
        var refreshes = 0
        composeTestRule.setContent {
            ScreenshotTheme {
                CartScreen(
                    state = state.value,
                    onMenu = {},
                    onQuantityChange = { _, _ -> },
                    onNotesChange = {},
                    onDestinationChange = {},
                    onPaymentChange = {},
                    onConfirm = {},
                    onCheckoutOpened = { refreshes++ },
                )
            }
        }

        composeTestRule.onNodeWithText("Pagar").performClick()
        composeTestRule.onNodeWithText("¿Cómo quieres pagar?").assertIsDisplayed()
        assertEquals(1, refreshes)
        composeTestRule.onAllNodesWithText("Pagar al final").assertCountEquals(0)

        state.value = state.value.copy(operationalStatus = status)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Pagar al final").assertIsDisplayed()
    }
}
