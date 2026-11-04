package com.vaiinilla.app.data.counter

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.vaiinilla.app.domain.counter.ConnectivityWatcher

class AndroidConnectivityWatcher(
    context: Context,
) : ConnectivityWatcher {
    private val manager =
        context.applicationContext.getSystemService(
            Context.CONNECTIVITY_SERVICE,
        ) as? ConnectivityManager

    override fun onReconnect(callback: () -> Unit): AutoCloseable {
        val manager = manager ?: return AutoCloseable {}
        val networkCallback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = callback()
            }
        val registered = runCatching { manager.registerDefaultNetworkCallback(networkCallback) }.isSuccess
        return AutoCloseable { if (registered) runCatching { manager.unregisterNetworkCallback(networkCallback) } }
    }

    // Solo «no» cuando de plano no hay red o no tiene salida a internet. No se exige VALIDATED: un Wi‑Fi que corta
    // la prueba de Google, o los segundos en que Android aún valida, harían guardar todo como sin internet. Ante la
    // duda (sin servicio o sin permiso) se dice que sí: se intenta con red como antes.
    override fun hasInternet(): Boolean {
        val manager = manager ?: return true
        return runCatching {
            val network = manager.activeNetwork ?: return false
            val capabilities = manager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(true)
    }
}
