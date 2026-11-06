package com.vaiinilla.app.domain.hotel

/**
 * Todo lo que el modo Hotel le pide al servidor, por rol: huésped, personal (recepción y ama de llaves) y admin.
 * Las llamadas son bloqueantes, como en los demás repositorios: se invocan desde `Dispatchers.IO`.
 * Cada operación que escribe lleva su llave de idempotencia.
 */
interface HotelRepository {
    // region Huésped

    /** La estancia vigente de esta cuenta; `null` si todavía no se ha ligado con el código. */
    fun myStay(): Result<Stay?>

    /** Liga la cuenta con la estancia: el código de 6 dígitos más la habitación (por id o por el token de su QR). */
    fun linkStay(
        code: String,
        spaceId: Int? = null,
        qrToken: String? = null,
    ): Result<Stay>

    fun setDoNotDisturb(enabled: Boolean): Result<Stay>

    fun createRequest(
        type: RequestType,
        quantity: Int,
        note: String?,
        idempotencyKey: String,
    ): Result<HotelRequest>

    fun myRequests(): Result<List<HotelRequest>>

    fun cancelRequest(
        requestId: String,
        idempotencyKey: String,
    ): Result<HotelRequest>

    // endregion

    // region Personal (recepción y ama de llaves)

    fun stays(status: StayStatus? = null): Result<List<Stay>>

    fun stay(stayId: String): Result<Stay>

    /** Check-in: la respuesta trae el código de estancia para el huésped. */
    fun checkIn(
        draft: NewStay,
        idempotencyKey: String,
    ): Result<Stay>

    fun addCharge(
        stayId: String,
        concept: String,
        amount: String,
        category: ChargeCategory,
        idempotencyKey: String,
    ): Result<StayCharge>

    /** Cobra lo pendiente de la cuenta, cierra la estancia y deja la habitación por limpiar. */
    fun checkout(
        stayId: String,
        method: CheckoutMethod,
        idempotencyKey: String,
    ): Result<Stay>

    /** Las solicitudes abiertas de todo el hotel. */
    fun openRequests(): Result<List<HotelRequest>>

    fun transition(
        requestId: String,
        action: RequestAction,
        idempotencyKey: String,
    ): Result<HotelRequest>

    fun rooms(): Result<List<HotelRoom>>

    fun setCleaning(
        roomId: Int,
        cleaning: RoomCleaning,
        idempotencyKey: String,
    ): Result<HotelRoom>

    // endregion

    // region Admin

    fun summary(): Result<HotelSummary>

    // endregion
}