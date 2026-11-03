package com.vaiinilla.app.ui.screenshot

import android.graphics.BitmapFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.model.DeliveryAddress
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderHomeDelivery
import com.vaiinilla.app.domain.model.OrderItemOption
import com.vaiinilla.app.domain.model.OrderItemRejection
import com.vaiinilla.app.domain.model.OrderSpace
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.order.JourneyVenue
import com.vaiinilla.app.ui.screens.JOURNEY_SHOW_CODE_TAG
import com.vaiinilla.app.ui.screens.LocalVenueArt
import com.vaiinilla.app.ui.screens.OrderJourneyPage
import com.vaiinilla.app.ui.screens.VenueArt
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/** El seguimiento de cada tipo de pedido abierto desde Pedidos, como los marcos P1 a P13 de Doop, en oscuro. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class OrderJourneyScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private data class Line(
        val qty: Int,
        val name: String,
        val price: String,
        val options: List<String> = emptyList(),
        val rejected: String? = null,
    )

    private fun order(
        state: OrderState,
        folio: Int,
        total: String,
        lines: List<Line>,
        payment: PaymentMethod = PaymentMethod.STRIPE,
        destination: OrderDestination = OrderDestination.TAKE_AWAY,
        space: OrderSpace? = null,
        cashback: String = "0.00",
    ): OrderDetail {
        val base = ScreenshotFixtures.sampleOrder(state = state, paymentMethod = payment, destination = destination)
        val template = base.items.first()
        return base.copy(
            summary =
                base.summary.copy(
                    folio = folio,
                    total = total,
                    subtotal = total,
                    space = space ?: base.summary.space,
                    cashbackAwarded = cashback,
                ),
            kitchenNotes = "",
            items =
                lines.mapIndexed { index, line ->
                    template.copy(
                        id = 900 + index,
                        productName = line.name,
                        quantity = line.qty,
                        subtotal = line.price,
                        options = line.options.mapIndexed { i, name -> OrderItemOption(i, name, "0.00") },
                        rejection = line.rejected?.let { OrderItemRejection(it, line.price) },
                    )
                },
            pickupToken = null,
        )
    }

    private val latte =
        listOf(
            Line(1, "Latte helado grande", "86.00", listOf("Leche de avena", "Sin azúcar")),
            Line(1, "Croissant", "42.00"),
        )

    private fun capture(
        name: String,
        order: OrderDetail,
        venue: JourneyVenue = JourneyVenue(name = "Café Lumbre"),
        qrPayload: String? = null,
        waiter: Boolean = false,
        before: () -> Unit = {},
    ) {
        val photo =
            javaClass.classLoader!!
                .getResourceAsStream("venue/foto.jpg")!!
                .use { BitmapFactory.decodeStream(it) }
                .asImageBitmap()
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            ScreenshotTheme(VaiinillaThemeMode.Dark) {
                CompositionLocalProvider(LocalVenueArt provides VenueArt(photo = photo)) {
                    OrderJourneyPage(
                        order = order,
                        qrPayload = qrPayload,
                        onBack = {},
                        venue = venue,
                        onAnnounceArrival = {},
                        onCurrentWaiterCall = if (waiter) ({ Result.success(null) }) else null,
                        onCallWaiter = if (waiter) ({ _, _, _ -> Result.failure(IllegalStateException("sin red")) }) else null,
                        onCancelWaiter = if (waiter) ({ Result.failure(IllegalStateException("sin red")) }) else null,
                        onOpenMenu = {},
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_500)
        before()
        composeTestRule.mainClock.advanceTimeBy(1_500)
        composeTestRule.onRoot().captureRoboImage("seguimiento/$name.png")
    }

    @Test
    fun p01_para_llevar_preparando() =
        capture("p01_para_llevar_preparando", order(OrderState.PREPARING, 48, "128.00", latte, cashback = "6.40"))

    @Test
    fun p02_para_llevar_listo() =
        capture(
            "p02_para_llevar_listo",
            order(OrderState.READY, 48, "128.00", latte).copy(pickupToken = "VN-48"),
            qrPayload = "vaiinilla://pedido/48",
        )

    @Test
    fun p03_drive_thru() =
        capture(
            "p03_drive_thru",
            order(
                OrderState.PREPARING,
                112,
                "289.00",
                listOf(Line(2, "Combo Patio", "218.00", listOf("Sin cebolla")), Line(1, "Malteada de vainilla", "71.00")),
            ),
            JourneyVenue(driveThru = true, name = "Burger Patio · Periférico", whatsappUrl = "https://wa.me/5216140000000"),
        )

    @Test
    fun p04_mesa() {
        val base =
            order(
                OrderState.PREPARING,
                73,
                "186.00",
                listOf(Line(3, "Tacos de arrachera", "156.00"), Line(1, "Agua de jamaica", "30.00")),
                payment = PaymentMethod.CASH,
                destination = OrderDestination.IN_SPACE,
                space = OrderSpace(id = 4, name = "Mesa 4", type = "mesa"),
            )
        capture(
            "p04_mesa",
            base.copy(summary = base.summary.copy(payAtEnd = true, amountPaid = "0.00", balanceDue = "186.00")),
            JourneyVenue(name = "La Cocina de Doña Toña"),
            waiter = true,
        )
    }

    @Test
    fun p05_barra() =
        capture(
            "p05_barra",
            order(
                OrderState.PREPARING,
                27,
                "240.00",
                listOf(Line(2, "Mezcalita de maracuyá", "240.00", listOf("Una sin chile en el borde"))),
                destination = OrderDestination.IN_SPACE,
                space = OrderSpace(id = 9, name = "Barra principal", type = "barra"),
                cashback = "12.00",
            ).copy(pickupToken = "VN-27"),
            JourneyVenue(name = "Bar El Patio"),
            qrPayload = "vaiinilla://pedido/27",
            waiter = true,
        )

    @Test
    fun p05_barra_codigo() =
        capture(
            "p05_barra_codigo",
            order(
                OrderState.PREPARING,
                27,
                "240.00",
                listOf(Line(2, "Mezcalita de maracuyá", "240.00")),
                destination = OrderDestination.IN_SPACE,
                space = OrderSpace(id = 9, name = "Barra principal", type = "barra"),
            ).copy(pickupToken = "VN-27"),
            JourneyVenue(name = "Bar El Patio"),
            qrPayload = "vaiinilla://pedido/27",
            waiter = true,
        ) {
            composeTestRule.onNodeWithTag(JOURNEY_SHOW_CODE_TAG).performClick()
            composeTestRule.mainClock.advanceTimeBy(1_000)
            composeTestRule.onNodeWithText("FOLIO").assertIsDisplayed()
        }

    @Test
    fun p06_asiento() =
        capture(
            "p06_asiento",
            order(
                OrderState.PREPARING,
                305,
                "215.00",
                listOf(Line(1, "Palomitas grandes", "135.00"), Line(2, "Refrescos", "80.00")),
                destination = OrderDestination.IN_SPACE,
                space = OrderSpace(id = 12, name = "Sala 5 · F12", type = "asiento"),
            ),
            JourneyVenue(name = "Cine Plaza Sendero"),
            waiter = true,
        )

    @Test
    fun p07_cancha() =
        capture(
            "p07_cancha",
            order(
                OrderState.READY,
                64,
                "96.00",
                listOf(Line(2, "Bebida isotónica", "70.00"), Line(1, "Agua mineral", "26.00")),
                destination = OrderDestination.IN_SPACE,
                space = OrderSpace(id = 2, name = "Cancha 2", type = "cancha"),
                cashback = "4.80",
            ),
            JourneyVenue(name = "Club Pádel Norte"),
            waiter = true,
        )

    @Test
    fun p08_escuela() {
        val base =
            order(
                OrderState.PAID,
                212,
                "65.00",
                listOf(Line(1, "Sándwich de pavo", "45.00"), Line(1, "Fruta picada", "20.00")),
                payment = PaymentMethod.BALANCE,
            )
        capture(
            "p08_escuela",
            base.copy(summary = base.summary.copy(deliveryDate = "2026-11-09", deliverySlot = "12:00-13:00")),
            JourneyVenue(school = true, name = "Colegio Montessori Chihuahua"),
        )
    }

    private fun preorder(state: OrderState): OrderDetail {
        val base =
            order(
                state,
                2,
                "1070.29",
                listOf(Line(1, "Pastel de conejita · grande", "1070.29", listOf("Vainilla", "Dice «Feliz cumpleaños, Sofi»"))),
            )
        return base.copy(
            summary = base.summary.copy(deliveryDate = "2026-11-07", deliverySlot = "11:00-13:00"),
            homeDelivery =
                OrderHomeDelivery(
                    address = DeliveryAddress(address = "", phone = ""),
                    deposit = "535.15",
                    dueOnArrival = "535.14",
                ),
            answerBy = Instant.parse("2026-11-03T23:30:00Z"),
        )
    }

    @Test
    fun p09a_encargo_por_aceptar() =
        capture(
            "p09a_encargo_por_aceptar",
            preorder(OrderState.AWAITING_ACCEPTANCE),
            JourneyVenue(name = "Pastelería Vaini", whatsappUrl = "https://wa.me/5216140000000"),
        )

    @Test
    fun p09b_encargo_anticipo() =
        capture(
            "p09b_encargo_anticipo",
            preorder(OrderState.PREPARING),
            JourneyVenue(name = "Pastelería Vaini", whatsappUrl = "https://wa.me/5216140000000"),
        )

    @Test
    fun p10_efectivo() =
        capture(
            "p10_efectivo",
            order(OrderState.PENDING_PAYMENT, 51, "128.00", latte, payment = PaymentMethod.CASH).copy(pickupToken = "VN-51"),
            qrPayload = "vaiinilla://pedido/51",
        )

    @Test
    fun p11_cancelado() {
        val base = order(OrderState.CANCELED, 48, "128.00", latte, payment = PaymentMethod.BALANCE)
        capture(
            "p11_cancelado",
            base.copy(
                summary =
                    base.summary.copy(cancelReason = "Se nos acabó la leche de avena, perdón.", canceledByRole = "cajero"),
            ),
        )
    }

    @Test
    fun p12_un_producto_no_se_pudo() =
        capture(
            "p12_un_producto_no_se_pudo",
            order(
                OrderState.PREPARING,
                48,
                "86.00",
                listOf(latte[0], latte[1].copy(rejected = "Se acabó")),
                payment = PaymentMethod.BALANCE,
            ),
        )

    @Test
    fun p13_no_recogido() =
        capture(
            "p13_no_recogido",
            order(OrderState.NOT_PICKED_UP, 48, "128.00", latte),
            JourneyVenue(name = "Café Lumbre", whatsappUrl = "https://wa.me/5216140000000"),
        )

    private fun homeOrder(state: OrderState): OrderDetail {
        val base = preorder(state)
        return base.copy(
            summary = base.summary.copy(destination = OrderDestination.HOME_DELIVERY, folio = 1),
            homeDelivery =
                base.homeDelivery!!.copy(
                    address = DeliveryAddress(address = "Aldama 1408, Centro", phone = "6140000000"),
                    code = if (state == OrderState.ON_THE_WAY) "4827" else null,
                    dueOnArrival = if (state == OrderState.DELIVERED) "0.00" else "535.14",
                ),
        )
    }

    private val pasteleria = JourneyVenue(name = "Pastelería Vaini", whatsappUrl = "https://wa.me/5216140000000")

    @Test
    fun d01_domicilio_por_aceptar() = capture("d01_domicilio_por_aceptar", homeOrder(OrderState.AWAITING_ACCEPTANCE), pasteleria)

    @Test
    fun d02_domicilio_haciendose() = capture("d02_domicilio_haciendose", homeOrder(OrderState.PREPARING), pasteleria)

    @Test
    fun d03_domicilio_en_camino() = capture("d03_domicilio_en_camino", homeOrder(OrderState.ON_THE_WAY), pasteleria)

    @Test
    fun d05_domicilio_entregado() = capture("d05_domicilio_entregado", homeOrder(OrderState.DELIVERED), pasteleria)

    @Test
    fun d06_domicilio_no_aceptado() {
        val base = homeOrder(OrderState.CANCELED)
        capture(
            "d06_domicilio_no_aceptado",
            base.copy(
                summary =
                    base.summary.copy(
                        cancelReason = "Lo siento mucho, ya tengo la agenda llena ese día. El domingo sí podría.",
                        canceledByRole = "admin",
                    ),
            ),
            pasteleria,
        )
    }
}