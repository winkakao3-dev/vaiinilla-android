package com.vaiinilla.app.data.guardianship

import com.vaiinilla.app.domain.guardianship.CardReload
import com.vaiinilla.app.domain.guardianship.CardReloadCheckout
import com.vaiinilla.app.domain.guardianship.CardReloadRange
import com.vaiinilla.app.domain.guardianship.CardReloadStatus
import com.vaiinilla.app.domain.guardianship.ChildGuardian
import com.vaiinilla.app.domain.guardianship.ChildMovement
import com.vaiinilla.app.domain.guardianship.ChildWallet
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.DependentContextGrant
import com.vaiinilla.app.domain.guardianship.DependentControls
import com.vaiinilla.app.domain.guardianship.GuardianLinkCode
import com.vaiinilla.app.domain.guardianship.MenuCalendar
import com.vaiinilla.app.domain.guardianship.MenuDay
import com.vaiinilla.app.domain.guardianship.MyGuardianControls
import com.vaiinilla.app.domain.guardianship.NoServiceDay
import com.vaiinilla.app.domain.guardianship.OrderSchedule
import com.vaiinilla.app.domain.guardianship.PrivacyNotice
import com.vaiinilla.app.domain.guardianship.RefundMethod
import com.vaiinilla.app.domain.guardianship.ScheduledDayOrder
import com.vaiinilla.app.domain.guardianship.ScheduledOrder
import com.vaiinilla.app.domain.guardianship.StudentRequest
import com.vaiinilla.app.domain.guardianship.StudentRequestKind
import com.vaiinilla.app.domain.guardianship.StudentRequestStatus
import com.vaiinilla.app.domain.guardianship.toMoneyOrNull
import com.vaiinilla.app.domain.school.SchoolAllergen
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.math.BigDecimal

/**
 * JSON de Padres e hijos. Se lee a mano (JsonObject) porque varias rutas mandan nulos con
 * significado: un hijo pendiente trae `saldo: null` y `controles: null`, que no son cero.
 */
class GuardianshipJson {
    private val json = Json { ignoreUnknownKeys = true }

    /** El `data` del sobre; un sobre con `error` o sin `data` no se acepta. */
    fun data(raw: String): JsonElement {
        val root = json.parseToJsonElement(raw).jsonObject
        val error = root["error"]
        require(error == null || error is JsonNull) { "La respuesta contiene error." }
        return root["data"] ?: JsonNull
    }

    fun parseDependents(raw: String): List<Dependent> = data(raw).jsonArray.map { dependent(it.jsonObject) }

    fun parseDependent(raw: String): Dependent = dependent(data(raw).jsonObject)

    fun dependent(obj: JsonObject): Dependent {
        val pending = obj.string("estado") == "pendiente"
        val credentials = obj.obj("credenciales")
        return Dependent(
            userId = obj.requireString("usuario_id"),
            name = obj.requireString("nombre"),
            group = obj.string("grupo"),
            photoUrl = obj.string("foto_url"),
            studentId = obj.string("matricula"),
            hasOwnAccount = obj.bool("tiene_cuenta") ?: false,
            balance = if (pending) null else obj.money("saldo"),
            controls = if (pending) null else obj.obj("controles")?.let(::controls),
            isAwaitingSchool = pending,
            // Un backend sin la migración 99 no lo manda: cada tutor seguía siendo dueño de los controles.
            isPrincipal = obj.bool("soy_principal") ?: true,
            guardians =
                obj.array("tutores")?.map { item ->
                    val guardian = item.jsonObject
                    ChildGuardian(
                        id = guardian.requireString("tutela_id"),
                        name = guardian.string("nombre").orEmpty(),
                        isPrincipal = guardian.bool("principal") ?: false,
                        isYou = guardian.bool("eres_tu") ?: false,
                    )
                } ?: emptyList(),
            hasPin = obj.bool("tiene_pin") ?: false,
            hasCredential = credentials?.bool("qr") == true || credentials?.bool("nfc") == true,
            needsConsent = obj.bool("requiere_consentimiento") ?: false,
        )
    }

