package com.vaiinilla.app.domain.admin

import com.vaiinilla.app.domain.model.OrderWindow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

// region Personal

/** Los roles que la administración puede invitar o asignar. */
enum class StaffRole(
    val wire: String,
    val label: String,
) {
    CASHIER("cajero", "Caja"),
    KITCHEN("cocina", "Cocina"),
    WAITER("mesero", "Servicio en mesa"),
    ADMIN("admin", "Administración"),
    ;

    companion object {
        fun fromWire(value: String): StaffRole? = entries.firstOrNull { it.wire == value }
    }
}

enum class InvitationState(
    val wire: String,
    val label: String,
) {
    PENDING("pendiente", "Pendiente"),
    ACCEPTED("aceptada", "Aceptada"),
    REVOKED("revocada", "Revocada"),
    REPLACED("reemplazada", "Reemplazada"),
    EXPIRED("expirada", "Expirada"),
    ;

    /** Se puede mandar de nuevo: el enlace anterior queda reemplazado. */
    val canResend: Boolean get() = this == PENDING || this == EXPIRED || this == REVOKED

    companion object {
        fun fromWire(value: String): InvitationState? = entries.firstOrNull { it.wire == value }
    }
}

data class StaffInvitation(
    val id: String,
    val email: String,
    val roleWire: String,
    val stateWire: String,
    val expiresAt: String,
    val createdAt: String,
    val membershipId: String?,
) {
    val role: StaffRole? get() = StaffRole.fromWire(roleWire)
    val state: InvitationState? get() = InvitationState.fromWire(stateWire)
}

data class InvitationsPage(
    val invitations: List<StaffInvitation>,
    val cursor: String?,
)

/** Un acceso activo del personal (sin clientes), con la administración primero. */
data class StaffMembership(
    val id: String,
    val name: String,
    val email: String,
    val roleWire: String,
    val createdAt: String,
) {
    val role: StaffRole? get() = StaffRole.fromWire(roleWire)
}

object StaffRules {
    private val EMAIL = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

    /** El correo como lo guarda el backend; null si no parece un correo. */
    fun email(raw: String): String? {
        val email = raw.trim().lowercase()
        return email.takeIf { it.length in 3..254 && EMAIL.matches(it) }
    }
}

// endregion

// region Flujo de mi tienda

enum class BusinessType(
    val wire: String,
    val label: String,
) {
    CAFETERIA("cafeteria", "Cafetería"),
    RESTAURANT("restaurante", "Restaurante"),
    PADEL("padel", "Club de pádel"),
    BAR("bar", "Bar o cantina"),
    FOOD_TRUCK("food_truck", "Food truck o puesto"),
    DRIVE_THRU("drive_thru", "Drive-thru"),
    CANTEEN("comedor", "Comedor de empresa"),
    EVENT("evento", "Cine, estadio o evento"),
    ;

    companion object {
        fun fromWire(value: String): BusinessType? = entries.firstOrNull { it.wire == value }
    }
}

/** Las plantillas de "Flujo de mi tienda" en la web: cada una deja el flujo listo para ese tipo de negocio. */
enum class StoreTemplate(
    val label: String,
    val type: BusinessType,
    val deliveryRequiresQr: Boolean,
    val allowsPayAtEnd: Boolean,
) {
    SCHOOL_CAFETERIA("Cafetería escolar", BusinessType.CAFETERIA, true, false),
    CAFE_WITH_TABLES("Cafetería con mesas", BusinessType.CAFETERIA, true, false),
    RESTAURANT("Restaurante con mesero", BusinessType.RESTAURANT, false, true),
    BAR("Bar o cantina", BusinessType.BAR, false, true),
    PADEL("Club de pádel", BusinessType.PADEL, false, true),
    FOOD_TRUCK("Food truck o puesto", BusinessType.FOOD_TRUCK, true, false),
    DRIVE_THRU("Drive-thru", BusinessType.DRIVE_THRU, true, false),
    CANTEEN("Comedor de empresa", BusinessType.CANTEEN, true, false),
    EVENT("Cine, estadio o evento", BusinessType.EVENT, true, false),
}

data class StoreVideo(
    val title: String,
    val url: String,
)

