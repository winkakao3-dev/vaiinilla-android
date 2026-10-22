package com.vaiinilla.app.data.admin

import com.vaiinilla.app.domain.admin.AdminMetric
import com.vaiinilla.app.domain.admin.AdminWalletSummary
import com.vaiinilla.app.domain.admin.CashbackRule
import com.vaiinilla.app.domain.admin.InvitationsPage
import com.vaiinilla.app.domain.admin.ManagedSpace
import com.vaiinilla.app.domain.admin.ReservationSchedule
import com.vaiinilla.app.domain.admin.SpaceBatchResult
import com.vaiinilla.app.domain.admin.SpacePatch
import com.vaiinilla.app.domain.admin.StaffInvitation
import com.vaiinilla.app.domain.admin.StaffMembership
import com.vaiinilla.app.domain.admin.StoreConfig
import com.vaiinilla.app.domain.admin.StoreFlow
import com.vaiinilla.app.domain.admin.StoreVideo
import com.vaiinilla.app.domain.admin.StripeSetup
import com.vaiinilla.app.domain.model.OrderWindow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/** Contratos de Personal, Flujo de mi tienda, Stripe, cashback y espacios. */
class AdminBusinessJson {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    // region Personal

    fun parseInvitationsPage(raw: String): InvitationsPage {
        val envelope = json.decodeFromString<BizPageEnvelope<List<InvitationDto>>>(raw)
        return InvitationsPage(envelope.data.map { it.toDomain() }, envelope.meta?.cursor)
    }

    fun parseInvitation(raw: String): StaffInvitation =
        json.decodeFromString<BizEnvelope<InvitationDto>>(raw).data.toDomain()

    fun parseMemberships(raw: String): List<StaffMembership> =
        json
            .decodeFromString<BizEnvelope<List<MembershipDto>>>(
                raw,
            ).data
            .filter { it.activo != false }
            .map { it.toDomain() }

    fun parseMembership(raw: String): StaffMembership =
        json.decodeFromString<BizEnvelope<MembershipDto>>(raw).data.toDomain()

    fun encodeInvitation(
        email: String,
        role: String,
    ): String =
        buildJsonObject {
            put("email", email)
            put("rol", role)
        }.toString()

    fun encodeRole(role: String): String = buildJsonObject { put("rol", role) }.toString()

    // endregion

    // region Flujo de mi tienda

    fun parseStoreConfig(raw: String): StoreConfig =
        json.decodeFromString<BizEnvelope<StoreConfigDto>>(raw).data.toDomain()

    fun encodeFlow(flow: StoreFlow): String =
        buildJsonObject {
            put("tipo", flow.typeWire)
            put("entrega_requiere_qr", flow.deliveryRequiresQr)
            put("permite_pago_al_final", flow.allowsPayAtEnd)
            put("permite_saldo", flow.allowsBalance)
            put("gracia_liberacion_min", flow.releaseGraceMinutes)
            put(
                "franjas_pedido",
                buildJsonArray {
                    flow.orderWindows.forEach { window ->
                        add(
                            buildJsonObject {
                                put("desde", window.from)
                                put("hasta", window.to)
                            },
                        )
                    }
                },
            )
        }.toString()

    fun encodeVideos(videos: List<StoreVideo>): String =
        buildJsonObject {
            put(
                "videos_local",
                buildJsonArray {
                    videos.forEach { video ->
                        add(
                            buildJsonObject {
                                put("titulo", video.title.trim())
                                put("url", video.url.trim())
                            },
                        )
                    }
                },
            )
        }.toString()

    fun encodeDirectory(visible: Boolean): String = buildJsonObject { put("visible_en_directorio", visible) }.toString()

    fun encodeSchoolMode(isSchool: Boolean): String = buildJsonObject { put("modo_escuela", isSchool) }.toString()

    // endregion

    // region Stripe

    /** `data: null` = el negocio aún no conecta Stripe. */
    fun parseStripeSetup(raw: String): StripeSetup? =
        json.decodeFromString<BizNullableEnvelope<StripeDto>>(raw).data?.toDomain()

