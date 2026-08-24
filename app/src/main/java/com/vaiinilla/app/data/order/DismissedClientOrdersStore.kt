package com.vaiinilla.app.data.order

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Persists client-only order dismissals so polling cannot resurrect hidden order cards. */
@Singleton
class DismissedClientOrdersStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val preferences =
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

        fun read(): Set<String> =
            preferences
                .getStringSet(KEY_DISMISSED_ORDER_IDS, emptySet())
                ?.toSet()
                .orEmpty()

        fun dismiss(orderId: String) {
            if (orderId.isBlank()) return
            val updated = read().toMutableSet().apply { add(orderId) }
            preferences.edit().putStringSet(KEY_DISMISSED_ORDER_IDS, updated).apply()
        }

        fun isDismissed(orderId: String): Boolean = orderId in read()

        internal fun clear() {
            preferences.edit().remove(KEY_DISMISSED_ORDER_IDS).apply()
        }

        private companion object {
            const val PREFERENCES_NAME = "vaiinilla_dismissed_client_orders"
            const val KEY_DISMISSED_ORDER_IDS = "dismissed_order_ids"
        }
    }
