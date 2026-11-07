package com.vaiinilla.app.domain.model

import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.data.order.OrderDetailDto
import com.vaiinilla.app.data.order.toDomain
import com.vaiinilla.app.domain.admin.HomeBusinessRules
import com.vaiinilla.app.domain.admin.HomeDeliverySettings
import com.vaiinilla.app.domain.model.VenueContext
import com.vaiinilla.app.ui.admin.AdminOrderFilter
import com.vaiinilla.app.ui.components.needsPickupQr
import com.vaiinilla.app.ui.components.paymentWireLabel
import com.vaiinilla.app.ui.components.trackingStepDescription
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** Las mismas pruebas que `HomeDeliveryTests.swift` de iOS (CASA-01 a 11), contra el contrato del 30 oct. */
class HomeDeliveryTest {
    private val venue = VenueContext.STANDARD

    private val items = listOf(CreateOrderItem(productId = 1, quantity = 1, optionIds = emptyList()))
    private val zone = ZoneId.of("America/Chihuahua")

    private fun body(request: CreateOrderRequest): JsonObject =
        Json.parseToJsonElement(OrderContractJson().encodeCreateRequest(request)).jsonObject

    private fun request(
        destination: OrderDestination = OrderDestination.TAKE_AWAY,
        payment: PaymentMethod = PaymentMethod.CASH,
    ) = CreateOrderRequest(payment, destination, spaceId = null, kitchenNotes = "", items = items)

    @Test
    fun `un pedido normal no manda nada nuevo`() {
        val json = body(request())
        for (key in listOf("entrega", "fecha_entrega", "franja_entrega", "al_recibir", "con_anticipo")) {
            assertNull("Un servidor anterior rechaza $key", json[key])
        }
    }

    @Test
    fun `un pedido a domicilio manda direccion, dia y forma de pago`() {
        val json =
            body(
                request(destination = OrderDestination.HOME_DELIVERY).copy(
                    delivery =
                        DeliveryAddress(
                            "Calle 5 #123, Centro",
                            reference = " ",
                            phone = "614 123 4567",
                            28.63,
                            -106.08,
                        ),
                    deliveryDate = "2026-11-07",
                    paysOnArrival = true,
                ),
            )
        assertEquals("a_domicilio", json["destino"]!!.jsonPrimitive.content)
        assertTrue(json["al_recibir"]!!.jsonPrimitive.boolean)
        assertNull(json["con_anticipo"])
        assertEquals("2026-11-07", json["fecha_entrega"]!!.jsonPrimitive.content)
        val delivery = json["entrega"]!!.jsonObject
        assertEquals("6141234567", delivery["telefono"]!!.jsonPrimitive.content)
        assertNull("Una referencia vacía no viaja", delivery["referencia"])
        assertEquals(28.63, delivery["lat"]!!.jsonPrimitive.double, 0.0)
    }

    @Test
    fun `el pedido lee la entrega y el codigo`() {
        val raw =
            """
            {"id":"p1","folio":3,"fecha_operativa":"2026-10-30","estado":"en_camino","metodo_pago":"efectivo",
             "destino":"a_domicilio","subtotal":"250.00","ahorro_combinado":"0.00","cashback_otorgado":"0.00",
             "total":"290.00","version":4,"creado_en":"2026-10-30T12:00:00Z","actualizado_en":"2026-10-30T12:01:00Z",
             "entrega":{"direccion":"Calle 5 #123","referencia":"Portón verde","telefono":"6141234567","lat":"28.63","lng":-106.08},
             "costo_envio":"40.00","anticipo":null,"por_cobrar_al_entregar":"290.00","fecha_entrega":"2026-11-07",
             "codigo_entrega":4812,"items":[]}
            """.trimIndent()
        val detail = order(raw)
        assertEquals(OrderState.ON_THE_WAY, detail.summary.state)
        assertEquals(OrderDestination.HOME_DELIVERY, detail.summary.destination)
        val delivery = detail.homeDelivery!!
        assertEquals("4812", delivery.code)
        assertEquals("Portón verde", delivery.address.reference)
        assertEquals(28.63, delivery.address.latitude!!, 0.0)
        assertEquals("40.00", delivery.fee)
        assertTrue(delivery.hasAmountDue)
        assertFalse("A domicilio no hay QR", detail.needsPickupQr(deliveryRequiresQr = true))
        assertEquals(3, detail.summary.state.trackingIndex)
    }

