package com.vaiinilla.app.domain.counter

import com.vaiinilla.app.domain.model.NameInitials
import com.vaiinilla.app.domain.model.OptionGroup
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.school.SchoolAllergen
import java.math.BigDecimal
import java.text.Normalizer

// Caja con alumnos sin teléfono (padres e hijos, PR 3 y migración 101): encontrarlo, ver su ficha,
// venderle con su saldo y entregarle lo apartado con su credencial.

/** Los alérgenos del alumno con el nombre del catálogo de la escuela ([SchoolAllergen]). */
object StudentAllergens {
    /** Solo los códigos que el catálogo conoce; uno nuevo del servidor no rompe la ficha. */
    fun known(codes: List<String>): List<String> =
        codes.map { it.trim().lowercase() }.filter { SchoolAllergen.fromCode(it) != null }

    fun label(code: String): String = SchoolAllergen.fromCode(code)?.label ?: code
}

data class StudentTodayOrder(
    val id: String,
    val folio: Int,
    val statusWire: String,
    val version: Int,
    val total: BigDecimal,
    val summary: String,
) {
    val isReady: Boolean get() = statusWire == "listo"

    val statusLabel: String
        get() =
            when (statusWire) {
                "cobrado" -> "Pagado"
                "preparando" -> "En cocina"
                "listo" -> "Listo"
                "entregado" -> "Entregado"
                "por_cobrar" -> "Por cobrar"
                else -> statusWire
            }
}

/** Lo que Caja ve de un alumno para cobrarle y entregarle sin teléfono. */
data class StudentCard(
    val userId: String,
    val name: String,
    val studentId: String?,
    val group: String?,
    val hasOwnAccount: Boolean,
    val balance: BigDecimal,
    val spentToday: BigDecimal,
    val dailyLimit: BigDecimal?,
    /** Códigos del catálogo (`cacahuate`, `nueces`…). */
    val allergens: List<String>,
    val todayOrders: List<StudentTodayOrder>,
    /** Su tutor le puso PIN: Caja lo pide antes de cobrar. */
    val requiresPin: Boolean = false,
) {
    /** Lo que todavía puede gastar hoy: el menor entre su saldo y lo que le queda del límite. */
    val spendableToday: BigDecimal
        get() {
            val limit = dailyLimit ?: return balance
            val left = (limit - spentToday).max(BigDecimal.ZERO)
            return left.min(balance)
        }

    val identity: String
        get() = listOfNotNull(studentId?.let { "Matrícula $it" }, group).joinToString(" · ")

    val firstName: String
        get() =
            name
                .trim()
                .split(" ")
                .firstOrNull()
                ?.takeIf { it.isNotEmpty() } ?: name

    val initials: String
        get() = NameInitials.of(name)
}

enum class StudentCredentialKind(
    val wire: String,
) {
    QR("qr"),
    NFC("nfc"),
}

data class StudentCredential(
    val id: String,
    val kind: StudentCredentialKind,
    /** Solo en la respuesta que la crea: el texto del QR a imprimir. */
    val printableValue: String?,
)

object StudentCredentialPayload {
    const val PREFIX = "vaiinilla:alumno:"

    /** Lo que la cámara de Caja leyó es una credencial impresa de alumno. */
    fun isStudentCredential(scanned: String): Boolean = scanned.trim().startsWith(PREFIX)

    /** El UID de una tarjeta NFC en hexadecimal, como lo guarda el backend. */
    fun nfcUid(bytes: ByteArray): String = bytes.joinToString("") { "%02X".format(it) }
}

/** Una línea de la compra: producto, cuántas y las opciones elegidas (`opcion_ids`). */
data class StudentPurchaseItem(
    val productId: Int,
    val quantity: Int,
    val optionIds: List<Int> = emptyList(),
)

/** Lo que Caja le vende a un alumno sin teléfono. */
object StudentPurchaseRules {
    const val MAX_PER_PRODUCT = 20

    /**
     * Las alergias del alumno que tiene el producto: por los chips que la escuela revisó y, además, por el texto
     * viejo de alérgenos que escribió el dueño. El servidor es quien bloquea; esto solo avisa a la cajera.
     */
    fun allergenWarnings(
        product: Product,
        student: List<String>,
    ): List<String> {
        val byText = allergenWarnings(product.allergens, student)
        val byCode = if (product.allergensReviewed) student.filter { it in product.allergenCodes } else emptyList()
        return student.filter { it in byCode || it in byText }
    }

    /** Las alergias del alumno que aparecen en los alérgenos que el dueño escribió del producto. */
    fun allergenWarnings(
        productAllergens: String?,
        student: List<String>,
    ): List<String> {
        val text = productAllergens?.searchKey().orEmpty()
        if (text.isEmpty()) return emptyList()
        return student.filter { code -> text.contains(code) || text.contains(StudentAllergens.label(code).searchKey()) }
    }

    fun isSellable(product: Product): Boolean = product.available

    /** Lo que pide elegir opciones (tamaño, sabor) se agrega desde su ficha, no con el "+". */
    fun needsChoice(product: Product): Boolean = product.optionGroups.any { it.minimumSelections > 0 }

    /** Opción → lo que suma al precio. */
    fun extras(products: List<Product>): Map<Int, BigDecimal> =
        products
            .flatMap { it.optionGroups.flatMap(OptionGroup::options) }
            .associate { it.id to (it.extraPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO) }

    fun total(
        cart: Map<Int, Int>,
        prices: Map<Int, BigDecimal>,
        choices: Map<Int, List<Int>> = emptyMap(),
        extras: Map<Int, BigDecimal> = emptyMap(),
    ): BigDecimal =
        cart.entries.fold(BigDecimal.ZERO) { sum, (productId, quantity) ->
            val price = prices[productId] ?: return@fold sum
            val unit =
                choices[productId].orEmpty().fold(price) { acc, option ->
                    acc +
                        (extras[option] ?: BigDecimal.ZERO)
                }
            sum + unit * quantity.toBigDecimal()
        }
}

/** "Para mañana": cuánto de cada producto se apartó para un día. */
data class ReservedDay(
    val date: String,
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

    val units: Int get() = lines.sumOf { it.quantity }
}

/** Lo del estado operativo que decide si hay Alumnos y Para mañana, y cómo se entrega. */
data class SchoolPolicy(
    val isSchool: Boolean,
    val deliveryRequiresQr: Boolean,
)

/** Igual que la búsqueda de iOS: sin acentos, sin mayúsculas y sin espacios en los extremos. */
fun String.searchKey(): String =
    Normalizer
        .normalize(trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()

/** Los días de "Para mañana": hoy y los siguientes días de escuela, con nombres de la cocina. */
object ReservedCalendar {
    private val names = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    /** Hoy y los próximos 6 días sin sábados ni domingos (hoy sale aunque sea fin de semana). */
    fun dates(today: java.time.LocalDate): List<String> =
        (0..6L)
            .map { today.plusDays(it) }
            .filter { it == today || it.dayOfWeek.value < 6 }
            .map { it.toString() }

    /** "Hoy", "Mañana" o "Jueves 24". */
    fun label(
        iso: String,
        today: java.time.LocalDate,
    ): String {
        val day = runCatching { java.time.LocalDate.parse(iso) }.getOrNull() ?: return iso
        return when (day) {
            today -> "Hoy"
            today.plusDays(1) -> "Mañana"
            else -> "${names[day.dayOfWeek.value - 1]} ${day.dayOfMonth}"
        }
    }
}