    private fun controls(obj: JsonObject): DependentControls =
        DependentControls(
            dailyLimit = obj.money("limite_diario"),
            lowBalanceAlert = obj.money("umbral_saldo_bajo"),
            notes = obj.string("notas").orEmpty(),
            blockedProductIds = obj.ints("productos_bloqueados").toSet(),
            blockedCategoryIds = obj.ints("categorias_bloqueadas").toSet(),
            allergens = obj.strings("alergenos").mapNotNull(SchoolAllergen::fromCode).toSet(),
            spentToday = obj.money("gastado_hoy"),
            availableToday = obj.money("disponible_hoy"),
        )

    /** Los nulos viajan: `limite_diario: null` quita el límite. Las listas se reemplazan completas. */
    fun encodeControls(controls: DependentControls): String =
        buildJsonObject {
            put("limite_diario", controls.dailyLimit?.toPlainString())
            put("umbral_saldo_bajo", controls.lowBalanceAlert?.toPlainString())
            put("notas", controls.notes.trim().ifEmpty { null })
            put("productos_bloqueados", intArray(controls.blockedProductIds.sorted()))
            put("categorias_bloqueadas", intArray(controls.blockedCategoryIds.sorted()))
            put(
                "alergenos",
                buildJsonArray {
                    controls.allergens
                        .map { it.code }
                        .sorted()
                        .forEach { add(JsonPrimitive(it)) }
                },
            )
        }.toString()

    /** Solo `umbral_saldo_bajo`, también en nulo: el backend rechaza a quien no es el principal si manda algo más. */
    fun encodeLowBalanceAlert(amount: BigDecimal?): String =
        buildJsonObject { put("umbral_saldo_bajo", amount?.toPlainString()) }.toString()

    fun encodeNewDependent(
        name: String,
        studentId: String,
        group: String?,
        consentVersion: String,
    ): String =
        buildJsonObject {
            put("nombre", name.trim())
            put("matricula", studentId.trim())
            group?.trim()?.takeIf { it.isNotEmpty() }?.let { put("grupo", it) }
            put("consentimiento_version", consentVersion)
        }.toString()

    fun encodeLink(
        code: String,
        consentVersion: String,
    ): String =
        buildJsonObject {
            put("codigo", code)
            put("consentimiento_version", consentVersion)
        }.toString()

    fun encodeConsent(version: String): String = buildJsonObject { put("consentimiento_version", version) }.toString()

    fun encodeCode(code: String): String = buildJsonObject { put("codigo", code) }.toString()

    fun encodeSchoolCode(code: String): String = buildJsonObject { put("codigo_union", code) }.toString()

    fun encodePin(pin: String): String = buildJsonObject { put("pin", pin) }.toString()

    fun encodeStudentRequest(
        kind: StudentRequestKind,
        refundMethod: RefundMethod?,
        note: String?,
    ): String =
        buildJsonObject {
            put("tipo", kind.wireValue)
            if (kind == StudentRequestKind.REFUND) refundMethod?.let { put("como_devolver", it.wireValue) }
            note?.trim()?.takeIf { it.isNotEmpty() }?.let { put("nota", it) }
        }.toString()

    fun parsePrivacyNotice(raw: String): PrivacyNotice {
        val obj = data(raw).jsonObject
        return PrivacyNotice(
            version = obj.requireString("version"),
            isDraft = obj.bool("borrador") ?: false,
            title = obj.string("titulo").orEmpty(),
            paragraphs = obj.strings("parrafos"),
        )
    }

    fun parseLinkCode(raw: String): GuardianLinkCode {
        val obj = data(raw).jsonObject
        return GuardianLinkCode(code = obj.requireString("codigo"), expiresAt = obj.string("vence_en"))
    }

    fun parseContext(raw: String): DependentContextGrant {
        val obj = data(raw).jsonObject
        return DependentContextGrant(
            accessToken = obj.requireString("access_token"),
            expiresInSeconds = obj.primitive("expires_in")?.longOrNull ?: DEFAULT_CONTEXT_SECONDS,
        )
    }

    fun parseClaimedName(raw: String): String = data(raw).jsonObject.requireString("nombre")

    fun parseBlocked(raw: String): Int = data(raw).jsonObject.primitive("bloqueadas")?.intOrNull ?: 0

