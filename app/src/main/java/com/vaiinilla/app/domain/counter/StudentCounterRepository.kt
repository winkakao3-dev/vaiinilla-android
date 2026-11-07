package com.vaiinilla.app.domain.counter

import com.vaiinilla.app.domain.model.OrderDetail

/** Caja sin teléfono: encontrar al alumno, cobrarle con su saldo, entregarle y darle su credencial. */
interface StudentCounterRepository {
    fun search(text: String): Result<List<StudentCard>>

    /** El texto del QR impreso o el UID de la tarjeta NFC; null si la credencial ya no sirve. */
    fun find(credential: String): Result<StudentCard?>

    /** `pin` solo si su tutor le puso uno ([StudentCard.requiresPin]). */
    fun purchase(
        studentId: String,
        items: List<StudentPurchaseItem>,
        pin: String?,
        idempotencyKey: String,
    ): Result<OrderDetail>

    /** La misma venta con el token de otra membresía, para mandarla desde la bandeja de salida con la app cerrada. */
    fun purchaseAs(
        bearer: String,
        studentId: String,
        items: List<StudentPurchaseItem>,
        idempotencyKey: String,
    ): Result<OrderDetail> = Result.failure(UnsupportedOperationException("Sin soporte para mandar con otro token."))

    fun assignCredential(
        studentId: String,
        kind: StudentCredentialKind,
        nfcUid: String?,
        idempotencyKey: String,
    ): Result<StudentCredential>

    /** Entrega un pedido listo del alumno; con QR obligatorio, su credencial hace de QR del pedido. */
    fun deliver(
        orderId: String,
        expectedVersion: Int,
        credential: String?,
        idempotencyKey: String,
    ): Result<OrderDetail>

    /** "Para mañana": lo apartado de un día (admin, cocina y cajero). */
    fun reservedDay(date: String): Result<ReservedDay>

    /** Si es escuela y si la entrega pide QR; Cocina lo necesita aunque no tenga mostrador. */
    fun schoolPolicy(): Result<SchoolPolicy>
}
