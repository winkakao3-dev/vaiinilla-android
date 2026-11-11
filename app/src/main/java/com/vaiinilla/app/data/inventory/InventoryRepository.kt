package com.vaiinilla.app.data.inventory

import com.vaiinilla.app.core.network.ApiJson
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.core.network.mapApiFailure
import com.vaiinilla.app.domain.repository.CatalogRepositoryException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/** Unidades que acepta el backend (migración 140), con cómo se dicen en pantalla. */
enum class InventoryUnit(
    val wire: String,
    val label: String,
    val short: String,
) {
    PIECE("pieza", "Piezas", "pz"),
    BOTTLE("botella", "Botellas", "bot"),
    ML("ml", "Mililitros", "ml"),
    LITER("l", "Litros", "L"),
    GRAM("g", "Gramos", "g"),
    KILO("kg", "Kilos", "kg"),
    OUNCE("oz", "Onzas", "oz"),
    PORTION("porcion", "Porciones", "porc"),
    ;

    companion object {
        fun of(wire: String): InventoryUnit = entries.firstOrNull { it.wire == wire } ?: PIECE
    }
}

/** Lo que se registra a mano sobre un insumo. */
enum class InventoryMovementType(
    val wire: String,
    val label: String,
) {
    ENTRY("entrada", "Entrada"),
    WASTE("merma", "Merma"),
    COUNT("conteo", "Conteo"),
    ADJUST("ajuste", "Ajuste"),
}

data class InventorySupply(
    val id: Long,
    val name: String,
    val category: String?,
    val unit: InventoryUnit,
    val stock: Double,
    val minimum: Double,
    val unitCost: Double,
    val value: Double,
    val low: Boolean,
    val active: Boolean,
    val usedBy: Int,
)

data class InventorySupplies(
    val supplies: List<InventorySupply>,
    val lowCount: Int,
    val totalValue: Double,
)

/**
 * Lo que valen los insumos en uso. [InventorySupplies.totalValue] es el del servidor, que suma también los dados de
 * baja cuando vienen en la lista.
 */
val InventorySupplies.inUseValue: Double
    get() = supplies.filter { it.active }.sumOf { it.value }

/**
 * Al editar, unidad y costo en null no se mandan: el servidor conserva lo que tiene. [category] es la etiqueta propia
 * del insumo (no la categoría del menú); "" la borra, null la deja como está (con `explicitNulls = false` no viaja).
 */
data class InventorySupplyInput(
    val name: String,
    val category: String?,
    val unit: InventoryUnit?,
    val minimum: Double,
    val unitCost: Double?,
    val initialStock: Double? = null,
    val active: Boolean? = null,
)

data class InventoryProduct(
    val id: Int,
    val name: String,
    val category: String,
    val price: Double,
    val available: Boolean,
    val soldOutByInventory: Boolean,
    val hasRecipe: Boolean,
    val enoughFor: Int?,
    val recipeCost: Double,
    val margin: Double?,
)

/** Un producto del menú cuya receta base gasta el insumo ([quantity] por unidad del producto). */
data class SupplyProductLink(
    val productId: Int,
    val name: String,
    val categoryId: Int,
    val categoryName: String,
    val quantity: Double,
)

data class RecipeLine(
    val supplyId: Long,
    val supplyName: String,
    val unit: InventoryUnit,
    val quantity: Double,
    val optionId: Int?,
    val optionName: String?,
    val cost: Double,
)

data class InventoryMovement(
    val id: String,
    val supplyName: String,
    val unit: InventoryUnit,
    val type: String,
    val quantity: Double,
    val stockAfter: Double,
    val expected: Double?,
    val note: String?,
    val folio: Int?,
    val by: String?,
    val at: String,
)

data class InventoryReportRow(
    val id: Long,
    val name: String,
    val unit: InventoryUnit,
    val start: Double,
    val entries: Double,
    val sold: Double,
    val waste: Double,
    val differences: Double,
    val end: Double,
    val low: Boolean,
    val soldCost: Double,
    val wasteCost: Double,
    val differenceCost: Double,
)

