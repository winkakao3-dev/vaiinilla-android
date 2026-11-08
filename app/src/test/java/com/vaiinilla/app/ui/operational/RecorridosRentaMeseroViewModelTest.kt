package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.model.CourtDay
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationAccess
import com.vaiinilla.app.domain.model.ReservationPayment
import com.vaiinilla.app.domain.model.ReservationPaymentMethod
import com.vaiinilla.app.domain.model.ReservationRefund
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.repository.ReservationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

private class RentalReservations : ReservationRepository {
    data class Create(
        val courtId: Int,
        val minutes: Int,
        val name: String?,
        val players: Int?,
        val key: String,
    )

    val creates = java.util.concurrent.CopyOnWriteArrayList<Create>()
    val payments = java.util.concurrent.CopyOnWriteArrayList<Pair<String, String?>>()
    val cancels = java.util.concurrent.CopyOnWriteArrayList<String>()

    @Volatile var createFails = false

    private fun reservation(
        courtId: Int,
        minutes: Int,
        name: String?,
        players: Int?,
    ) = Reservation(
        id = "r-" + (creates.size),
        courtId = courtId,
        courtName = "Pista 1",
        start = Instant.now(),
        end = Instant.now().plusSeconds(minutes * 60L),
        durationMinutes = minutes,
        amount = "200.00",
        customerPrice = null,
        state = ReservationState.PENDING_PAYMENT,
        holdExpiresAt = null,
        orderId = null,
        channel = "personal",
        customerName = name,
        version = 1,
        players = players,
    )

    override fun day(
        date: String?,
        access: ReservationAccess,
    ): Result<CourtDay> = Result.failure(IllegalStateException("no"))

    override fun list(
        access: ReservationAccess,
        date: String?,
    ): Result<List<Reservation>> = Result.success(emptyList())

    override fun create(
        access: ReservationAccess,
        courtId: Int,
        start: Instant?,
        durationMinutes: Int,
        customerName: String?,
        idempotencyKey: String,
        players: Int?,
    ): Result<Reservation> {
        creates += Create(courtId, durationMinutes, customerName, players, idempotencyKey)
        if (createFails) return Result.failure(IllegalStateException("sin red"))
        return Result.success(reservation(courtId, durationMinutes, customerName, players))
    }

    override fun pay(
        access: ReservationAccess,
        reservationId: String,
        method: ReservationPaymentMethod,
        cashReceived: String?,
        idempotencyKey: String,
    ): Result<ReservationPayment> {
        payments += reservationId to cashReceived
        val paid = reservation(1, 60, null, null).copy(id = reservationId, state = ReservationState.CONFIRMED)
        return Result.success(ReservationPayment(paid, null, cashReceived, "50.00"))
    }

    override fun cancel(
        access: ReservationAccess,
        reservationId: String,
        idempotencyKey: String,
        refund: ReservationRefund?,
    ): Result<Reservation> {
        cancels += reservationId
        return Result.success(
            reservation(1, 60, null, null).copy(id = reservationId, state = ReservationState.CANCELLED),
        )
    }
}

/**
 * Rentar una cancha o pista desde el mesero (U02): se aparta con nombre y jugadores, se cobra en efectivo
 * con cambio, y soltar el cobro libera el horario. Corre en la JVM con Robolectric; no cuenta como teléfono.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecorridosRentaMeseroViewModelTest {
    private val reservations = RentalReservations()
    private lateinit var vm: WaiterViewModel

    private val ui get() = vm.uiState.value

    @Before
    fun setUp() {
        StaffHarness.installMain()
        vm = WaiterHarness.viewModel(FakeWaiterRepository(), reservations = reservations)
        vm.bindContext("m-1")
    }

    @Test
    fun u02_apartaConNombreYJugadoresYLaLlaveEsLaMismaAlReintentar() {
        reservations.createFails = true
        vm.startRental(1, 60, null, players = 4, customerName = "  Luis ")
        StaffHarness.awaitUntil("el primer intento") { reservations.creates.size == 1 && !ui.acting }
        assertNull(ui.pendingRental)
        reservations.createFails = false
        vm.startRental(1, 60, null, players = 4, customerName = "Luis")
        StaffHarness.awaitUntil("el apartado") { ui.pendingRental != null }
        val first = reservations.creates[0]
        val second = reservations.creates[1]
        assertEquals("Luis", first.name)
        assertEquals(4, second.players)
        assertEquals(60, second.minutes)
        assertEquals(first.key, second.key)
        assertEquals("Luis", ui.pendingRental?.customerName)
    }

    @Test
    fun u02_cobraEnEfectivoYLaRentaQuedaPagadaSinPendiente() {
        vm.startRental(1, 90, null, players = 2)
        StaffHarness.awaitUntil("el apartado") { ui.pendingRental != null }
        val id = ui.pendingRental!!.id
        vm.confirmRental("250.00")
        StaffHarness.awaitUntil("el cobro") { ui.pendingRental == null && !ui.acting }
        assertEquals(listOf(id to "250.00"), reservations.payments.toList())
        assertTrue(ui.toastMessage.orEmpty().contains("cambio $50.00"))
        assertNull(ui.rentalDraft)
    }

    @Test
    fun u02_cerrarElCobroSinCobrarLiberaElHorarioYGuardaLoElegido() {
        vm.startRental(1, 90, null, players = 3)
        StaffHarness.awaitUntil("el apartado") { ui.pendingRental != null }
        val id = ui.pendingRental!!.id
        vm.cancelPendingRental(players = 3)
        StaffHarness.awaitUntil("el horario liberado") { reservations.cancels.isNotEmpty() }
        assertEquals(listOf(id), reservations.cancels.toList())
        assertNull(ui.pendingRental)
        val draft = ui.rentalDraft
        assertNotNull(draft)
        assertEquals(90, draft!!.minutes)
        assertEquals(3, draft.players)
    }
}
