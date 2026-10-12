package com.vaiinilla.app.domain.repository

import com.vaiinilla.app.domain.model.PendingRefund
import com.vaiinilla.app.domain.model.RefundMethod

/** Devoluciones pendientes (docs/devoluciones.md del backend). Solo Caja confirma. */
interface RefundRepository {
    /** Devoluciones pendientes del establecimiento. */
    fun pending(): Result<List<PendingRefund>>

    /** Caja confirma que ya devolvió el dinero y cómo. `idempotencyKey` es un UUID estable por intento. */
    fun confirm(
        refundId: String,
        method: RefundMethod,
        note: String?,
        idempotencyKey: String,
    ): Result<PendingRefund>
}