data class InventoryReport(
    val from: String,
    val to: String,
    val rows: List<InventoryReportRow>,
    val soldCost: Double,
    val wasteCost: Double,
    val differenceCost: Double,
    val purchases: Double,
)

data class MovementResult(
    val stock: Double,
    val change: Double,
    val duplicate: Boolean,
)

interface InventoryRepository {
    fun supplies(includeInactive: Boolean = false): Result<InventorySupplies>

    fun createSupply(input: InventorySupplyInput): Result<InventorySupply>

    fun updateSupply(
        id: Long,
        input: InventorySupplyInput,
    ): Result<InventorySupply>

    fun registerMovement(
        id: String,
        supplyId: Long,
        type: InventoryMovementType,
        quantity: Double,
        unitCost: Double? = null,
        note: String? = null,
    ): Result<MovementResult>

    /** Conteo físico de varios insumos a la vez; regresa cuántos tuvieron diferencia. */
    fun registerCount(
        counts: List<Triple<String, Long, Double>>,
        note: String?,
    ): Result<Int>

    fun products(): Result<List<InventoryProduct>>

    fun recipe(productId: Int): Result<List<RecipeLine>>

    fun saveRecipe(
        productId: Int,
        lines: List<RecipeLine>,
    ): Result<List<RecipeLine>>

    /** `GET /inventario/insumos/:id/productos`: los productos cuya receta base usa este insumo. */
    fun supplyProducts(supplyId: Long): Result<List<SupplyProductLink>>

    /**
     * `PUT /inventario/insumos/:id/productos`: este insumo queda exactamente en esos productos (id → cantidad) y en los
     * productos que hoy tiene cada categoría de [categories] (id → cantidad; lo suelto gana). Las líneas «si eligen…»
     * no se tocan. Regresa cómo quedó.
     */
    fun saveSupplyProducts(
        supplyId: Long,
        products: Map<Int, Double>,
        categories: Map<Int, Double> = emptyMap(),
    ): Result<List<SupplyProductLink>>

    fun movements(
        fromIso: String?,
        toIso: String?,
        supplyId: Long? = null,
    ): Result<List<InventoryMovement>>

    fun report(
        fromIso: String?,
        toIso: String?,
    ): Result<InventoryReport>
}

@Serializable
private data class Envelope<T>(
    val data: T,
)

@Serializable
private data class SupplyDto(
    val id: Long,
    val nombre: String,
    val categoria: String? = null,
    val unidad: String,
    val existencia: Double = 0.0,
    val minimo: Double = 0.0,
    @SerialName("costo_unitario") val costoUnitario: Double = 0.0,
    val valor: Double = 0.0,
    @SerialName("bajo_minimo") val bajoMinimo: Boolean = false,
    val activo: Boolean = true,
    @SerialName("productos_que_lo_usan") val productos: Int = 0,
) {
    fun toDomain() =
        InventorySupply(
            id,
            nombre,
            categoria,
            InventoryUnit.of(unidad),
            existencia,
            minimo,
            costoUnitario,
            valor,
            bajoMinimo,
            activo,
            productos,
        )
}

@Serializable
private data class SummaryDto(
    val total: Int = 0,
    @SerialName("bajo_minimo") val bajoMinimo: Int = 0,
    @SerialName("valor_total") val valorTotal: Double = 0.0,
)

@Serializable
private data class SuppliesDto(
    val insumos: List<SupplyDto>,
    val resumen: SummaryDto = SummaryDto(),
)

@Serializable
private data class SupplyBody(
    val nombre: String,
    val categoria: String?,
    val unidad: String? = null,
    val minimo: Double,
    @SerialName("costo_unitario") val costoUnitario: Double? = null,
    @SerialName("existencia_inicial") val existenciaInicial: Double? = null,
    val activo: Boolean? = null,
)

@Serializable
private data class MovementBody(
    val id: String,
    @SerialName("insumo_id") val insumoId: Long,
    val tipo: String,
    val cantidad: Double,
    @SerialName("costo_unitario") val costoUnitario: Double? = null,
    val nota: String? = null,
)

@Serializable
private data class MovementResultDto(
    val existencia: Double,
    val cantidad: Double,
    val duplicado: Boolean = false,
)

