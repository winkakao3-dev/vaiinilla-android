package com.vaiinilla.app.ui.screenshot

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.screens.CartScreen
import com.vaiinilla.app.ui.screens.FloorLogo
import com.vaiinilla.app.ui.screens.LocalVenueArt
import com.vaiinilla.app.ui.screens.PickupJourneyScreen
import com.vaiinilla.app.ui.screens.StudentTrackingScreen
import com.vaiinilla.app.ui.screens.VenueArt
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Las tres escenas del cliente con el logo y la foto de un negocio de prueba. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class VenueArtScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun image(name: String): ImageBitmap =
        javaClass.classLoader!!
            .getResourceAsStream("venue/$name")!!
            .use { BitmapFactory.decodeStream(it) }
            .asImageBitmap()

    private val art by lazy {
        val logo = image("logo.png")
        VenueArt(logo = logo, photo = image("foto.jpg"), floorLogo = FloorLogo.of("prueba", logo))
    }

    private fun shot(
        name: String,
        mode: VaiinillaThemeMode = VaiinillaThemeMode.Light,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            ScreenshotTheme(mode) { CompositionLocalProvider(LocalVenueArt provides art) { content() } }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(3_000)
        composeTestRule.onRoot().captureRoboImage("venue/$name.png")
    }

    @Test
    fun carrito() =
        shot("carrito") {
            ScreenshotWithStudentNav(activeTab = StudentTab.CART, cartCount = 1) {
                CartScreen(
                    state = ScreenshotFixtures.cartState(destination = OrderDestination.IN_SPACE, spaceId = 2),
                    onMenu = {},
                    onQuantityChange = { _, _ -> },
                    onNotesChange = {},
                    onDestinationChange = {},
                    onPaymentChange = {},
                    onConfirm = {},
                )
            }
        }

    private fun tableOrder(state: OrderState) =
        ScreenshotFixtures.sampleOrder(state = state, destination = OrderDestination.IN_SPACE, spaceId = 2)

    @Test
    fun seguimiento() =
        shot("seguimiento") {
            PickupJourneyScreen(
                order = tableOrder(OrderState.DELIVERED),
                qrPayload = null,
                onBack = {},
                onViewTracking = {},
            )
        }

    @Test
    fun seguimiento_noche() =
        shot("seguimiento_noche", VaiinillaThemeMode.Dark) {
            PickupJourneyScreen(
                order = tableOrder(OrderState.PREPARING),
                qrPayload = null,
                onBack = {},
                onViewTracking = {},
            )
        }

    @Test
    fun mis_pedidos() =
        shot("mis_pedidos") {
            ScreenshotWithStudentNav(activeTab = StudentTab.ORDERS, cartCount = 0) {
                StudentTrackingScreen(
                    state = ScreenshotFixtures.trackingState(tableOrder(OrderState.PREPARING), selected = false),
                    orderState = ScreenshotFixtures.catalogLoadedState(),
                    onMenu = {},
                    onAssistant = {},
                    onWallet = {},
                    onCart = {},
                    onOpenCatalog = {},
                    onSelectOrder = {},
                )
            }
        }
}