    fun parseMyControls(raw: String): MyGuardianControls? {
        val obj = data(raw).jsonObject
        val controls = obj.obj("controles") ?: return null
        return MyGuardianControls(
            tutorNames = obj.array("tutores")?.mapNotNull { it.jsonObject.string("nombre") } ?: emptyList(),
            dailyLimit = controls.money("limite_diario"),
            availableToday = controls.money("disponible_hoy"),
            blockedProductIds = controls.ints("productos_bloqueados").toSet(),
            allergenProductIds = controls.ints("productos_con_alergeno").toSet(),
            unreviewedProductIds = controls.ints("productos_sin_revisar").toSet(),
            allergens = controls.strings("alergenos").mapNotNull(SchoolAllergen::fromCode),
            blockedCategoryIds = controls.ints("categorias_bloqueadas").toSet(),
        )
    }

    fun parseStudentRequests(raw: String): List<StudentRequest> =
        data(raw).jsonArray.mapNotNull {
            studentRequest(it.jsonObject)
        }

    fun parseStudentRequest(raw: String): StudentRequest =
        requireNotNull(studentRequest(data(raw).jsonObject)) { "La solicitud tiene un tipo o estado desconocido." }

    /** Un tipo o estado que esta versión no conoce se omite. */
    private fun studentRequest(obj: JsonObject): StudentRequest? {
        val kind = obj.string("tipo")?.let(StudentRequestKind::fromWire) ?: return null
        val status = obj.string("estado")?.let(StudentRequestStatus::fromWire) ?: return null
        return StudentRequest(
            id = obj.requireString("id"),
            kind = kind,
            status = status,
            refundMethod = RefundMethod.fromWire(obj.string("como_devolver")),
            note = obj.string("nota"),
            balanceAtRequest = obj.money("saldo_al_pedir") ?: BigDecimal.ZERO.setScale(2),
            refunded = obj.money("monto_devuelto"),
            rejectionReason = obj.string("motivo_rechazo"),
            createdAt = obj.string("creado_en"),
            resolvedAt = obj.string("resuelta_en"),
        )
    }

    // Lo que se ve con el contexto del hijo

    fun parseChildWallet(raw: String): ChildWallet {
        val obj = data(raw).jsonObject
        val range = obj.obj("recarga_tarjeta")
        return ChildWallet(
            balance = obj.obj("wallet")?.money("saldo") ?: BigDecimal.ZERO.setScale(2),
            movements =
                obj.array("movimientos")?.map { item ->
                    val movement = item.jsonObject
                    ChildMovement(
                        id = movement.requireString("id"),
                        type = movement.string("tipo").orEmpty(),
                        description = movement.string("descripcion").orEmpty(),
                        amount = movement.money("monto") ?: BigDecimal.ZERO,
                        balanceAfter = movement.money("saldo_posterior") ?: BigDecimal.ZERO,
                        createdAt = movement.string("creado_en").orEmpty(),
                    )
                } ?: emptyList(),
            reloadRange = range?.let { CardReloadRange(minimum = it.money("minima"), maximum = it.money("maxima")) },
        )
    }

    fun encodeReload(amount: BigDecimal): String =
        buildJsonObject {
            put("monto", amount.setScale(2).toPlainString())
        }.toString()

    fun parseReloadCheckout(raw: String): CardReloadCheckout {
        val obj = data(raw).jsonObject
        val payment = requireNotNull(obj.obj("pago")) { "La recarga no trae el pago." }
        return CardReloadCheckout(
            reload = reload(requireNotNull(obj.obj("recarga")) { "Falta la recarga." }),
            clientSecret = payment.requireString("client_secret"),
            publishableKey = payment.requireString("publishable_key"),
            stripeAccountId = payment.requireString("stripe_account_id"),
        )
    }

    fun parseReloadStatus(raw: String): CardReload =
        reload(
            requireNotNull(data(raw).jsonObject.obj("recarga")) {
                "Falta la recarga."
            },
        )

    private fun reload(obj: JsonObject): CardReload {
        val amount = requireNotNull(obj.money("monto")) { "La recarga no trae monto." }
        return CardReload(
            id = obj.requireString("id"),
            amount = amount,
            chargedCents = obj.primitive("total_cobrado_cents")?.longOrNull ?: amount.movePointRight(2).toLong(),
            status = CardReloadStatus.fromWire(obj.string("estado").orEmpty()),
        )
    }

