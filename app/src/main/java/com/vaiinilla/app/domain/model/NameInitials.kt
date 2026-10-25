package com.vaiinilla.app.domain.model

object NameInitials {
    /**
     * La primera letra de cada palabra, hasta [max] palabras. Las palabras sin letras (un emoji) se saltan:
     * "🦄 Sofi Ramírez" da "SR". Vacío si no hay ninguna letra.
     */
    fun of(
        name: String,
        max: Int = 2,
    ): String =
        name
            .trim()
            .split(Regex("\\s+"))
            .mapNotNull { word ->
                word
                    .codePoints()
                    .filter { Character.isLetterOrDigit(it) }
                    .findFirst()
                    .orElse(-1)
                    .takeIf { it >= 0 }
            }.take(max)
            .joinToString("") { String(Character.toChars(it)).uppercase() }
}
