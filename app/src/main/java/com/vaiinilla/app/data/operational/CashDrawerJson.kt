package com.vaiinilla.app.data.operational

import com.vaiinilla.app.domain.repository.CashDrawerMovement
import com.vaiinilla.app.domain.repository.CashDrawerMovementType
import com.vaiinilla.app.domain.repository.CashDrawerReport
import com.vaiinilla.app.domain.repository.CashDrawerSummary
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Lee y arma lo de `sesiones-caja/{id}/movimientos` (N11): el resumen del cajón y sus renglones. */
internal object CashDrawerJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun encodeMovement(
        type: CashDrawerMovementType,
        amount: String,
        reason: String,
    ): String =
        buildJsonObject {
            put("tipo", type.wireValue)
            put("monto", amount)
            put("motivo", reason.trim())
        }.toString()

    fun parseReport(raw: String): CashDrawerReport {
        val data = data(raw)
        val summary = data["resumen"] as? JsonObject ?: error("Falta el resumen del cajón.")
        fun amount(key: String): String = text(summary, key) ?: "0.00"
        return CashDrawerReport(
            summary =
                CashDrawerSummary(
                    opening = amount("fondo"),
                    cashSales = amount("cobros_efectivo"),
                    tips = amount("propinas"),
                    reloads = amount("recargas"),
                    cashIn = amount("entradas"),
                    cashOut = amount("salidas"),
                    refunds = amount("devoluciones_efectivo"),
                    expected = text(summary, "efectivo_esperado") ?: error("Falta el efectivo esperado."),
                ),
            movements =
                (data["movimientos"] as? JsonArray)
                    .orEmpty()
                    .mapNotNull { (it as? JsonObject)?.let(::parseMovement) },
        )
    }

    /** `efectivo_esperado` que devuelve el servidor tras anotar un movimiento; null si no llegó. */
    fun parseExpectedAfterMovement(raw: String): String? = text(data(raw), "efectivo_esperado")

    private fun parseMovement(row: JsonObject): CashDrawerMovement? {
        val id = text(row, "id") ?: return null
        val type = text(row, "tipo") ?: return null
        val order = row["pedido"] as? JsonObject
        return CashDrawerMovement(
            id = id,
            type = type,
            outgoing = (row["sale"] as? JsonPrimitive)?.content == "true",
            amount = text(row, "monto") ?: "0.00",
            reason = text(row, "motivo"),
            registeredBy = (row["registrado_por"] as? JsonObject)?.let { text(it, "nombre") },
            orderFolio = order?.let { (it["folio"] as? JsonPrimitive)?.content?.toIntOrNull() },
            isRefund = order != null || text(row, "devolucion_id") != null,
            createdAt = text(row, "creado_en").orEmpty(),
        )
    }

    private fun data(raw: String): JsonObject {
        val root = json.parseToJsonElement(raw) as? JsonObject ?: error("Respuesta del cajón inválida.")
        return root["data"] as? JsonObject ?: error("Respuesta del cajón sin datos.")
    }

    /** Montos y textos: el servidor manda decimales como texto; un número también se acepta. */
    private fun text(
        obj: JsonObject,
        key: String,
    ): String? {
        val value: JsonElement = obj[key] ?: return null
        if (value is JsonNull) return null
        return (value as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
    }
}
