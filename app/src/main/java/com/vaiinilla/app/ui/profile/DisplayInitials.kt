package com.vaiinilla.app.ui.profile

fun displayInitials(name: String): String {
    // Por puntos de código y solo letras o dígitos: un emoji al inicio no se parte en «�» (como initialOf).
    val parts = name.trim().split(Regex("\\s+")).map(::lettersOf).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "?"
    if (parts.size == 1) return parts[0].take(2).joinToString("").uppercase()
    return (parts.first().first() + parts.last().first()).uppercase()
}

private fun lettersOf(word: String): List<String> =
    word
        .codePoints()
        .toArray()
        .filter { Character.isLetterOrDigit(it) }
        .map { String(Character.toChars(it)) }
