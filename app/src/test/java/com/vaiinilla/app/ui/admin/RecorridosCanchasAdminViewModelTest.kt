package com.vaiinilla.app.ui.admin

import com.vaiinilla.app.core.venue.VenueContextStore
import com.vaiinilla.app.domain.admin.ManagedSpace
import com.vaiinilla.app.domain.admin.ReservationSchedule
import com.vaiinilla.app.domain.model.CourtDay
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationAccess
import com.vaiinilla.app.domain.model.ReservationPayment
import com.vaiinilla.app.domain.model.ReservationPaymentMethod
import com.vaiinilla.app.domain.model.ReservationRefund
import com.vaiinilla.app.domain.model.ReservationState
import com.vaiinilla.app.domain.repository.ReservationRepository
import com.vaiinilla.app.ui.operational.StaffHarness
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

private class AdminReservations(
    var existing: List<Reservation> = emptyList(),
) : ReservationRepository {
    val payments = CopyOnWriteArrayList<Pair<String, String?>>()
    val cancels = CopyOnWriteArrayList<Pair<String, ReservationRefund?>>()

    private fun <T> no(): Result<T> = Result.failure(IllegalStateException("no"))

    override fun day(
        date: String?,
        access: ReservationAccess,
    ): Result<CourtDay> = no()

    override fun list(
        access: ReservationAccess,
        date: String?,
    ): Result<List<Reservation>> = Result.success(existing)

    override fun create(
        access: ReservationAccess,
        courtId: Int,
        start: Instant?,
        durationMinutes: Int,
        customerName: String?,
        idempotencyKey: String,
        players: Int?,
    ): Result<Reservation> = no()

    override fun pay(
        access: ReservationAccess,
        reservationId: String,
        method: ReservationPaymentMethod,
        cashReceived: String?,
        idempotencyKey: String,
    ): Result<ReservationPayment> {
        payments += reservationId to cashReceived
        return Result.success(ReservationPayment(existing.first { it.id == reservationId }, null, cashReceived, "0.00"))
    }

    override fun cancel(
        access: ReservationAccess,
        reservationId: String,
        idempotencyKey: String,
        refund: ReservationRefund?,
    ): Result<Reservation> {
        cancels += reservationId to refund
        return Result.success(existing.first { it.id == reservationId }.copy(state = ReservationState.CANCELLED))
    }
}

