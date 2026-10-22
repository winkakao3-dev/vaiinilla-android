package com.vaiinilla.app.domain.school

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

// Negocio › Escuela (docs/padres-hijos.md del backend): cómo entran los papás, cuánto se recarga, qué se
// aparta cada día, el padrón, los tutores y lo que los papás piden cuando su hijo se va.

/** Los alérgenos del catálogo del backend (`cat_alergenos`), con su nombre para la pantalla. */
enum class SchoolAllergen(
    val code: String,
    val label: String,
) {
    GLUTEN("gluten", "Gluten"),
    CRUSTACEOS("crustaceos", "Crustáceos"),
    HUEVO("huevo", "Huevo"),
    PESCADO("pescado", "Pescado"),
    CACAHUATE("cacahuate", "Cacahuate"),
    SOYA("soya", "Soya"),
    LACTEOS("lacteos", "Lácteos"),
    NUECES("nueces", "Nueces"),
    APIO("apio", "Apio"),
    MOSTAZA("mostaza", "Mostaza"),
    AJONJOLI("ajonjoli", "Ajonjolí"),
    SULFITOS("sulfitos", "Sulfitos"),
    ALTRAMUCES("altramuces", "Altramuces"),
    MOLUSCOS("moluscos", "Moluscos"),
    LACTOSA("lactosa", "Lactosa"),
    COLORANTES("colorantes", "Colorantes"),
    ;

    companion object {
        fun fromCode(code: String): SchoolAllergen? = entries.firstOrNull { it.code == code }
    }
}

data class SchoolSettings(
    /** El código con que los papás se unen; `null` sin código. */
    val joinCode: String?,
    val joinRequiresCode: Boolean,
    /** Cada tutor nuevo espera a que la administración lo apruebe. */
    val guardiansNeedApproval: Boolean,
    /** Como se escribe en el campo: "50", "50.50". */
    val reloadMinimum: String,
    val reloadMaximum: String,
    val daysAhead: Int,
    /** "HH:MM" o vacío (sin hora). */
    val orderCutoff: String,
    val cancelCutoff: String,
)

object SchoolSettingsRules {
    const val MAX_DAYS_AHEAD = 14

    fun problem(settings: SchoolSettings): String? {
        if (settings.daysAhead !in 0..MAX_DAYS_AHEAD) return "Se aparta de 0 a $MAX_DAYS_AHEAD días antes."
        for (hour in listOf(settings.orderCutoff, settings.cancelCutoff)) {
            if (hour.isNotBlank() && !isHour(hour.trim())) return "Escribe las horas como 08:30."
        }
        // El servidor siempre tiene un rango: vaciar el campo no lo quita.
        val minimum = amount(settings.reloadMinimum) ?: return "Escribe el mínimo de recarga como 50 o 50.00."
        val maximum = amount(settings.reloadMaximum) ?: return "Escribe el máximo de recarga como 2000."
        // Como números: como texto, "50" sería mayor que "2000".
        if (minimum > maximum) return "El mínimo de recarga no puede pasar del máximo."
        return null
    }

    /** "50" → "50.00"; vacío o inválido, `null`. */
    fun money(text: String): String? = amount(text)?.setScale(2, RoundingMode.HALF_UP)?.toPlainString()

    /** Un monto positivo con a lo más dos decimales. */
    fun amount(text: String): BigDecimal? {
        val clean = text.trim().removePrefix("$").replace(",", "")
        if (!Regex("""\d{1,7}(\.\d{1,2})?""").matches(clean)) return null
        return clean.toBigDecimal().takeIf { it.signum() > 0 }
    }

    /** "50.00" → "50" para el campo; "50.50" se queda. */
    fun fieldText(server: String?): String = server?.removeSuffix(".00").orEmpty()

    private fun isHour(text: String): Boolean =
        Regex("""\d{2}:\d{2}""").matches(text) &&
            try {
                LocalTime.parse(text)
                true
            } catch (_: DateTimeParseException) {
                false
            }
}

/** Un tutor que pide cuidar a un alumno y espera a la administración. */
data class GuardianRequest(
    val id: String,
    val guardianName: String,
    val guardianEmail: String?,
    val studentName: String,
    val studentId: String?,
    val group: String?,
    val studentHasAccount: Boolean,
    val inRoster: Boolean,
    /** Tutores ya aprobados del mismo alumno. */
    val otherGuardians: Int,
) {
    /** "A-001 · 3°B" o lo que haya. */
    val studentLine: String get() = listOfNotNull(studentId, group).filter { it.isNotBlank() }.joinToString(" · ")

    /** Lo que la escuela sabe del alumno; el padrón solo se nombra si la escuela tiene uno. */
    fun facts(hasRoster: Boolean): String =
        buildList {
            if (hasRoster) add(if (inRoster) "En el padrón" else "No está en el padrón")
            add(
                when (otherGuardians) {
                    0 -> "Sin otro tutor"
                    1 -> "Ya tiene 1 tutor"
                    else -> "Ya tiene $otherGuardians tutores"
                },
            )
            add(if (studentHasAccount) "Tiene su propia cuenta" else "Sin cuenta propia")
        }.joinToString(" · ")
}