    @Test
    fun `el pedido entregado dice con que se cobro en la puerta`() {
        val raw =
            """
            {"id":"p1","folio":3,"fecha_operativa":"2026-10-30","estado":"entregado","metodo_pago":"stripe",
             "destino":"a_domicilio","subtotal":"250.00","ahorro_combinado":"0.00","cashback_otorgado":"0.00",
             "total":"290.00","version":5,"creado_en":"2026-10-30T12:00:00Z","actualizado_en":"2026-10-30T13:00:00Z",
             "entrega":{"direccion":"Calle 5 #123","telefono":"6141234567"},"metodo_pago_cobrado":"mixto",
             "anticipo":"145.00","por_cobrar_al_entregar":"0.00",
             "cobrado_al_entregar":{"metodo":"transferencia","en":"2026-10-30T13:00:00Z"},"items":[]}
            """.trimIndent()
        assertEquals("transferencia", order(raw).homeDelivery!!.collectedAtDoorMethod)
        assertEquals("Transferencia", paymentWireLabel("transferencia"))
    }

    @Test
    fun `el estado de la tienda ofrece domicilio solo si esta activo`() {
        val parser = ContractResponseParser()
        val base =
            """"recibiendo_pedidos":true,"sesion_caja_abierta":true,"caja_en_linea":true,"cocina_en_linea":true,""" +
                """"tiempo_estimado_min":5,"consultado_en":"2026-10-30T12:00:00Z""""

        fun status(extra: String) = parser.parseOperationalStatus("""{"data":{$base$extra},"meta":{},"error":null}""")
        val old = status("")
        assertNull(old.homeDelivery)
        assertNull(old.preorders)
        assertNull(
            status(""","reparto":{"activo":false,"radio_km":"5.0","costo":"40.00","minimo":null}""").homeDelivery,
        )
        val on =
            status(
                ""","reparto":{"activo":true,"radio_km":"5.0","costo":40,"minimo":"200.00"},""" +
                    """"permite_contra_entrega":true,"anticipo_porcentaje":50,"datos_transferencia":"  ",""" +
                    """"encargos_requieren_aceptar":true,"encargos_minutos_aceptar":120,""" +
                    """"dias_adelanto_pedido":7,"hora_corte_pedido":"18:00:00"""",
            )
        assertEquals(
            HomeDeliveryOffer("40", "200.00", "5.0", payOnArrival = true, transferDetails = null),
            on.homeDelivery,
        )
        assertEquals(
            PreorderOffer(7, "18:00", requiresAcceptance = true, depositPercent = 50, answerMinutes = 120),
            on.preorders,
        )
    }

    @Test
    fun `los dias respetan la preparacion y la hora de corte`() {
        val offer = PreorderOffer(daysAhead = 3, cutoff = "16:00", requiresAcceptance = true, depositPercent = null)
        val morning = ZonedDateTime.of(2026, 10, 30, 10, 0, 0, 0, zone)
        val evening = ZonedDateTime.of(2026, 10, 30, 17, 0, 0, 0, zone)
        assertEquals(listOf("2026-10-31", "2026-11-01", "2026-11-02"), HomeCheckoutRules.dates(offer, 1, morning))
        assertEquals(listOf("2026-11-01", "2026-11-02"), HomeCheckoutRules.dates(offer, 1, evening))
        assertEquals("Mañana", HomeCheckoutRules.dayLabel("2026-10-31", LocalDate.of(2026, 10, 30)))
        assertFalse(HomeCheckoutRules.asksForDate(null, 2))
        assertFalse(HomeCheckoutRules.asksForDate(PreorderOffer(0, null, false, null), 0))
    }

