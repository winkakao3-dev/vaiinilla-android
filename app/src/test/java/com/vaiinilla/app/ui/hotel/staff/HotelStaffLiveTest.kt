package com.vaiinilla.app.ui.hotel.staff

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.core.security.InMemoryPickupTokenStore
import com.vaiinilla.app.core.venue.VenueContextStore
import com.vaiinilla.app.data.hotel.RemoteHotelRepository
import com.vaiinilla.app.data.operational.RemoteCashSessionRepository
import com.vaiinilla.app.data.operational.RemoteWaiterRepository
import com.vaiinilla.app.data.operational.WaiterContractJson
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.data.order.RemoteOrderRepository
import com.vaiinilla.app.domain.hotel.ChargeCategory
import com.vaiinilla.app.domain.hotel.CheckoutMethod
import com.vaiinilla.app.domain.hotel.HotelErrors
import com.vaiinilla.app.domain.hotel.HotelException
import com.vaiinilla.app.domain.hotel.HotelRequest
import com.vaiinilla.app.domain.hotel.HotelRoom
import com.vaiinilla.app.domain.hotel.NewStay
import com.vaiinilla.app.domain.hotel.RequestStatus
import com.vaiinilla.app.domain.hotel.RequestType
import com.vaiinilla.app.domain.hotel.RoomCleaning
import com.vaiinilla.app.domain.hotel.StayStatus
import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.SpaceCopy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.net.URLEncoder
import java.time.LocalDate
import java.util.UUID

