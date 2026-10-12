package com.vaiinilla.app.domain.repository

import com.vaiinilla.app.domain.model.SharedTable
import com.vaiinilla.app.domain.model.TableSession

/** Mesa compartida del cliente (docs/mesa-compartida.md y docs/dividir-cuenta.md del backend). */
interface SharedTableRepository {
    /** La mesa donde está el cliente, o null si no está en ninguna. */
    fun current(): Result<SharedTable?>

    /** Se une a la mesa del QR con un alias que escribe el cliente. */
    fun join(
        qrToken: String,
        alias: String,
    ): Result<SharedTable>

    /** Sesión abierta del QR con sus participantes, para elegir quién se es (GET /mesas/espacio/:token). */
    fun session(qrToken: String): Result<TableSession> =
        Result.failure<TableSession>(UnsupportedOperationException("La mesa no está disponible."))

    /** Continúa como un participante que ya está en la mesa (`participante_id`). */
    fun continueAs(
        qrToken: String,
        participantId: String,
    ): Result<SharedTable> = Result.failure<SharedTable>(UnsupportedOperationException("La mesa no está disponible."))

    fun leave(): Result<Unit>

    /** "Esto lo pago yo" (o ya no) sobre un pedido de la cuenta de la mesa, por su folio. */
    fun claim(
        folio: Int,
        payIt: Boolean,
    ): Result<SharedTable>
}