/**
 * Canchas y reservas del admin (J06 y T06): precio por hora, horario de reservas, cobrar una renta en efectivo y
 * cancelar con reembolso. Corre en la JVM; no cuenta como recorrido en teléfono.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecorridosCanchasAdminViewModelTest {
    private val spaces = FakeSpaces()
    private val reservations = AdminReservations()
    private lateinit var vm: AdminSpacesViewModel

    private val ui get() = vm.uiState.value

    private val court =
        ManagedSpace(
            id = 9,
            name = "Pista 1",
            typeWire = "cancha",
            active = true,
            qrUrl = "https://x/q",
            code = "P1",
            hourlyPrice = "300.00",
            description = null,
            imageUrl = null,
            features = emptyList(),
        )

    private fun reservation(
        id: String,
        startOffsetMin: Long,
        state: ReservationState,
    ) = Reservation(
        id = id,
        courtId = 9,
        courtName = "Pista 1",
        start = Instant.parse("2026-11-07T18:00:00Z").plusSeconds(startOffsetMin * 60),
        end = Instant.parse("2026-11-07T19:00:00Z").plusSeconds(startOffsetMin * 60),
        durationMinutes = 60,
        amount = "300.00",
        customerPrice = null,
        state = state,
        holdExpiresAt = null,
        orderId = null,
        channel = "personal",
        customerName = "Luis",
        version = 1,
    )

    @Before
    fun setUp() {
        StaffHarness.installMain()
        spaces.spaceList = listOf(court)
        spaces.schedule = ReservationSchedule("08:00", "22:00", 7)
        reservations.existing =
            listOf(
                reservation("r2", 120, ReservationState.CONFIRMED),
                reservation("rx", 30, ReservationState.CANCELLED),
                reservation("r1", 0, ReservationState.PENDING_PAYMENT),
            )
        vm = AdminSpacesViewModel(spaces, reservations, VenueContextStore())
        vm.onVisible("negocio-1")
        StaffHarness.awaitUntil("las canchas y reservas") {
            ui.spaces != null && ui.schedule != null && ui.reservations.isNotEmpty()
        }
    }

    @Test
    fun j06_veElHorarioYLasReservasDeHoyEnOrdenSinLasCanceladas() {
        assertEquals("08:00", ui.schedule?.opening)
        assertEquals(listOf("r1", "r2"), ui.reservations.map { it.id })
    }

    @Test
    fun j06_elPrecioPorHoraSeGuardaYUnPrecioInvalidoNoLlegaAlServidor() {
        vm.setHourlyPrice(court, "abc")
        StaffHarness.awaitUntil("el aviso") {
            ui.notice == "Escribe el precio por hora, por ejemplo 300." &&
                ui.noticeIsError
        }
        assertTrue(spaces.patches.isEmpty())
        vm.setHourlyPrice(court, "350")
        StaffHarness.awaitUntil("el guardado") { ui.notice == "Precio por hora guardado" }
        val (id, patch) = spaces.patches.single()
        assertEquals(9, id)
        assertTrue(patch.setsHourlyPrice)
        assertTrue(patch.hourlyPrice!!.startsWith("350"))
    }

    @Test
    fun j06_dejarElPrecioVacioLaQuitaDeLaRenta() {
        vm.setHourlyPrice(court, "")
        StaffHarness.awaitUntil("el aviso") { ui.notice == "Pista 1 ya no se renta" }
        assertNull(
            spaces.patches
                .single()
                .second.hourlyPrice,
        )
    }

    @Test
    fun t06_unHorarioConLaMismaHoraOReservarMuyLejosNoSeGuarda() {
        vm.saveSchedule(ReservationSchedule("08:00", "08:00", 7))
        StaffHarness.awaitUntil("el aviso") {
            ui.notice == "La hora de cierre debe ser distinta a la de apertura." &&
                ui.noticeIsError
        }
        vm.saveSchedule(ReservationSchedule("08:00", "22:00", 90))
        StaffHarness.awaitUntil("el aviso de días") { ui.notice == "Se puede reservar de 0 a 60 días antes." }
        assertTrue(spaces.savedSchedules.isEmpty())
    }

    @Test
    fun t06_unHorarioValidoSeGuardaYAvisa() {
        vm.saveSchedule(ReservationSchedule("09:00", "23:00", 14))
        StaffHarness.awaitUntil("el aviso") { ui.notice == "Horario de reservas guardado" }
        assertEquals("09:00", spaces.savedSchedules.single().opening)
        assertEquals(14, spaces.savedSchedules.single().daysAhead)
    }

    @Test
    fun t06_cobrarUnaRentaEnEfectivoLaMandaConLoRecibido() {
        var done = false
        vm.payReservation(reservations.existing.first { it.id == "r1" }, "300.00") { done = true }
        StaffHarness.awaitUntil("el cobro") { ui.notice == "Renta cobrada" && done }
        assertEquals(listOf("r1" to "300.00"), reservations.payments.toList())
    }

    @Test
    fun t06_cancelarUnaRentaPagadaPideMotivoYMandaElReembolso() {
        val paid = reservations.existing.first { it.id == "r2" }
        vm.refundReservation(paid, "300", "ab") {}
        StaffHarness.awaitUntil("el aviso del motivo") { ui.notice == "Escribe el motivo (de 3 a 240 letras)." }
        assertTrue(reservations.cancels.isEmpty())
        var done = false
        vm.refundReservation(paid, "150", "El cliente avisó que no llega") { done = true }
        StaffHarness.awaitUntil("la cancelación") { done && reservations.cancels.isNotEmpty() }
        val (id, refund) = reservations.cancels.single()
        assertEquals("r2", id)
        assertEquals("El cliente avisó que no llega", refund?.reason)
        assertTrue(refund!!.amount.startsWith("150"))
    }
}
