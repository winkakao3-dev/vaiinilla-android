package com.vaiinilla.app.domain.repository

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.ReservationState
import java.time.Instant

enum class CallReason(
    val wireValue: String,
    val label: String,
    val staffLabel: String,
) {
    ATENCION("atencion", "Necesito algo", "Necesita atención"),
    UTENSILIOS("utensilios", "Cubiertos o servilletas", "Pide cubiertos o servilletas"),
    PROBLEMA("problema", "Algo está mal con mi pedido", "Algo está mal con su pedido"),
    CUENTA("cuenta", "Pedir la cuenta", "Pide la cuenta"),
    ;

    companion object {
        fun fromWireValue(value: String): CallReason =
            entries.firstOrNull { it.wireValue == value }
                ?: throw IllegalArgumentException("motivo de llamada no soportado: $value")
    }
}

enum class CallStatus(
    val wireValue: String,
) {
    PENDIENTE("pendiente"),
    EN_CAMINO("en_camino"),
    ATENDIDA("atendida"),
    CANCELADA("cancelada"),
    EXPIRADA("expirada"),
    ;

    companion object {
        fun fromWireValue(value: String): CallStatus =
            entries.firstOrNull { it.wireValue == value }
                ?: throw IllegalArgumentException("estado de llamada no soportado: $value")
    }
}

data class TableSpace(
    val id: Int,
    val name: String,
    val type: String,
)

data class TableCallTaker(
    val userId: String,
    val name: String,
)

data class TableCall(
    val id: String,
    val space: TableSpace,
    val orderId: String?,
    val reason: CallReason,
    val status: CallStatus,
    val clientName: String?,
    val takenBy: TableCallTaker?,
    val createdAt: String,
    val takenAt: String?,
    val closedAt: String?,
    val version: Int,
)

data class BoardOrder(
    val id: String,
    val folio: Int,
    val state: OrderState,
    val version: Int,
    val clientName: String?,
    val itemsSummary: String,
    val updatedAt: String,
)

/** Estado de un espacio: libre, ocupado (con turno o mesa abierta), en gracia o por cobrar. */
enum class SpaceAvailabilityState(
    val wireValue: String,
) {
    LIBRE("libre"),
    OCUPADA("ocupada"),
    EN_GRACIA("en_gracia"),
    POR_COBRAR("por_cobrar"),
    ;

    companion object {
        fun fromWireValue(value: String): SpaceAvailabilityState =
            entries.firstOrNull { it.wireValue == value }
                ?: throw IllegalArgumentException("estado de espacio no soportado: $value")
    }
}

data class SpaceAvailability(
    val space: TableSpace,
    val state: SpaceAvailabilityState,
    /** No queda nada por cobrar en la cuenta del espacio. */
    val settled: Boolean,
    val endsAt: String?,
    val releasesAt: String?,
    val remainingSeconds: Int?,
    val graceMinutes: Int,
    /** Cuándo empezó la sesión; con `endsAt` da el avance del turno. */
    val startedAt: String? = null,
    /** Precio por hora: con precio, la cancha se renta y se paga primero (no se abre a mano). */
    val pricePerHour: String? = null,
    /** Siguiente reserva (próximas 24 h), para avisar al personal. */
    val nextReservationStart: String? = null,
    val nextReservationEnd: String? = null,
    /** `pendiente_pago` o `confirmada`. */
    val nextReservationState: String? = null,
) {
    val rentable: Boolean
        get() = space.type == "cancha" && pricePerHour != null

    /**
     * Sin turno abierto pero apartada por una reserva que ya empezó: una renta "ahora" que espera
     * pago, o una pagada a la que todavía no se le abre el turno. No se debe mostrar libre.
     */
    fun isHeldAt(now: Instant = Instant.now()): Boolean {
        if (state != SpaceAvailabilityState.LIBRE) return false
        val start = nextReservationStart?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return false
        val end = nextReservationEnd?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return false
        return !now.isBefore(start) && now.isBefore(end)
    }

    /** Libre de verdad: sin turno y sin una reserva en su horario. */
    val isFreeNow: Boolean
        get() = state == SpaceAvailabilityState.LIBRE && !isHeldAt()

    val isWaitingPayment: Boolean
        get() = nextReservationState == "pendiente_pago"
}

data class SpaceSessionInfo(
    val id: String,
    val startedAt: String,
    val endsAt: String?,
    val version: Int,
)

data class AccountOrder(
    val id: String,
    val folio: Int,
    val state: String,
    val total: String,
    val clientName: String?,
    val itemsSummary: String,
    val payAtEnd: Boolean,
    /** Aún no se cobra: por cobrar en Caja, o a la cuenta sin cobrar. */
    val pending: Boolean,
)

data class SpaceAccount(
    val orders: List<AccountOrder>,
    val total: String,
    val pending: String,
    val paid: String,
    val settled: Boolean,
)

data class SpaceSessionDetail(
    val availability: SpaceAvailability,
    val session: SpaceSessionInfo?,
    val account: SpaceAccount?,
)

