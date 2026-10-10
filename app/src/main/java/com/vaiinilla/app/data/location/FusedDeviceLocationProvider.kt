package com.vaiinilla.app.data.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.vaiinilla.app.domain.location.DeviceLocationProvider
import com.vaiinilla.app.domain.model.GeoCoordinates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class FusedDeviceLocationProvider(
    context: Context,
) : DeviceLocationProvider {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    // El permiso lo pide la pantalla antes de llamar; aquí solo se lee la posición.
    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): GeoCoordinates? {
        val cancellation = CancellationTokenSource()
        val location =
            try {
                client
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
                    .await()
                    ?: client.lastLocation.await()
            } catch (cancelled: CancellationException) {
                cancellation.cancel()
                throw cancelled
            }
        return location?.let { GeoCoordinates(latitude = it.latitude, longitude = it.longitude) }
    }
}
