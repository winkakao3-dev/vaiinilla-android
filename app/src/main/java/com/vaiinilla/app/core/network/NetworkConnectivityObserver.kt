package com.vaiinilla.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lets repositories/ViewModels check connectivity proactively (e.g. show "Sin conexión a
 * internet" immediately) instead of only reacting after a timeout has already elapsed.
 * This complements, and does not replace, [toUserFacingMessage]'s post-hoc error mapping.
 */
@Singleton
class NetworkConnectivityObserver
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val connectivityManager =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        @Suppress("DEPRECATION")
        fun isOnline(): Boolean {
            val manager = connectivityManager ?: return true
            return manager.activeNetworkInfo?.isConnectedOrConnecting == true
        }
    }
