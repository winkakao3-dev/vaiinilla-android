package com.vaiinilla.app.domain.repository

/**
 * Dinero que el negocio debe regresar a mano (`/devoluciones`, docs/devoluciones.md del backend): lo abonado de
 * más a una cuenta, o un pedido o artículo quitado que se pagó con terminal, saldo o pago mixto.
 */
data class PendingRefund(
    val id: String,
    /** `sobrante_cuenta`, `rechazo_articulo` o `cancelacion`. */
    val origin: String,
    val spaceId: Int?,
    val orderId: String?,
    /** `efectivo`, `terminal`, `saldo` o una mezcla (`efectivo+terminal`). */
    val originalMethod: String,
    val amount: String,
    /** `pendiente` o `devuelta`. */
    val state: String,
    val createdAt: String,
)

/** Cómo devolvió Caja el dinero. */
enum class RefundMethod(
    val wireValue: String,
    val label: String,
) {
    CASH("efectivo", "efectivo"),
    TERMINAL("terminal", "terminal"),
    OTHER("otro", "otro"),
}

interface PendingRefundRepository {
    /** Las pendientes del negocio, o solo las de un espacio. */
    fun pending(spaceId: Int? = null): Result<List<PendingRefund>>

    /** Caja confirma que ya devolvió el dinero; en efectivo sale del cajón. Se confirma una sola vez. */
    fun confirm(
        refundId: String,
        method: RefundMethod,
        note: String?,
        idempotencyKey: String,
    ): Result<PendingRefund>
}
