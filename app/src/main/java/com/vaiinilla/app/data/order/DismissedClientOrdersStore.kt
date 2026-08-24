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
        @param:ApplicationContext private val context: Context,
    ) {
        private val preferences =
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

        init {
            discardUnsafeLegacyDismissals()
        }

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
            preferences.edit().remove(KEY_DISMISSED_ORDER_IDS).commit()
        }

        /**
         * v1 dismissed an order as soon as a swipe crossed the threshold, before confirmation existed.
         * Those IDs are unsafe to carry forward because accidental swipes became persistent removals.
         * v2 starts clean once, then only stores dismissals made through the confirmation dialog.
         */
        private fun discardUnsafeLegacyDismissals() {
            val legacy = context.getSharedPreferences(LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE)
            if (legacy.contains(KEY_DISMISSED_ORDER_IDS)) {
                legacy.edit().remove(KEY_DISMISSED_ORDER_IDS).commit()
            }
        }

        internal companion object {
            const val LEGACY_PREFERENCES_NAME = "vaiinilla_dismissed_client_orders"
            const val PREFERENCES_NAME = "vaiinilla_dismissed_client_orders_v2"
            const val KEY_DISMISSED_ORDER_IDS = "dismissed_order_ids"
        }
    }
