package com.vaiinilla.app.core.error

/**
 * Quita datos personales y secretos de lo que viaja a Crashlytics. Un cierre no necesita el correo,
 * el teléfono ni el token de nadie para poder diagnosticarse: basta la pila y el tipo de error.
 */
object CrashDataScrubber {
    private val patterns: List<Pair<Regex, String>> =
        listOf(
            Regex("""eyJ[A-Za-z0-9_-]{5,}\.[A-Za-z0-9_-]{5,}\.[A-Za-z0-9_-]*""") to "[token]",
            Regex(
                """(?i)(bearer|authorization:?|token=?|apikey=?|api_key=?|password=?|clave=?)\s*[:=]?\s*[A-Za-z0-9._~+/=-]{8,}""",
            ) to
                "$1 [secreto]",
            Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""") to "[correo]",
            Regex("""\+?\d[\d\s().-]{7,}\d""") to "[numero]",
        )

    fun scrub(text: String?): String? =
        text?.let {
            patterns.fold(it) { acc, (regex, replacement) -> regex.replace(acc, replacement) }
        }

    /**
     * Copia de [original] con el mensaje limpio y la misma pila. Conserva el nombre de la clase original
     * en el mensaje porque Crashlytics agrupa por pila, no por tipo.
     */
    fun scrub(original: Throwable): Throwable = scrub(original, depth = 0)

    private fun scrub(
        original: Throwable,
        depth: Int,
    ): Throwable {
        val cause = original.cause?.takeIf { depth < MAX_CAUSE_DEPTH && it !== original }
        val copy =
            ScrubbedThrowable(original.javaClass.name, scrub(original.message), cause?.let { scrub(it, depth + 1) })
        copy.stackTrace = original.stackTrace
        return copy
    }

    private const val MAX_CAUSE_DEPTH = 8
}

class ScrubbedThrowable(
    originalClass: String,
    scrubbedMessage: String?,
    cause: Throwable?,
) : RuntimeException(if (scrubbedMessage.isNullOrBlank()) originalClass else "$originalClass: $scrubbedMessage", cause)