/** Cómo se cobra una cuenta abierta: efectivo en la caja o con la terminal de tarjeta del negocio. */
enum class AccountPaymentMethod(
    val wire: String,
) {
    CASH("efectivo"),
    TERMINAL("terminal"),
    ;

    companion object {
        fun fromWire(value: String?): AccountPaymentMethod = entries.firstOrNull { it.wire == value } ?: CASH
    }
}

data class AccountCollection(
    val ordersCollected: Int,
    val total: String,
    val received: String,
    val change: String,
    /** Lo que quedó sin cobrar al dividir la cuenta; "0.00" si quedó saldada. */
    val remaining: String = "0.00",
    val method: AccountPaymentMethod = AccountPaymentMethod.CASH,
) {
    /** Con este cobro la cuenta quedó saldada. */
    val settled: Boolean get() = remaining.toBigDecimalOrNull()?.signum() != 1
}

data class BoardTable(
    val space: TableSpace,
    val call: TableCall?,
    val orders: List<BoardOrder>,
    val availability: SpaceAvailability? = null,
)

data class WaiterBoard(
    val tables: List<BoardTable>,
    val callsEnabled: Boolean,
    /** Si es false, el mesero entrega en el espacio sin escanear el QR del cliente. */
    val deliveryRequiresQr: Boolean = true,
)

enum class TableState {
    CALL,
    READY,
    ACTIVE,
    FREE,
}

fun BoardTable.tableState(): TableState =
    when {
        call != null -> TableState.CALL
        orders.any { it.state == OrderState.READY } -> TableState.READY
        orders.isNotEmpty() -> TableState.ACTIVE
        availability != null && availability.state != SpaceAvailabilityState.LIBRE -> TableState.ACTIVE
        else -> TableState.FREE
    }

fun sortWaiterTables(tables: List<BoardTable>): List<BoardTable> =
    tables.sortedWith(
        compareBy<BoardTable> {
            when (it.tableState()) {
                TableState.CALL -> 0
                TableState.READY -> 1
                TableState.ACTIVE -> 2
                TableState.FREE -> 3
            }
        }.thenBy { table ->
            table.call?.createdAt ?: table.space.name
        }.thenBy { it.space.name },
    )

/**
 * La cancha a la que llama el botón "Llamar al mesero": la del pedido a una mesa o cancha, o la de una
 * renta en juego (el backend deja llamar con una renta pagada y en curso aunque no se haya pedido comida).
 */
fun callWaiterSpaceId(
    order: OrderDetail,
    now: Instant = Instant.now(),
): Int? {
    val summary = order.summary
    val space = summary.space
    if (summary.destination == OrderDestination.IN_SPACE && space != null && summary.state != OrderState.CANCELED) {
        return space.id
    }
    val reservation = order.reservation ?: return null
    val playing =
        (reservation.state == ReservationState.CONFIRMED || reservation.state == ReservationState.IN_PROGRESS) &&
            !now.isBefore(reservation.start) &&
            now.isBefore(reservation.end)
    return reservation.space?.id.takeIf { playing }
}

fun canCallWaiter(order: OrderDetail): Boolean = callWaiterSpaceId(order) != null

class CallsUnavailableException(
    message: String = "Llamar al mesero todavía no está disponible en esta cafetería.",
) : IllegalStateException(message)

interface WaiterRepository {
    fun board(): Result<WaiterBoard>

    fun transitionCall(
        call: TableCall,
        target: CallStatus,
        idempotencyKey: String,
    ): Result<TableCall>

    /** Con `qrToken` nulo, entrega sin QR (el establecimiento lo dispensa en espacios). */
    fun deliver(
        order: BoardOrder,
        qrToken: String?,
        idempotencyKey: String,
    ): Result<Unit>

    fun spaceSession(spaceId: Int): Result<SpaceSessionDetail>

    /** Estado de todos los espacios activos; lo puede leer también el cliente (mapa de canchas). */
    fun availability(): Result<List<SpaceAvailability>>

    /** Lo mismo sin sesión: el mapa público de un establecimiento, para quien solo está mirando el menú. */
    fun publicAvailability(slug: String): Result<List<SpaceAvailability>>

    /** Abre el turno; sin duración abre la cuenta de una mesa sin turno. */
    fun openSession(
        spaceId: Int,
        durationMinutes: Int?,
        idempotencyKey: String,
    ): Result<Unit>

    fun extendSession(
        spaceId: Int,
        minutes: Int,
        expectedVersion: Int?,
        idempotencyKey: String,
    ): Result<Unit>

    /** Libera el espacio: el backend no deja hacerlo con pedidos sin cobrar. */
    fun releaseSpace(
        spaceId: Int,
        expectedVersion: Int?,
        idempotencyKey: String,
    ): Result<Unit>

    fun collectAccount(
        spaceId: Int,
        method: AccountPaymentMethod,
        received: String?,
        expectedTotal: String?,
        orderIds: List<String>?,
        idempotencyKey: String,
        tip: String? = null,
    ): Result<AccountCollection>

    fun currentCall(espacioId: Int): Result<TableCall?>

    fun call(
        espacioId: Int,
        reason: CallReason,
        orderId: String?,
        idempotencyKey: String,
    ): Result<TableCall>

    fun cancelCall(
        call: TableCall,
        idempotencyKey: String,
    ): Result<TableCall>
}
