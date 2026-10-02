package com.vaiinilla.app.ui.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.screens.StudentTrackingScreen
import com.vaiinilla.app.ui.screens.itemImageUrls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** El seguimiento muestra la foto que trae el pedido aunque el catálogo no haya cargado. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class OrderPhotoScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun orderWithPhoto() =
        ScreenshotFixtures.sampleOrder(state = OrderState.PAID).let { base ->
            base.copy(items = base.items.map { it.copy(imageUrl = "fixture://burrito_norteno") })
        }

    @Test
    fun `the item photo wins without a catalog`() {
        assertEquals("fixture://burrito_norteno", itemImageUrls(orderWithPhoto(), null).first())
        assertNull(itemImageUrls(ScreenshotFixtures.sampleOrder(), null).first())
    }

    @Test
    fun tracking_photo_without_catalog() {
        val order = orderWithPhoto()
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            ScreenshotTheme {
                StudentTrackingScreen(
                    state = ScreenshotFixtures.trackingState(order, selected = true),
                    orderState = OrderFlowUiState(loading = false, catalog = null),
                    onMenu = {},
                    onAssistant = {},
                    onWallet = {},
                    onCart = {},
                    onOpenCatalog = {},
                    onSelectOrder = {},
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_500)
        composeTestRule.onRoot().captureRoboImage("tracking_photo_without_catalog.png")
    }
}
