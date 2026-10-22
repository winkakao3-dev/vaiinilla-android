package com.vaiinilla.app.data.counter

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.counter.ReservedDay
import com.vaiinilla.app.domain.counter.SchoolPolicy
import com.vaiinilla.app.domain.counter.StudentCard
import com.vaiinilla.app.domain.counter.StudentCounterRepository
import com.vaiinilla.app.domain.counter.StudentCredential
import com.vaiinilla.app.domain.counter.StudentCredentialKind
import com.vaiinilla.app.domain.counter.StudentPurchaseItem
import com.vaiinilla.app.domain.model.OrderDetail

/**
 * Los errores se quedan como `ApiClientException`: la cola sin conexión necesita su estado HTTP para saber
 * si guarda la venta, y Caja enseña el mensaje del servidor (ya escrito para la cajera).
 */
class RemoteStudentCounterRepository(
    private val apiClient: VaiinillaApiClient,
    private val orderJson: OrderContractJson,
    private val json: StudentCounterJson = StudentCounterJson(),
) : StudentCounterRepository {
    override fun search(text: String): Result<List<StudentCard>> =
        apiClient.get(STUDENTS, mapOf("q" to text.trim())).mapCatching(json::parseCards)

    override fun find(credential: String): Result<StudentCard?> =
        apiClient
            .get(STUDENTS, mapOf("credencial" to credential.trim()))
            .mapCatching { json.parseCards(it).firstOrNull() }

    override fun purchase(
        studentId: String,
        items: List<StudentPurchaseItem>,
        pin: String?,
        idempotencyKey: String,
    ): Result<OrderDetail> =
        apiClient
            .post(
                "$STUDENTS/$studentId/compras",
                json.encodePurchase(items, pin),
                mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching(orderJson::parseOrderDetail)

    override fun assignCredential(
        studentId: String,
        kind: StudentCredentialKind,
        nfcUid: String?,
        idempotencyKey: String,
    ): Result<StudentCredential> =
        apiClient
            .post(
                "$STUDENTS/$studentId/credenciales",
                json.encodeCredential(kind, nfcUid),
                mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching(json::parseCredential)

    override fun deliver(
        orderId: String,
        expectedVersion: Int,
        credential: String?,
        idempotencyKey: String,
    ): Result<OrderDetail> =
        apiClient
            .post(
                "pedidos/$orderId/transiciones",
                json.encodeDelivery(expectedVersion, credential),
                mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching(orderJson::parseOrderDetail)

    override fun reservedDay(date: String): Result<ReservedDay> =
        apiClient.get("menu-dias/$date/apartados").mapCatching(json::parseReservedDay)

    override fun schoolPolicy(): Result<SchoolPolicy> =
        apiClient.get("estado-operativo").mapCatching(json::parseSchoolPolicy)

    private companion object {
        const val STUDENTS = "alumnos"
    }
}