    @Test
    fun `el carrito dice lo que falta`() {
        val offer = HomeDeliveryOffer("40.00", "200.00", "5", payOnArrival = true, transferDetails = null)
        var draft = HomeCheckoutDraft()
        assertNotNull(HomeCheckoutRules.problem(draft, offer, "250.00", needsDate = false))
        draft = draft.copy(address = DeliveryAddress("Calle 5 #123, Centro", phone = "6141234567"))
        assertEquals(
            "Revisa la dirección para calcular el envío.",
            HomeCheckoutRules.problem(draft, offer, "250.00", false),
        )
        draft = draft.copy(quote = HomeDeliveryQuote(reaches = false, distanceKm = "9", fee = null, minimum = null))
        assertEquals(
            "Esta dirección queda fuera de la zona de entrega.",
            HomeCheckoutRules.problem(draft, offer, "250.00", false),
        )
        draft = draft.copy(quote = HomeDeliveryQuote(reaches = true, distanceKm = "3", fee = "50.00", minimum = null))
        assertNotNull("Bajo el mínimo", HomeCheckoutRules.problem(draft, offer, "150.00", false))
        assertNotNull("Falta el día", HomeCheckoutRules.problem(draft, offer, "250.00", true))
        draft = draft.copy(deliveryDate = "2026-11-07")
        assertNull(HomeCheckoutRules.problem(draft, offer, "250.00", true))
        assertEquals("50.00", HomeCheckoutRules.fee(draft, offer))
        assertEquals(listOf(HomePayment.ON_ARRIVAL), HomeCheckoutRules.payments(offer, null, acceptsCard = false))
        assertEquals(
            listOf(HomePayment.CARD, HomePayment.DEPOSIT, HomePayment.ON_ARRIVAL),
            HomeCheckoutRules.payments(offer, PreorderOffer(1, null, true, 50), acceptsCard = true),
        )
    }

    @Test
    fun `las acciones del negocio siguen el contrato`() {
        val json = OrderContractJson()
        assertEquals("en-camino", HomeOrderAction.SendOut.path)
        assertEquals("""{"codigo":"4812"}""", json.encodeHomeOrderAction(HomeOrderAction.Deliver("4812")))
        assertEquals("{}", json.encodeHomeOrderAction(HomeOrderAction.Deliver(null)))
        assertEquals("""{"motivo":"Sin horno"}""", json.encodeHomeOrderAction(HomeOrderAction.Reject(" Sin horno ")))
        assertEquals("cobrar-entrega", HomeOrderAction.Collect("transferencia").path)
    }

    @Test
    fun `el tablero ofrece el siguiente paso de cada pedido`() {
        assertEquals(HomeOrderStep.DECIDE, HomeOrderBoard.step(home("por_aceptar")))
        assertEquals(HomeOrderStep.SEND_OUT, HomeOrderBoard.step(home("listo")))
        assertEquals(
            "Sin Cocina, sale en cuanto se acepta",
            HomeOrderStep.SEND_OUT,
            HomeOrderBoard.step(home("cobrado")),
        )
        assertEquals(HomeOrderStep.SEND_OUT, HomeOrderBoard.step(home("preparando")))
        assertNull("Para llevar se recoge, no sale", HomeOrderBoard.step(home("listo", destination = "para_llevar")))
        assertEquals(HomeOrderStep.DELIVER, HomeOrderBoard.step(home("en_camino")))
        assertEquals(HomeOrderStep.COLLECT, HomeOrderBoard.step(home("entregado", due = "290.00")))
        assertNull(HomeOrderBoard.step(home("entregado", due = "0.00")))
    }

    @Test
    fun `me deben lista cada entregado una vez con su total`() {
        val first = home("entregado", due = "290.00", id = "a")
        val second = home("entregado", due = "150.50", id = "b")
        val owed = HomeOrderBoard.owed(listOf(first, second, first, home("en_camino", due = "90.00", id = "c")))
        assertEquals(listOf("a", "b"), owed.map { it.summary.id })
        assertEquals("440.50", HomeOrderBoard.total(owed))
    }

