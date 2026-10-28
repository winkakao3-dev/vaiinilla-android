package com.vaiinilla.app.data.counter

import com.vaiinilla.app.domain.counter.CachedStudentCard
import com.vaiinilla.app.domain.counter.OfflineCounterSnapshot
import com.vaiinilla.app.domain.counter.OfflineStudentPurchase
import com.vaiinilla.app.domain.counter.ReservedDay
import com.vaiinilla.app.domain.counter.SchoolPolicy
import com.vaiinilla.app.domain.counter.StudentAllergens
import com.vaiinilla.app.domain.counter.StudentCard
import com.vaiinilla.app.domain.counter.StudentCredential
import com.vaiinilla.app.domain.counter.StudentCredentialKind
import com.vaiinilla.app.domain.counter.StudentPurchaseItem
import com.vaiinilla.app.domain.counter.StudentTodayOrder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.math.BigDecimal
import java.time.Instant

/** Contratos de `/alumnos`, `/menu-dias/{fecha}/apartados` y la cola sin conexión que se guarda en el teléfono. */
class StudentCounterJson {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }

    fun parseCards(raw: String): List<StudentCard> =
        json.decodeFromString<Envelope<List<StudentCardDto>>>(raw).requireData().map { it.toDomain() }

    fun encodePurchase(
        items: List<StudentPurchaseItem>,
        pin: String?,
    ): String =
        json.encodeToString(
            PurchaseRequestDto.serializer(),
            PurchaseRequestDto(
                items = items.map { PurchaseItemDto(it.productId, it.quantity, it.optionIds) },
                pin = pin,
            ),
        )

    fun encodeCredential(
        kind: StudentCredentialKind,
        nfcUid: String?,
    ): String =
        json.encodeToString(
            CredentialRequestDto.serializer(),
            CredentialRequestDto(kind.wire, if (kind == StudentCredentialKind.NFC) nfcUid else null),
        )

    fun parseCredential(raw: String): StudentCredential {
        val dto = json.decodeFromString<Envelope<CredentialResponseDto>>(raw).requireData().credential
        val kind = StudentCredentialKind.entries.firstOrNull { it.wire == dto.kind }
        requireNotNull(kind) { "Tipo de credencial desconocido: ${dto.kind}" }
        return StudentCredential(id = dto.id, kind = kind, printableValue = dto.value)
    }

    /** Con `credencial_alumno` en lugar de `qr_token`: la credencial del alumno hace de QR del pedido. */
    fun encodeDelivery(
        expectedVersion: Int,
        credential: String?,
    ): String =
        json.encodeToString(
            DeliveryRequestDto.serializer(),
            DeliveryRequestDto(expectedVersion = expectedVersion, credential = credential),
        )

    fun parseReservedDay(raw: String): ReservedDay {
        val dto = json.decodeFromString<Envelope<ReservedDayDto>>(raw).requireData()
        return ReservedDay(
            date = dto.date.take(10),
            orders = dto.orders,
            closedReason = dto.closedReason,
            lines = dto.products.map { ReservedDay.Line(it.productId, it.name, it.quantity, it.orders) },
        )
    }

    fun parseSchoolPolicy(raw: String): SchoolPolicy {
        val dto = json.decodeFromString<Envelope<SchoolPolicyDto>>(raw).requireData()
        return SchoolPolicy(isSchool = dto.isSchool, deliveryRequiresQr = dto.deliveryRequiresQr)
    }

    fun encodeSnapshot(snapshot: OfflineCounterSnapshot): String =
        json.encodeToString(SnapshotDto.serializer(), SnapshotDto.of(snapshot))

    fun decodeSnapshot(raw: String): OfflineCounterSnapshot = json.decodeFromString<SnapshotDto>(raw).toDomain()

    private fun <T> Envelope<T>.requireData(): T {
        require(error == null || error is kotlinx.serialization.json.JsonNull) { "La respuesta contiene error." }
        return requireNotNull(data) { "La respuesta no trae data." }
    }

    @Serializable
    private data class Envelope<T>(
        val data: T? = null,
        val error: JsonElement? = null,
    )

    @Serializable
    private data class StudentCardDto(
        @SerialName("usuario_id") val userId: String,
        @SerialName("nombre") val name: String,
        @SerialName("matricula") val studentId: String? = null,
        @SerialName("grupo") val group: String? = null,
        @SerialName("tiene_cuenta") val hasAccount: Boolean = false,
        @SerialName("saldo") val balance: String? = null,
        @SerialName("gastado_hoy") val spentToday: String? = null,
        @SerialName("limite_diario") val dailyLimit: String? = null,
        @SerialName("alergenos") val allergens: List<String> = emptyList(),
        @SerialName("pedidos_hoy") val todayOrders: List<TodayOrderDto> = emptyList(),
        @SerialName("pide_pin") val requiresPin: Boolean = false,
        @SerialName("nota_tutor") val guardianNote: String? = null,
        // Un servidor sin controles del tutor no los manda: no se bloquea nada.
        @SerialName("productos_bloqueados") val blockedProducts: List<Int> = emptyList(),
        @SerialName("categorias_bloqueadas") val blockedCategories: List<Int> = emptyList(),
        @SerialName("productos_con_alergeno") val allergenProducts: List<Int> = emptyList(),
        @SerialName("productos_sin_revisar") val unreviewedProducts: List<Int> = emptyList(),
    ) {
        fun toDomain(): StudentCard =
            StudentCard(
                userId = userId,
                name = name,
                studentId = studentId?.takeIf { it.isNotBlank() },
                group = group?.takeIf { it.isNotBlank() },
                hasOwnAccount = hasAccount,
                balance = money(balance) ?: BigDecimal.ZERO,
                spentToday = money(spentToday) ?: BigDecimal.ZERO,
                dailyLimit = money(dailyLimit),
                allergens = StudentAllergens.known(allergens),
                todayOrders =
                    todayOrders.map {
                        StudentTodayOrder(
                            id = it.id,
                            folio = it.folio,
                            statusWire = it.status,
                            version = it.version,
                            total = money(it.total) ?: BigDecimal.ZERO,
                            summary = it.summary.orEmpty(),
                        )
                    },
                requiresPin = requiresPin,
                guardianNote = guardianNote?.trim()?.takeIf { it.isNotEmpty() },
                blockedProducts = blockedProducts.toSet(),
                blockedCategories = blockedCategories.toSet(),
                allergenProducts = allergenProducts.toSet(),
                unreviewedProducts = unreviewedProducts.toSet(),
            )
    }

    @Serializable
    private data class TodayOrderDto(
        val id: String,
        val folio: Int,
        @SerialName("estado") val status: String,
        val version: Int,
        val total: String,
        @SerialName("resumen") val summary: String? = null,
    )

    @Serializable
    private data class PurchaseItemDto(
        @SerialName("producto_id") val productId: Int,
        @SerialName("cantidad") val quantity: Int,
        @SerialName("opcion_ids") val optionIds: List<Int> = emptyList(),
    )

    @Serializable
    private data class PurchaseRequestDto(
        val items: List<PurchaseItemDto>,
        val pin: String? = null,
    )

    @Serializable
    private data class CredentialRequestDto(
        @SerialName("tipo") val kind: String,
        @SerialName("valor") val value: String? = null,
    )

    @Serializable
    private data class CredentialDto(
        val id: String,
        @SerialName("tipo") val kind: String,
        @SerialName("valor") val value: String? = null,
    )

    @Serializable
    private data class CredentialResponseDto(
        @SerialName("credencial") val credential: CredentialDto,
    )

    @Serializable
    private data class DeliveryRequestDto(
        @SerialName("estado_objetivo") val targetState: String = "entregado",
        @SerialName("version_esperada") val expectedVersion: Int,
        @SerialName("credencial_alumno") val credential: String? = null,
    )

    @Serializable
    private data class ReservedLineDto(
        @SerialName("producto_id") val productId: Int,
        @SerialName("nombre") val name: String,
        @SerialName("cantidad") val quantity: Int,
        @SerialName("pedidos") val orders: Int,
    )

    @Serializable
    private data class ReservedDayDto(
        @SerialName("fecha") val date: String,
        @SerialName("pedidos") val orders: Int = 0,
        @SerialName("sin_servicio") val closedReason: String? = null,
        @SerialName("productos") val products: List<ReservedLineDto> = emptyList(),
    )

    @Serializable
    private data class SchoolPolicyDto(
        /** Un servidor anterior no lo envía: no es escuela. */
        @SerialName("modo_escuela") val isSchool: Boolean = false,
        @SerialName("entrega_requiere_qr") val deliveryRequiresQr: Boolean = true,
    )

    // Lo que se guarda en el teléfono. Los montos van como texto para no perder centavos.

    @Serializable
    private data class CachedCardDto(
        val userId: String,
        val name: String,
        val studentId: String? = null,
        val group: String? = null,
        val balance: String,
        val spentToday: String,
        val dailyLimit: String? = null,
        val allergens: List<String> = emptyList(),
        val requiresPin: Boolean = false,
    )

    @Serializable
    private data class StoredItemDto(
        val productId: Int,
        val quantity: Int,
        val optionIds: List<Int> = emptyList(),
    )

    @Serializable
    private data class StoredPurchaseDto(
        val id: String,
        val studentId: String,
        val studentName: String,
        val items: List<StoredItemDto>,
        val total: String,
        val createdAt: String,
        val problem: String? = null,
    )

    @Serializable
    private data class SnapshotDto(
        val day: String,
        val cards: List<CachedCardDto> = emptyList(),
        val purchases: List<StoredPurchaseDto> = emptyList(),
    ) {
        fun toDomain(): OfflineCounterSnapshot =
            OfflineCounterSnapshot(
                day = day,
                cards =
                    cards.map {
                        CachedStudentCard(
                            userId = it.userId,
                            name = it.name,
                            studentId = it.studentId,
                            group = it.group,
                            balance = BigDecimal(it.balance),
                            spentToday = BigDecimal(it.spentToday),
                            dailyLimit = it.dailyLimit?.let(::BigDecimal),
                            allergens = it.allergens,
                            requiresPin = it.requiresPin,
                        )
                    },
                purchases =
                    purchases.map { purchase ->
                        OfflineStudentPurchase(
                            id = purchase.id,
                            studentId = purchase.studentId,
                            studentName = purchase.studentName,
                            items = purchase.items.map { StudentPurchaseItem(it.productId, it.quantity, it.optionIds) },
                            total = BigDecimal(purchase.total),
                            createdAt = Instant.parse(purchase.createdAt),
                            problem = purchase.problem,
                        )
                    },
            )

        companion object {
            fun of(snapshot: OfflineCounterSnapshot): SnapshotDto =
                SnapshotDto(
                    day = snapshot.day,
                    cards =
                        snapshot.cards.map {
                            CachedCardDto(
                                userId = it.userId,
                                name = it.name,
                                studentId = it.studentId,
                                group = it.group,
                                balance = it.balance.toPlainString(),
                                spentToday = it.spentToday.toPlainString(),
                                dailyLimit = it.dailyLimit?.toPlainString(),
                                allergens = it.allergens,
                                requiresPin = it.requiresPin,
                            )
                        },
                    purchases =
                        snapshot.purchases.map { purchase ->
                            StoredPurchaseDto(
                                id = purchase.id,
                                studentId = purchase.studentId,
                                studentName = purchase.studentName,
                                items = purchase.items.map { StoredItemDto(it.productId, it.quantity, it.optionIds) },
                                total = purchase.total.toPlainString(),
                                createdAt = purchase.createdAt.toString(),
                                problem = purchase.problem,
                            )
                        },
                )
        }
    }

    private companion object {
        /** Los montos del servidor vienen como texto ("390.00", y "0" en `gastado_hoy`). */
        fun money(value: String?): BigDecimal? = value?.trim()?.toBigDecimalOrNull()
    }
}
