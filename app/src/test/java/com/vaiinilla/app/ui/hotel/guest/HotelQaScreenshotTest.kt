package com.vaiinilla.app.ui.hotel.guest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.vaiinilla.app.core.venue.LocalVenueContext
import com.vaiinilla.app.domain.hotel.HotelConfig
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.PublicSpace
import com.vaiinilla.app.domain.model.VenueContext
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.ui.components.MenuWaiterLayer
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.VaiinillaBottomNav
import com.vaiinilla.app.ui.hotel.staff.UndoReady
import com.vaiinilla.app.ui.screens.CashierNewOrderSheet
import com.vaiinilla.app.ui.screens.CashierQuickActions
import com.vaiinilla.app.ui.screens.CatalogScreen
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Lo que Kao vio en el OnePlus el 7 nov, con la barra de pestañas encima como en la app, a letra normal y grande. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp-normal-long-notround-any-xxxhdpi", sdk = [33])
class HotelQaScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val hotel = VenueContext.of("hotel")

    private fun dark(
        fontScale: Float,
        content: @Composable () -> Unit,
    ) = composeTestRule.setContent {
        ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalVenueContext provides hotel,
                LocalDensity provides Density(density.density, fontScale),
            ) {
                Box(Modifier.fillMaxSize().background(LocalVaiinillaColors.current.paper)) { content() }
            }
        }
    }

    private fun settle() {
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.waitForIdle()
    }

    private fun nav(cart: Int) =
        @Composable {
            Box(Modifier.fillMaxSize()) {
                VaiinillaBottomNav(
                    activeTab = StudentTab.MENU,
                    cartCount = cart,
                    onTabSelected = {},
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

    private fun roomMenu(
        withCart: Boolean,
        fontScale: Float,
    ) {
        val base = ScreenshotFixtures.catalogLoadedState().copy(venue = hotel)
        val products = base.catalog!!.products
        val state =
            base.copy(
                guestVenue =
                    GuestVenueContext(
                        PublicEstablishment("e1", "Gran Hotel Vaini", "gran-hotel", "", false),
                        PublicSpace(id = 103, name = "Habitación 103", type = "mesa"),
                    ),
                checkoutDestination = OrderDestination.IN_SPACE,
                hotelConfig = HotelConfig(chargeToRoom = true),
                cartLines = if (withCart) listOf(CartLine(products[0], 1, emptySet())) else emptyList(),
            )
        dark(fontScale) {
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
                waiterLayer = { cart, visible ->
                    MenuWaiterLayer(
                        spaceId = 103,
                        visible = visible,
                        cart = cart,
                        onCurrent = { Result.success(null) },
                        onCall = { _, _, _ -> Result.failure(IllegalStateException()) },
                        onCancel = { Result.failure(IllegalStateException()) },
                        onJoinTable = null,
                        place = "la Habitación 103",
                        onOpenAssistant = {},
                    )
                },
            )
            nav(if (withCart) 1 else 0)()
        }
        settle()
    }

    private fun cartBarAboveNav(scale: Float) {
        roomMenu(withCart = true, fontScale = scale)
        val bar = composeTestRule.onNodeWithTag("hotel_cart_bar").fetchSemanticsNode().boundsInRoot
        val navTop = composeTestRule.onNodeWithText("Menú").fetchSemanticsNode().boundsInRoot.top
        assertTrue("la barra (${bar.bottom}) tapa la barra de pestañas ($navTop)", bar.bottom < navTop)
        captureScreenRoboImage("hotel/hotelqa_menu_con_pedido_$scale.png")
    }

    @Test
    fun menu_con_pedido_la_barra_queda_arriba_de_las_pestanas() = cartBarAboveNav(1f)

    @Test
    fun menu_con_pedido_y_letra_grande_la_barra_queda_arriba() = cartBarAboveNav(1.3f)

    @Test
    fun menu_sin_pedido_recepcion_y_asistente_caben() {
        roomMenu(withCart = false, fontScale = 1.3f)
        captureScreenRoboImage("hotel/hotelqa_menu_sin_pedido_1.3.png")
        val pill = composeTestRule.onNodeWithContentDescription("Llamar a recepción")
        pill.assertIsDisplayed()
        composeTestRule.onNodeWithText("Pregúntame qué pedir").assertIsDisplayed()
        pill.performClick()
        settle()
        composeTestRule.onNodeWithText("La cuenta").assertDoesNotExist()
        captureScreenRoboImage("hotel/hotelqa_hoja_recepcion.png")
    }

    @Test
    fun cocina_deshacer_cabe_junto_a_lista() {
        dark(1.3f) {
            UndoReady(
                deadline = System.currentTimeMillis() + 5_000,
                onUndo = {},
                modifier = Modifier.padding(16.dp),
            )
        }
        settle()
        composeTestRule.onNodeWithText("Marcada como lista").assertIsDisplayed()
        composeTestRule.onNodeWithText("Deshacer", substring = true).assertIsDisplayed()
        captureScreenRoboImage("hotel/hotelqa_cocina_deshacer_1.3.png")
    }

    @Test
    fun caja_nuevo_pedido_con_habitaciones_no_se_mueve() {
        val rooms =
            (1..12).map { TableSpace(100 + it, "Habitación ${100 + it}", "mesa") } +
                listOf(TableSpace(1, "Mesa 1", "mesa"), TableSpace(2, "Barra", "barra"))
        dark(1f) {
            CashierNewOrderSheet(
                tables = rooms,
                tablesLoading = false,
                tablesError = null,
                colors = rememberOperationalColors(),
                onDismiss = {},
                onTakeAway = {},
                onTable = {},
                onRefreshTables = {},
            )
        }
        settle()
        composeTestRule.onNodeWithText("O a una habitación o mesa:").assertIsDisplayed()
        val before = composeTestRule.onNodeWithText("Nuevo pedido").fetchSemanticsNode().boundsInRoot.top
        composeTestRule.mainClock.advanceTimeBy(3_000)
        composeTestRule.waitForIdle()
        val after = composeTestRule.onNodeWithText("Nuevo pedido").fetchSemanticsNode().boundsInRoot.top
        assertEquals("la hoja se movió sola", before, after, 0.5f)
        captureScreenRoboImage("hotel/hotelqa_caja_nuevo_pedido.png")
    }

    @Test
    fun caja_recepcion_es_un_boton() {
        dark(1.3f) {
            CashierQuickActions(
                colors = rememberOperationalColors(),
                enabled = true,
                onNewOrder = {},
                onReload = {},
                onSpaces = {},
                onReception = {},
                modifier = Modifier.padding(16.dp),
            )
        }
        settle()
        composeTestRule.onNodeWithText("Recepción · llegadas y salidas").assertIsDisplayed()
        captureScreenRoboImage("hotel/hotelqa_caja_recepcion_1.3.png")
    }
}