/** Una tutela aprobada, como la ve la administración al buscar a un alumno. */
data class SchoolGuardianship(
    val id: String,
    val guardianName: String,
    val guardianEmail: String?,
    val isPrincipal: Boolean,
    val studentUserId: String,
    val studentName: String,
    val studentId: String?,
    val group: String?,
) {
    val studentLine: String get() = listOfNotNull(studentId, group).filter { it.isNotBlank() }.joinToString(" · ")

    companion object {
        const val MIN_SEARCH_LENGTH = 2

        /** Los tutores de cada alumno juntos, en el orden en que llegaron, con el principal primero. */
        fun byStudent(rows: List<SchoolGuardianship>): List<List<SchoolGuardianship>> =
            rows
                .groupBy { it.studentUserId }
                .values
                .map { group -> group.filter { it.isPrincipal } + group.filterNot { it.isPrincipal } }
    }
}

/** Un alumno del padrón. Con padrón, un papá solo da de alta matrículas que estén en él. */
data class RosterStudent(
    val studentId: String,
    val name: String?,
    val group: String?,
)

/**
 * Lo que la administración pega desde su hoja de cálculo: una línea por alumno, "matrícula, nombre, grupo",
 * separado por tabulador (como sale de Excel), coma o punto y coma. Lo que no se pudo leer se dice por línea.
 */
object SchoolRosterParser {
    const val MAX_STUDENTS = 5000

    data class Problem(
        /** 0 cuando no es de una línea (la lista completa). */
        val line: Int,
        val reason: String,
    ) {
        val text: String get() = if (line > 0) "Línea $line: $reason" else reason.replaceFirstChar { it.uppercase() }
    }

    data class Result(
        val students: List<RosterStudent>,
        val problems: List<Problem>,
    ) {
        /** Solo se guarda una lista limpia: algo leído y ningún problema. */
        val canSave: Boolean get() = students.isNotEmpty() && problems.isEmpty()
    }

    fun parse(text: String): Result {
        val students = mutableListOf<RosterStudent>()
        val problems = mutableListOf<Problem>()
        val seen = mutableSetOf<String>()
        text.lines().forEachIndexed { index, raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEachIndexed
            val columns = split(line)
            val studentId = columns[0].uppercase()
            // El encabezado de la hoja ("Matrícula, Nombre, Grupo") no es un alumno.
            if (students.isEmpty() && problems.isEmpty() && studentId.lowercase().startsWith("matr")) {
                return@forEachIndexed
            }
            val name = columns.getOrNull(1)?.takeIf { it.isNotEmpty() }
            val group = columns.getOrNull(2)?.takeIf { it.isNotEmpty() }
            val number = index + 1
            when {
                studentId.isEmpty() -> problems += Problem(number, "sin matrícula")
                studentId.length > 64 || (name?.length ?: 0) > 120 || (group?.length ?: 0) > 40 ->
                    problems += Problem(number, "texto demasiado largo")
                !seen.add(studentId) -> problems += Problem(number, "matrícula repetida")
                else -> students += RosterStudent(studentId, name, group)
            }
        }
        if (students.size > MAX_STUDENTS) problems += Problem(0, "más de $MAX_STUDENTS alumnos")
        return Result(students, problems)
    }

    /** "En 3 grupos: 1°A, 2°B, 3°C" (hasta cuatro nombres). */
    fun groupsLine(roster: List<RosterStudent>): String? {
        val groups = roster.mapNotNull { it.group }.toSortedSet().toList()
        if (groups.isEmpty()) return null
        val shown = groups.take(4).joinToString(", ") + if (groups.size > 4) "…" else ""
        return if (groups.size == 1) "Grupo $shown" else "En ${groups.size} grupos: $shown"
    }

    private fun split(line: String): List<String> {
        val separator =
            when {
                '\t' in line -> '\t'
                ';' in line -> ';'
                else -> ','
            }
        return line.split(separator).map { it.trim() }
    }
}

enum class StudentRequestKind(
    val wire: String,
) {
    REFUND("devolucion"),
    DATA_REMOVAL("baja_datos"),
    ;

    companion object {
        fun fromWire(wire: String): StudentRequestKind? = entries.firstOrNull { it.wire == wire }
    }
}

enum class RefundMethod(
    val wire: String,
    val label: String,
) {
    CASH("efectivo", "En efectivo"),
    TRANSFER("transferencia", "Por transferencia"),
    ;

    companion object {
        fun fromWire(wire: String?): RefundMethod? = entries.firstOrNull { it.wire == wire }
    }
}

