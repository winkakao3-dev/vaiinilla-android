package com.vaiinilla.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class LastOrderedPlaceTest {
    private val zone = ZoneId.of("America/Chihuahua")

    private fun date(
        day: Int,
        hour: Int = 12,
    ) = LocalDateTime.of(2026, 11, day, hour, 0).atZone(zone).toInstant()

    private val establishment =
        PublicEstablishment(
            id = "7",
            name = "Pastelería Vaini",
            slug = "pasteleria-vaini",
            clientIdLabel = "Matrícula",
            clientIdRequired = false,
        )

    private fun place(
        others: Int,
        day: Int,
        hour: Int = 12,
    ) = LastOrderedPlace(establishment, "Pastel Michi con fresas", others, date(day, hour))

    @Test
    fun `el resumen dice el día y el primer platillo`() {
        val now = date(2, 9)
        assertEquals("Hoy · Pastel Michi con fresas", place(0, 2, 8).summary(now, zone))
        assertEquals("Ayer · Pastel Michi con fresas", place(0, 1, 22).summary(now, zone))
        assertEquals("Ayer · Pastel Michi con fresas y 2 más", place(2, 1).summary(now, zone))
    }

    @Test
    fun `los pedidos más viejos usan el día de la semana y después la fecha`() {
        val now = date(9)
        assertEquals("El jueves", LastOrderedPlace.dayLabel(date(5), now, zone))
        assertEquals("El 1 nov", LastOrderedPlace.dayLabel(date(1), now, zone))
    }

    @Test
    fun `del pedido se toman los platillos vigentes y la hora del servidor`() {
        val place =
            LastOrderedPlace.from(
                establishment,
                order(
                    item(1, "Café", rejected = true),
                    item(2, "Pastel Michi con fresas"),
                    item(3, "Galleta"),
                ),
            )
        assertEquals("Pastel Michi con fresas", place?.firstItemName)
        assertEquals(1, place?.otherItemsCount)
        assertEquals(Instant.parse("2026-11-01T18:00:00Z"), place?.orderedAt)
    }

    @Test
    fun `un pedido sin platillos vigentes no se guarda`() {
        assertNull(LastOrderedPlace.from(establishment, order()))
        assertNull(LastOrderedPlace.from(establishment, order(item(1, "Café", rejected = true))))
    }

    private fun item(
        id: Int,
        name: String,
        rejected: Boolean = false,
    ) = OrderItem(
        id = id,
        productId = id,
        productName = name,
        preparationStation = PreparationStation.KITCHEN,
        quantity = 1,
        unitDigitalPrice = "10.00",
        subtotal = "10.00",
        options = emptyList(),
        rejection = if (rejected) OrderItemRejection(reason = "Se acabó", amount = "10.00") else null,
    )

    private fun order(vararg items: OrderItem) =
        OrderDetail(
            summary =
                OrderSummary(
                    id = "1",
                    folio = 42,
                    operationalDate = "2026-11-01",
                    state = OrderState.PENDING_PAYMENT,
                    paymentMethod = PaymentMethod.CASH,
                    destination = OrderDestination.IN_SPACE,
                    space = null,
                    subtotal = "10.00",
                    combinedSavings = "0.00",
                    cashbackAwarded = "0.00",
                    total = "10.00",
                    version = 1,
                    createdAt = "2026-11-01T18:00:00Z",
                    updatedAt = "2026-11-01T18:00:00Z",
                ),
            user = null,
            kitchenNotes = "",
            items = items.toList(),
        )
}
