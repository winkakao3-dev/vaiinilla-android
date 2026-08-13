package com.vaiinilla.app.ui.profile

fun displayInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "?"
    if (parts.size == 1) return parts[0].take(2).uppercase()
    return "${parts.first().first()}${parts.last().first()}".uppercase()
}