@Serializable
private data class CountItem(
    val id: String,
    @SerialName("insumo_id") val insumoId: Long,
    val cantidad: Double,
)

@Serializable
private data class CountBody(
    val nota: String?,
    val conteos: List<CountItem>,
)

@Serializable
private data class CountResultDto(
    @SerialName("con_diferencia") val conDiferencia: Int = 0,
)

@Serializable
private data class ProductDto(
    val id: Int,
    val nombre: String,
    val categoria: String = "",
    val precio: Double = 0.0,
    val disponible: Boolean = true,
    @SerialName("agotado_por_inventario") val agotado: Boolean = false,
    @SerialName("tiene_receta") val tieneReceta: Boolean = false,
    @SerialName("alcanza_para") val alcanzaPara: Int? = null,
    @SerialName("costo_receta") val costoReceta: Double = 0.0,
    val margen: Double? = null,
)

@Serializable
private data class ProductsDto(
    val productos: List<ProductDto>,
)

@Serializable
private data class LineDto(
    @SerialName("insumo_id") val insumoId: Long,
    val insumo: String = "",
    val unidad: String = "pieza",
    val cantidad: Double,
    @SerialName("opcion_id") val opcionId: Int? = null,
    val opcion: String? = null,
    val costo: Double = 0.0,
) {
    fun toDomain() = RecipeLine(insumoId, insumo, InventoryUnit.of(unidad), cantidad, opcionId, opcion, costo)
}

@Serializable
private data class RecipeDto(
    val lineas: List<LineDto>,
)

@Serializable
private data class LineBody(
    @SerialName("insumo_id") val insumoId: Long,
    val cantidad: Double,
    @SerialName("opcion_id") val opcionId: Int? = null,
)

@Serializable
private data class RecipeBody(
    val lineas: List<LineBody>,
)

@Serializable
private data class SupplyProductDto(
    @SerialName("producto_id") val productoId: Int,
    val nombre: String = "",
    @SerialName("categoria_id") val categoriaId: Int = 0,
    @SerialName("categoria_nombre") val categoriaNombre: String = "",
    val cantidad: Double = 0.0,
) {
    fun toDomain() = SupplyProductLink(productoId, nombre, categoriaId, categoriaNombre, cantidad)
}

@Serializable
private data class SupplyProductBody(
    @SerialName("producto_id") val productoId: Int,
    val cantidad: Double,
)

@Serializable
private data class SupplyCategoryBody(
    @SerialName("categoria_id") val categoriaId: Int,
    val cantidad: Double,
)

@Serializable
private data class SupplyProductsBody(
    val productos: List<SupplyProductBody>,
    val categorias: List<SupplyCategoryBody>,
)

@Serializable
private data class MovementDto(
    val id: String,
    val insumo: String,
    val unidad: String,
    val tipo: String,
    val cantidad: Double,
    @SerialName("existencia_resultante") val existenciaResultante: Double,
    @SerialName("existencia_esperada") val existenciaEsperada: Double? = null,
    val nota: String? = null,
    val folio: Int? = null,
    @SerialName("registrado_por") val registradoPor: String? = null,
    @SerialName("creado_en") val creadoEn: String,
)

@Serializable
private data class MovementsDto(
    val movimientos: List<MovementDto>,
)

@Serializable
private data class ReportRowDto(
    val id: Long,
    val nombre: String,
    val unidad: String,
    val inicial: Double = 0.0,
    val entradas: Double = 0.0,
    val vendido: Double = 0.0,
    val mermas: Double = 0.0,
    val diferencias: Double = 0.0,
    val final: Double = 0.0,
    @SerialName("bajo_minimo") val bajoMinimo: Boolean = false,
    @SerialName("costo_vendido") val costoVendido: Double = 0.0,
    @SerialName("costo_mermas") val costoMermas: Double = 0.0,
    @SerialName("costo_diferencias") val costoDiferencias: Double = 0.0,
)

