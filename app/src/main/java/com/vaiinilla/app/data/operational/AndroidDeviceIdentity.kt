package com.vaiinilla.app.data.operational

import android.content.Context
import com.vaiinilla.app.domain.repository.DeviceIdentity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** App-scoped installation identifier. It deliberately avoids hardware/device identifiers. */
@Singleton
class AndroidDeviceIdentity
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : DeviceIdentity {
        private val applicationContext = context.applicationContext
        private val stableId: String by lazy {
            val preferences =
                applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            preferences
                .getString(KEY_DEVICE_ID, null)
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: "android-${UUID.randomUUID()}".also { generated ->
                    preferences.edit().putString(KEY_DEVICE_ID, generated).apply()
                }
        }

        override fun id(): String = stableId

        private companion object {
            const val PREFERENCES_NAME = "vaiinilla_device_identity"
            const val KEY_DEVICE_ID = "stable_device_id_v2"
        }
    }
