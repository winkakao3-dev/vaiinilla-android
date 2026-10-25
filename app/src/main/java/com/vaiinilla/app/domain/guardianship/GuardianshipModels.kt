package com.vaiinilla.app.domain.guardianship

import com.vaiinilla.app.domain.model.NameInitials
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.school.SchoolAllergen
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

// Padres e hijos (docs/padres-hijos.md del backend): un tutor ve a sus hijos, les recarga
// saldo con tarjeta, les pone controles y les aparta la comida de la semana.

/** Lo que el tutor decide sobre lo que su hijo compra sin él (en Caja o en su propia app). */
data class DependentControls(
    val dailyLimit: BigDecimal? = null,
    val lowBalanceAlert: BigDecimal? = null,
    val notes: String = "",
    val blockedProductIds: Set<Int> = emptySet(),
    val blockedCategoryIds: Set<Int> = emptySet(),
    val allergens: Set<SchoolAllergen> = emptySet(),
)

/** Uno de los tutores de un hijo, como lo ve otro de sus tutores. [id] es la tutela. */
data class ChildGuardian(
    val id: String,
    val name: String,
    val isPrincipal: Boolean,
    val isYou: Boolean,
) {
    val firstName: String get() = PersonNames.first(name)
}

data class Dependent(
    val userId: String,
    val name: String,
    val group: String?,
    val photoUrl: String?,
    val studentId: String?,
    /** Tiene su propia cuenta de Vaiinilla (se vinculó con código) o solo existe para Caja. */
    val hasOwnAccount: Boolean,
    /** `null` mientras la escuela no aprueba al tutor: no es cero, es que no se ve. */
    val balance: BigDecimal?,
    /** `null` mientras la escuela no aprueba al tutor. */
    val controls: DependentControls?,
    val isAwaitingSchool: Boolean = false,
    /** Quien lo dio de alta decide los controles e invita o quita a los demás tutores. */
    val isPrincipal: Boolean = true,
    val guardians: List<ChildGuardian> = emptyList(),
    /** Caja le pide un PIN antes de cobrarle con su saldo. */
    val hasPin: Boolean = false,
    /** Tiene un QR o una tarjeta activos que se pueden bloquear si los pierde. */
    val hasCredential: Boolean = false,
    /** El tutor no ha aceptado el aviso de privacidad vigente para este hijo. */
    val needsConsent: Boolean = false,
) {
    val firstName: String get() = PersonNames.first(name)

    val initials: String get() = PersonNames.initials(name)

    /** El nombre de pila de quien pone los controles, si no eres tú. */
    val principalFirstName: String?
        get() = if (isPrincipal) null else guardians.firstOrNull { it.isPrincipal }?.firstName

    /** El saldo ya bajó del aviso que puso el tutor. */
    val isLowBalance: Boolean
        get() {
            val threshold = controls?.lowBalanceAlert ?: return false
            val current = balance ?: return false
            // El servidor avisa con el saldo estrictamente por debajo del umbral: la ficha dice lo mismo.
            return current < threshold
        }

    /** "3°B · A-001", lo que identifica al hijo en la escuela. */
    val schoolLine: String?
        get() =
            listOfNotNull(group, studentId)
                .map(String::trim)
                .filter(String::isNotEmpty)
                .takeIf { it.isNotEmpty() }
                ?.joinToString(" · ")

    companion object {
        /** El PIN de Caja: cuatro dígitos, para que un niño lo recuerde. */
        fun isValidPin(pin: String): Boolean = pin.length == 4 && pin.all { it in '0'..'9' }
    }
}

object PersonNames {
    fun first(name: String): String =
        name
            .trim()
            .split(Regex("\\s+"))
            .firstOrNull()
            ?.takeIf { it.isNotEmpty() } ?: name

    fun initials(name: String): String = NameInitials.of(name).ifEmpty { "?" }

    /** "Tilin", "Tilin y Ana" o "Tilin, Ana y Leo". */
    fun list(names: List<String>): String =
        when (names.size) {
            0 -> ""
            1 -> names[0]
            else -> names.dropLast(1).joinToString(", ") + " y " + names.last()
        }
}

/** El código que un alumno (6), el principal (6) o el tutor para la cuenta propia (8) saca. */
data class GuardianLinkCode(
    val code: String,
    /** ISO-8601 del servidor; la UI lo muestra en hora local. */
    val expiresAt: String?,
)