@Serializable
private data class ReportTotalsDto(
    @SerialName("costo_vendido") val costoVendido: Double = 0.0,
    @SerialName("costo_mermas") val costoMermas: Double = 0.0,
    @SerialName("costo_diferencias") val costoDiferencias: Double = 0.0,
    @SerialName("compras_monto") val comprasMonto: Double = 0.0,
)

@Serializable
private data class ReportDto(
    val desde: String,
    val hasta: String,
    val insumos: List<ReportRowDto>,
    val totales: ReportTotalsDto = ReportTotalsDto(),
)

private fun <T> Result<T>.mapApiErrors(): Result<T> =
    mapApiFailure {
        CatalogRepositoryException(
            it.code,
            it.message ?: it.code,
            it,
        )
    }

/** `/api/v1/inventario` (backend, migración 140). */
class RemoteInventoryRepository(
    private val apiClient: VaiinillaApiClient,
) : InventoryRepository {
    private val json =
        ApiJson.omitNullsCoercing

    override fun supplies(includeInactive: Boolean): Result<InventorySupplies> =
        apiClient
            .get("inventario/insumos", if (includeInactive) mapOf("inactivos" to "true") else emptyMap())
            .mapCatching { raw ->
                val dto = json.decodeFromString<Envelope<SuppliesDto>>(raw).data
                InventorySupplies(dto.insumos.map { it.toDomain() }, dto.resumen.bajoMinimo, dto.resumen.valorTotal)
            }.mapApiErrors()

    private fun body(input: InventorySupplyInput) =
        json.encodeToString(
            SupplyBody(
                input.name,
                input.category,
                input.unit?.wire,
                input.minimum,
                input.unitCost,
                input.initialStock,
                input.active,
            ),
        )

    override fun createSupply(input: InventorySupplyInput): Result<InventorySupply> =
        apiClient
            .post("inventario/insumos", body(input))
            .mapCatching { json.decodeFromString<Envelope<SupplyDto>>(it).data.toDomain() }
            .mapApiErrors()

    override fun updateSupply(
        id: Long,
        input: InventorySupplyInput,
    ): Result<InventorySupply> =
        apiClient
            .patch("inventario/insumos/$id", body(input.copy(initialStock = null)))
            .mapCatching { json.decodeFromString<Envelope<SupplyDto>>(it).data.toDomain() }
            .mapApiErrors()

    override fun registerMovement(
        id: String,
        supplyId: Long,
        type: InventoryMovementType,
        quantity: Double,
        unitCost: Double?,
        note: String?,
    ): Result<MovementResult> =
        apiClient
            .post(
                "inventario/movimientos",
                json.encodeToString(MovementBody(id, supplyId, type.wire, quantity, unitCost, note?.ifBlank { null })),
            ).mapCatching {
                val dto = json.decodeFromString<Envelope<MovementResultDto>>(it).data
                MovementResult(dto.existencia, dto.cantidad, dto.duplicado)
            }.mapApiErrors()

    override fun registerCount(
        counts: List<Triple<String, Long, Double>>,
        note: String?,
    ): Result<Int> =
        apiClient
            .post(
                "inventario/conteos",
                json.encodeToString(
                    CountBody(note?.ifBlank { null }, counts.map { (id, supply, qty) -> CountItem(id, supply, qty) }),
                ),
            ).mapCatching { json.decodeFromString<Envelope<CountResultDto>>(it).data.conDiferencia }
            .mapApiErrors()

    override fun products(): Result<List<InventoryProduct>> =
        apiClient
            .get("inventario/productos")
            .mapCatching { raw ->
                json.decodeFromString<Envelope<ProductsDto>>(raw).data.productos.map {
                    InventoryProduct(
                        it.id,
                        it.nombre,
                        it.categoria,
                        it.precio,
                        it.disponible,
                        it.agotado,
                        it.tieneReceta,
                        it.alcanzaPara,
                        it.costoReceta,
                        it.margen,
                    )
                }
            }.mapApiErrors()

    override fun recipe(productId: Int): Result<List<RecipeLine>> =
        apiClient
            .get("inventario/productos/$productId/receta")
            .mapCatching {
                json
                    .decodeFromString<Envelope<RecipeDto>>(it)
                    .data.lineas
                    .map(LineDto::toDomain)
            }.mapApiErrors()

    override fun saveRecipe(
        productId: Int,
        lines: List<RecipeLine>,
    ): Result<List<RecipeLine>> =
        apiClient
            .put(
                "inventario/productos/$productId/receta",
                json.encodeToString(RecipeBody(lines.map { LineBody(it.supplyId, it.quantity, it.optionId) })),
            ).mapCatching {
                json
                    .decodeFromString<Envelope<RecipeDto>>(it)
                    .data.lineas
                    .map(LineDto::toDomain)
            }.mapApiErrors()

    override fun supplyProducts(supplyId: Long): Result<List<SupplyProductLink>> =
        apiClient
            .get("inventario/insumos/$supplyId/productos")
            .mapCatching { decodeSupplyProducts(it) }
            .mapApiErrors()

    override fun saveSupplyProducts(
        supplyId: Long,
        products: Map<Int, Double>,
        categories: Map<Int, Double>,
    ): Result<List<SupplyProductLink>> =
        apiClient
            .put(
                "inventario/insumos/$supplyId/productos",
                encodeSupplyProducts(products, categories),
            ).mapCatching { decodeSupplyProducts(it) }
            .mapApiErrors()

    override fun movements(
        fromIso: String?,
        toIso: String?,
        supplyId: Long?,
    ): Result<List<InventoryMovement>> {
        val query =
            buildMap {
                fromIso?.let { put("desde", it) }
                toIso?.let { put("hasta", it) }
                supplyId?.let { put("insumo_id", it.toString()) }
                put("limite", "300")
            }
        return apiClient
            .get("inventario/movimientos", query)
            .mapCatching { raw ->
                json.decodeFromString<Envelope<MovementsDto>>(raw).data.movimientos.map {
                    InventoryMovement(
                        it.id,
                        it.insumo,
                        InventoryUnit.of(it.unidad),
                        it.tipo,
                        it.cantidad,
                        it.existenciaResultante,
                        it.existenciaEsperada,
                        it.nota,
                        it.folio,
                        it.registradoPor,
                        it.creadoEn,
                    )
                }
            }.mapApiErrors()
    }

    override fun report(
        fromIso: String?,
        toIso: String?,
    ): Result<InventoryReport> {
        val query =
            buildMap {
                fromIso?.let { put("desde", it) }
                toIso?.let { put("hasta", it) }
            }
        return apiClient
            .get("inventario/reporte", query)
            .mapCatching { raw ->
                val dto = json.decodeFromString<Envelope<ReportDto>>(raw).data
                InventoryReport(
                    dto.desde,
                    dto.hasta,
                    dto.insumos.map {
                        InventoryReportRow(
                            it.id,
                            it.nombre,
                            InventoryUnit.of(it.unidad),
                            it.inicial,
                            it.entradas,
                            it.vendido,
                            it.mermas,
                            it.diferencias,
                            it.final,
                            it.bajoMinimo,
                            it.costoVendido,
                            it.costoMermas,
                            it.costoDiferencias,
                        )
                    },
                    dto.totales.costoVendido,
                    dto.totales.costoMermas,
                    dto.totales.costoDiferencias,
                    dto.totales.comprasMonto,
                )
            }.mapApiErrors()
    }
}

private val supplyProductsJson =
    ApiJson.coercing

/** Contrato 5: `{data: [{producto_id, nombre, categoria_id, categoria_nombre, cantidad}]}`. */
internal fun decodeSupplyProducts(raw: String): List<SupplyProductLink> =
    supplyProductsJson.decodeFromString<Envelope<List<SupplyProductDto>>>(raw).data.map(SupplyProductDto::toDomain)

/** Contrato 5: `{productos: [{producto_id, cantidad}], categorias: [{categoria_id, cantidad}]}`. */
internal fun encodeSupplyProducts(
    products: Map<Int, Double>,
    categories: Map<Int, Double>,
): String =
    supplyProductsJson.encodeToString(
        SupplyProductsBody(
            products.map { (id, qty) -> SupplyProductBody(id, qty) },
            categories.map { (id, qty) -> SupplyCategoryBody(id, qty) },
        ),
    )
