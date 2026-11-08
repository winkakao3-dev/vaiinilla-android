package com.vaiinilla.app.data.cashcut

import com.vaiinilla.app.core.network.ApiJson
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.core.network.mapApiFailure
import com.vaiinilla.app.domain.repository.CatalogRepositoryException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.time.LocalDate

/** Si lo contado al cerrar cuadra con lo esperado, sobra o falta. */
enum class CashCountResult(
    val wire: String,
    val label: String,
) {
    BALANCED("cuadra", "Cuadra"),
    OVER("sobra", "Sobra"),
    SHORT("falta", "Falta"),
    ;

    companion object {
        fun of(wire: String?): CashCountResult? = entries.firstOrNull { it.wire == wire }
    }
}

/** El efectivo del cajón de un turno o de todo el día. Los montos llegan como texto con dos decimales. */
data class CutCash(
    val float: String,
    val cashSales: String,
    val tips: String,
    val reloads: String,
    val cashIn: String,
    val cashOut: String,
    val refunds: String,
    val expected: String,
    /** Lo contado al cerrar; null con el turno abierto. */
    val counted: String?,
    /** Contado menos esperado: positivo si sobra. */
    val difference: String?,
    val result: CashCountResult?,
)

data class CutShift(
    val id: String,
    val date: String,
    /** ISO-8601 en UTC. */
    val openedAt: String?,
    val closedAt: String?,
    val automatic: Boolean,
    val openedBy: String?,
    val closedBy: String?,
    val cash: CutCash,
) {
    val open: Boolean get() = closedAt == null
}

data class SalesSummary(
    val sales: String,
    val orders: Int,
    val averageTicket: String,
    val items: Int,
    val itemsAmount: String,
    val discounts: String,
    val delivery: String,
    val rentals: String,
)

/** Cómo se dice cada forma de pago en pantalla y en el CSV. */
fun paymentMethodLabel(code: String): String =
    when (code) {
        "efectivo" -> "Efectivo"
        "terminal" -> "Terminal"
        "stripe" -> "Tarjeta en línea"
        "saldo" -> "Saldo"
        "transferencia" -> "Transferencia"
        else -> code.replaceFirstChar { it.uppercase() }
    }

fun staffRoleLabel(code: String?): String =
    when (code) {
        "admin" -> "Administración"
        "cajero" -> "Caja"
        "mesero" -> "Mesero"
        "cocina" -> "Cocina"
        else -> ""
    }

data class PaymentLine(
    val method: String,
    val orders: Int,
    val amount: String,
) {
    val label: String get() = paymentMethodLabel(method)
}

data class CategoryLine(
    val name: String,
    val quantity: Int,
    val amount: String,
)

data class ProductLine(
    val name: String,
    val category: String,
    val quantity: Int,
    val amount: String,
)

/** Quién tomó los pedidos; [userId] null son los que hizo el cliente con su app o el QR. */
data class StaffLine(
    val userId: String?,
    val name: String?,
    val role: String?,
    val orders: Int,
    val sales: String,
    val averageTicket: String?,
) {
    val label: String get() = if (userId == null) "Clientes (app o QR)" else name ?: "Sin nombre"
}

/** Mesa, pista o cancha; [spaceId] null es para llevar o mostrador. */
data class SpaceLine(
    val spaceId: Long?,
    val name: String?,
    val type: String?,
    val orders: Int,
    val sales: String,
    val rentals: String,
    val consumption: String,
) {
    val label: String get() = if (spaceId == null) "Para llevar o mostrador" else name ?: "Sin nombre"
}

data class DayLine(
    val date: String,
    val orders: Int,
    val sales: String,
)

data class Tips(
    val total: String,
    val cash: String,
    val terminal: String,
)

data class Cancelled(
    val orders: Int,
    val amount: String,
)

/** Lo que se le devolvió al cliente. Informativo: ya no está en las ventas. */
data class Refunds(
    val rejectedItems: String = "0.00",
    val reservations: String = "0.00",
    val handedBack: String = "0.00",
)

/** Lo cobrado el día sin caja abierta (en línea); ya va sumado en las ventas del día. */
data class OutsideShift(
    val orders: Int = 0,
    val sales: String = "0.00",
)