/** La configuración del negocio (`/establecimiento/configuracion`). */
data class StoreConfig(
    val typeWire: String,
    val deliveryRequiresQr: Boolean,
    val allowsPayAtEnd: Boolean,
    val allowsBalance: Boolean,
    val releaseGraceMinutes: Int,
    val orderWindows: List<OrderWindow>,
    val videos: List<StoreVideo>,
    val visibleInDirectory: Boolean,
    val storeUrl: String?,
    val imageUrl: String?,
    val logoUrl: String?,
    /** Escuela: prende Mis hijos, Caja › Alumnos y Negocio › Escuela. */
    val isSchool: Boolean = false,
) {
    val type: BusinessType? get() = BusinessType.fromWire(typeWire)

    fun flow(): StoreFlow =
        StoreFlow(
            typeWire = typeWire,
            deliveryRequiresQr = deliveryRequiresQr,
            allowsPayAtEnd = allowsPayAtEnd,
            allowsBalance = allowsBalance,
            releaseGraceMinutes = releaseGraceMinutes,
            orderWindows = orderWindows,
        )
}

/** Lo que se guarda con "Guardar flujo". */
data class StoreFlow(
    val typeWire: String,
    val deliveryRequiresQr: Boolean,
    val allowsPayAtEnd: Boolean,
    val allowsBalance: Boolean,
    val releaseGraceMinutes: Int,
    val orderWindows: List<OrderWindow>,
) {
    fun applying(template: StoreTemplate): StoreFlow =
        copy(
            typeWire = template.type.wire,
            deliveryRequiresQr = template.deliveryRequiresQr,
            allowsPayAtEnd = template.allowsPayAtEnd,
            releaseGraceMinutes = 5,
            orderWindows = emptyList(),
        )
}

object StoreRules {
    const val MAX_WINDOWS = 6
    const val MAX_GRACE_MINUTES = 60
    const val MAX_VIDEOS = 5

    /** La franja que se agrega: 12:00–15:00 la primera, 18:00–20:00 la segunda, luego una hora después. */
    fun nextWindow(current: List<OrderWindow>): OrderWindow =
        when (current.size) {
            0 -> OrderWindow("12:00", "15:00")
            1 -> OrderWindow("18:00", "20:00")
            else -> {
                val last = time(current.last().to) ?: LocalTime.of(20, 0)
                OrderWindow(hhmm(last), hhmm(last.plusHours(1)))
            }
        }

    fun problem(flow: StoreFlow): String? {
        if (flow.releaseGraceMinutes !in 0..MAX_GRACE_MINUTES) {
            return "La gracia al terminar un turno va de 0 a $MAX_GRACE_MINUTES minutos."
        }
        return windowsProblem(flow.orderWindows)
    }

    fun windowsProblem(windows: List<OrderWindow>): String? {
        if (windows.size > MAX_WINDOWS) return "Puedes tener hasta $MAX_WINDOWS franjas de pedidos."
        val ranges =
            windows.map { window ->
                val from = time(window.from) ?: return "Escribe las horas como 13:30."
                val to = time(window.to) ?: return "Escribe las horas como 13:30."
                if (from == to) return "Una franja no puede empezar y terminar a la misma hora."
                minutes(from) to minutes(to).let { if (it <= minutes(from)) it + DAY else it }
            }
        // Una franja que cruza la medianoche termina "mañana": se compara también corrida un día.
        for (i in ranges.indices) {
            for (j in i + 1 until ranges.size) {
                val (a, b) = ranges[i]
                if (listOf(0, DAY, -DAY).any { shift ->
                        val (c, d) = ranges[j]
                        a < d + shift && c + shift < b
                    }
                ) {
                    return "Las franjas de pedidos no pueden encimarse."
                }
            }
        }
        return null
    }

    /** Ordenadas por hora de inicio, como las manda la web. */
    fun sortedWindows(windows: List<OrderWindow>): List<OrderWindow> =
        windows.sortedBy {
            time(it.from)?.let(::minutes)
                ?: 0
        }

    fun videosProblem(videos: List<StoreVideo>): String? {
        if (videos.size > MAX_VIDEOS) return "Puedes tener hasta $MAX_VIDEOS videos."
        videos.forEach { video ->
            if (video.title.trim().length !in 1..60) return "Cada video necesita un título de hasta 60 letras."
            val url = video.url.trim()
            if (!url.startsWith("https://") ||
                url.length > 500
            ) {
                return "Los enlaces de video deben empezar con https://."
            }
        }
        return null
    }

    fun time(raw: String): LocalTime? =
        Regex("""^(\d{1,2}):(\d{2})$""").matchEntire(raw.trim())?.let { match ->
            val (h, m) = match.destructured
            runCatching { LocalTime.of(h.toInt(), m.toInt()) }.getOrNull()
        }

    fun hhmm(time: LocalTime): String = "%02d:%02d".format(time.hour, time.minute)

