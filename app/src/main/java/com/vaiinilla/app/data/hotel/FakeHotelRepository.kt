package com.vaiinilla.app.data.hotel

import com.vaiinilla.app.domain.hotel.ChargeCategory
import com.vaiinilla.app.domain.hotel.CheckoutMethod
import com.vaiinilla.app.domain.hotel.HotelConfig
import com.vaiinilla.app.domain.hotel.HotelErrors
import com.vaiinilla.app.domain.hotel.HotelException
import com.vaiinilla.app.domain.hotel.HotelRepository
import com.vaiinilla.app.domain.hotel.HotelRequest
import com.vaiinilla.app.domain.hotel.HotelRoom
import com.vaiinilla.app.domain.hotel.HotelSummary
import com.vaiinilla.app.domain.hotel.HotelTopItem
import com.vaiinilla.app.domain.hotel.NewStay
import com.vaiinilla.app.domain.hotel.RequestAction
import com.vaiinilla.app.domain.hotel.RequestStatus
import com.vaiinilla.app.domain.hotel.RequestType
import com.vaiinilla.app.domain.hotel.RoomBatchResult
import com.vaiinilla.app.domain.hotel.RoomCleaning
import com.vaiinilla.app.domain.hotel.Stay
import com.vaiinilla.app.domain.hotel.StayCharge
import com.vaiinilla.app.domain.hotel.StayOrder
import com.vaiinilla.app.domain.hotel.StayStatus
import java.math.BigDecimal

/**
 * El hotel en memoria, con los datos de las pantallas de Doop (Habitación 304, código 482916), para que las
 * pantallas y sus pruebas JVM trabajen sin servidor. Se ve como un hotel con una estancia ya ligada; `reset()`
 * lo deja como al principio. Vive en `main` porque `HotelBackend.USE_FAKE` lo elige en la app.
 */
class FakeHotelRepository : HotelRepository {
    private val lock = Any()
    private var nextId = 100
    private var linked = true
    private var rooms = seedRooms()
    private var stays = seedStays()
    private var requests = seedRequests()
    private var config = HotelConfig(lateCheckoutTime = "14:00", lateCheckoutPrice = "350.00", checkoutTime = "12:00")

    /** El huésped de esta cuenta; arranca en la 304 ya ligado. Con `linked = false` se prueba el código de estancia. */
    var guestStayId: String = GUEST_STAY_ID

    fun reset(linkedGuest: Boolean = true) =
        synchronized(lock) {
            nextId = 100
            linked = linkedGuest
            guestStayId = GUEST_STAY_ID
            rooms = seedRooms()
            stays = seedStays()
            requests = seedRequests()
            config = HotelConfig(lateCheckoutTime = "14:00", lateCheckoutPrice = "350.00", checkoutTime = "12:00")
        }

    override fun myStay(): Result<Stay?> =
        synchronized(lock) { Result.success(guestStay()?.withTotal()) }

    override fun linkStay(
        code: String,
        spaceId: Int?,
        qrToken: String?,
    ): Result<Stay> =
        synchronized(lock) {
            val stay = stays.firstOrNull { it.code == code.trim() && it.status != StayStatus.CHECKED_OUT }
            when {
                stay == null -> failure("CODIGO_INVALIDO", "Ese código no coincide con ninguna estancia.")
                spaceId != null && stay.roomId != spaceId -> failure("HABITACION_DISTINTA", "Ese código es de otra habitación.")
                else -> {
                    linked = true
                    guestStayId = stay.id
                    Result.success(stay.withTotal())
                }
            }
        }

    override fun setDoNotDisturb(enabled: Boolean): Result<Stay> =
        synchronized(lock) {
            val stay = guestStay() ?: return failure(HotelErrors.ROOM_REQUIRES_STAY, SIN_ESTANCIA)
            replaceStay(stay.copy(doNotDisturb = enabled))
            rooms = rooms.map { if (it.id == stay.roomId) it.copy(doNotDisturb = enabled) else it }
            Result.success(stays.first { it.id == stay.id }.withTotal())
        }

