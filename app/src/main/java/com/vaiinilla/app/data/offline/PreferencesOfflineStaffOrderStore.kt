package com.vaiinilla.app.data.offline

import android.content.Context
import androidx.core.content.edit
import com.vaiinilla.app.domain.offline.OfflineStaffOrder
import com.vaiinilla.app.domain.offline.OfflineStaffOrderStore
import com.vaiinilla.app.domain.offline.OfflineStaffOrdersSnapshot
import com.vaiinilla.app.domain.offline.OfflineStaffScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import kotlin.random.Random

/**
 * Los pedidos que Mesero o Caja tomó sin internet. Viven solo en este teléfono y sobreviven a matar la app,
 * igual que la cola de la cafetería ([com.vaiinilla.app.data.counter.PreferencesOfflineCounterStore]).
 */
class PreferencesOfflineStaffOrderStore(
    context: Context,
) : OfflineStaffOrderStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun load(
        scope: OfflineStaffScope,
        today: String,
    ): OfflineStaffOrdersSnapshot {
        // Un archivo que no se entiende no debe tumbar la app; se empieza de cero.
        val stored =
            preferences
                .getString(key(scope), null)
                ?.let { raw -> runCatching { decode(raw) }.getOrNull() }
        return (stored ?: OfflineStaffOrdersSnapshot(day = today)).startingDay(today)
    }

    override fun save(
        scope: OfflineStaffScope,
        snapshot: OfflineStaffOrdersSnapshot,
    ) {
        // commit: el pedido tiene que estar en disco antes de intentar mandarlo.
        preferences.edit(commit = true) { putString(key(scope), encode(snapshot)) }
    }

    override fun deviceTag(): Int {
        // Tres dígitos: con dos, dos meseros del mismo local compartían marca con frecuencia (1 en 100 por par).
        val stored = preferences.getInt(DEVICE_TAG_3, -1)
        if (stored in 0..999) return stored
        val tag = Random.nextInt(0, 1000)
        preferences.edit(commit = true) { putInt(DEVICE_TAG_3, tag) }
        return tag
    }

    override fun scopesWithOrders(): List<OfflineStaffScope> =
        preferences.all.keys
            .filter { it.startsWith(KEY_PREFIX) }
            .map { OfflineStaffScope(membershipId = it.removePrefix(KEY_PREFIX)) }
            .filter { scope -> load(scope, "").orders.any { it.problem == null } }

    private fun key(scope: OfflineStaffScope): String = "$KEY_PREFIX${scope.membershipId.lowercase()}"

    internal fun encode(snapshot: OfflineStaffOrdersSnapshot): String =
        json.encodeToString(SnapshotDto.serializer(), SnapshotDto.of(snapshot))

    internal fun decode(raw: String): OfflineStaffOrdersSnapshot =
        json.decodeFromString(SnapshotDto.serializer(), raw).toDomain()

    @Serializable
    private data class SnapshotDto(
        val day: String,
        val nextSequence: Int = 1,
        val orders: List<OrderDto> = emptyList(),
    ) {
        fun toDomain(): OfflineStaffOrdersSnapshot =
            OfflineStaffOrdersSnapshot(day = day, nextSequence = nextSequence, orders = orders.map { it.toDomain() })

        companion object {
            fun of(snapshot: OfflineStaffOrdersSnapshot): SnapshotDto =
                SnapshotDto(
                    day = snapshot.day,
                    nextSequence = snapshot.nextSequence,
                    orders = snapshot.orders.map { OrderDto.of(it) },
                )
        }
    }

    @Serializable
    private data class OrderDto(
        val id: String,
        val idempotencyKey: String,
        val localFolio: String,
        val body: String,
        val placeLabel: String,
        val summary: String,
        val capturedAt: String,
        val problem: String? = null,
    ) {
        fun toDomain(): OfflineStaffOrder =
            OfflineStaffOrder(
                id = id,
                idempotencyKey = idempotencyKey,
                localFolio = localFolio,
                body = body,
                placeLabel = placeLabel,
                summary = summary,
                capturedAt = Instant.parse(capturedAt),
                problem = problem,
            )

        companion object {
            fun of(order: OfflineStaffOrder): OrderDto =
                OrderDto(
                    id = order.id,
                    idempotencyKey = order.idempotencyKey,
                    localFolio = order.localFolio,
                    body = order.body,
                    placeLabel = order.placeLabel,
                    summary = order.summary,
                    capturedAt = order.capturedAt.toString(),
                    problem = order.problem,
                )
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "vaiinilla_offline_staff_orders"
        const val DEVICE_TAG_3 = "device-tag-3"
        const val KEY_PREFIX = "offline-orders-"
        val json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            }
    }
}
