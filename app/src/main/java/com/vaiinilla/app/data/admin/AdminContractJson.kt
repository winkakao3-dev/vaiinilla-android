package com.vaiinilla.app.data.admin

import com.vaiinilla.app.core.network.ApiJson
import com.vaiinilla.app.domain.admin.AdminCategoryInput
import com.vaiinilla.app.domain.admin.AdminCount
import com.vaiinilla.app.domain.admin.AdminDailySales
import com.vaiinilla.app.domain.admin.AdminMetric
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminProductInput
import com.vaiinilla.app.domain.admin.AdminSummary
import com.vaiinilla.app.domain.admin.AdminUnpaidAccount
import com.vaiinilla.app.domain.admin.AdminUnpaidAccounts
import com.vaiinilla.app.domain.admin.AdminUnpaidOrder
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.OrderState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/** Contratos de la administración: el resumen, las cuentas sin pagar y la escritura del menú. */
class AdminContractJson {
    private val json =
        ApiJson.omitNulls

    // El backend exige los textos opcionales del producto aunque vayan en null; los `id` nulos (grupos y opciones
    // nuevos) llevan default y no se escriben, porque un `id: null` lo rechaza.
    private val writer = ApiJson.plain

    fun parseSummary(
        raw: String,
        period: AdminPeriod,
    ): AdminSummary {
        val data = json.decodeFromString<Envelope<SummaryDto>>(raw).data
        val totals = data.resumen
        return AdminSummary(
            period = period,
            sales = totals.ventasTotales,
            orders = totals.pedidos,
            averageTicket = totals.ticketPromedio,
            productsSold = totals.productosVendidos,
            tips = totals.propinas,
            walletReloads = totals.recargas,
            canceledOrders = totals.pedidosCancelados,
            commissions = totals.comisiones,
            dailySales = data.ventasPorDia.map { AdminDailySales(it.fecha, it.ventas, it.pedidos) },
            paymentMethods = data.metodosPago.map { AdminMetric(paymentMethodLabel(it.metodo), it.ventas, it.pedidos) },
            ordersByStatus =
                data.pedidosPorEstado.map { metric ->
                    AdminCount(
                        OrderState.entries.firstOrNull { it.wireValue == metric.estado }?.label ?: metric.estado,
                        metric.pedidos,
                        key = metric.estado,
                    )
                },
            topProducts = data.productos.map { AdminCount(it.nombre, it.cantidad, amount = it.ventas) },
        )
    }

    fun parseUnpaidAccounts(raw: String): AdminUnpaidAccounts {
        val data = json.decodeFromString<Envelope<UnpaidAccountsDto>>(raw).data
        return AdminUnpaidAccounts(
            accounts =
                data.cuentas.map {
                    // El servidor manda la lista en pedidos_detalle; antes venía en pedidos (#204).
                    val orderLines = unpaidOrders(it.pedidosDetalle ?: it.pedidos)
                    AdminUnpaidAccount(
                        spaceId = it.espacio.id,
                        spaceName = it.espacio.nombre,
                        orders = it.numPedidos ?: (it.pedidos as? JsonPrimitive)?.intOrNull ?: orderLines.size,
                        total = it.total,
                        hours = it.horas,
                        abandoned = it.abandonada,
                        // R33: lo abonado ya entró a la caja; «Por cobrar» es lo que falta (restante).
                        paid = it.abonado ?: "0.00",
                        remaining = it.restante ?: it.total,
                        sessionId = it.sesionId,
                        orderLines = orderLines,
                    )
                },
            total = data.total,
            abandoned = data.abandonadas,
            remaining = data.restante ?: data.total,
        )
    }

    fun parseCursor(raw: String): String? = json.decodeFromString<CursorEnvelope>(raw).meta?.cursor

    fun parseCategory(raw: String): Category {
        val data = json.decodeFromString<Envelope<CategoryDto>>(raw).data
        return Category(
            id = requireNotNull(data.id) { "La categoría llegó sin id." },
            name = data.nombre,
            order = data.orden,
        )
    }

    fun encodeCategory(input: AdminCategoryInput): String =
        writer.encodeToString(CategoryDto(id = null, nombre = input.name.trim(), orden = input.order))

    fun encodeProduct(input: AdminProductInput): String =
        writer.encodeToString(
            ProductWriteDto(
                categoriaId = input.categoryId,
                estacion = input.preparationStation.wireValue,
                nombre = input.name.trim(),
                descripcion = input.description.trim().ifEmpty { null },
                ingredientes = input.ingredients.trim().ifEmpty { null },
                alergenos = input.allergens.trim().ifEmpty { null },
                tiempo = input.estimatedTimeMinutes,
                precio = input.counterPrice,
                disponible = input.available,
                grupos =
                    input.optionGroups.map { group ->
                        GroupWriteDto(
                            id = group.id,
                            nombre = group.name.trim(),
                            min = group.minimumSelections,
                            max = group.maximumSelections,
                            opciones =
                                group.options.map { option ->
                                    OptionWriteDto(
                                        id = option.id,
                                        nombre = option.name.trim(),
                                        precioExtra = option.extraPrice,
                                    )
                                },
                        )
                    },
                // Sin la clave, el backend deja los alérgenos como estaban.
                alergenosCodigos = input.allergenCodes?.sorted()?.takeUnless { input.markUnreviewed },
                alergenosRevisados = false.takeIf { input.markUnreviewed },
                diasPreparacion = input.preparation?.days,
                maximoPorDia = input.preparation?.let { it.maximumPerDay?.let(::JsonPrimitive) ?: JsonNull },
            ),
        )