    fun parseOnboardingLink(raw: String): String =
        json.decodeFromString<BizEnvelope<OnboardingDto>>(raw).data.accountLinkUrl

    fun encodeCardPayments(enabled: Boolean): String = buildJsonObject { put("stripe_enabled", enabled) }.toString()

    fun encodeFeePaidByCustomer(paid: Boolean): String =
        buildJsonObject {
            put("pasar_comision_al_cliente", paid)
        }.toString()

    // endregion

    // region Cashback

    fun parseCashbackRule(raw: String): CashbackRule? =
        json.decodeFromString<BizNullableEnvelope<CashbackDto>>(raw).data?.toDomain()

    /** Todas las claves van, con null explícito donde no hay valor: la regla se reemplaza completa. */
    fun encodeCashbackRule(rule: CashbackRule): String =
        buildJsonObject {
            put("nombre", rule.name)
            put("porcentaje", rule.percent)
            put("hora_inicio", rule.startTime.orNull())
            put("hora_fin", rule.endTime.orNull())
            put("dias_activos", rule.activeDays?.let { days -> JsonArray(days.map(::JsonPrimitive)) } ?: JsonNull)
            put("vigencia_inicio", rule.validFrom.orNull())
            put("vigencia_fin", rule.validTo.orNull())
            put("activa", rule.active)
        }.toString()

    fun parseWalletSummary(raw: String): AdminWalletSummary {
        val data = json.decodeFromString<BizEnvelope<WalletReportDto>>(raw).data
        return AdminWalletSummary(
            reloads = data.resumen.recargas ?: "0.00",
            purchases = data.resumen.comprasSaldo ?: "0.00",
            cashback = data.resumen.cashbackOtorgado ?: "0.00",
            cancellations = data.resumen.cancelacionesWallet ?: "0.00",
            canceledOrders = data.resumen.pedidosCancelados,
            movements =
                data.wallet
                    ?.movimientos
                    .orEmpty()
                    .map { AdminMetric(walletMovementLabel(it.tipo), it.monto, it.operaciones) },
            walletsReviewed = data.wallet?.conciliacion?.walletsRevisadas ?: 0,
            alerts = data.wallet?.conciliacion?.alertas ?: 0,
        )
    }

    // endregion

    // region Espacios

    fun parseSpaces(raw: String): List<ManagedSpace> =
        json.decodeFromString<BizEnvelope<List<ManagedSpaceDto>>>(raw).data.map {
            it.toDomain()
        }

    fun parseSpace(raw: String): ManagedSpace = json.decodeFromString<BizEnvelope<ManagedSpaceDto>>(raw).data.toDomain()

    fun parseBatch(raw: String): SpaceBatchResult {
        val data = json.decodeFromString<BizEnvelope<BatchDto>>(raw).data
        return SpaceBatchResult(data.solicitados, data.creados, data.omitidos)
    }

    fun encodeNewSpace(
        name: String,
        type: String,
    ): String =
        buildJsonObject {
            put("nombre", name.trim())
            put("tipo", type)
        }.toString()

    fun encodeBatch(
        type: String,
        prefix: String,
        from: Int,
        to: Int,
    ): String =
        buildJsonObject {
            put("tipo", type)
            put("prefijo", prefix.trim())
            put("desde", from)
            put("hasta", to)
        }.toString()

    fun encodeSpacePatch(patch: SpacePatch): String =
        buildJsonObject {
            patch.name?.let { put("nombre", it.trim()) }
            patch.typeWire?.let { put("tipo", it) }
            patch.active?.let { put("activo", it) }
            if (patch.setsHourlyPrice) put("precio_hora", patch.hourlyPrice.orNull())
            if (patch.setsProfile) {
                put(
                    "descripcion",
                    patch.description
                        ?.trim()
                        ?.ifEmpty { null }
                        .orNull(),
                )
                put("caracteristicas", JsonArray(patch.features.map(::JsonPrimitive)))
            }
        }.toString()

    fun parseSchedule(raw: String): ReservationSchedule {
        val data = json.decodeFromString<BizEnvelope<ScheduleDto>>(raw).data
        return ReservationSchedule(
            opening = data.apertura.take(5),
            closing = data.cierre.take(5),
            daysAhead = data.diasAdelanto,
        )
    }

