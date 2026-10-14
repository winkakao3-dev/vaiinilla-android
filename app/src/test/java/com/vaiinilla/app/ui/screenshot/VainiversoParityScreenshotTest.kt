package com.vaiinilla.app.ui.screenshot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.core.qr.QrCodeEncoder
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderItem
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.ui.discovery.QrPayloadParser
import com.vaiinilla.app.ui.order.TrayItemKind
import com.vaiinilla.app.ui.screens.CheckoutStage
import com.vaiinilla.app.ui.screens.KitchenPeek
import com.vaiinilla.app.ui.screens.LocalScene
import com.vaiinilla.app.ui.screens.SceneMotion
import com.vaiinilla.app.ui.screens.drawStorefront
import com.vaiinilla.app.ui.screens.kitchenCamera
import com.vaiinilla.app.ui.screens.StoreEntrance
import com.vaiinilla.app.ui.screens.StoreEntranceOutcome
import com.vaiinilla.app.ui.screens.StoreEntranceOverlay
import com.vaiinilla.app.ui.screens.WalletCodeOverlay
import com.vaiinilla.app.ui.screens.jarFillLevel
import com.vaiinilla.app.ui.screens.scenePaletteForTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Las escenas del Vainiverso que llegaron de iOS, quietas en sus momentos clave, a cada hora del día. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class VainiversoParityScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun capture(
        name: String,
        mode: VaiinillaThemeMode = VaiinillaThemeMode.Light,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            ScreenshotTheme(mode) {
                CompositionLocalProvider(LocalScene provides scenePaletteForTheme(), content = content)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("vainiverso/$name.png")
    }

    private val walletMatrix = QrCodeEncoder.encode(QrPayloadParser.encodeUser("sandbox-agente-cliente"))

    @Composable
    private fun WalletCode(at: Float) {
        WalletCodeOverlay(
            matrix = walletMatrix,
            shown = true,
            fill = jarFillLevel("120.00"),
            label = "$120.00",
            onClose = {},
            frozenAt = at,
        )
    }

    @Test
    fun `wallet_code_flight_day`() = capture("wallet_code_flight_day") { WalletCode(0.42f) }

    @Test
    fun `wallet_code_day`() = capture("wallet_code_day") { WalletCode(1f) }

    @Test
    fun `wallet_code_night`() = capture("wallet_code_night", VaiinillaThemeMode.Dark) { WalletCode(1f) }

    @Composable
    private fun Entrance(at: Float) {
        StoreEntranceOverlay(
            entrance = StoreEntrance(code = "https://vaiinilla.app/e/sandbox-agentes"),
            outcome = StoreEntranceOutcome.WAITING,
            onFinished = {},
            frozenAt = at,
        )
    }

    @Test
    fun `entrance_qr_day`() = capture("entrance_1_qr_day") { Entrance(0.08f) }

    @Test
    fun `entrance_mosaic_day`() = capture("entrance_2_mosaic_day") { Entrance(0.3f) }

    @Test
    fun `entrance_storefront_day`() = capture("entrance_3_storefront_day") { Entrance(0.5f) }

    @Test
    fun `entrance_door_day`() = capture("entrance_4_door_day") { Entrance(0.7f) }

    @Test
    fun `entrance_mosaic_night`() = capture("entrance_2_mosaic_night", VaiinillaThemeMode.Dark) { Entrance(0.3f) }

    @Test
    fun `entrance_storefront_night`() = capture("entrance_3_storefront_night", VaiinillaThemeMode.Dark) { Entrance(0.5f) }

    private val checkoutOrder =
        OrderDetail(
            summary =
                OrderSummary(
                    id = "43",
                    folio = 43,
                    operationalDate = "2026-10-14",
                    state = OrderState.PENDING_PAYMENT,
                    paymentMethod = PaymentMethod.CASH,
                    destination = OrderDestination.TAKE_AWAY,
                    space = null,
                    subtotal = "145.00",
                    combinedSavings = "0.00",
                    cashbackAwarded = "0.00",
                    total = "145.00",
                    version = 1,
                    createdAt = "2026-10-14T00:00:00Z",
                    updatedAt = "2026-10-14T00:00:00Z",
                ),
            user = null,
            kitchenNotes = "",
            items =
                listOf(
                    "Latte de vainilla" to "55.00",
                    "Galleta de chocolate" to "30.00",
                    "Chilaquiles verdes con pollo" to "60.00",
                    "Agua mineral" to "0.00",
                ).mapIndexed { index, (name, price) ->
                    OrderItem(
                        id = index,
                        productId = index,
                        productName = name,
                        preparationStation = PreparationStation.KITCHEN,
                        quantity = 1,
                        unitDigitalPrice = price,
                        subtotal = price,
                        options = emptyList(),
                    )
                },
        )

    @Composable
    private fun Checkout(at: Float) {
        CheckoutStage(t = at, order = checkoutOrder, cardRect = null, reduceMotion = false)
    }

    @Test
    fun `checkout_sheet_day`() = capture("checkout_1_sheet_day") { Checkout(0.12f) }

    @Test
    fun `checkout_printing_day`() = capture("checkout_2_printing_day") { Checkout(0.45f) }

    @Test
    fun `checkout_printed_day`() = capture("checkout_3_printed_day") { Checkout(0.6f) }

    @Test
    fun `checkout_flight_day`() = capture("checkout_4_flight_day") { Checkout(0.8f) }

    @Test
    fun `checkout_printed_night`() = capture("checkout_3_printed_night", VaiinillaThemeMode.Dark) { Checkout(0.6f) }

    @Composable
    private fun Kitchen(
        peek: Float,
        lit: Float,
        item: TrayItemKind = TrayItemKind.DRINK,
    ) {
        val palette = LocalScene.current
        val textMeasurer = rememberTextMeasurer()
        Canvas(Modifier.fillMaxSize()) {
            val camera = kitchenCamera(peek, size)
            val kitchen = KitchenPeek(item = item, lit = lit, alpha = (peek / 0.25f).coerceAtMost(1f), steam = 0.3f)
            drawContext.transform.translate(camera.translation.x, camera.translation.y)
            drawContext.transform.scale(camera.zoom, camera.zoom, androidx.compose.ui.geometry.Offset.Zero)
            drawStorefront(size, textMeasurer, palette, SceneMotion(happy = lit > 0.5f, wave = if (lit > 0.5f) -112f else 0f), kitchen)
        }
    }

    @Test
    fun `kitchen_halfway_day`() = capture("kitchen_1_halfway_day") { Kitchen(0.5f, 0f) }

    @Test
    fun `kitchen_preparing_day`() = capture("kitchen_2_preparing_day") { Kitchen(1f, 0f) }

    @Test
    fun `kitchen_ready_day`() = capture("kitchen_3_ready_day") { Kitchen(1f, 1f, TrayItemKind.PLATE) }

    @Test
    fun `kitchen_preparing_night`() = capture("kitchen_2_preparing_night", VaiinillaThemeMode.Dark) { Kitchen(1f, 0f) }

    @Test
    fun `kitchen_ready_night`() = capture("kitchen_3_ready_night", VaiinillaThemeMode.Dark) { Kitchen(1f, 1f) }
}
