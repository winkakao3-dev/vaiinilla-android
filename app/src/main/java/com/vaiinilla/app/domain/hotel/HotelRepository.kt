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

    /** Check-in (alta de la estancia): la respuesta trae el código de estancia para el huésped. */
    fun checkIn(
        draft: NewStay,
        idempotencyKey: String,
    ): Result<Stay>

    /** Registra el cargo en la cuenta (no mueve dinero); devuelve el cargo recién agregado. */
    fun addCharge(
        stayId: String,
        concept: String,
        amount: String,
        category: ChargeCategory,
        idempotencyKey: String,
    ): Result<StayCharge>

    /**
     * Cobra lo pendiente de la cuenta, cierra la estancia y deja la habitación por limpiar.
     * [expectedTotal] es lo que recepción vio en pantalla: si la cuenta cambió el servidor responde
     * `ACCOUNT_TOTAL_CHANGED`. [receivedAmount] solo aplica a efectivo.
     */
    fun checkout(
        stayId: String,
        method: CheckoutMethod,
        idempotencyKey: String,
        receivedAmount: String? = null,
        expectedTotal: String? = null,
        expectedVersion: Int? = null,
    ): Result<Stay>

    /** `POST estancias/:id/check-in`: una estancia `reservada` pasa a `en_casa`. */
    fun confirmArrival(
        stayId: String,
        expectedVersion: Int,
        idempotencyKey: String,
    ): Result<Stay>

    /** `POST estancias/:id/cancelaciones`: solo una `reservada`. */
    fun cancelStay(
        stayId: String,
        expectedVersion: Int,
        idempotencyKey: String,
    ): Result<Stay>

    /** Las solicitudes abiertas de todo el hotel. */
    fun openRequests(): Result<List<HotelRequest>>

    fun transition(
        requestId: String,
        action: RequestAction,
        idempotencyKey: String,
        expectedVersion: Int? = null,
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

    /** `GET hotel/configuracion`: solicitudes, no molestar, salida tardía y hora de salida. */
    fun hotelConfig(): Result<HotelConfig>

    /** Lo mismo que [hotelConfig]; el huésped (H03) lo lee para saber si ofrece «tarjeta ahora». */
    fun config(): Result<HotelConfig> = hotelConfig()

    /** `PUT hotel/configuracion`: guarda la configuración completa. */
    fun saveHotelConfig(
        config: HotelConfig,
        idempotencyKey: String,
    ): Result<HotelConfig>

    /** `POST habitaciones/lote`: crea «prefijo desde»…«prefijo hasta» con piso y categoría. */
    fun createRooms(
        prefix: String,
        from: Int,
        to: Int,
        floor: Int?,
        category: String?,
        idempotencyKey: String,
    ): Result<RoomBatchResult>

    /** `PATCH habitaciones/:id`: cambia el piso y la categoría. */
    fun updateRoom(
        roomId: Int,
        floor: Int?,
        category: String?,
        idempotencyKey: String,
    ): Result<HotelRoom>

    // endregion
}