/** El aviso de privacidad que el tutor acepta antes de agregar a su hijo; su versión viaja con el alta y el vínculo. */
data class PrivacyNotice(
    val version: String,
    /** Texto aún sin revisar por un abogado: la hoja lo dice. */
    val isDraft: Boolean,
    val title: String,
    val paragraphs: List<String>,
) {
    companion object {
        /** El servidor responde así cuando el aviso cambió desde que la hoja lo cargó. */
        const val CONSENT_REQUIRED = "CONSENT_REQUIRED"
    }
}

object GuardianLinkCodeRules {
    private const val ALLOWED = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    /** Seis letras o números sin los que se confunden (I, O, 0, 1). */
    fun normalized(text: String): String? = normalized(text, 6)

    /** El de "Pasar a su propia cuenta": ocho, con las mismas letras. */
    fun normalizedAccountCode(text: String): String? = normalized(text, 8)

    /** Lo tecleado sin espacios ni guiones, para saber si ya se puede juzgar. */
    fun typedLength(text: String): Int = text.count { !it.isWhitespace() && it != '-' }

    private fun normalized(
        text: String,
        length: Int,
    ): String? {
        val code = text.uppercase().filter { !it.isWhitespace() && it != '-' }
        return code.takeIf { it.length == length && it.all(ALLOWED::contains) }
    }
}

/** El código que la escuela le da a los papás: de 5 a 8 letras o números, sin I, O, 0 ni 1. */
object SchoolJoinCodeRules {
    const val MAX_LENGTH = 8
    private const val ALLOWED = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    /** Lo que queda de lo tecleado: mayúsculas válidas, hasta ocho. */
    fun sanitized(text: String): String = text.uppercase().filter(ALLOWED::contains).take(MAX_LENGTH)

    fun isComplete(code: String): Boolean = code.length in 5..MAX_LENGTH && code.all(ALLOWED::contains)
}

/** El token del hijo con el que el tutor consulta y pide por él (15 minutos). */
data class DependentContextGrant(
    val accessToken: String,
    val expiresInSeconds: Long,
)

// Lo que ve el hijo

/** Por qué un producto no se le vende al hijo: en su propia app, o lo que su tutor le aparta. */
data class ChildRestriction(
    val reason: Reason,
    val label: String,
) {
    enum class Reason { BLOCKED, ALLERGEN, ALLERGENS_UNKNOWN, SOLD_OUT }

    companion object {
        val Blocked = ChildRestriction(Reason.BLOCKED, "Tu tutor lo bloqueó")
        val Allergen = ChildRestriction(Reason.ALLERGEN, "Tiene algo que no puedes comer")
        val AllergensUnknown = ChildRestriction(Reason.ALLERGENS_UNKNOWN, "Sin información de alérgenos")

        /** Lo mismo, dicho al tutor. */
        val ChildAllergen = ChildRestriction(Reason.ALLERGEN, "Tiene algo a lo que es alérgico")

        /** El negocio lo apagó: se ve en el menú, pero nadie lo puede pedir (B10). */
        val SoldOut = ChildRestriction(Reason.SOLD_OUT, "Agotado")
    }
}

/**
 * Por qué no se puede agregar un producto del menú: agotado para cualquiera, y para el alumno con tutor
 * además lo que su tutor bloqueó o tiene un alérgeno suyo.
 */
fun MyGuardianControls?.restrictionFor(product: Product): ChildRestriction? =
    if (!product.available) ChildRestriction.SoldOut else this?.restriction(product.id)

/**
 * Los controles que el tutor le puso al hijo, vistos por el hijo antes de pagar o por el tutor al apartarle.
 * Solo existen con una tutela activa.
 */