    fun encodeSchedule(schedule: ReservationSchedule): String =
        buildJsonObject {
            put("apertura", schedule.opening)
            put("cierre", schedule.closing)
            put("dias_adelanto", schedule.daysAhead)
        }.toString()

    fun encodeForcedClose(note: String?): String =
        buildJsonObject {
            put("forzar", true)
            note?.trim()?.takeIf { it.isNotEmpty() }?.let { put("nota", it) }
        }.toString()

    // endregion

    fun parseCursorCount(raw: String): Pair<Int, Boolean> {
        val envelope = json.decodeFromString<BizPageEnvelope<List<JsonElement>>>(raw)
        return envelope.data.size to (envelope.meta?.cursor != null)
    }

    companion object {
        fun walletMovementLabel(type: String): String =
            when (type) {
                "recarga_efectivo" -> "Recargas en efectivo"
                "compra" -> "Compras con saldo"
                "cashback" -> "Cashback"
                "cancelacion" -> "Cancelaciones"
                "ajuste" -> "Ajustes"
                else -> type.replace('_', ' ').replaceFirstChar { it.uppercase() }
            }
    }
}

private fun String?.orNull(): JsonElement = this?.let(::JsonPrimitive) ?: JsonNull

@Serializable
private data class BizEnvelope<T>(
    val data: T,
)

@Serializable
private data class BizNullableEnvelope<T>(
    val data: T? = null,
)

@Serializable
private data class BizPageEnvelope<T>(
    val data: T,
    val meta: PageMetaDto? = null,
)

@Serializable
private data class PageMetaDto(
    val cursor: String? = null,
)

@Serializable
private data class InvitationDto(
    val id: String,
    val email: String,
    val rol: String,
    val estado: String,
    @SerialName("expira_en") val expiraEn: String = "",
    @SerialName("creado_en") val creadoEn: String = "",
    @SerialName("membresia_id") val membresiaId: String? = null,
) {
    fun toDomain() = StaffInvitation(id, email, rol, estado, expiraEn, creadoEn, membresiaId)
}

@Serializable
private data class MembershipDto(
    val id: String,
    val nombre: String? = null,
    val email: String? = null,
    val rol: String,
    val activo: Boolean? = null,
    @SerialName("creado_en") val creadoEn: String = "",
) {
    fun toDomain() = StaffMembership(id, nombre.orEmpty(), email.orEmpty(), rol, creadoEn)
}

@Serializable
private data class StoreConfigDto(
    val tipo: String = "cafeteria",
    @SerialName("entrega_requiere_qr") val entregaRequiereQr: Boolean = true,
    @SerialName("permite_pago_al_final") val permitePagoAlFinal: Boolean = false,
    @SerialName("permite_saldo") val permiteSaldo: Boolean = true,
    @SerialName("gracia_liberacion_min") val graciaLiberacionMin: Int = 5,
    @SerialName("franjas_pedido") val franjasPedido: List<WindowDto> = emptyList(),
    @SerialName("videos_local") val videosLocal: List<VideoDto> = emptyList(),
    @SerialName("visible_en_directorio") val visibleEnDirectorio: Boolean = false,
    @SerialName("tienda_url") val tiendaUrl: String? = null,
    @SerialName("imagen_url") val imagenUrl: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("modo_escuela") val modoEscuela: Boolean = false,
) {
    fun toDomain() =
        StoreConfig(
            typeWire = tipo,
            deliveryRequiresQr = entregaRequiereQr,
            allowsPayAtEnd = permitePagoAlFinal,
            allowsBalance = permiteSaldo,
            releaseGraceMinutes = graciaLiberacionMin,
            orderWindows = franjasPedido.map { OrderWindow(it.desde.take(5), it.hasta.take(5)) },
            videos = videosLocal.map { StoreVideo(it.titulo, it.url) },
            visibleInDirectory = visibleEnDirectorio,
            storeUrl = tiendaUrl,
            imageUrl = imagenUrl,
            logoUrl = logoUrl,
            isSchool = modoEscuela,
        )
}

@Serializable
private data class WindowDto(
    val desde: String,
    val hasta: String,
)