/** Lo mismo para el corte y el reporte: ventas y sus desgloses. */
data class SalesBreakdown(
    val summary: SalesSummary,
    val payments: List<PaymentLine>,
    val tips: Tips,
    val cancelled: Cancelled,
    val refunds: Refunds = Refunds(),
    val categories: List<CategoryLine>,
    val products: List<ProductLine>,
    val staff: List<StaffLine>,
    val spaces: List<SpaceLine>,
    val days: List<DayLine>,
)

/** Corte de un turno: [final] cuando la caja ya cerró; si no, es parcial (lo que va del turno). */
data class ShiftCut(
    val final: Boolean,
    val shift: CutShift,
    val breakdown: SalesBreakdown,
    val generatedAt: String?,
)

data class DayCut(
    val date: String,
    val final: Boolean,
    val shifts: List<CutShift>,
    val cash: CutCash,
    val breakdown: SalesBreakdown,
    val generatedAt: String?,
    val outsideShift: OutsideShift = OutsideShift(),
)

data class AccountingReport(
    val business: String?,
    val from: String,
    val to: String,
    val breakdown: SalesBreakdown,
)

/** `/sesiones-caja/{id}/corte`, `/sesiones-caja/cortes` y `/reportes/contable` (backend PR #201). */
interface CashCutRepository {
    fun shiftCut(sessionId: String): Result<ShiftCut>

    /** null = hoy en la hora del negocio. */
    fun dayCut(date: LocalDate?): Result<DayCut>

    fun accounting(
        from: LocalDate,
        to: LocalDate,
    ): Result<AccountingReport>

    /** El mismo reporte en CSV, listo para Excel. */
    fun accountingCsv(
        from: LocalDate,
        to: LocalDate,
    ): Result<String>

    /** A qué hora empieza el día del negocio ("05:00"); null si el servidor todavía no la conoce. */
    fun dayStart(): Result<String?>

    /** Cambia la hora en que empieza el día del negocio y devuelve la que quedó. */
    fun setDayStart(hhmm: String): Result<String>
}

@Serializable
private data class Envelope<T>(
    val data: T,
)

@Serializable
private data class CashDto(
    val fondo: String = "0.00",
    @SerialName("cobros_efectivo") val cobrosEfectivo: String = "0.00",
    val propinas: String = "0.00",
    val recargas: String = "0.00",
    val entradas: String = "0.00",
    val salidas: String = "0.00",
    @SerialName("devoluciones_efectivo") val devoluciones: String = "0.00",
    @SerialName("efectivo_esperado") val esperado: String = "0.00",
    val contado: String? = null,
    val diferencia: String? = null,
    val arqueo: String? = null,
) {
    fun toDomain() =
        CutCash(
            fondo,
            cobrosEfectivo,
            propinas,
            recargas,
            entradas,
            salidas,
            devoluciones,
            esperado,
            contado,
            diferencia,
            CashCountResult.of(arqueo),
        )
}

@Serializable
private data class PersonDto(
    val nombre: String? = null,
)

@Serializable
private data class ShiftDto(
    val id: String,
    @SerialName("fecha_operativa") val fecha: String = "",
    @SerialName("abierta_en") val abiertaEn: String? = null,
    @SerialName("cerrada_en") val cerradaEn: String? = null,
    @SerialName("cierre_automatico") val automatico: Boolean = false,
    @SerialName("abierta_por") val abiertaPor: PersonDto? = null,
    @SerialName("cerrada_por") val cerradaPor: PersonDto? = null,
    val efectivo: CashDto = CashDto(),
) {
    fun toDomain() =
        CutShift(
            id = id,
            date = fecha,
            openedAt = abiertaEn,
            closedAt = cerradaEn,
            automatic = automatico,
            openedBy = abiertaPor?.nombre,
            closedBy = cerradaPor?.nombre,
            cash = efectivo.toDomain(),
        )
}

@Serializable
private data class SummaryDto(
    val ventas: String = "0.00",
    val pedidos: Int = 0,
    @SerialName("ticket_promedio") val ticket: String = "0.00",
    val articulos: Int = 0,
    @SerialName("importe_articulos") val importeArticulos: String = "0.00",
    val descuentos: String = "0.00",
    val envios: String = "0.00",
    val rentas: String = "0.00",
)