data class MyGuardianControls(
    val tutorNames: List<String>,
    val dailyLimit: BigDecimal?,
    val availableToday: BigDecimal?,
    val blockedProductIds: Set<Int>,
    val allergenProductIds: Set<Int>,
    /** Sin alérgenos revisados por la escuela; solo llegan si el hijo tiene alguna alergia. */
    val unreviewedProductIds: Set<Int> = emptySet(),
    val allergens: List<SchoolAllergen>,
) {
    /** El bloqueo del tutor gana sobre el alérgeno: es lo que el backend responde primero. */
    fun restriction(productId: Int): ChildRestriction? =
        when (productId) {
            in blockedProductIds -> ChildRestriction.Blocked
            in allergenProductIds -> ChildRestriction.Allergen
            in unreviewedProductIds -> ChildRestriction.AllergensUnknown
            else -> null
        }

    /** Lo que el tutor no puede apartarle: sus bloqueos y su límite no lo frenan a él, las alergias sí. */
    fun guardianRestriction(productId: Int): ChildRestriction? =
        when (productId) {
            in allergenProductIds -> ChildRestriction.ChildAllergen
            in unreviewedProductIds -> ChildRestriction.AllergensUnknown
            else -> null
        }

    /** ¿Este total cabe en lo que le queda hoy? Sin límite, siempre. */
    fun fitsToday(total: BigDecimal): Boolean = availableToday == null || total <= availableToday

    /** Bajo "Te cuida…" en Cartera, como iOS: su límite y lo que le queda hoy, y cuántos productos no puede pedir. */
    val walletDetail: String?
        get() {
            val details = mutableListOf<String>()
            if (dailyLimit != null && availableToday != null) {
                details +=
                    "Límite de ${compactMoney(dailyLimit)} al día; hoy te quedan ${compactMoney(availableToday)}."
            }
            val restricted = (blockedProductIds + allergenProductIds + unreviewedProductIds).size
            if (restricted > 0) {
                details +=
                    if (restricted ==
                        1
                    ) {
                        "1 producto no está a tu alcance."
                    } else {
                        "$restricted productos no están a tu alcance."
                    }
            }
            return details.takeIf { it.isNotEmpty() }?.joinToString("\n")
        }

    /** "Te cuida Ana" o "Te cuidan Ana y Luis". */
    val caretakerLine: String?
        get() =
            when {
                tutorNames.isEmpty() -> null
                tutorNames.size == 1 -> "Te cuida ${tutorNames[0]}"
                else -> "Te cuidan ${PersonNames.list(tutorNames)}"
            }
}

// Recarga con tarjeta

/** El rango que el negocio permite en una recarga con tarjeta; sin límite, `null`. */
data class CardReloadRange(
    val minimum: BigDecimal?,
    val maximum: BigDecimal?,
) {
    /** "De $50 a $2000", o `null` si no hay límites. */
    val label: String?
        get() =
            when {
                minimum != null && maximum != null -> "De ${compactMoney(minimum)} a ${compactMoney(maximum)}"
                minimum != null -> "Desde ${compactMoney(minimum)}"
                maximum != null -> "Hasta ${compactMoney(maximum)}"
                else -> null
            }

    /** Se compara como número: como texto, "50.00" sería mayor que "2000.00". */
    fun problem(amount: BigDecimal?): String? {
        if (amount == null || amount.signum() <= 0) return null
        if (minimum != null && amount < minimum) return "El mínimo es ${compactMoney(minimum)}."
        if (maximum != null && amount > maximum) return "El máximo es ${compactMoney(maximum)}."
        return null
    }
}

enum class CardReloadStatus {
    AWAITING_PAYMENT,
    CREDITED,
    FAILED,
    CANCELED,
    OTHER,
    ;

    val isFinal: Boolean get() = this == CREDITED || this == FAILED || this == CANCELED

    companion object {
        fun fromWire(value: String): CardReloadStatus =
            when (value) {
                "pendiente_pago", "creada", "pendiente" -> AWAITING_PAYMENT
                "acreditada" -> CREDITED
                "fallida" -> FAILED
                "cancelada" -> CANCELED
                else -> OTHER
            }
    }
}

data class CardReload(
    val id: String,
    val amount: BigDecimal,
    val chargedCents: Long,
    val status: CardReloadStatus,
)

/** Lo que Stripe necesita para cobrar una recarga. El secreto no se guarda en disco. */
data class CardReloadCheckout(
    val reload: CardReload,
    val clientSecret: String,
    val publishableKey: String,
    val stripeAccountId: String,
)

/** La cartera del hijo vista con su contexto. */
data class ChildWallet(
    val balance: BigDecimal,
    val movements: List<ChildMovement>,
    val reloadRange: CardReloadRange?,
)

data class ChildMovement(
    val id: String,
    val type: String,
    val description: String,
    val amount: BigDecimal,
    val balanceAfter: BigDecimal,
    val createdAt: String,
) {
    val isDebit: Boolean get() = amount.signum() < 0
}

// Menú por fecha y semana

/** Hasta cuándo y cuántos días antes se aparta (`GET /menu-dias`). */
data class OrderSchedule(
    /** Hoy en la zona del negocio, `YYYY-MM-DD`. */
    val today: String,
    val daysAhead: Int,
    val orderCutoff: String?,
    val cancelCutoff: String?,
    /** "13:00-14:00" */
    val slots: List<String>,
)

