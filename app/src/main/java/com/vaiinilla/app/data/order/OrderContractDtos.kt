package com.vaiinilla.app.data.order

import com.vaiinilla.app.data.contract.MetaDto
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CreateOrderRequestDto(
    @SerialName("metodo_pago") val paymentMethod: String,
    @SerialName("destino") val destination: String,
    @SerialName("espacio_id") val spaceId: Int?,
    @SerialName("notas_cocina") val kitchenNotes: String?,
    @SerialName("items") val items: List<CreateOrderItemDto>,
)

@Serializable
data class CreateOrderItemDto(
    @SerialName("producto_id") val productId: Int,
    @SerialName("cantidad") val quantity: Int,
    @SerialName("opcion_ids") val optionIds: List<Int>,
)

@Serializable
data class OrderListEnvelopeDto(
    val data: List<OrderDetailDto>,
    val meta: MetaDto,
    val error: JsonElement? = null,
)

@Serializable
data class CashCollectionRequestDto(
    @SerialName("monto_recibido") val amountReceived: String,
    @SerialName("version_esperada") val expectedVersion: Int,
)

@Serializable
data class CashCollectionDataDto(
    @SerialName("pedido") val order: OrderDetailDto,
    @SerialName("monto_recibido") val amountReceived: String,
    @SerialName("cambio") val change: String,
)

@Serializable
data class CashCollectionEnvelopeDto(
    val data: CashCollectionDataDto,
    val meta: MetaDto,
    val error: JsonElement? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class TransitionRequestDto(
    @SerialName("estado_objetivo") val targetState: String,
    @SerialName("version_esperada") val expectedVersion: Int,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("qr_token") val pickupToken: String? = null,
)

@Serializable
data class OrderDetailEnvelopeDto(
    val data: OrderDetailDto,
    val meta: MetaDto,
    val error: JsonElement? = null,
)

@Serializable
data class StripeRetryDataDto(
    @SerialName("pago") val payment: OrderPaymentDto,
)

@Serializable
data class StripeRetryEnvelopeDto(
    val data: StripeRetryDataDto,
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
    @SerialName("notas_cocina") val kitchenNotes: String? = null,
    @SerialName("items") val items: List<OrderItemDto> = emptyList(),
    @SerialName("qr_token") val pickupToken: String? = null,
    @SerialName("pago") val payment: OrderPaymentDto? = null,
)

@Serializable
data class OrderPaymentDto(
    @SerialName("payment_attempt_id") val paymentAttemptId: String,
    @SerialName("payment_intent_id") val paymentIntentId: String,
    @SerialName("stripe_account_id") val stripeAccountId: String,
    @SerialName("payment_status") val paymentStatus: String,
    @SerialName("client_secret") val clientSecret: String? = null,
    @SerialName("publishable_key") val publishableKey: String? = null,
)

@Serializable
data class OpenCashSessionRequestDto(
    @SerialName("monto_inicial") val initialAmount: String,
)

@Serializable
data class CashSessionDto(
    val id: String,
    @SerialName("fecha_operativa") val operationalDate: String,
    @SerialName("monto_inicial") val initialAmount: String,
    @SerialName("abierta_en") val openedAt: String,
)

@Serializable
data class CashSessionEnvelopeDto(
    val data: CashSessionDto?,
    val meta: MetaDto,
    val error: JsonElement? = null,
)

@Serializable
data class OrderUserDto(
    @SerialName("nombre") val name: String,
    @SerialName("matricula") val enrollment: String? = null,
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
    @SerialName("precio_cobro_unitario") val unitCollectionPrice: String? = null,
    @SerialName("subtotal") val subtotal: String,
    @SerialName("opciones") val options: List<OrderItemOptionDto>,
)

@Serializable
data class OrderItemOptionDto(
    @SerialName("opcion_id") val optionId: Int,
    @SerialName("nombre") val name: String,
    @SerialName("precio_extra") val extraPrice: String,
)