@Serializable
private data class PaymentDto(
    val metodo: String,
    val pedidos: Int = 0,
    val monto: String = "0.00",
)

@Serializable
private data class TipsDto(
    val total: String = "0.00",
    val efectivo: String = "0.00",
    val terminal: String = "0.00",
)

@Serializable
private data class CancelledDto(
    val pedidos: Int = 0,
    val monto: String = "0.00",
)

@Serializable
private data class RefundsDto(
    @SerialName("articulos_rechazados") val articulosRechazados: String = "0.00",
    @SerialName("reembolsos_reserva") val reembolsosReserva: String = "0.00",
    @SerialName("devoluciones_entregadas") val devolucionesEntregadas: String = "0.00",
)

@Serializable
private data class OutsideShiftDto(
    val pedidos: Int = 0,
    val ventas: String = "0.00",
)

@Serializable
private data class CategoryDto(
    val categoria: String = "Sin categoría",
    val cantidad: Int = 0,
    val importe: String = "0.00",
)

@Serializable
private data class ProductDto(
    val producto: String = "",
    val categoria: String = "Sin categoría",
    val cantidad: Int = 0,
    val importe: String = "0.00",
)

@Serializable
private data class StaffDto(
    @SerialName("usuario_id") val usuarioId: String? = null,
    val nombre: String? = null,
    val rol: String? = null,
    val pedidos: Int = 0,
    val ventas: String = "0.00",
    @SerialName("ticket_promedio") val ticket: String? = null,
)

@Serializable
private data class SpaceDto(
    @SerialName("espacio_id") val espacioId: Long? = null,
    val nombre: String? = null,
    val tipo: String? = null,
    val pedidos: Int = 0,
    val ventas: String = "0.00",
    val rentas: String = "0.00",
    val consumo: String = "0.00",
)

@Serializable
private data class DayDto(
    val fecha: String,
    val pedidos: Int = 0,
    val ventas: String = "0.00",
)

/** Los campos que comparten el corte y el reporte. */
@Serializable
private data class BreakdownDto(
    val resumen: SummaryDto = SummaryDto(),
    @SerialName("formas_pago") val formasPago: List<PaymentDto> = emptyList(),
    val propinas: TipsDto = TipsDto(),
    val cancelados: CancelledDto = CancelledDto(),
    val devoluciones: RefundsDto = RefundsDto(),
    @SerialName("sin_turno") val sinTurno: OutsideShiftDto? = null,
    @SerialName("por_categoria") val porCategoria: List<CategoryDto> = emptyList(),
    @SerialName("por_producto") val porProducto: List<ProductDto> = emptyList(),
    @SerialName("por_personal") val porPersonal: List<StaffDto> = emptyList(),
    @SerialName("por_espacio") val porEspacio: List<SpaceDto> = emptyList(),
    @SerialName("por_dia") val porDia: List<DayDto> = emptyList(),
    // Corte de turno
    val tipo: String? = null,
    val turno: ShiftDto? = null,
    // Corte del día
    val fecha: String? = null,
    val turnos: List<ShiftDto> = emptyList(),
    val efectivo: CashDto? = null,
    // Reporte contable
    val negocio: String? = null,
    val periodo: PeriodDto? = null,
    @SerialName("generado_en") val generadoEn: String? = null,
) {
    fun breakdown() =
        SalesBreakdown(
            summary =
                with(resumen) {
                    SalesSummary(ventas, pedidos, ticket, articulos, importeArticulos, descuentos, envios, rentas)
                },
            payments = formasPago.map { PaymentLine(it.metodo, it.pedidos, it.monto) },
            tips = Tips(propinas.total, propinas.efectivo, propinas.terminal),
            cancelled = Cancelled(cancelados.pedidos, cancelados.monto),
            refunds =
                with(devoluciones) { Refunds(articulosRechazados, reembolsosReserva, devolucionesEntregadas) },
            categories = porCategoria.map { CategoryLine(it.categoria, it.cantidad, it.importe) },
            products = porProducto.map { ProductLine(it.producto, it.categoria, it.cantidad, it.importe) },
            staff = porPersonal.map { StaffLine(it.usuarioId, it.nombre, it.rol, it.pedidos, it.ventas, it.ticket) },
            spaces =
                porEspacio.map {
                    SpaceLine(it.espacioId, it.nombre, it.tipo, it.pedidos, it.ventas, it.rentas, it.consumo)
                },
            days = porDia.map { DayLine(it.fecha, it.pedidos, it.ventas) },
        )
}