    override fun createRequest(
        type: RequestType,
        quantity: Int,
        note: String?,
        idempotencyKey: String,
    ): Result<HotelRequest> =
        synchronized(lock) {
            val stay = guestStay() ?: return failure(HotelErrors.ROOM_REQUIRES_STAY, SIN_ESTANCIA)
            val request =
                HotelRequest(
                    id = "${nextId++}",
                    stayId = stay.id,
                    roomId = stay.roomId,
                    roomName = stay.roomName,
                    floor = stay.floor,
                    type = type,
                    quantity = quantity,
                    note = note?.trim()?.ifEmpty { null },
                    createdAt = NOW,
                )
            requests = requests + request
            refreshRoomCounts()
            Result.success(request)
        }

    override fun myRequests(): Result<List<HotelRequest>> =
        synchronized(lock) { Result.success(requests.filter { it.stayId == guestStayId }) }

    override fun cancelRequest(
        requestId: String,
        idempotencyKey: String,
    ): Result<HotelRequest> = updateRequest(requestId) { it.copy(status = RequestStatus.CANCELED) }

    override fun stays(status: StayStatus?): Result<List<Stay>> =
        synchronized(lock) { Result.success(stays.filter { status == null || it.status == status }.map { it.withTotal() }) }

    override fun stay(stayId: String): Result<Stay> =
        synchronized(lock) {
            stays.firstOrNull { it.id == stayId }?.let { Result.success(it.withTotal()) }
                ?: failure("ESTANCIA_NO_ENCONTRADA", "No existe esa estancia.")
        }

    override fun checkIn(
        draft: NewStay,
        idempotencyKey: String,
    ): Result<Stay> =
        synchronized(lock) {
            val room = rooms.firstOrNull { it.id == draft.roomId } ?: return failure("HABITACION_NO_ENCONTRADA", "No existe esa habitación.")
            if (room.occupied) return failure("HABITACION_OCUPADA", "La ${room.name} ya tiene un huésped.")
            val stay =
                Stay(
                    id = "S${nextId++}",
                    guestName = draft.guestName,
                    contact = draft.contact,
                    code = (100_000 + nextId * 7_919 % 900_000).toString(),
                    roomId = room.id,
                    roomName = room.name,
                    floor = room.floor,
                    checkIn = draft.checkIn,
                    checkOut = draft.checkOut,
                    nightlyRate = draft.nightlyRate,
                    status = StayStatus.IN_HOUSE,
                    nights = 1,
                )
            stays = stays + stay
            rooms = rooms.map { if (it.id == room.id) it.copy(occupied = true, stayId = stay.id, guestName = stay.guestName) else it }
            Result.success(stay.withTotal())
        }

    override fun addCharge(
        stayId: String,
        concept: String,
        amount: String,
        category: ChargeCategory,
        idempotencyKey: String,
    ): Result<StayCharge> =
        synchronized(lock) {
            val stay = stays.firstOrNull { it.id == stayId } ?: return failure("ESTANCIA_NO_ENCONTRADA", "No existe esa estancia.")
            val charge = StayCharge("C${nextId++}", concept.trim(), amount, category, NOW)
            replaceStay(stay.copy(charges = stay.charges + charge))
            Result.success(charge)
        }

    override fun checkout(
        stayId: String,
        method: CheckoutMethod,
        idempotencyKey: String,
    ): Result<Stay> =
        synchronized(lock) {
            val stay = stays.firstOrNull { it.id == stayId } ?: return failure("ESTANCIA_NO_ENCONTRADA", "No existe esa estancia.")
            val closed = stay.copy(status = StayStatus.CHECKED_OUT, paymentMethod = method.wire, paidAt = NOW)
            replaceStay(closed)
            rooms =
                rooms.map {
                    if (it.id == stay.roomId) {
                        it.copy(occupied = false, stayId = null, guestName = null, cleaning = RoomCleaning.DIRTY, doNotDisturb = false)
                    } else {
                        it
                    }
                }
            if (stay.id == guestStayId) linked = false
            Result.success(closed.withTotal())
        }