    private fun minutes(time: LocalTime) = time.hour * 60 + time.minute

    private const val DAY = 24 * 60
}

// endregion

// region Pagos con tarjeta (Stripe)

/** La cuenta de Stripe del negocio; `null` en el repositorio si aún no se conecta. */
data class StripeSetup(
    val enabled: Boolean,
    val chargesEnabled: Boolean,
    val payoutsEnabled: Boolean,
    val onboardingState: String,
    val passFeeToCustomer: Boolean,
)

enum class StripeStep { CONNECT, FINISH, READY }

fun stripeStep(setup: StripeSetup?): StripeStep =
    when {
        setup == null -> StripeStep.CONNECT
        !setup.chargesEnabled || !setup.payoutsEnabled -> StripeStep.FINISH
        else -> StripeStep.READY
    }

// endregion

// region Cashback

/** La regla de cashback vigente. `activeDays` nulo = todos los días (0 = domingo). */
data class CashbackRule(
    val name: String,
    val percent: String,
    val startTime: String?,
    val endTime: String?,
    val activeDays: List<Int>?,
    val validFrom: String?,
    val validTo: String?,
    val active: Boolean,
    val updatedAt: String? = null,
)

object CashbackRules {
    /** Lunes a domingo, con el número que usa el backend (0 = domingo). */
    val WEEK = listOf(1 to "Lun", 2 to "Mar", 3 to "Mié", 4 to "Jue", 5 to "Vie", 6 to "Sáb", 0 to "Dom")

    private val PERCENT = Regex("""^\d{1,3}(\.\d{1,2})?$""")

    /** "5" → "5.00"; null si no es un porcentaje de 0 a 100. */
    fun percent(raw: String): String? {
        val text = raw.trim().removeSuffix("%").trim()
        if (!PERCENT.matches(text)) return null
        val value = BigDecimal(text)
        if (value > BigDecimal(100)) return null
        return value.setScale(2).toPlainString()
    }

    fun problem(rule: CashbackRule): String? {
        if (rule.name.trim().length !in 1..120) return "Ponle nombre a la regla (hasta 120 letras)."
        percent(rule.percent) ?: return "El porcentaje va de 0 a 100, por ejemplo 5 o 5.50."
        if ((rule.startTime == null) != (rule.endTime == null)) return "Pon las dos horas del horario, o ninguna."
        if (rule.startTime != null) {
            val start = StoreRules.time(rule.startTime) ?: return "Escribe las horas como 13:30."
            val end = StoreRules.time(rule.endTime.orEmpty()) ?: return "Escribe las horas como 13:30."
            if (start == end) return "El horario no puede empezar y terminar a la misma hora."
        }
        val from =
            rule.validFrom?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
                    ?: return "Escribe la fecha como 2026-10-31."
            }
        val to =
            rule.validTo?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
                    ?: return "Escribe la fecha como 2026-10-31."
            }
        if (from != null && to != null && from.isAfter(to)) return "La vigencia debe empezar antes de terminar."
        return null
    }

    fun normalized(rule: CashbackRule): CashbackRule =
        rule.copy(
            name = rule.name.trim(),
            percent = percent(rule.percent) ?: rule.percent,
            activeDays = rule.activeDays?.takeIf { it.isNotEmpty() }?.sorted(),
            startTime = rule.startTime?.let { StoreRules.time(it)?.let(StoreRules::hhmm) ?: it },
            endTime = rule.endTime?.let { StoreRules.time(it)?.let(StoreRules::hhmm) ?: it },
        )

    fun daysLabel(days: List<Int>?): String =
        if (days.isNullOrEmpty() || days.size == 7) {
            "Todos los días"
        } else {
            WEEK.filter { it.first in days }.joinToString(", ") { it.second }
        }
}

/** Lo de la cartera en el resumen del periodo. */
data class AdminWalletSummary(
    val reloads: String,
    val purchases: String,
    val cashback: String,
    val cancellations: String,
    val canceledOrders: Int?,
    val movements: List<AdminMetric>,
    val walletsReviewed: Int,
    val alerts: Int,
)

// endregion

// region Mesas y espacios

enum class SpaceType(
    val wire: String,
    val label: String,
) {
    TABLE("mesa", "Mesa"),
    BAR("barra", "Barra"),
    COURT("cancha", "Cancha"),
    DRIVE_THRU("drive_thru", "Drive-thru"),
    SEAT("asiento", "Asiento"),
    ;

    companion object {
        fun fromWire(value: String): SpaceType? = entries.firstOrNull { it.wire == value }

        /** Los que se pueden crear en lote: las canchas se dan de alta una por una. */
        val batchTypes = listOf(SEAT, TABLE, BAR, DRIVE_THRU)
    }
}

