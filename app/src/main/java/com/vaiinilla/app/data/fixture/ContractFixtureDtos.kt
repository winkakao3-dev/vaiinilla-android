package com.vaiinilla.app.data.fixture

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CatalogEnvelopeDto(
    val data: CatalogDataDto,
    val meta: MetaDto,
    val error: JsonElement? = null,
)

@Serializable
data class CatalogDataDto(
    @SerialName("categorias") val categories: List<CategoryDto>,
    @SerialName("productos") val products: List<ProductDto>,
)

@Serializable
data class OperationalStatusEnvelopeDto(
    val data: OperationalStatusDto,
    val meta: MetaDto,
    val error: JsonElement? = null,
)

@Serializable
data class MetaDto(
    val page: Int? = null,
    @SerialName("total_pages") val totalPages: Int? = null,
    @SerialName("total_items") val totalItems: Int? = null,
    val cursor: String? = null,
)

@Serializable
data class OperationalStatusDto(
    @SerialName("recibiendo_pedidos") val acceptingOrders: Boolean,
    @SerialName("sesion_caja_abierta") val cashSessionOpen: Boolean,
    @SerialName("caja_en_linea") val cashierOnline: Boolean,
    @SerialName("cocina_en_linea") val kitchenOnline: Boolean,
    @SerialName("tiempo_estimado_min") val estimatedTimeMinutes: Int,
    @SerialName("consultado_en") val consultedAt: String,
)

@Serializable
data class CategoryDto(
    val id: Int,
    @SerialName("nombre") val name: String,
    @SerialName("orden") val order: Int,
)

@Serializable
data class ProductDto(
    val id: Int,
    @SerialName("categoria_id") val categoryId: Int,
    @SerialName("estacion_preparacion") val preparationStation: String,
    @SerialName("nombre") val name: String,
    @SerialName("descripcion") val description: String,
    @SerialName("ingredientes") val ingredients: String,
    @SerialName("alergenos") val allergens: String,
    @SerialName("tiempo_estimado_min") val estimatedTimeMinutes: Int,
    @SerialName("precio_mostrador") val counterPrice: String,
    @SerialName("precio_digital") val digitalPrice: String,
    @SerialName("disponible") val available: Boolean,
    @SerialName("imagen_url") val imageUrl: String,
    @SerialName("grupos_opcion") val optionGroups: List<OptionGroupDto>,
)

@Serializable
data class OptionGroupDto(
    val id: Int,
    @SerialName("nombre") val name: String,
    @SerialName("min_selecciones") val minimumSelections: Int,
    @SerialName("max_selecciones") val maximumSelections: Int,
    @SerialName("opciones") val options: List<ProductOptionDto>,
)

@Serializable
data class ProductOptionDto(
    val id: Int,
    @SerialName("nombre") val name: String,
    @SerialName("precio_extra") val extraPrice: String,
)
