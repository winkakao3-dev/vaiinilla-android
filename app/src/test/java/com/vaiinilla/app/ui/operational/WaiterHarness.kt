package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.core.venue.VenueContextStore
import com.vaiinilla.app.domain.counter.ConnectivityWatcher
import com.vaiinilla.app.domain.model.CourtDay
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.Reservation
import com.vaiinilla.app.domain.model.ReservationAccess
import com.vaiinilla.app.domain.model.ReservationPayment
import com.vaiinilla.app.domain.model.ReservationPaymentMethod
import com.vaiinilla.app.domain.model.ReservationRefund
import com.vaiinilla.app.domain.offline.OfflineStaffOrderStore
import com.vaiinilla.app.domain.offline.OfflineStaffOrdersSnapshot
import com.vaiinilla.app.domain.offline.OfflineStaffScope
import com.vaiinilla.app.domain.repository.AbonoMode
import com.vaiinilla.app.domain.repository.AbonoResult
import com.vaiinilla.app.domain.repository.AccountCollection
import com.vaiinilla.app.domain.repository.AccountPaymentMethod
import com.vaiinilla.app.domain.repository.BoardOrder
import com.vaiinilla.app.domain.repository.BoardTable
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.ReservationRepository
import com.vaiinilla.app.domain.repository.SpaceAvailability
import com.vaiinilla.app.domain.repository.SpaceSessionDetail
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.domain.repository.WaiterBoard
import com.vaiinilla.app.domain.repository.WaiterRepository
import com.vaiinilla.app.domain.usecase.BuildCreateOrderRequestUseCase
import com.vaiinilla.app.domain.usecase.CreateRemoteOrderUseCase
import java.time.Instant

internal class FakeWaiterRepository : WaiterRepository {
    @Volatile var board: WaiterBoard = WaiterBoard(tables = emptyList(), callsEnabled = true)
    val transitions = java.util.concurrent.CopyOnWriteArrayList<Pair<String, CallStatus>>()
    val deliveries = java.util.concurrent.CopyOnWriteArrayList<Pair<String, String?>>()

    override fun board(): Result<WaiterBoard> = Result.success(board)

    override fun transitionCall(
        call: TableCall,
        target: CallStatus,
        idempotencyKey: String,
    ): Result<TableCall> {
        transitions += call.id to target
        return Result.success(call.copy(status = target, version = call.version + 1))
    }

    override fun deliver(
        order: BoardOrder,
        qrToken: String?,
        idempotencyKey: String,
    ): Result<Unit> {
        deliveries += order.id to qrToken
        return Result.success(Unit)
    }

    private fun <T> no(): Result<T> = Result.failure(IllegalStateException("no"))

    override fun spaceSession(spaceId: Int): Result<SpaceSessionDetail> = no()

    override fun availability(): Result<List<SpaceAvailability>> = Result.success(emptyList())

    override fun publicAvailability(slug: String): Result<List<SpaceAvailability>> = Result.success(emptyList())

    override fun openSession(
        spaceId: Int,
        durationMinutes: Int?,
        idempotencyKey: String,
    ): Result<Unit> = no()

    override fun extendSession(
        spaceId: Int,
        minutes: Int,
        expectedVersion: Int?,
        idempotencyKey: String,
    ): Result<Unit> = no()

    override fun releaseSpace(
        spaceId: Int,
        expectedVersion: Int?,
        idempotencyKey: String,
    ): Result<Unit> = no()

    override fun collectAccount(
        spaceId: Int,
        method: AccountPaymentMethod,
        received: String?,
        expectedTotal: String?,
        orderIds: List<String>?,
        idempotencyKey: String,
        tip: String?,
    ): Result<AccountCollection> = no()

    override fun abonar(
        spaceId: Int,
        method: AccountPaymentMethod,
        mode: AbonoMode,
        amount: String?,
        parts: Int?,
        received: String?,
        expectedRemaining: String,
        idempotencyKey: String,
        tip: String?,
    ): Result<AbonoResult> = no()

