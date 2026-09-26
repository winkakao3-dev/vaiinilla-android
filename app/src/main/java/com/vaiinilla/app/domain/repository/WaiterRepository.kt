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

data class BoardTable(
    val space: TableSpace,
    val call: TableCall?,
    val orders: List<BoardOrder>,
)

data class WaiterBoard(
    val tables: List<BoardTable>,
    val callsEnabled: Boolean,
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

    fun deliver(
        order: BoardOrder,
        qrToken: String,
        idempotencyKey: String,
    ): Result<Unit>

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
