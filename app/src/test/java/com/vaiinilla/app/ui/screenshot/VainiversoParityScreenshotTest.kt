package com.vaiinilla.app.ui.screenshot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.vaiinilla.app.ui.screens.CourtRental
import com.vaiinilla.app.ui.screens.FloorPlate
import com.vaiinilla.app.ui.screens.FloorTable
import com.vaiinilla.app.ui.screens.KitchenPeek
import com.vaiinilla.app.ui.screens.LocalScene
import com.vaiinilla.app.ui.screens.SceneMotion
import com.vaiinilla.app.ui.screens.StoreEntrance
import com.vaiinilla.app.ui.screens.StoreEntranceOutcome
import com.vaiinilla.app.ui.screens.StoreEntranceOverlay
import com.vaiinilla.app.ui.screens.WalletCodeOverlay
import com.vaiinilla.app.ui.screens.drawCourt
import com.vaiinilla.app.ui.screens.drawFloorPlan
import com.vaiinilla.app.ui.screens.drawStorefront
import com.vaiinilla.app.ui.screens.jarFillLevel
import com.vaiinilla.app.ui.screens.kitchenCamera
import com.vaiinilla.app.ui.screens.scenePaletteForTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

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
    fun `entrance_storefront_night`() =
        capture("entrance_3_storefront_night", VaiinillaThemeMode.Dark) { Entrance(0.5f) }

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
            drawStorefront(
                size,
                textMeasurer,
                palette,
                SceneMotion(
                    happy = lit > 0.5f,
                    wave =
                        if (lit >
                            0.5f
                        ) {
                            -112f
                        } else {
                            0f
                        },
                ),
                kitchen,
            )
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

    private val mesa4 = FloorTable(index = 3, name = "Mesa 4")

    @Composable
    private fun Floor(plate: FloorPlate?) {
        val palette = LocalScene.current
        Canvas(Modifier.fillMaxSize()) {
            drawFloorPlan(size, palette, mesa4, tick = 1, cheering = plate != null, plate = plate)
        }
    }

    @Test
    fun `floor_plan_day`() = capture("floor_1_plan_day") { Floor(null) }

    @Test
    fun `floor_served_day`() = capture("floor_2_served_day") { Floor(FloorPlate(1f, 0f)) }

    @Test
    fun `floor_flying_day`() = capture("floor_3_flying_day") { Floor(FloorPlate(1f, 0.5f)) }

    @Test
    fun `floor_delivered_night`() =
        capture("floor_4_delivered_night", VaiinillaThemeMode.Dark) {
            Floor(FloorPlate(1f, 1f))
        }

    @Test
    fun `floor_plan_midnight`() = capture("floor_5_plan_midnight", VaiinillaThemeMode.Amoled) { Floor(null) }

    @Composable
    private fun TableEntrance(at: Float) {
        StoreEntranceOverlay(
            entrance = StoreEntrance(code = "https://vaiinilla.app/m/sandbox-mesa-4", table = mesa4),
            outcome = StoreEntranceOutcome.WAITING,
            onFinished = {},
            frozenAt = at,
        )
    }

    @Test
    fun `table_entrance_mosaic_day`() = capture("table_entrance_1_mosaic_day") { TableEntrance(0.3f) }

    @Test
    fun `table_entrance_plan_day`() = capture("table_entrance_2_plan_day") { TableEntrance(0.5f) }

    @Test
    fun `table_entrance_descend_day`() = capture("table_entrance_3_descend_day") { TableEntrance(0.7f) }

    private val court =
        CourtRental("Cancha 2", Instant.parse("2026-10-14T18:00:00Z"), Instant.parse("2026-10-14T19:00:00Z"))

    @Composable
    private fun Court(
        minute: Long,
        tick: Int = 5,
    ) {
        val palette = LocalScene.current
        Canvas(Modifier.fillMaxSize()) { drawCourt(size, palette, court, court.start.plusSeconds(minute * 60), tick) }
    }

    @Test
    fun `court_booked_day`() = capture("court_1_booked_day") { Court(-20) }

    @Test
    fun `court_playing_day`() = capture("court_2_playing_day") { Court(25) }

    @Test
    fun `court_last_minutes_day`() = capture("court_3_last_minutes_day") { Court(57) }

    @Test
    fun `court_playing_night`() = capture("court_2_playing_night", VaiinillaThemeMode.Dark) { Court(40, tick = 21) }

    @Test
    fun `court_done_midnight`() = capture("court_4_done_midnight", VaiinillaThemeMode.Amoled) { Court(65) }
}
