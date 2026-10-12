package com.vaiinilla.app.domain.model

/** Dinero que el negocio debe regresar a mano (docs/devoluciones.md del backend). */
data class PendingRefund(
    val id: String,
    val origin: RefundOrigin,
    val spaceId: Int?,
    /** Método con el que se pagó: efectivo, terminal, saldo o mezcla ("efectivo+terminal"). */
    val originalMethod: String,
    val amount: String,
)

enum class RefundOrigin(
    val wire: String,
) {
    OVERPAID_ACCOUNT("sobrante_cuenta"),
    REJECTED_ITEM("rechazo_articulo"),
    CANCELLED_ORDER("cancelacion"),
    ;

    companion object {
        fun fromWire(value: String): RefundOrigin =
            entries.firstOrNull { it.wire == value }
                ?: throw IllegalArgumentException("origen de devolución no soportado: $value")
    }
}

/** Cómo Caja devolvió el dinero. */
enum class RefundMethod(
    val wire: String,
) {
    CASH("efectivo"),
    CARD("terminal"),
    OTHER("otro"),
}
