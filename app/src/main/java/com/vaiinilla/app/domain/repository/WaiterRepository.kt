package com.vaiinilla.app.domain.repository

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState

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
) {
    val rentable: Boolean
        get() = space.type == "cancha" && pricePerHour != null
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

data class AccountCollection(
    val ordersCollected: Int,
    val total: String,
    val received: String,
    val change: String,
    /** Lo que quedó sin cobrar al dividir la cuenta; "0.00" si quedó saldada. */
    val remaining: String = "0.00",
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

fun canCallWaiter(order: OrderDetail): Boolean =
    order.summary.destination == OrderDestination.IN_SPACE &&
        order.summary.space != null &&
        order.summary.state != OrderState.CANCELED

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
        received: String,
        expectedTotal: String?,
        orderIds: List<String>?,
        idempotencyKey: String,
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