@Serializable
private data class PeriodDto(
    val desde: String,
    val hasta: String,
)

/** Lectura del JSON, aparte para probarla sin red. */
object CashCutJson {
    /** `hora_inicio_dia` de la configuración del negocio; null si el servidor no la manda (sin la migración 203). */
    fun dayStart(raw: String): String? {
        val data = (json.parseToJsonElement(raw) as? JsonObject)?.get("data") as? JsonObject
        return (data?.get("hora_inicio_dia") as? JsonPrimitive)?.contentOrNull?.take(5)
    }

    fun dayStartBody(hhmm: String): String = buildJsonObject { put("hora_inicio_dia", hhmm) }.toString()

    private val json =
        ApiJson.omitNullsCoercing

    private fun parse(raw: String) = json.decodeFromString<Envelope<BreakdownDto>>(raw).data

    fun shiftCut(raw: String): ShiftCut {
        val dto = parse(raw)
        val shift = requireNotNull(dto.turno) { "El corte no trae el turno" }.toDomain()
        return ShiftCut(dto.tipo == "final", shift, dto.breakdown(), dto.generadoEn)
    }

    fun dayCut(raw: String): DayCut {
        val dto = parse(raw)
        return DayCut(
            date = dto.fecha.orEmpty(),
            final = dto.tipo == "final",
            shifts = dto.turnos.map { it.toDomain() },
            cash = (dto.efectivo ?: CashDto()).toDomain(),
            breakdown = dto.breakdown(),
            generatedAt = dto.generadoEn,
            outsideShift = dto.sinTurno?.let { OutsideShift(it.pedidos, it.ventas) } ?: OutsideShift(),
        )
    }

    fun accounting(raw: String): AccountingReport {
        val dto = parse(raw)
        return AccountingReport(
            business = dto.negocio,
            from = dto.periodo?.desde.orEmpty(),
            to = dto.periodo?.hasta.orEmpty(),
            breakdown = dto.breakdown(),
        )
    }
}

private fun <T> Result<T>.mapApiErrors(): Result<T> =
    mapApiFailure {
        CatalogRepositoryException(
            it.code,
            it.message ?: it.code,
            it,
        )
    }

class RemoteCashCutRepository(
    private val apiClient: VaiinillaApiClient,
) : CashCutRepository {
    override fun shiftCut(sessionId: String): Result<ShiftCut> =
        apiClient.get("sesiones-caja/$sessionId/corte").mapCatching(CashCutJson::shiftCut).mapApiErrors()

    override fun dayCut(date: LocalDate?): Result<DayCut> =
        apiClient
            .get("sesiones-caja/cortes", date?.let { mapOf("fecha" to it.toString()) } ?: emptyMap())
            .mapCatching(CashCutJson::dayCut)
            .mapApiErrors()

    override fun accounting(
        from: LocalDate,
        to: LocalDate,
    ): Result<AccountingReport> =
        apiClient
            .get("reportes/contable", mapOf("desde" to from.toString(), "hasta" to to.toString()))
            .mapCatching(CashCutJson::accounting)
            .mapApiErrors()

    override fun accountingCsv(
        from: LocalDate,
        to: LocalDate,
    ): Result<String> =
        apiClient
            .get("reportes/contable", mapOf("desde" to from.toString(), "hasta" to to.toString(), "formato" to "csv"))
            .mapApiErrors()

    override fun dayStart(): Result<String?> =
        apiClient.get("establecimiento/configuracion").mapCatching(CashCutJson::dayStart).mapApiErrors()

    override fun setDayStart(hhmm: String): Result<String> =
        apiClient
            .patch("establecimiento/configuracion", CashCutJson.dayStartBody(hhmm))
            .mapCatching { CashCutJson.dayStart(it) ?: hhmm }
            .mapApiErrors()
}
