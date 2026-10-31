package com.vaiinilla.app.core.location

import android.net.Uri
import java.net.URLDecoder

/**
 * Un enlace de Google Maps o Apple Maps pegado en un campo: dónde está y qué punto trae. Igual que `MapsLink.swift`
 * de iOS (CASA-02, CASA-05). No toca la red; lo que hay que resolver lo hace [HomeAddressLocator].
 */
object MapsLink {
    /** Lo más largo que se acepta en el campo: un enlace de Compartir con su texto. */
    const val MAX_TEXT = 500

    private val URL_IN_TEXT = Regex("""https?://\S+""")
    private val PIN = Regex("""!3d(-?\d{1,3}(?:\.\d+)?)!4d(-?\d{1,3}(?:\.\d+)?)""")
    private val AT = Regex("""@(-?\d{1,3}(?:\.\d+)?),(-?\d{1,3}(?:\.\d+)?)""")
    private val PAIR = Regex("""^\s*(-?\d{1,3}(?:\.\d+)?)\s*,\s*\+?(-?\d{1,3}(?:\.\d+)?)\s*$""")
    private val PATH = Regex("""/maps/(?:search|place|dir)/(-?\d{1,3}(?:\.\d+)?),\s*\+?(-?\d{1,3}(?:\.\d+)?)""")

    data class Point(
        val latitude: Double,
        val longitude: Double,
    )

    /** El primer enlace de mapas del texto (el de Compartir trae el nombre del lugar antes). */
    fun find(text: String): String? =
        URL_IN_TEXT.findAll(text).map { it.value.trimEnd('.', ',', ')') }.firstOrNull(::isMaps)

    fun isMaps(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase()
        if (scheme != "https" && scheme != "http") return false
        val host = uri.host?.lowercase() ?: return false
        if (isShort(url) || host == "maps.apple.com" || host.startsWith("maps.google.")) return true
        val domain = host.removePrefix("www.")
        return domain.startsWith("google.") && (uri.path ?: "").startsWith("/maps")
    }

    fun isShort(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase()
        return host == "maps.app.goo.gl" || (host == "goo.gl" && (uri.path ?: "").startsWith("/maps"))
    }

    /** El punto que el enlace trae escrito. El pin (!3d…!4d…) es el lugar elegido; @lat,lng solo el centro. */
    fun coordinate(url: String): Point? {
        val text = decode(url)
        point(PIN, text)?.let { return it }
        val uri = runCatching { Uri.parse(url) }.getOrNull()
        for (name in listOf("q", "query", "ll", "destination", "daddr", "sll", "center")) {
            uri?.getQueryParameter(name)?.let { value -> point(PAIR, value)?.let { return it } }
        }
        point(PATH, text)?.let { return it }
        // Google a veces manda primero a su aviso de cookies, con el enlace de verdad en `continue`.
        uri?.getQueryParameter("continue")?.takeIf { it != url }?.let { inner -> coordinate(inner)?.let { return it } }
        return point(AT, text)
    }

    /** El nombre o la dirección del lugar, para buscarlo cuando el enlace no trae el punto. */
    fun placeName(url: String): String? {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        for (name in listOf("q", "query", "address", "destination", "daddr")) {
            uri
                .getQueryParameter(name)
                ?.let(::readable)
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
        }
        val parts = uri.pathSegments
        val index = parts.indexOf("place")
        if (index >= 0 &&
            index + 1 < parts.size
        ) {
            readable(parts[index + 1]).takeIf { it.isNotEmpty() }?.let { return it }
        }
        return uri.getQueryParameter("continue")?.takeIf { it != url }?.let(::placeName)
    }

    private fun readable(text: String): String = decode(text).replace('+', ' ').trim()

    private fun decode(text: String): String =
        runCatching {
            URLDecoder.decode(text.replace("+", "%2B"), "UTF-8")
        }.getOrDefault(text)

    private fun point(
        pattern: Regex,
        text: String,
    ): Point? {
        val match = pattern.find(text) ?: return null
        val latitude = match.groupValues[1].toDoubleOrNull() ?: return null
        val longitude = match.groupValues[2].toDoubleOrNull() ?: return null
        if (kotlin.math.abs(latitude) > 90 || kotlin.math.abs(longitude) > 180) return null
        if (latitude == 0.0 && longitude == 0.0) return null
        return Point(latitude, longitude)
    }
}