object ScheduledOrderRules {
    /**
     * Lo apartado se cancela hasta el corte de su día aunque ya esté listo. El servidor tiene la última palabra;
     * aquí solo se deja de ofrecer el botón cuando ya es seguro que va a decir que no. Sin hora de corte vale
     * el inicio del día, igual que en el servidor. Con [now] nulo (sin el calendario cargado) solo cuenta el estado.
     */
    fun canCancel(
        statusWire: String,
        deliveryDate: String? = null,
        cancelCutoff: String? = null,
        now: LocalDateTime? = null,
    ): Boolean {
        if (statusWire != "cobrado" && statusWire != "listo") return false
        if (deliveryDate == null || now == null) return true
        val date = runCatching { LocalDate.parse(deliveryDate) }.getOrNull() ?: return true
        val cutoff = cancelCutoff?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: LocalTime.MIDNIGHT
        return now.isBefore(date.atTime(cutoff))
    }

    /** Lo que se le dice al papá antes de apartar: hasta cuándo cancela y qué pasa si no lo recoge. */
    fun policy(cancelCutoff: String?): String {
        val until = cancelCutoff?.let { "hasta las $it de ese día" } ?: "hasta el día anterior"
        return "Se cancela $until. Si no lo recoge, no se devuelve: ya se preparó."
    }
}

/** Un día en que la escuela no da servicio: no se aparta y lo apartado se canceló. */
data class NoServiceDay(
    val date: String,
    val reason: String,
)

data class MenuDay(
    val date: String,
    val productIds: List<Int>,
)

data class MenuCalendar(
    val schedule: OrderSchedule,
    val days: List<MenuDay>,
    val closedDays: List<NoServiceDay> = emptyList(),
) {
    /** El motivo, si ese día la escuela no da servicio. */
    fun closure(date: String): String? = closedDays.firstOrNull { it.date == date }?.reason

    /** Lo que se puede apartar ese día; `null` cuando no hay menú y vale el catálogo completo. */
    fun menu(date: String): Set<Int>? =
        days
            .firstOrNull { it.date == date }
            ?.productIds
            ?.takeIf { it.isNotEmpty() }
            ?.toSet()
}

/** Una línea de lo que se aparta: producto, cuántos y las opciones elegidas. */
data class PlannedItem(
    val productId: Int,
    val quantity: Int,
    val optionIds: List<Int>,
)

/** Lo que se manda por día en `POST /pedidos/semana`. */
data class ScheduledDayOrder(
    val date: String,
    val slot: String?,
    val items: List<PlannedItem>,
)

/** Lo apartado de un hijo, como lo ve su tutor en la ficha. */
data class ScheduledOrder(
    val id: String,
    val folio: Int,
    val statusWire: String,
    val version: Int,
    val total: BigDecimal,
    val deliveryDate: String?,
    val deliverySlot: String?,
    val summary: String,
) {
    companion object {
        /** Lo apartado que todavía no se entrega, del día más próximo al más lejano. */
        fun upcoming(orders: List<ScheduledOrder>): List<ScheduledOrder> =
            orders
                .filter { it.deliveryDate != null && it.statusWire in setOf("cobrado", "preparando", "listo") }
                .distinctBy { it.id }
                .sortedWith(compareBy({ it.deliveryDate }, { it.folio }))
    }
}

/**
 * La semana que el tutor arma antes de apartarla: por día, producto → cantidad y sus opciones.
 * Un producto lleva una sola elección por día: elegir otra vez la reemplaza.
 */