/** Una devolución de saldo o baja que espera a la escuela, con el saldo que el alumno tiene hoy. */
data class SchoolStudentRequest(
    val id: String,
    val kind: StudentRequestKind,
    val refundMethod: RefundMethod?,
    val note: String?,
    /** Montos como los manda el servidor ("150.00"). */
    val balanceAtRequest: String,
    val currentBalance: String,
    val guardianName: String,
    val guardianEmail: String?,
    val studentName: String,
    val studentId: String?,
    val group: String?,
) {
    /** "Matrícula A-102 · 3° B" */
    val studentLine: String?
        get() = listOfNotNull(studentId?.let { "Matrícula $it" }, group).joinToString(" · ").ifEmpty { null }

    /** Lo que se entrega al resolver: el saldo de hoy, que puede ser menor al que había al pedirlo. */
    val amountToRefund: String get() = currentBalance

    private val current: BigDecimal get() = currentBalance.toBigDecimalOrNull() ?: BigDecimal.ZERO

    val balanceChanged: Boolean
        get() = (balanceAtRequest.toBigDecimalOrNull() ?: BigDecimal.ZERO).compareTo(current) != 0

    /** Una devolución necesita saldo; una baja, saldo en cero. */
    val canResolve: Boolean
        get() = if (isRefund) current.signum() > 0 else current.signum() == 0

    val isRefund: Boolean get() = kind == StudentRequestKind.REFUND

    val isCashRefund: Boolean get() = kind == StudentRequestKind.REFUND && refundMethod == RefundMethod.CASH

    val hasBalance: Boolean get() = current.signum() > 0

    companion object {
        const val MIN_REJECTION_REASON = 3
    }
}

data class NoServiceDay(
    val date: LocalDate,
    val reason: String,
) {
    companion object {
        val REASON_LENGTH = 3..80
    }
}

/** El menú y los días cerrados que la escuela ya puso, con su "hoy" (zona del establecimiento). */
data class MenuCalendar(
    val today: LocalDate,
    val days: Map<LocalDate, List<Int>>,
    val closedDays: List<NoServiceDay>,
) {
    /** El motivo, si ese día la escuela no da servicio. */
    fun closure(date: LocalDate): String? = closedDays.firstOrNull { it.date == date }?.reason

    /** Lo que se vende ese día; `null` cuando no hay menú y vale el catálogo completo. */
    fun menu(date: LocalDate): Set<Int>? = days[date]?.takeIf { it.isNotEmpty() }?.toSet()
}

/** Lo que pasó al cerrar un día. */
data class DayClosure(
    val cancelled: Int,
    /** "100.00" */
    val refunded: String,
    /** Pedidos que no se pudieron cancelar; cerrar el día otra vez los termina. */
    val pending: Int,
) {
    /** "Día cerrado. Se cancelaron 3 apartados y regresaron $100 a su saldo." */
    fun summary(money: (String) -> String): String =
        buildString {
            append("Día cerrado.")
            when (cancelled) {
                0 -> append(" No había nada apartado.")
                1 -> append(" Se canceló 1 apartado y regresaron ${money(refunded)} a su saldo.")
                else -> append(" Se cancelaron $cancelled apartados y regresaron ${money(refunded)} a su saldo.")
            }
            if (pending > 0) append(" $pending no se pudo cancelar: ciérralo otra vez.")
        }
}

/** Cuánto de cada producto se apartó para un día. */
data class ReservedDay(
    val date: LocalDate,
    val orders: Int,
    val closedReason: String?,
    val lines: List<Line>,
) {
    data class Line(
        val productId: Int,
        val name: String,
        val quantity: Int,
        val orders: Int,
    )
}

/** Fechas de la escuela: siempre parten del "hoy" que manda el servidor, no del reloj del teléfono. */
object SchoolDates {
    private val SHORT = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    private val LONG = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    /** De hoy a hoy + [daysAhead], en orden. */
    fun dates(
        today: LocalDate,
        daysAhead: Int,
    ): List<LocalDate> = (0..daysAhead.coerceAtLeast(0)).map { today.plusDays(it.toLong()) }

    fun weekdayShort(date: LocalDate): String = SHORT[date.dayOfWeek.value - 1]

    fun isWeekend(date: LocalDate): Boolean = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY

    /** "Hoy", "Mañana" o "Jueves 24". */
    fun longLabel(
        date: LocalDate,
        today: LocalDate,
    ): String =
        when (date) {
            today -> "Hoy"
            today.plusDays(1) -> "Mañana"
            else -> "${LONG[date.dayOfWeek.value - 1]} ${date.dayOfMonth}"
        }

    /** "2026-10-21" o "2026-10-21T00:00:00Z" → fecha; lo demás, `null`. */
    fun parse(text: String?): LocalDate? = text?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}