/**
 * S01 a S04 contra el backend de pruebas de VPS2 (instancia del hotel). Corre solo si existe el archivo de
 * tokens (variable HOTEL_LIVE_TOKENS o /private/tmp/hotel-live/tokens.json); sin él se omite. Usa los repositorios
 * reales de la app y los ViewModel de esta pieza, y deja las habitaciones como las encontró.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HotelStaffLiveTest {
    private val base = "https://hotel.169-58-43-188.sslip.io/api/v1/"
    private var tokens: Map<String, String> = emptyMap()
    private val venueStore = VenueContextStore().apply { update("hotel") }

    @Before
    fun setUp() {
        val file = File(System.getenv("HOTEL_LIVE_TOKENS") ?: "/private/tmp/hotel-live/tokens.json")
        assumeTrue("sin tokens de VPS2", file.exists())
        tokens = Json.parseToJsonElement(file.readText()).jsonObject.mapValues { it.value.jsonPrimitive.content }
    }

    private class Response(
        val body: String,
        val status: Int,
    )

    private class LiveClient(
        override val baseUrl: String,
        private val bearer: String,
    ) : VaiinillaApiClient {
        private fun call(
            method: String,
            path: String,
            body: String? = null,
            headers: Map<String, String> = emptyMap(),
            query: Map<String, String> = emptyMap(),
        ): Result<String> {
            val qs =
                if (query.isEmpty()) {
                    ""
                } else {
                    "?" +
                        query.entries.joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }
                }
            // curl: el classpath de las pruebas es el de Android y no trae java.net.http ni PATCH en HttpURLConnection.
            val command =
                mutableListOf(
                    "curl",
                    "-s",
                    "-m",
                    "30",
                    "-X",
                    method,
                    "-w",
                    "\n%{http_code}",
                    "-H",
                    "Authorization: Bearer $bearer",
                    "-H",
                    "Content-Type: application/json",
                )
            headers.forEach { (k, v) -> command += listOf("-H", "$k: $v") }
            if (body != null) command += listOf("--data-binary", body)
            command += baseUrl + path.trimStart('/') + qs
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            val response =
                Response(
                    output.substringBeforeLast("\n"),
                    output.substringAfterLast("\n").trim().toIntOrNull() ?: 0,
                )
            if (response.status in 200..299) return Result.success(response.body)
            val error =
                runCatching {
                    Json
                        .parseToJsonElement(
                            response.body,
                        ).jsonObject["error"] as? JsonObject
                }.getOrNull()
            return Result.failure(
                ApiClientException(
                    code = error?.get("code")?.jsonPrimitive?.content ?: "HTTP_${response.status}",
                    message = error?.get("message")?.jsonPrimitive?.content ?: response.body.take(200),
                    httpStatus = response.status,
                ),
            )
        }

        override fun get(
            path: String,
            query: Map<String, String>,
        ) = call("GET", path, query = query)

        override fun post(
            path: String,
            body: String,
            headers: Map<String, String>,
        ) = call("POST", path, body, headers)

        override fun postWithoutBody(
            path: String,
            headers: Map<String, String>,
        ) = call("POST", path, "{}", headers)

        override fun patch(
            path: String,
            body: String,
            headers: Map<String, String>,
        ) = call("PATCH", path, body, headers)

        override fun put(
            path: String,
            body: String,
            headers: Map<String, String>,
        ) = call("PUT", path, body, headers)
    }

    private fun client(role: String) = LiveClient(base, tokens.getValue(role))

    private fun hotel(role: String) = RemoteHotelRepository(client(role))

    private fun orders(role: String) =
        RemoteOrderRepository(client(role), OrderContractJson(), InMemoryPickupTokenStore())

    private fun waiter(role: String) =
        RemoteWaiterRepository(client(role), WaiterContractJson(), OrderContractJson(), venueStore)

    /** Deja correr los hilos de IO y el hilo principal hasta que se cumpla la condición. */
    private fun await(
        what: String,
        timeoutMs: Long = 30_000,
        condition: () -> Boolean,
    ) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            shadowOf(android.os.Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(60)
        }
        fail("Tiempo agotado esperando: $what")
    }

    private fun <T> Result<T>.ok(what: String): T = getOrElse { throw AssertionError("$what: ${it.message}") }

    private fun <T> Result<T>.errorCode(): String? =
        exceptionOrNull()?.let {
            (it as? HotelException)?.code
                ?: it.javaClass.simpleName
        }

    @Test
    fun recorrido_s01_a_s04_contra_vps2() {
        val ts = System.currentTimeMillis().toString().takeLast(5)
        val cajero = hotel("cajero")
        val mesero = hotel("mesero")
        val cliente = hotel("cliente")
        val cash = RemoteCashSessionRepository(client("cajero"), OrderContractJson())
        var openedCash = false
        val cashWasOpen = cash.activeSessionId().ok("sesión de caja") != null

        // S03 · el tablero del ama de llaves lee las habitaciones reales.
        val rooms = mesero.rooms().ok("habitaciones")
        assertTrue("el hotel de pruebas debe tener habitaciones", rooms.isNotEmpty())
        assertTrue("las habitaciones traen piso", rooms.all { it.floor != null })
        val free = rooms.filter { !it.occupied }
        assertTrue("hace falta al menos 2 habitaciones libres para la prueba (hay ${free.size})", free.size >= 2)
        val room: HotelRoom = free[0]
        val spare: HotelRoom = free[1]

        // Recepción · check-in con el ViewModel.
        val reception = HotelReceptionViewModel(cajero)
        reception.refresh()
        await("lista de recepción") { reception.state.value.loaded }
        assertTrue(
            "recepción ve habitaciones libres",
            reception.state.value.freeRooms
                .any { it.id == room.id },
        )
        val today = LocalDate.now()
        reception.checkIn(
            NewStay("Prueba A2 $ts", null, room.id, today.toString(), today.plusDays(2).toString(), "1500.00"),
        )
        await("check-in") { reception.state.value.created != null || reception.state.value.error != null }
        assertNull("check-in sin error: ${reception.state.value.error}", reception.state.value.error)
        val stay = reception.state.value.created!!
        assertEquals(StayStatus.IN_HOUSE, stay.status)
        assertEquals("el código de estancia lleva 6 dígitos", 6, stay.code.orEmpty().length)
        assertEquals(2, stay.nights)

        try {
            // El huésped se vincula y pide toallas.
            cliente.linkStay(stay.code!!, spaceId = room.id).ok("vincular")
            val request: HotelRequest =
                cliente
                    .createRequest(
                        RequestType.TOWELS,
                        2,
                        "prueba A2",
                        UUID.randomUUID().toString(),
                    ).ok("solicitud")

            // S03 · el ama de llaves la ve, la toma y la resuelve.
            val board = HotelRoomsViewModel(mesero)
            board.refresh()
            await("tablero") { board.state.value.loaded }
            assertNotNull("el tablero muestra un piso", board.state.value.floorShown)
            val seen =
                board.state.value.requests
                    .firstOrNull { it.id == request.id }
            assertNotNull("la solicitud aparece en el tablero", seen)
            assertTrue("el título lleva cantidad y tipo", requestTitle(seen!!).endsWith("2 toallas"))
            board.advance(seen)
            await("tomar") {
                board.state.value.busyId == null &&
                    board.state.value.requests
                        .firstOrNull { it.id == request.id }
                        ?.status == RequestStatus.TAKEN
            }
            val taken =
                board.state.value.requests
                    .first { it.id == request.id }
            assertTrue("tomada con la versión nueva", taken.version > seen.version)
            board.advance(taken)
            await("resolver") {
                board.state.value.busyId == null &&
                    board.state.value.requests
                        .none { it.id == request.id }
            }

            // S03 · una habitación libre alterna limpia y por limpiar, y se restaura.
            val spareNow =
                board.state.value.rooms
                    .first { it.id == spare.id }
            board.toggleCleaning(spareNow)
            val flipped = if (spareNow.cleaning == RoomCleaning.DIRTY) RoomCleaning.READY else RoomCleaning.DIRTY
            await("limpieza") {
                board.state.value.busyId == null &&
                    board.state.value.rooms
                        .first { it.id == spare.id }
                        .cleaning == flipped
            }
            board.toggleCleaning(
                board.state.value.rooms
                    .first { it.id == spare.id },
            )
            await("limpieza restaurada") {
                board.state.value.busyId == null &&
                    board.state.value.rooms
                        .first { it.id == spare.id }
                        .cleaning == spareNow.cleaning
            }

            // S01/S02 · un pedido a la habitación: cocina lo ve y el mesero lo entrega sin código.
            val catalog = client("cliente").get("catalogo").ok("catálogo")
            val product =
                Json
                    .parseToJsonElement(catalog)
                    .jsonObject["data"]!!
                    .jsonObject["productos"]!!
                    .jsonArray
                    .first { p ->
                        p.jsonObject["estacion_preparacion"]?.jsonPrimitive?.content == "cocina" &&
                            p.jsonObject["disponible"]?.jsonPrimitive?.content == "true"
                    }.jsonObject
            val productId = product["id"]!!.jsonPrimitive.content.toInt()
            val created =
                orders("cliente")
                    .createOrder(
                        CreateOrderRequest(
                            paymentMethod = PaymentMethod.CASH,
                            destination = OrderDestination.IN_SPACE,
                            spaceId = room.id,
                            kitchenNotes = "Prueba A2 $ts, tocar suave",
                            items = listOf(CreateOrderItem(productId, 1, emptyList())),
                        ),
                        UUID.randomUUID().toString(),
                    ).ok("pedido a la habitación")
            val orderId = created.summary.id
            assertTrue(
                "el pedido a habitación va a la cuenta",
                created.summary.payAtEnd || created.summary.paymentPending,
            )

            // S01 · cocina: el pedido llega con la habitación como espacio de tipo mesa.
            val kitchen = orders("cocina").listOrders(OperationalRole.KITCHEN).ok("pedidos de cocina")
            val inKitchen = kitchen.firstOrNull { it.summary.id == orderId }
            assertNotNull("cocina ve el pedido de la habitación", inKitchen)
            val space = inKitchen!!.summary.space
            assertNotNull(space)
            assertTrue("el espacio se reconoce como habitación", SpaceCopy.isRoom(venueStore.current, space!!.type))
            assertEquals(
                "Hab. " + room.name.filter { it.isDigit() },
                SpaceCopy.short(venueStore.current, space.type, space.name),
            )
            assertTrue(
                "cocina lo mete a su fila",
                com.vaiinilla.app.ui.screens
                    .kitchenQueue(listOf(inKitchen!!))
                    .isNotEmpty(),
            )

            // Cocina empieza y marca listo.
            var current = inKitchen!!
            for (target in listOf(OrderState.PREPARING, OrderState.READY)) {
                current =
                    orders("cocina")
                        .transition(orderId, target, current.summary.version, UUID.randomUUID().toString())
                        .ok("cocina → ${target.name}")
            }
            assertEquals(OrderState.READY, current.summary.state)

            // S02 · el mesero ve el pedido listo en la habitación y lo entrega sin QR ni código.
            val waiterRepo = waiter("mesero")
            val tableBoard = waiterRepo.board().ok("tablero del mesero")
            val table = tableBoard.tables.firstOrNull { t -> t.orders.any { it.id == orderId } }
            assertNotNull("el mesero ve la habitación con el pedido", table)
            assertTrue("la habitación es de tipo mesa", SpaceCopy.isRoom(venueStore.current, table!!.space.type))
            val boardOrder = table.orders.first { it.id == orderId }
            assertEquals(OrderState.READY, boardOrder.state)
            assertFalse("no hay que cobrar antes de entregar", boardOrder.mustCollectBeforeDelivery)
            assertTrue("la entrega de habitación no pide QR (entrega_sin_qr)", boardOrder.deliveryWithoutQr)
            waiterRepo.deliver(boardOrder, null, UUID.randomUUID().toString()).ok("entregar sin código")
            val afterBoard = waiterRepo.board().ok("tablero después de entregar")
            assertTrue(
                "el pedido ya no está listo en el tablero",
                afterBoard.tables.flatMap { it.orders }.none { it.id == orderId && it.state == OrderState.READY },
            )

            // S04 · cargo manual y cuenta completa.
            reception.addCharge(stay.id, "Minibar", "90.00", ChargeCategory.MINIBAR)
            await("cargo manual") { !reception.state.value.busy }
            assertNull("cargo sin error: ${reception.state.value.error}", reception.state.value.error)
            reception.open(ReceptionPane.Checkout(stay.id))
            await("detalle de la cuenta") {
                reception.state.value.detail
                    ?.id == stay.id
            }
            val detail = reception.state.value.detail!!
            val bill = checkoutBreakdown(detail)
            assertTrue(
                "la cuenta trae el minibar",
                bill.charges.any {
                    it.first == "Minibar" &&
                        it.second.compareTo(java.math.BigDecimal("90.00")) == 0
                },
            )
            assertEquals("3000.00", bill.stayAmount.setScale(2).toPlainString())
            val independent =
                bill.stayAmount + bill.roomServiceTotal +
                    bill.charges.fold(java.math.BigDecimal.ZERO) { s, l -> s + l.second }
            assertEquals(
                "el total de la cuenta coincide con la suma de sus partes",
                0,
                independent.compareTo(bill.total),
            )

            // Con pedidos por cobrar y sin sesión de caja, el servidor no deja cerrar; la pantalla lo dice con claridad.
            if (!cashWasOpen) {
                val noCash =
                    hotel(
                        "cajero",
                    ).checkout(
                        stay.id,
                        CheckoutMethod.CARD,
                        UUID.randomUUID().toString(),
                        expectedTotal = bill.total.setScale(2).toPlainString(),
                        expectedVersion = detail.version,
                    )
                assertEquals("ESTABLISHMENT_NOT_RECEIVING", noCash.errorCode())
                assertTrue(
                    "el mensaje manda abrir la caja",
                    staffErrorMessage(noCash.exceptionOrNull(), "x").contains("caja", ignoreCase = true),
                )
                cash.openSession("500.00", UUID.randomUUID().toString()).ok("abrir caja")
                openedCash = true
            }

            // El servidor no acepta transferencia, y un total viejo se rechaza con el total real.
            val badMethod = hotel("cajero").checkout(stay.id, CheckoutMethod.TRANSFER, UUID.randomUUID().toString())
            assertTrue("transferencia debe fallar", badMethod.isFailure)
            val stale =
                hotel(
                    "cajero",
                ).checkout(
                    stay.id,
                    CheckoutMethod.CARD,
                    UUID.randomUUID().toString(),
                    expectedTotal = "1.00",
                    expectedVersion = detail.version,
                )
            assertEquals(HotelErrors.ACCOUNT_TOTAL_CHANGED, stale.errorCode())

            // S04 · cerrar la cuenta con tarjeta (terminal externa) con el ViewModel.
            reception.setMethod(CheckoutMethod.CARD)
            var done = false
            reception.checkout(stay.id) { done = true }
            await("check-out") { done || reception.state.value.error != null }
            assertNull("check-out sin error: ${reception.state.value.error}", reception.state.value.error)
            val closed = hotel("cajero").stay(stay.id).ok("estancia cerrada")
            assertEquals(StayStatus.CHECKED_OUT, closed.status)
            assertEquals("tarjeta", closed.paymentMethod)
            val after = hotel("mesero").rooms().ok("habitaciones después").first { it.id == room.id }
            assertEquals("el check-out deja la habitación por limpiar", RoomCleaning.DIRTY, after.cleaning)
            assertFalse(after.occupied)
        } finally {
            // Deja la habitación como estaba: lista y sin estancia.
            if (!openedCash &&
                !cashWasOpen
            ) {
                cash.openSession("500.00", UUID.randomUUID().toString()).onSuccess { openedCash = true }
            }
            val still = hotel("cajero").stay(stay.id).getOrNull()
            if (still != null && still.status == StayStatus.IN_HOUSE) {
                hotel("cajero").checkout(stay.id, CheckoutMethod.CARD, UUID.randomUUID().toString())
            }
            hotel("mesero").setCleaning(room.id, RoomCleaning.READY, UUID.randomUUID().toString())
            if (openedCash) {
                cash.activeSessionId().getOrNull()?.let {
                    cash.closeSession(it, "500.00", UUID.randomUUID().toString())
                }
            }
        }
    }
}