    @Test
    fun `activos pide los estados nuevos solo si el negocio entrega a domicilio`() {
        assertFalse(OrderState.AWAITING_ACCEPTANCE in AdminOrderFilter.ACTIVE.states(homeOrders = false))
        assertTrue(OrderState.AWAITING_ACCEPTANCE in AdminOrderFilter.ACTIVE.states(homeOrders = true))
        assertTrue(OrderState.ON_THE_WAY in AdminOrderFilter.ACTIVE.states(homeOrders = true))
        assertEquals(setOf(OrderState.DELIVERED), AdminOrderFilter.DELIVERED.states(homeOrders = true))
    }

    @Test
    fun `la zona puede quedar vacia y el radio tiene limites`() {
        val base = HomeDeliverySettings(true, "", "40", "", payOnDelivery = false, transferDetails = "")
        assertNull("Sin radio llega a cualquier dirección", HomeBusinessRules.problem(base))
        assertNotNull(HomeBusinessRules.problem(base.copy(radiusKm = "80")))
        assertNotNull(HomeBusinessRules.problem(base.copy(fee = "")))
        assertEquals("1250.50", HomeBusinessRules.money("1,250.5"))
    }

    private fun home(
        state: String,
        destination: String = "a_domicilio",
        due: String = "0.00",
        id: String = "p1",
    ) = order(
        """
        {"id":"$id","folio":3,"fecha_operativa":"2026-10-30","estado":"$state","metodo_pago":"efectivo",
         "destino":"$destination","subtotal":"250.00","ahorro_combinado":"0.00","cashback_otorgado":"0.00",
         "total":"290.00","version":4,"creado_en":"2026-10-30T12:00:00Z","actualizado_en":"2026-10-30T12:01:00Z",
         "entrega":{"direccion":"Calle 5 #123","telefono":"6141234567"},"por_cobrar_al_entregar":"$due","items":[]}
        """.trimIndent(),
    )

    @Test
    fun `el encargo trae sus plazos y se leen con la hora del negocio`() {
        val dto =
            Json { ignoreUnknownKeys = true }.decodeFromString<OrderDetailDto>(
                """
                {"id":"p9","folio":4,"fecha_operativa":"2026-11-02","estado":"por_cobrar","metodo_pago":"stripe",
                 "destino":"para_llevar","subtotal":"500.00","ahorro_combinado":"0.00","cashback_otorgado":"0.00",
                 "total":"500.00","version":2,"creado_en":"2026-11-02T18:00:00Z","actualizado_en":"2026-11-02T18:00:00Z",
                 "aceptar_antes_de":null,"pagar_antes_de":"2026-11-03T22:30:00.123456+00:00","items":[]}
                """.trimIndent(),
            )
        val detail = dto.toDomain()
        assertNull(detail.answerBy)
        assertEquals(java.time.Instant.parse("2026-11-03T22:30:00.123456Z"), detail.payBy)
        val today = LocalDate.of(2026, 11, 2)
        assertEquals("mañana a las 16:30", deadlineLabel(detail.payBy!!, zone, today))
        assertEquals("hoy a las 16:30", deadlineLabel(detail.payBy!!, zone, today.plusDays(1)))
        assertEquals("el 3 nov a las 16:30", deadlineLabel(detail.payBy!!, zone, today.minusDays(3)))
    }

    @Test
    fun `el plazo para contestar se dice en palabras`() {
        assertEquals("30 minutos", answerTimeLabel(30))
        assertEquals("1 hora", answerTimeLabel(60))
        assertEquals("3 horas", answerTimeLabel(180))
        assertEquals("1 día", answerTimeLabel(1440))
        assertEquals("2 días", answerTimeLabel(2880))
        assertEquals("1 h 30 min", answerTimeLabel(90))
    }

    @Test
    fun `listo a domicilio no dice que se recoge en la barra`() {
        assertEquals(
            "Está listo para salir.",
            trackingStepDescription(venue, OrderState.READY, OrderDestination.HOME_DELIVERY, PaymentMethod.CASH, null),
        )
        assertEquals(
            "Recógelo en la barra.",
            trackingStepDescription(venue, OrderState.READY, OrderDestination.TAKE_AWAY, PaymentMethod.CASH, null),
        )
    }

    private fun order(raw: String) = Json { ignoreUnknownKeys = true }.decodeFromString<OrderDetailDto>(raw).toDomain()
}
