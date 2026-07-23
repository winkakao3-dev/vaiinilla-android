package com.vaiinilla.app.core.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesPickupTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) : PickupTokenStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun save(orderId: String, pickupToken: String) {
        if (orderId.isBlank() || pickupToken.isBlank()) return
        preferences.edit().putString(orderId, pickupToken).apply()
    }

    override fun read(orderId: String): String? = preferences.getString(orderId, null)

    private companion object {
        const val PREFERENCES_NAME = "pickup_tokens"
    }
}