    fun parseMenuCalendar(raw: String): MenuCalendar {
        val obj = data(raw).jsonObject
        val schedule = requireNotNull(obj.obj("programacion")) { "Falta la programación." }
        return MenuCalendar(
            schedule =
                OrderSchedule(
                    today = schedule.requireString("hoy").take(10),
                    daysAhead = schedule.primitive("dias_adelanto_pedido")?.intOrNull ?: 0,
                    orderCutoff = schedule.string("hora_corte_pedido")?.take(5),
                    cancelCutoff = schedule.string("hora_corte_cancelacion")?.take(5),
                    slots = schedule.strings("franjas"),
                ),
            days =
                obj.array("dias")?.map {
                    val day = it.jsonObject
                    MenuDay(date = day.requireString("fecha").take(10), productIds = day.ints("producto_ids"))
                } ?: emptyList(),
            closedDays =
                obj.array("sin_servicio")?.map {
                    val day = it.jsonObject
                    NoServiceDay(date = day.requireString("fecha").take(10), reason = day.string("motivo").orEmpty())
                } ?: emptyList(),
        )
    }

    fun encodeWeek(days: List<ScheduledDayOrder>): String =
        buildJsonObject {
            put(
                "pedidos",
                buildJsonArray {
                    days.forEach { day ->
                        add(
                            buildJsonObject {
                                put("fecha_entrega", day.date)
                                day.slot?.let { put("franja_entrega", it) }
                                put(
                                    "items",
                                    buildJsonArray {
                                        day.items.forEach { item ->
                                            add(
                                                buildJsonObject {
                                                    put("producto_id", item.productId)
                                                    put("cantidad", item.quantity)
                                                    put("opcion_ids", intArray(item.optionIds))
                                                },
                                            )
                                        }
                                    },
                                )
                            },
                        )
                    }
                },
            )
        }.toString()

    fun parseWeekCount(raw: String): Int = data(raw).jsonObject.array("pedidos")?.size ?: 0

    /** La lista de pedidos del hijo, solo con lo que la ficha muestra de lo apartado. */
    fun parseScheduledOrders(raw: String): List<ScheduledOrder> =
        data(raw).jsonArray.mapNotNull { item ->
            val obj = item.jsonObject
            runCatching {
                ScheduledOrder(
                    id = obj.requireString("id"),
                    folio = obj.primitive("folio")?.intOrNull ?: 0,
                    statusWire = obj.string("estado").orEmpty(),
                    version = obj.primitive("version")?.intOrNull ?: 0,
                    total = obj.money("total") ?: BigDecimal.ZERO,
                    deliveryDate = obj.string("fecha_entrega")?.take(10),
                    deliverySlot = obj.string("franja_entrega"),
                    summary =
                        obj
                            .array("items")
                            ?.joinToString(", ") {
                                val line = it.jsonObject
                                "${line
                                    .primitive(
                                        "cantidad",
                                    )?.intOrNull ?: 1}× ${line.string("nombre_producto").orEmpty()}"
                            }.orEmpty(),
                )
            }.getOrNull()
        }

    fun encodeCancel(
        expectedVersion: Int,
        reason: String,
    ): String =
        buildJsonObject {
            put("version_esperada", expectedVersion)
            put("motivo", reason)
        }.toString()

    private fun intArray(values: List<Int>): JsonArray = buildJsonArray { values.forEach { add(JsonPrimitive(it)) } }

    private companion object {
        const val DEFAULT_CONTEXT_SECONDS = 900L
    }
}

private fun JsonObject.primitive(key: String): JsonPrimitive? =
    (this[key] as? JsonPrimitive)?.takeUnless {
        it is JsonNull
    }

private fun JsonObject.string(key: String): String? = primitive(key)?.contentOrNull

private fun JsonObject.requireString(key: String): String = requireNotNull(string(key)) { "Falta '$key'." }

private fun JsonObject.bool(key: String): Boolean? = primitive(key)?.booleanOrNull

private fun JsonObject.money(key: String): BigDecimal? = string(key)?.toMoneyOrNull()

private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

private fun JsonObject.array(key: String): JsonArray? = this[key] as? JsonArray

private fun JsonObject.ints(key: String): List<Int> =
    array(key)?.mapNotNull { (it as? JsonPrimitive)?.intOrNull } ?: emptyList()

private fun JsonObject.strings(key: String): List<String> =
    array(key)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()
