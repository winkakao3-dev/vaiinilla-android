package com.vaiinilla.app.data.order

import com.vaiinilla.app.data.contract.MetaDto
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CreateOrderRequestDto(
    @SerialName("metodo_pago") val paymentMethod: String,
    @SerialName("destino") val destination: String,
    @SerialName("espacio_id") val spaceId: Int?,
    @SerialName("notas_cocina") val kitchenNotes: String?,
    @SerialName("items") val items: List<CreateOrderItemDto>,
    // Solo viaja cuando es true: los demás pedidos se envían exactamente igual que antes.
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("pago_diferido") val payAtEnd: Boolean? = null,
    // El backend lee `nombre_cliente` al crear; con `nombre_cliente_snapshot` el nombre se perdía.
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("nombre_cliente") val customerName: String? = null,
    // Lo de domicilio y encargos solo viaja cuando se usa: un servidor anterior rechaza las claves.
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("entrega") val delivery: DeliveryDto? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("fecha_entrega") val deliveryDate: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("franja_entrega") val deliverySlot: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("al_recibir") val paysOnArrival: Boolean? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("con_anticipo") val withDeposit: Boolean? = null,
    // «Pasa por él»: solo viaja cuando es true, para no cambiar los pedidos de siempre.
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("recoge_en_barra") val pickUpAtBar: Boolean? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeliveryDto(
    @SerialName("direccion") val address: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("referencia") val reference: String? = null,
    @SerialName("telefono") val phone: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("lat") val latitude: JsonPrimitive? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("lng") val longitude: JsonPrimitive? = null,
)

@Serializable
data class HomeDeliveryQuoteDto(
    val llega: Boolean,
    @SerialName("distancia_km") val distanceKm: JsonPrimitive? = null,
    val costo: JsonPrimitive? = null,
    val minimo: JsonPrimitive? = null,
)

@Serializable
data class HomeDeliveryQuoteEnvelopeDto(
    val data: HomeDeliveryQuoteDto,
)

@Serializable
data class AgendaDayDto(
    val fecha: String,
    val pedidos: List<OrderDetailDto> = emptyList(),
)

@Serializable
data class AgendaEnvelopeDto(
    val data: List<AgendaDayDto>,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CreateOrderItemDto(
    @SerialName("producto_id") val productId: Int,
    @SerialName("cantidad") val quantity: Int,
    @SerialName("opcion_ids") val optionIds: List<Int>,
    // Solo viaja cuando hay nota: el resto de líneas se envían exactamente igual que antes.
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("notas") val notes: String? = null,
)

@Serializable
data class OrderListEnvelopeDto(
    val data: List<OrderDetailDto>,
    val meta: MetaDto,
    val error: JsonElement? = null,
)

@Serializable
data class CancelOrderRequestDto(
    @SerialName("version_esperada") val expectedVersion: Int,
    @SerialName("motivo") val reason: String,
)

@Serializable
data class CancelOrderDataDto(
    @SerialName("pedido") val order: OrderDetailDto,
    @SerialName("efectivo_devuelto") val cashReturned: RefundAmountDto? = null,
    @SerialName("devolucion_pendiente") val pendingRefund: RefundAmountDto? = null,
    @SerialName("devolucion") val balanceRefund: RefundAmountDto? = null,
    @SerialName("reembolso") val cardRefund: JsonElement? = null,
)

/** Rechazar un artículo: aquí `devolucion` es la tarea de Caja y el saldo va en `saldo_devuelto`. */
@Serializable
data class RejectItemDataDto(
    @SerialName("pedido") val order: OrderDetailDto,
    @SerialName("devolucion") val pendingRefund: RefundAmountDto? = null,
    @SerialName("saldo_devuelto") val balanceRefund: RefundAmountDto? = null,
)

@Serializable
data class RefundAmountDto(
    @SerialName("monto") val amount: JsonPrimitive? = null,
)

@Serializable
data class RejectItemEnvelopeDto(
    val data: RejectItemDataDto,
    val meta: MetaDto,
    val error: JsonElement? = null,
)

@Serializable
data class CancelOrderEnvelopeDto(
    val data: CancelOrderDataDto,
    val meta: MetaDto,
    val error: JsonElement? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CashCollectionRequestDto(
    @SerialName("monto_recibido") val amountReceived: String,
    @SerialName("version_esperada") val expectedVersion: Int,
    /** Propina (docs/propinas.md): solo se manda si hay. */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("propina") val tip: String? = null,
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
data class PickupTokenDataDto(
    @SerialName("qr_token") val pickupToken: String,
)

@Serializable
data class PickupTokenEnvelopeDto(
    val data: PickupTokenDataDto,
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
    /** Día y franja de un pedido apartado para después (padres e hijos); nulos en un pedido para ahora. */
    @SerialName("fecha_entrega") val deliveryDate: String? = null,
    @SerialName("franja_entrega") val deliverySlot: String? = null,
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
    @SerialName("pago_diferido") val payAtEnd: Boolean = false,
    @SerialName("pago_pendiente") val paymentPending: Boolean = false,
    /** Cuánto lleva pagado el pedido y cuánto falta; un servidor anterior no los manda. */
    @SerialName("monto_pagado") val amountPaid: String? = null,
    @SerialName("saldo_pendiente") val balanceDue: String? = null,
    @SerialName("motivo_cancelacion") val cancelReason: String? = null,
    @SerialName("cancelado_por_rol") val canceledByRole: String? = null,
    @SerialName("reserva") val reservation: OrderReservationDto? = null,
    @SerialName("llegada_en") val arrivedAt: String? = null,
    @SerialName("sesion_espacio_id") val tableSessionId: String? = null,
    /** El backend dispensa el QR para entregar este pedido (va a una cuenta abierta); uno anterior no lo envía. */
    @SerialName("entrega_sin_qr") val deliveryWithoutQr: Boolean = false,
    /** «Pasa por él»; un servidor anterior no lo envía y lo lleva el mesero. */
    @SerialName("recoge_en_barra") val pickUpAtBar: Boolean = false,
    /** Solo viene en pedidos de mostrador; un servidor anterior no lo envía. */
    @SerialName("nombre_cliente_snapshot") val customerName: String? = null,
    /** Con qué se cobró de verdad, `"mixto"` si hubo partes con métodos distintos; un servidor anterior no lo envía. */
    @SerialName("metodo_pago_cobrado") val chargedMethod: String? = null,
    @SerialName("pagos_por_metodo") val paymentsByMethod: List<PaymentPartDto> = emptyList(),
    /** Solo a domicilio; un servidor anterior no los envía (CASA-05 a 09). */
    @SerialName("entrega") val delivery: DeliveryDto? = null,
    @SerialName("costo_envio") val deliveryFee: JsonPrimitive? = null,
    @SerialName("anticipo") val deposit: JsonPrimitive? = null,
    @SerialName("por_cobrar_al_entregar") val dueOnArrival: JsonPrimitive? = null,
    @SerialName("codigo_entrega") val deliveryCode: JsonPrimitive? = null,
    @SerialName("repartidor_nombre") val courierName: String? = null,
    /** Plazos de un encargo (EQUIPO-53); un servidor anterior no los envía. */
    @SerialName("aceptar_antes_de") val answerBy: String? = null,
    @SerialName("pagar_antes_de") val payBy: String? = null,
    /** Pedido tomado sin internet (migración 161); un servidor anterior no lo envía. */
    @SerialName("sin_conexion") val offlineCapture: OfflineCaptureDto? = null,
)

@Serializable
data class OfflineCaptureDto(
    @SerialName("capturado_en") val capturedAt: String? = null,
    @SerialName("folio_local") val localFolio: String? = null,
)

@Serializable
data class PaymentPartDto(
    @SerialName("metodo") val method: String,
    @SerialName("monto") val amount: String,
)

/** La reserva de un pedido de renta de cancha; `null` en cualquier otro pedido. */
@Serializable
data class OrderReservationDto(
    val id: String,
    @SerialName("espacio") val space: OrderSpaceDto? = null,
    @SerialName("inicio") val start: String,
    @SerialName("fin") val end: String,
    @SerialName("duracion_min") val durationMinutes: Int,
    @SerialName("estado") val state: String,
)

@Serializable
data class OrderPaymentDto(
    @SerialName("payment_attempt_id") val paymentAttemptId: String,
    @SerialName("payment_intent_id") val paymentIntentId: String,
    @SerialName("stripe_account_id") val stripeAccountId: String,
    @SerialName("payment_status") val paymentStatus: String,
    @SerialName("client_secret") val clientSecret: String? = null,
    @SerialName("publishable_key") val publishableKey: String? = null,
    @SerialName("amount_cents") val amountCents: Long? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("pricing_policy_version") val pricingPolicyVersion: String? = null,
    @SerialName("application_fee_cents") val applicationFeeCents: Long? = null,
)

@Serializable
data class OpenCashSessionRequestDto(
    @SerialName("monto_inicial") val initialAmount: String,
)

@Serializable
data class CloseCashSessionRequestDto(
    @SerialName("monto_final") val finalAmount: String,
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
    /** Las alergias del alumno al pedir (migración 96); un servidor anterior no las manda. */
    @SerialName("alergias") val allergies: List<String>? = null,
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
    /** Foto actual del producto; un servidor anterior no la envía. */
    @SerialName("imagen_url") val imageUrl: String? = null,
    @SerialName("estacion_preparacion") val preparationStation: String,
    @SerialName("cantidad") val quantity: Int,
    @SerialName("precio_digital_unitario") val unitDigitalPrice: String,
    @SerialName("precio_cobro_unitario") val unitCollectionPrice: String? = null,
    @SerialName("subtotal") val subtotal: String,
    @SerialName("opciones") val options: List<OrderItemOptionDto>,
    @SerialName("rechazo") val rejection: OrderItemRejectionDto? = null,
    /** Nota de la línea; un servidor que no la conoce la omite. */
    @SerialName("notas") val notes: String? = null,
)

@Serializable
data class OrderItemRejectionDto(
    @SerialName("motivo") val reason: String,
    @SerialName("monto") val amount: String,
)

@Serializable
data class OrderItemOptionDto(
    @SerialName("opcion_id") val optionId: Int,
    @SerialName("nombre") val name: String,
    @SerialName("precio_extra") val extraPrice: String,
)
