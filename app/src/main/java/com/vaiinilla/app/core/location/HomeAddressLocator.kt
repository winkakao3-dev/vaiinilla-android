package com.vaiinilla.app.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Convierte lo que escribe la persona (un enlace de Google Maps o una dirección) o su ubicación actual en un punto,
 * igual que `HomeAddressLocator.swift`. Sirve al admin para su negocio (CASA-02) y al cliente para su entrega (CASA-05).
 */
object HomeAddressLocator {
    private val SPANISH_MX = Locale.forLanguageTag("es-MX")

    suspend fun locate(
        context: Context,
        text: String,
    ): MapsLink.Point? =
        withContext(Dispatchers.IO) {
            val link = MapsLink.find(text)
            if (link != null) locateLink(context, link) else geocode(context, text.trim())
        }

    /** La calle, colonia y ciudad del punto, para llenar el campo de dirección. */
    suspend fun address(
        context: Context,
        point: MapsLink.Point,
    ): String? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            @Suppress("DEPRECATION")
            val place =
                runCatching { Geocoder(context, SPANISH_MX).getFromLocation(point.latitude, point.longitude, 1) }
                    .getOrNull()
                    ?.firstOrNull() ?: return@withContext null
            val street = listOfNotNull(place.thoroughfare, place.subThoroughfare).joinToString(" ")
            listOfNotNull(street.ifBlank { place.featureName }, place.subLocality, place.locality)
                .map(String::trim)
                .filter(String::isNotEmpty)
                .takeIf { it.isNotEmpty() }
                ?.joinToString(", ")
        }

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** La última ubicación que conoce el teléfono; null sin permiso o si nunca tuvo una. */
    @SuppressLint("MissingPermission")
    fun currentLocation(context: Context): MapsLink.Point? {
        if (!hasLocationPermission(context)) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        return manager
            .getProviders(true)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.let { MapsLink.Point(it.latitude, it.longitude) }
    }

    private fun locateLink(
        context: Context,
        link: String,
    ): MapsLink.Point? {
        val url = if (MapsLink.isShort(link)) expand(link) else link
        MapsLink.coordinate(url)?.let { return it }
        val name = MapsLink.placeName(url) ?: return null
        return geocode(context, name)
    }

    private fun geocode(
        context: Context,
        address: String,
    ): MapsLink.Point? {
        if (address.isEmpty() || !Geocoder.isPresent()) return null
        @Suppress("DEPRECATION")
        val found = runCatching { Geocoder(context, SPANISH_MX).getFromLocationName(address, 1) }.getOrNull()?.firstOrNull()
        return found?.let { MapsLink.Point(it.latitude, it.longitude) }
    }

    /** A dónde lleva un enlace corto. Sigue solo los saltos dentro de goo.gl, sin bajar la página de Maps. */
    private fun expand(link: String): String {
        var current = link
        repeat(4) {
            val connection = (URL(current).openConnection() as? HttpURLConnection) ?: return current
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.requestMethod = "GET"
                val code = connection.responseCode
                val location = connection.getHeaderField("Location")
                if (code !in 300..399 || location == null) return current
                val next = URL(URL(current), location).toString()
                if (!MapsLink.isShort(next)) return next
                current = next
            } catch (error: java.io.IOException) {
                return current
            } finally {
                connection.disconnect()
            }
        }
        return current
    }
}