    override fun currentCall(espacioId: Int): Result<TableCall?> = Result.success(null)

    override fun call(
        espacioId: Int,
        reason: CallReason,
        orderId: String?,
        idempotencyKey: String,
    ): Result<TableCall> = no()

    override fun cancelCall(
        call: TableCall,
        idempotencyKey: String,
    ): Result<TableCall> = no()
}

internal class FakeReservations : ReservationRepository {
    private fun <T> no(): Result<T> = Result.failure(IllegalStateException("no"))

    override fun day(
        date: String?,
        access: ReservationAccess,
    ): Result<CourtDay> = no()

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
    ): Result<Reservation> = no()

    override fun pay(
        access: ReservationAccess,
        reservationId: String,
        method: ReservationPaymentMethod,
        cashReceived: String?,
        idempotencyKey: String,
    ): Result<ReservationPayment> = no()

    override fun cancel(
        access: ReservationAccess,
        reservationId: String,
        idempotencyKey: String,
        refund: ReservationRefund?,
    ): Result<Reservation> = no()
}

internal class MemoryOfflineStore : OfflineStaffOrderStore {
    private val saved = mutableMapOf<OfflineStaffScope, OfflineStaffOrdersSnapshot>()

    override fun load(
        scope: OfflineStaffScope,
        today: String,
    ) = (saved[scope] ?: OfflineStaffOrdersSnapshot(day = today)).startingDay(today)

    override fun save(
        scope: OfflineStaffScope,
        snapshot: OfflineStaffOrdersSnapshot,
    ) {
        saved[scope] = snapshot
    }

    override fun deviceTag(): Int = 7
}

internal object WaiterHarness {
    fun viewModel(
        waiter: FakeWaiterRepository,
        orders: FakeOrders = FakeOrders(),
        catalog: FakeCatalog = FakeCatalog(),
    ): WaiterViewModel {
        val queue =
            com.vaiinilla.app.ui.operational.OfflineStaffOrderQueue(
                repository = orders,
                store = MemoryOfflineStore(),
                connectivity = ConnectivityWatcher { AutoCloseable { } },
            )
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val field = unsafeClass.getDeclaredField("theUnsafe")
        field.isAccessible = true
        val rescuer =
            unsafeClass.getMethod("allocateInstance", Class::class.java).invoke(field.get(null), OfflineStaffOrderRescuer::class.java)
                as OfflineStaffOrderRescuer
        return WaiterViewModel(
            waiterRepository = waiter,
            reservationRepository = FakeReservations(),
            catalogRepository = catalog,
            buildCreateOrderRequest = BuildCreateOrderRequestUseCase(),
            createOrder = CreateRemoteOrderUseCase(orders),
            offlineOrders = queue,
            rescuer = rescuer,
            venueContext = VenueContextStore(),
        )
    }

    fun space(
        id: Int = 4,
        name: String = "Mesa 4",
    ) = TableSpace(id, name, "mesa")

    fun call(
        id: String = "c1",
        reason: CallReason = CallReason.PROBLEMA,
        status: CallStatus = CallStatus.PENDIENTE,
    ) = TableCall(
        id = id,
        space = space(),
        orderId = null,
        reason = reason,
        status = status,
        clientName = "Ana",
        takenBy = null,
        createdAt = "2026-11-07T10:00:00Z",
        takenAt = null,
        closedAt = null,
        version = 1,
    )

    fun order(
        id: String = "o1",
        withoutQr: Boolean = false,
    ) = BoardOrder(
        id = id,
        folio = 7,
        state = OrderState.READY,
        version = 2,
        clientName = "Ana",
        itemsSummary = "1 hamburguesa",
        updatedAt = "2026-11-07T10:05:00Z",
        deliveryWithoutQr = withoutQr,
    )

    fun table(
        call: TableCall? = null,
        orders: List<BoardOrder> = emptyList(),
    ) = BoardTable(space = space(), call = call, orders = orders)
}