    companion object {
        fun paymentMethodLabel(code: String): String =
            when (code) {
                "efectivo" -> "Efectivo"
                "saldo" -> "Saldo"
                "stripe" -> "Tarjeta en la app"
                "terminal" -> "Terminal"
                else -> code.replaceFirstChar { it.uppercase() }
            }
    }
}

@Serializable
private data class Envelope<T>(
    val data: T,
    val error: JsonElement? = null,
)

@Serializable
private data class CursorEnvelope(
    val meta: CursorMetaDto? = null,
)

@Serializable
private data class CursorMetaDto(
    val cursor: String? = null,
)

@Serializable
private data class SummaryDto(
    val resumen: SummaryTotalsDto,
    @SerialName("ventas_por_dia") val ventasPorDia: List<DailyDto> = emptyList(),
    @SerialName("metodos_pago") val metodosPago: List<PaymentDto> = emptyList(),
    @SerialName("pedidos_por_estado") val pedidosPorEstado: List<StatusDto> = emptyList(),
    val productos: List<ProductMetricDto> = emptyList(),
)

@Serializable
private data class SummaryTotalsDto(
    @SerialName("ventas_totales") val ventasTotales: String,
    val pedidos: Int,
    @SerialName("ticket_promedio") val ticketPromedio: String,
    @SerialName("productos_vendidos") val productosVendidos: Int = 0,
    val recargas: String = "0.00",
    val propinas: String? = null,
    @SerialName("pedidos_cancelados") val pedidosCancelados: Int? = null,
    val comisiones: String? = null,
)

@Serializable
private data class DailyDto(
    val fecha: String,
    val ventas: String,
    val pedidos: Int,
)

@Serializable
private data class PaymentDto(
    val metodo: String,
    val ventas: String,
    val pedidos: Int,
)

@Serializable
private data class StatusDto(
    val estado: String,
    val pedidos: Int,
)

@Serializable
private data class ProductMetricDto(
    val nombre: String,
    val cantidad: Int,
    val ventas: String? = null,
)

@Serializable
private data class UnpaidAccountsDto(
    val cuentas: List<UnpaidAccountDto> = emptyList(),
    val total: String = "0.00",
    val abandonadas: Int = 0,
    /** R33: sin lo abonado; un servidor anterior no lo manda. */
    val restante: String? = null,
)

@Serializable
private data class UnpaidAccountDto(
    val espacio: UnpaidSpaceDto,
    /** El conteo; con #204 vino un tiempo como lista (R1). Se aceptan las dos formas. */
    val pedidos: JsonElement? = null,
    @SerialName("num_pedidos") val numPedidos: Int? = null,
    /** La lista de pedidos con su restante; `pedidos` vuelve a ser el conteo. */
    @SerialName("pedidos_detalle") val pedidosDetalle: JsonElement? = null,
    val total: String,
    val horas: Int = 0,
    val abandonada: Boolean = false,
    /** R33: abonos sin liquidar y lo que falta; un servidor anterior no los manda. */
    val abonado: String? = null,
    val restante: String? = null,
    @SerialName("sesion_id") val sesionId: String? = null,
)

/** Los pedidos de una cuenta sin pagar; un conteo (servidor anterior) da lista vacía. */
private fun unpaidOrders(raw: JsonElement?): List<AdminUnpaidOrder> =
    (raw as? JsonArray).orEmpty().mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val id = (obj["id"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
        val total = (obj["total"] as? JsonPrimitive)?.contentOrNull ?: "0.00"
        AdminUnpaidOrder(
            id = id,
            folio = (obj["folio"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            total = total,
            remaining = (obj["restante"] as? JsonPrimitive)?.contentOrNull ?: total,
        )
    }

@Serializable
private data class UnpaidSpaceDto(
    val id: Int,
    val nombre: String,
)

@Serializable
private data class CategoryDto(
    val id: Int? = null,
    val nombre: String,
    val orden: Int,
)

@Serializable
private data class ProductWriteDto(
    @SerialName("categoria_id") val categoriaId: Int,
    @SerialName("estacion_preparacion") val estacion: String,
    val nombre: String,
    val descripcion: String?,
    val ingredientes: String?,
    val alergenos: String?,
    @SerialName("tiempo_estimado_min") val tiempo: Int,
    @SerialName("precio_mostrador") val precio: String,
    val disponible: Boolean,
    @SerialName("grupos_opcion") val grupos: List<GroupWriteDto>,
    @SerialName("alergenos_codigos") val alergenosCodigos: List<String>? = null,
    @SerialName("alergenos_revisados") val alergenosRevisados: Boolean? = null,
    @SerialName("dias_preparacion") val diasPreparacion: Int? = null,
    // JsonNull borra el tope; sin la clave queda como estaba.
    @SerialName("maximo_por_dia") val maximoPorDia: JsonElement? = null,
)

@Serializable
private data class GroupWriteDto(
    val id: Int? = null,
    val nombre: String,
    @SerialName("min_selecciones") val min: Int,
    @SerialName("max_selecciones") val max: Int,
    val opciones: List<OptionWriteDto>,
)

@Serializable
private data class OptionWriteDto(
    val id: Int? = null,
    val nombre: String,
    @SerialName("precio_extra") val precioExtra: String,
)
