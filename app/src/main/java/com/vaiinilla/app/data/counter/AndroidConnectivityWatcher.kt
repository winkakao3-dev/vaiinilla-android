package com.vaiinilla.app.data.counter

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
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
}