    override fun openRequests(): Result<List<HotelRequest>> =
        synchronized(lock) { Result.success(requests.filter { it.status.isOpen }) }

    override fun transition(
        requestId: String,
        action: RequestAction,
        idempotencyKey: String,
    ): Result<HotelRequest> =
        updateRequest(requestId) {
            when (action) {
                RequestAction.TAKE -> it.copy(status = RequestStatus.TAKEN, takenBy = "Ama de llaves")
                RequestAction.RESOLVE, RequestAction.APPROVE -> it.copy(status = RequestStatus.RESOLVED)
            }
        }

    override fun rooms(): Result<List<HotelRoom>> = synchronized(lock) { Result.success(rooms) }

    override fun setCleaning(
        roomId: Int,
        cleaning: RoomCleaning,
        idempotencyKey: String,
    ): Result<HotelRoom> =
        synchronized(lock) {
            val room = rooms.firstOrNull { it.id == roomId } ?: return failure("HABITACION_NO_ENCONTRADA", "No existe esa habitación.")
            val updated = room.copy(cleaning = cleaning)
            rooms = rooms.map { if (it.id == roomId) updated else it }
            Result.success(updated)
        }

    override fun summary(): Result<HotelSummary> =
        synchronized(lock) {
            Result.success(
                HotelSummary(
                    occupied = rooms.count { it.occupied },
                    totalRooms = rooms.size,
                    roomServiceToday = "4820.00",
                    roomOrdersToday = 14,
                    inKitchenNow = 3,
                    openRequests = requests.count { it.status.isOpen },
                    oldestRequestMinutes = 12,
                    topItems =
                        listOf(
                            HotelTopItem("Club sándwich", 9, "1350.00"),
                            HotelTopItem("Hamburguesa de la casa", 7, "1190.00"),
                            HotelTopItem("Café americano", 12, "720.00"),
                        ),
                    departuresToday = stays.count { it.status == StayStatus.IN_HOUSE && it.checkOut == TODAY },
                ),
            )
        }

    override fun hotelConfig(): Result<HotelConfig> = synchronized(lock) { Result.success(config) }

    override fun saveHotelConfig(
        config: HotelConfig,
        idempotencyKey: String,
    ): Result<HotelConfig> =
        synchronized(lock) {
            this.config = config
            Result.success(config)
        }

    override fun createRooms(
        prefix: String,
        from: Int,
        to: Int,
        floor: Int?,
        category: String?,
        idempotencyKey: String,
    ): Result<RoomBatchResult> =
        synchronized(lock) {
            if (to < from) return failure("RANGO_INVALIDO", "El rango no es válido.")
            val omitted = mutableListOf<String>()
            var created = 0
            for (number in from..to) {
                val name = "${prefix.trim()} $number"
                if (rooms.any { it.name == name }) {
                    omitted += name
                } else {
                    rooms = rooms + HotelRoom(id = number, name = name, floor = floor, category = category, qrToken = "qr-$number", qrUrl = "https://vaiinilla.app/q/qr-$number")
                    created++
                }
            }
            Result.success(RoomBatchResult(created, omitted))
        }

    override fun updateRoom(
        roomId: Int,
        floor: Int?,
        category: String?,
        idempotencyKey: String,
    ): Result<HotelRoom> =
        synchronized(lock) {
            val room = rooms.firstOrNull { it.id == roomId } ?: return failure("HABITACION_NO_ENCONTRADA", "No existe esa habitación.")
            val updated = room.copy(floor = floor, category = category?.trim()?.ifEmpty { null })
            rooms = rooms.map { if (it.id == roomId) updated else it }
            Result.success(updated)
        }

    private fun updateRequest(
        requestId: String,
        change: (HotelRequest) -> HotelRequest,
    ): Result<HotelRequest> =
        synchronized(lock) {
            val request = requests.firstOrNull { it.id == requestId } ?: return failure("SOLICITUD_NO_ENCONTRADA", "No existe esa solicitud.")
            val updated = change(request)
            requests = requests.map { if (it.id == requestId) updated else it }
            refreshRoomCounts()
            Result.success(updated)
        }

