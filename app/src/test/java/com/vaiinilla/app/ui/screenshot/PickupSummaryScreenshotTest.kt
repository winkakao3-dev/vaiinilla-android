package com.vaiinilla.app.ui.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderItemOption
import com.vaiinilla.app.domain.model.OrderItemRejection
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.ui.screens.PickupJourneyScreen
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** La hoja de seguimiento con un pedido largo: todo el pedido, con opciones, nota y un producto quitado. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class PickupSummaryScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun item(
        id: Int,
        name: String,
        quantity: Int,
        subtotal: String,
        options: List<String> = emptyList(),
        notes: String = "",
        rejectedFor: String? = null,
    ) = OrderItem(
        id = id,
        productId = id,
        productName = name,
        preparationStation = PreparationStation.entries.first(),
        quantity = quantity,
        unitDigitalPrice = subtotal,
        subtotal = subtotal,
        options = options.map { OrderItemOption(optionId = id, name = it, extraPrice = "0.00") },
        notes = notes,
        rejection = rejectedFor?.let { OrderItemRejection(reason = it, amount = subtotal) },
    )

    private val longOrder =
        ScreenshotFixtures
            .sampleOrder(state = OrderState.DELIVERED, destination = OrderDestination.IN_SPACE, spaceId = 2)
            .let { order ->
                order.copy(
                    items =
                        listOf(
                            item(1, "Churros con chocolate", 2, "130.00", options = listOf("Chocolate amargo")),
                            item(2, "Donas decoradas", 3, "105.00", notes = "Una sin chispas"),
                            item(3, "Frappé de Oreo", 1, "55.00", options = listOf("Grande", "Leche de avena")),
                            item(4, "Agua", 2, "40.00"),
                            item(5, "Molcajete de camarón", 1, "180.00", rejectedFor = "Se terminó el camarón"),
                            item(6, "Refresco", 1, "25.00"),
                        ),
                    summary = order.summary.copy(total = "355.00"),
                )
            }

    private fun capture(
        name: String,
        mode: VaiinillaThemeMode,
    ) {
        composeTestRule.setContent {
            ScreenshotTheme(mode) {
                PickupJourneyScreen(order = longOrder, qrPayload = null, onBack = {}, onViewTracking = {})
            }
        }
        composeTestRule.waitForIdle()
        // La hoja entra con resorte y la escena tiene vida pequeña: se adelanta hasta que todo asienta.
        composeTestRule.mainClock.advanceTimeBy(3_000)
        composeTestRule.onRoot().captureRoboImage("pickup/$name.png")
    }

    @Test
    fun `pickup_summary_long_order_night`() = capture("pickup_summary_long_order_night", VaiinillaThemeMode.Dark)

    @Test
    fun `pickup_summary_long_order_day`() = capture("pickup_summary_long_order_day", VaiinillaThemeMode.Light)
}