data class WeekPlan(
    val quantities: Map<String, Map<Int, Int>> = emptyMap(),
    val choices: Map<String, Map<Int, List<Int>>> = emptyMap(),
    val slots: Map<String, String> = emptyMap(),
) {
    fun quantity(
        productId: Int,
        date: String,
    ): Int = quantities[date]?.get(productId) ?: 0

    fun optionIds(
        productId: Int,
        date: String,
    ): List<Int> = choices[date]?.get(productId).orEmpty()

    /** `optionIds` nulo conserva lo ya elegido. */
    fun set(
        quantity: Int,
        productId: Int,
        date: String,
        optionIds: List<Int>? = null,
    ): WeekPlan {
        val clamped = quantity.coerceIn(0, MAX_PER_PRODUCT)
        val day = quantities[date].orEmpty().toMutableMap()
        if (clamped == 0) day.remove(productId) else day[productId] = clamped
        val picked = choices[date].orEmpty().toMutableMap()
        when {
            clamped == 0 -> picked.remove(productId)
            optionIds != null ->
                if (optionIds.isEmpty()) {
                    picked.remove(productId)
                } else {
                    picked[productId] =
                        optionIds.sorted()
                }
        }
        return copy(
            quantities = quantities.toMutableMap().apply { if (day.isEmpty()) remove(date) else put(date, day) },
            choices = choices.toMutableMap().apply { if (picked.isEmpty()) remove(date) else put(date, picked) },
        )
    }

    fun withSlot(
        date: String,
        slot: String?,
    ): WeekPlan = copy(slots = slots.toMutableMap().apply { if (slot == null) remove(date) else put(date, slot) })

    /** Los días con algo, en orden. */
    val plannedDates: List<String> get() = quantities.keys.sorted()

    fun itemCount(date: String): Int = quantities[date]?.values?.sum() ?: 0

    fun total(
        date: String,
        prices: Map<Int, BigDecimal>,
        extras: Map<Int, BigDecimal> = emptyMap(),
    ): BigDecimal =
        quantities[date].orEmpty().entries.fold(BigDecimal.ZERO) { sum, (productId, quantity) ->
            val price = prices[productId] ?: return@fold sum
            val unit =
                optionIds(
                    productId,
                    date,
                ).fold(price) { acc, option -> acc + (extras[option] ?: BigDecimal.ZERO) }
            sum + unit * quantity.toBigDecimal()
        }

    fun total(
        prices: Map<Int, BigDecimal>,
        extras: Map<Int, BigDecimal> = emptyMap(),
    ): BigDecimal = plannedDates.fold(BigDecimal.ZERO) { sum, date -> sum + total(date, prices, extras) }

    val orders: List<ScheduledDayOrder>
        get() =
            plannedDates.map { date ->
                ScheduledDayOrder(
                    date = date,
                    slot = slots[date],
                    items =
                        quantities[date]
                            .orEmpty()
                            .toSortedMap()
                            .map { (productId, quantity) ->
                                PlannedItem(productId, quantity, optionIds(productId, date))
                            },
                )
            }

    companion object {
        const val MAX_PER_PRODUCT = 20
    }
}

// Devoluciones y baja

/** Lo que el tutor principal le pide a la escuela sobre su hijo. */
enum class StudentRequestKind(
    val wireValue: String,
) {
    REFUND("devolucion"),
    DATA_REMOVAL("baja_datos"),
    ;

    companion object {
        fun fromWire(value: String): StudentRequestKind? = entries.firstOrNull { it.wireValue == value }
    }
}

enum class RefundMethod(
    val wireValue: String,
    val label: String,
) {
    CASH("efectivo", "En efectivo"),
    TRANSFER("transferencia", "Por transferencia"),
    ;

    companion object {
        fun fromWire(value: String?): RefundMethod? = entries.firstOrNull { it.wireValue == value }
    }
}

enum class StudentRequestStatus(
    val wireValue: String,
) {
    PENDING("pendiente"),
    RESOLVED("resuelta"),
    REJECTED("rechazada"),
    ;

    companion object {
        fun fromWire(value: String): StudentRequestStatus? = entries.firstOrNull { it.wireValue == value }
    }
}

data class StudentRequest(
    val id: String,
    val kind: StudentRequestKind,
    val status: StudentRequestStatus,
    val refundMethod: RefundMethod?,
    val note: String?,
    val balanceAtRequest: BigDecimal,
    val refunded: BigDecimal?,
    val rejectionReason: String?,
    val createdAt: String?,
    val resolvedAt: String?,
) {
    companion object {
        const val NOTE_LIMIT = 300

        /** La más reciente de cada tipo; el servidor las manda de la más nueva a la más vieja. */
        fun latest(
            kind: StudentRequestKind,
            requests: List<StudentRequest>,
        ): StudentRequest? = requests.firstOrNull { it.kind == kind }
    }
}

/** Montos que escribe el papá: "60", "60.5" o "60.50"; vacío o inválido, `null`. */
object TypedAmount {
    private val pattern = Regex("^\\d{1,6}([.,]\\d{1,2})?$")

    fun parse(text: String): BigDecimal? {
        val trimmed = text.trim()
        if (!pattern.matches(trimmed)) return null
        return trimmed.replace(',', '.').toBigDecimal().setScale(2, RoundingMode.HALF_UP)
    }

    /** "60.50" → "60.5"; "60.00" → "60". Para precargar un campo. */
    fun editable(value: BigDecimal?): String = value?.stripTrailingZeros()?.toPlainString().orEmpty()
}

/** "$50", "$12.50" o "$1,234.50": el formato compacto de la app, con miles como en México. */
fun compactMoney(value: BigDecimal): String {
    val symbols =
        DecimalFormatSymbols(Locale.ROOT).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
    val text = DecimalFormat("#,##0.00", symbols).format(value.setScale(2, RoundingMode.HALF_UP))
    return "$" + text.removeSuffix(".00")
}

/** Montos del servidor: siempre como número, nunca como texto. */
internal fun String.toMoneyOrNull(): BigDecimal? = trim().toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP)