@Serializable
private data class VideoDto(
    val titulo: String,
    val url: String,
)

@Serializable
private data class StripeDto(
    @SerialName("stripe_enabled") val stripeEnabled: Boolean = false,
    @SerialName("charges_enabled") val chargesEnabled: Boolean = false,
    @SerialName("payouts_enabled") val payoutsEnabled: Boolean = false,
    @SerialName("estado_onboarding") val estadoOnboarding: String = "pendiente",
    @SerialName("pasar_comision_al_cliente") val pasarComision: Boolean = false,
) {
    fun toDomain() = StripeSetup(stripeEnabled, chargesEnabled, payoutsEnabled, estadoOnboarding, pasarComision)
}

@Serializable
private data class OnboardingDto(
    @SerialName("account_link_url") val accountLinkUrl: String,
)

@Serializable
private data class CashbackDto(
    val nombre: String = "Regla principal",
    val porcentaje: String = "0.00",
    @SerialName("hora_inicio") val horaInicio: String? = null,
    @SerialName("hora_fin") val horaFin: String? = null,
    @SerialName("dias_activos") val diasActivos: List<Int>? = null,
    @SerialName("vigencia_inicio") val vigenciaInicio: String? = null,
    @SerialName("vigencia_fin") val vigenciaFin: String? = null,
    val activa: Boolean = false,
    @SerialName("actualizado_en") val actualizadoEn: String? = null,
) {
    // Las horas llegan como "HH:MM:SS"; la app trabaja con "HH:MM".
    fun toDomain() =
        CashbackRule(
            name = nombre,
            percent = porcentaje,
            startTime = horaInicio?.take(5),
            endTime = horaFin?.take(5),
            activeDays = diasActivos,
            validFrom = vigenciaInicio?.take(10),
            validTo = vigenciaFin?.take(10),
            active = activa,
            updatedAt = actualizadoEn,
        )
}

@Serializable
private data class WalletReportDto(
    val resumen: WalletTotalsDto,
    val wallet: WalletBlockDto? = null,
)

@Serializable
private data class WalletTotalsDto(
    val recargas: String? = null,
    @SerialName("compras_saldo") val comprasSaldo: String? = null,
    @SerialName("cashback_otorgado") val cashbackOtorgado: String? = null,
    @SerialName("cancelaciones_wallet") val cancelacionesWallet: String? = null,
    @SerialName("pedidos_cancelados") val pedidosCancelados: Int? = null,
)

@Serializable
private data class WalletBlockDto(
    val movimientos: List<WalletMovementDto> = emptyList(),
    val conciliacion: ConciliationDto? = null,
)

@Serializable
private data class WalletMovementDto(
    val tipo: String,
    val monto: String,
    val operaciones: Int = 0,
)

@Serializable
private data class ConciliationDto(
    @SerialName("wallets_revisadas") val walletsRevisadas: Int = 0,
    val alertas: Int = 0,
)

@Serializable
private data class ManagedSpaceDto(
    val id: Int,
    val nombre: String,
    val tipo: String,
    val activo: Boolean = true,
    @SerialName("qr_url") val qrUrl: String = "",
    val codigo: JsonPrimitive? = null,
    @SerialName("precio_hora") val precioHora: JsonPrimitive? = null,
    val descripcion: String? = null,
    @SerialName("imagen_url") val imagenUrl: String? = null,
    val caracteristicas: List<String> = emptyList(),
) {
    fun toDomain() =
        ManagedSpace(
            id = id,
            name = nombre,
            typeWire = tipo,
            active = activo,
            qrUrl = qrUrl,
            code = codigo?.contentOrNull,
            hourlyPrice = precioHora?.contentOrNull,
            description = descripcion,
            imageUrl = imagenUrl,
            features = caracteristicas,
        )
}

@Serializable
private data class BatchDto(
    val solicitados: Int = 0,
    val creados: Int = 0,
    val omitidos: List<String> = emptyList(),
)

@Serializable
private data class ScheduleDto(
    val apertura: String,
    val cierre: String,
    @SerialName("dias_adelanto") val diasAdelanto: Int,
)