    private fun refreshRoomCounts() {
        rooms = rooms.map { room -> room.copy(openRequests = requests.count { it.roomId == room.id && it.status.isOpen }) }
    }

    /** La estancia vigente de esta cuenta: ligada y sin check-out. */
    private fun guestStay(): Stay? =
        stays.firstOrNull { it.id == guestStayId && linked && it.status != StayStatus.CHECKED_OUT && it.status != StayStatus.CANCELED }

    private fun replaceStay(stay: Stay) {
        stays = stays.map { if (it.id == stay.id) stay else it }
    }

    private fun Stay.withTotal(): Stay {
        val nightly = nightlyRate?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val extras = charges.sumOf { it.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO }
        val food = orders.sumOf { it.total.toBigDecimalOrNull() ?: BigDecimal.ZERO }
        return copy(total = (nightly.multiply(BigDecimal(nights)) + extras + food).setScale(2).toPlainString())
    }

    private fun <T> failure(
        code: String,
        message: String,
    ): Result<T> = Result.failure(HotelException(code, message))

    private companion object {
        const val GUEST_STAY_ID = "S1"
        const val SIN_ESTANCIA = "Necesitas una estancia vigente para pedir a tu habitación."
        const val TODAY = "2026-11-08"
        const val NOW = "2026-11-06T20:00:00Z"

        fun seedRooms() =
            rawRooms().map { it.copy(qrToken = "qr-${it.id}", qrUrl = "https://vaiinilla.app/q/qr-${it.id}") }

        private fun rawRooms() =
            listOf(
                HotelRoom(301, "Habitación 301", 3, "doble"),
                HotelRoom(302, "Habitación 302", 3, "doble", RoomCleaning.DIRTY),
                HotelRoom(304, "Habitación 304", 3, "suite", occupied = true, stayId = GUEST_STAY_ID, guestName = "Laura Méndez", openRequests = 2),
                HotelRoom(305, "Habitación 305", 3, "doble", occupied = true, stayId = "S2", guestName = "Carlos Ibarra"),
                HotelRoom(201, "Habitación 201", 2, "doble"),
                HotelRoom(202, "Habitación 202", 2, "suite", RoomCleaning.DIRTY),
            )

        fun seedStays() =
            listOf(
                Stay(
                    id = GUEST_STAY_ID,
                    guestName = "Laura Méndez",
                    contact = "+52 614 555 0142",
                    userId = "guest",
                    code = "482916",
                    roomId = 304,
                    roomName = "Habitación 304",
                    floor = 3,
                    checkIn = "2026-11-06",
                    checkOut = TODAY,
                    nightlyRate = "1850.00",
                    status = StayStatus.IN_HOUSE,
                    nights = 2,
                    charges = listOf(StayCharge("C1", "Minibar", "180.00", ChargeCategory.MINIBAR, NOW)),
                    orders = listOf(StayOrder("P1", 41, "320.00", "entregado", NOW)),
                ),
                Stay(
                    id = "S2",
                    guestName = "Carlos Ibarra",
                    code = "731048",
                    roomId = 305,
                    roomName = "Habitación 305",
                    floor = 3,
                    checkIn = "2026-11-05",
                    checkOut = TODAY,
                    nightlyRate = "1450.00",
                    status = StayStatus.IN_HOUSE,
                    nights = 3,
                ),
            )

        fun seedRequests() =
            listOf(
                HotelRequest("R1", GUEST_STAY_ID, 304, "Habitación 304", 3, RequestType.TOWELS, 2, null, RequestStatus.PENDING, null, NOW),
                HotelRequest("R2", GUEST_STAY_ID, 304, "Habitación 304", 3, RequestType.CLEANING, 1, "Después de las 11", RequestStatus.TAKEN, "Ama de llaves", NOW),
            )
    }
}