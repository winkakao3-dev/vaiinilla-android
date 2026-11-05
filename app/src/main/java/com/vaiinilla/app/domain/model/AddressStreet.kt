package com.vaiinilla.app.domain.model

/**
 * La calle y el número dentro de la dirección de una entrega. El mapa a veces solo resuelve el código postal, la
 * colonia o las coordenadas; entonces falta la calle y la persona la escribe aparte, sin perder el resto.
 */
object AddressStreet {
    /** Lo que pone el mapa cuando no encuentra nada en ese punto. */
    const val LOCATION_PREFIX = "Ubicación "

    private val NUMBER = Regex("""\d|\bs/?n\b|sin n[uú]mero""", RegexOption.IGNORE_CASE)

    /** La primera parte si es una calle con número («Aldama 1408», «Roble s/n»); vacío si no hay. */
    fun street(address: String): String = parts(address).firstOrNull()?.takeIf(::isStreet).orEmpty()

    /** Lo demás de la dirección (colonia, código postal, ciudad), sin la calle ni las coordenadas sueltas. */
    fun area(address: String): List<String> {
        if (address.trim().startsWith(LOCATION_PREFIX)) return emptyList()
        val all = parts(address)
        return if (all.firstOrNull()?.let(::isStreet) == true) all.drop(1) else all
    }

    /** La dirección con la calle que escribió la persona delante de [area]; quita lo que la calle ya repite. */
    fun join(
        street: String,
        area: List<String>,
    ): String {
        val typed = street.trim()
        val rest = area.filterNot { typed.isNotEmpty() && typed.startsWith(it, ignoreCase = true) }
        return (listOf(typed) + rest).filter(String::isNotEmpty).joinToString(", ")
    }

    private fun isStreet(part: String): Boolean =
        part.any(Char::isLetter) && !part.startsWith(LOCATION_PREFIX) && NUMBER.containsMatchIn(part)

    private fun parts(address: String): List<String> = address.split(',').map(String::trim).filter(String::isNotEmpty)
}