/** Un espacio como lo administra el dueño: con su QR, código corto y, si es cancha, su ficha. */
data class ManagedSpace(
    val id: Int,
    val name: String,
    val typeWire: String,
    val active: Boolean,
    val qrUrl: String,
    val code: String?,
    val hourlyPrice: String?,
    val description: String?,
    val imageUrl: String?,
    val features: List<String>,
) {
    val type: SpaceType? get() = SpaceType.fromWire(typeWire)
    val isCourt: Boolean get() = type == SpaceType.COURT
}

data class SpaceBatchResult(
    val requested: Int,
    val created: Int,
    val skipped: List<String>,
)

/** Lo que cambia de un espacio; los campos nulos no se tocan. */
data class SpacePatch(
    val name: String? = null,
    val typeWire: String? = null,
    val active: Boolean? = null,
    /** Con [setsHourlyPrice], `null` quita el precio (ya no se renta). */
    val hourlyPrice: String? = null,
    val setsHourlyPrice: Boolean = false,
    /** Con [setsProfile], se guardan la descripción (null = sin descripción) y las características. */
    val description: String? = null,
    val features: List<String> = emptyList(),
    val setsProfile: Boolean = false,
)

/** El horario en que se reservan las canchas. */
data class ReservationSchedule(
    val opening: String,
    val closing: String,
    val daysAhead: Int,
)

object SpaceRules {
    const val MAX_BATCH = 500
    const val MAX_FEATURES = 8
    val FEATURE_SUGGESTIONS = listOf("Techada", "Con luz", "Vidrio panorámico", "Pasto sintético", "Aire libre")

    private val HOURLY = Regex("""^\d{1,5}(\.\d{1,2})?$""")

    fun nameProblem(name: String): String? = if (name.trim().length in 1..80) null else "El nombre va de 1 a 80 letras."

    fun batchProblem(
        prefix: String,
        from: Int?,
        to: Int?,
    ): String? {
        val trimmed = prefix.trim()
        if (trimmed.length !in 1..70) return "El nombre base va de 1 a 70 letras."
        if (from == null || to == null || from !in 0..99_999 || to !in 0..99_999) return "Los números van de 0 a 99999."
        if (to < from) return "El último número debe ser mayor o igual al primero."
        if (to - from + 1 > MAX_BATCH) return "Se crean hasta $MAX_BATCH a la vez."
        if ("$trimmed $to".length > 80) return "Los nombres quedarían de más de 80 letras."
        return null
    }

    /** "300" → "300.00"; vacío = no se renta (null); otro texto inválido → [Invalid]. */
    fun hourlyPrice(raw: String): HourlyPrice {
        val text = raw.trim()
        if (text.isEmpty()) return HourlyPrice.None
        if (!HOURLY.matches(text)) return HourlyPrice.Invalid
        val value = BigDecimal(text)
        if (value.signum() <= 0) return HourlyPrice.Invalid
        return HourlyPrice.Value(value.setScale(2).toPlainString())
    }

    sealed interface HourlyPrice {
        data object None : HourlyPrice

        data object Invalid : HourlyPrice

        data class Value(
            val amount: String,
        ) : HourlyPrice
    }

    /** Hasta 8 características de 1 a 30 letras, sin repetir (sin importar mayúsculas). */
    fun features(raw: List<String>): List<String> {
        val seen = mutableSetOf<String>()
        return raw
            .map { it.trim() }
            .filter { it.length in 1..30 && seen.add(it.lowercase()) }
            .take(MAX_FEATURES)
    }

    fun scheduleProblem(schedule: ReservationSchedule): String? {
        val opening = StoreRules.time(schedule.opening) ?: return "Escribe las horas como 07:00."
        val closing = StoreRules.time(schedule.closing) ?: return "Escribe las horas como 23:00."
        if (opening == closing) return "La hora de cierre debe ser distinta a la de apertura."
        if (schedule.daysAhead !in 0..60) return "Se puede reservar de 0 a 60 días antes."
        return null
    }
}

// endregion

// region Inicio

/** Lo de "Ahora mismo" en el panel de la web. */
data class AdminLiveCounts(
    val pendingInvitations: Int?,
    val cashOpen: Boolean?,
    val activeOrders: Int?,
    val moreActiveOrders: Boolean,
)

// endregion
