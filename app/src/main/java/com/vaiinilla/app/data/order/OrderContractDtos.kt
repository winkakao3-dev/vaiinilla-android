package com.vaiinilla.app.data.order

import com.vaiinilla.app.data.fixture.MetaDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CreateOrderRequestDto(
    @SerialName("metodo_pago") val paymentMethod: String,
    @SerialName("destino") val destination: String,
    @SerialName("espacio_id") val spaceId: Int?,
    @SerialName("notas_cocina") val kitchenNotes: String,
    @SerialName("items") val items: List<CreateOrderItemDto>,
)

@Serializable
data class CreateOrderItemDto(
    @SerialName("producto_id") val productId: Int,
    @SerialName("cantidad") val quantity: Int,
    @SerialName("opcion_ids") val optionIds: List<Int>,
)

@Serializable
data class OrderDetailEnvelopeDto(
    val data: OrderDetailDto,
    val meta: MetaDto,
    val error: JsonElement? = null,
)

@Serializable
data class OrderDetailDto(
    val id: String,
    val folio: Int,
    @SerialName("fecha_operativa") val operationalDate: String,
    @SerialName("estado") val state: String,
    @SerialName("metodo_pago") val paymentMethod: String,
    @SerialName("destino") val destination: String,
    @SerialName("espacio") val space: OrderSpaceDto? = null,
    @SerialName("subtotal") val subtotal: String,
    @SerialName("ahorro_combinado") val combinedSavings: String,
    @SerialName("cashback_otorgado") val cashbackAwarded: String,
    @SerialName("total") val total: String,
    @SerialName("version") val version: Int,
    @SerialName("creado_en") val createdAt: String,
    @SerialName("actualizado_en") val updatedAt: String,
    @SerialName("usuario") val user: OrderUserDto? = null,
    @SerialName("notas_cocina") val kitchenNotes: String,
    @SerialName("items") val items: List<OrderItemDto>,
)

@Serializable
data class OrderUserDto(
    @SerialName("nombre") val name: String,
    @SerialName("matricula") val enrollment: String,
)

@Serializable
data class OrderSpaceDto(
    val id: Int,
    @SerialName("nombre") val name: String,
    @SerialName("tipo") val type: String,
)

@Serializable
data class OrderItemDto(
    val id: Int,
    @SerialName("producto_id") val productId: Int,
    @SerialName("nombre_producto") val productName: String,
    @SerialName("estacion_preparacion") val preparationStation: String,
    @SerialName("cantidad") val quantity: Int,
    @SerialName("precio_digital_unitario") val unitDigitalPrice: String,
    @SerialName("subtotal") val subtotal: String,
    @SerialName("opciones") val options: List<OrderItemOptionDto>,
)

@Serializable
data class OrderItemOptionDto(
    @SerialName("opcion_id") val optionId: Int,
    @SerialName("nombre") val name: String,
    @SerialName("precio_extra") val extraPrice: String,
)
