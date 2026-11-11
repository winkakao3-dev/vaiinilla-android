package com.vaiinilla.app.data.guest

import androidx.test.core.app.ApplicationProvider
import com.vaiinilla.app.InMemoryGuestKeyVault
import com.vaiinilla.app.core.security.InMemoryPickupTokenStore
import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.data.discovery.RemoteDiscoveryRepository
import com.vaiinilla.app.data.reservations.RemoteReservationRepository
import com.vaiinilla.app.data.reservations.ReservationContractJson
import com.vaiinilla.app.data.tape.ServerTape
import com.vaiinilla.app.data.tape.TapeFloor
import com.vaiinilla.app.domain.admin.SpacePatch
import com.vaiinilla.app.domain.admin.SpaceType
import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.ReservationAccess
import com.vaiinilla.app.domain.model.ReservationPaymentMethod
import com.vaiinilla.app.domain.model.ReservationRefund
import com.vaiinilla.app.domain.model.ReservationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.Duration

/** Lo que hace quien no tiene cuenta (descubrir, entrar como invitado, pedir) y las rentas, contra el servidor de pruebas. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GuestOrderGatewayTapeTest {
    @get:Rule
    val tape = ServerTape()

    private val floor by lazy { TapeFloor(tape) }
    private val guests by lazy {
        GuestSessionStore(ApplicationProvider.getApplicationContext(), InMemoryGuestKeyVault()).also { it.clearGuest() }
    }

    private fun session() = GuestReservationSession(tape.client("publico"), guests, floor.venue)

    private fun gateway(session: GuestReservationSession) =
        GuestOrderGateway(
            apiClient = tape.client("publico"),
            guestSession = session,
            store = guests,
            orderContractJson = floor.orderJson,
            pickupTokenStore = InMemoryPickupTokenStore(),
            parser = ContractResponseParser(),
            venueContext = floor.venue,
        )

    @Test
    fun `sin cuenta se encuentra el negocio, se lee el QR de la mesa, se entra como invitado y se pide`() {
        floor.open()
        val product = floor.product()
        val table = floor.newSpace()
        val slug = tape.store("restaurante").slug

        val discovery = RemoteDiscoveryRepository(tape.client("publico"), ContractResponseParser())
        // La tienda de grabación no sale en el directorio: se busca algo que no existe para no guardar tiendas ajenas.
        assertTrue(discovery.searchEstablishments("zz-nada-zz", 20, null, null).getOrThrow().first.isEmpty())
        assertEquals("Restaurante de grabación", discovery.getEstablishment(slug).getOrThrow().name)
        assertTrue(
            discovery
                .getGuestCatalog(slug)
                .getOrThrow()
                .products
                .any { it.id == product.id },
        )
        val resolved = discovery.resolveSpaceToken(floor.qrToken(table)).getOrThrow()
        assertEquals(slug, resolved.establishment.slug)
        assertEquals(table.id, resolved.space?.id)

        val session = session()
        session.enter(slug, "Invitada de cinta", floor.qrToken(table)).getOrThrow()
        assertTrue(session.hasIdentity())
        val gateway = gateway(session)
        assertTrue(gateway.operationalStatus(slug).getOrThrow().acceptingOrders)
        val request =
            CreateOrderRequest(
                paymentMethod = PaymentMethod.CASH,
                destination = OrderDestination.IN_SPACE,
                spaceId = table.id,
                kitchenNotes = "",
                items = listOf(CreateOrderItem(product.id, 1, emptyList())),
            )
        val created = gateway.create(slug, request, floor.key()).getOrThrow().order
        assertEquals(OrderState.PENDING_PAYMENT, created.summary.state)
        assertEquals(BigDecimal(TapeFloor.PRICE), BigDecimal(created.summary.total))
        assertTrue(gateway.list(slug).getOrThrow().any { it.summary.id == created.summary.id })

        // Otra sesión con la misma llave guardada: así vuelve el invitado al abrir la app otro día.
        assertNotNull(session().bearer(slug).getOrThrow())
        floor
            .orders(
                "cajero",
            ).cancelOrder(created.summary.id, created.summary.version, "Grabación", floor.key())
            .getOrThrow()
    }

    @Test
    fun `una cancha se aparta, se paga en caja y se cancela con su devolucion`() {
        floor.open()
        val court = floor.newSpace(SpaceType.COURT)
        floor
            .spaces()
            .update(
                court.id,
                SpacePatch(hourlyPrice = "200.00", setsHourlyPrice = true),
                floor.key(),
            ).getOrThrow()
        val reservations =
            RemoteReservationRepository(
                floor.client("cajero"),
                ReservationContractJson(floor.orderJson, floor.venue),
                session(),
            )
        val today = reservations.day(null, ReservationAccess.Account).getOrThrow()
        val tomorrow =
            tape.value("dia") {
                java.time.LocalDate
                    .parse(today.today)
                    .plusDays(1)
                    .toString()
            }
        val day = reservations.day(tomorrow, ReservationAccess.Account).getOrThrow()
        val listed = day.courts.single { it.id == court.id }
        assertTrue(listed.rentable)
        assertEquals(BigDecimal("200.00"), BigDecimal(listed.pricePerHour))

        // El negocio decide cuánto se renta: la app solo ofrece las duraciones que manda el servidor.
        val minutes = (listed.durations.ifEmpty { day.durations }).first()
        val price = BigDecimal("200.00") * BigDecimal(minutes) / BigDecimal(60)
        val start = day.opensAt.plus(Duration.ofHours(2))
        val held =
            reservations
                .create(
                    ReservationAccess.Account,
                    court.id,
                    start,
                    minutes,
                    "Cliente de cinta",
                    floor.key(),
                    players = 2,
                ).getOrThrow()
        assertEquals(ReservationState.PENDING_PAYMENT, held.state)
        assertEquals(minutes, held.durationMinutes)
        assertEquals(0, price.compareTo(BigDecimal(held.amount)))
        assertEquals(start, held.start)
        assertEquals("Cliente de cinta", held.customerName)

        val paid =
            reservations
                .pay(ReservationAccess.Account, held.id, ReservationPaymentMethod.CASH, "2000.00", floor.key())
                .getOrThrow()
        assertEquals(ReservationState.CONFIRMED, paid.reservation.state)
        assertEquals(0, (BigDecimal("2000.00") - price).compareTo(BigDecimal(paid.cashChange)))
        assertTrue(reservations.list(ReservationAccess.Account, tomorrow).getOrThrow().any { it.id == held.id })
        val busy =
            reservations
                .day(
                    tomorrow,
                    ReservationAccess.Account,
                ).getOrThrow()
                .courts
                .single { it.id == court.id }
                .busy
        assertTrue("la hora apartada ya sale ocupada", busy.isNotEmpty())

        val refund = ReservationRefund(amount = held.amount, reason = "Se canceló el partido")
        val canceled = reservations.cancel(ReservationAccess.Account, held.id, floor.key(), refund).getOrThrow()
        assertEquals(ReservationState.CANCELLED, canceled.state)
        floor.spaces().delete(court.id, floor.key()).getOrThrow()
    }
}
