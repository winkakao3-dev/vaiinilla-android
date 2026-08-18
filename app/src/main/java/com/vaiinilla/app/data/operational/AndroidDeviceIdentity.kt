package com.vaiinilla.app.data.operational

import android.content.Context
import android.provider.Settings
import com.vaiinilla.app.domain.repository.DeviceIdentity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidDeviceIdentity
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : DeviceIdentity {
        private val applicationContext = context.applicationContext
        private val stableId: String by lazy {
            val androidId =
                Settings.Secure
                    .getString(
                        applicationContext.contentResolver,
                        Settings.Secure.ANDROID_ID,
                    )?.trim()
                    .orEmpty()
            if (androidId.isNotEmpty()) {
                "android-$androidId"
            } else {
                val preferences =
                    applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                val persisted =
                    preferences
                        .getString(KEY_DEVICE_ID, null)
                        ?.trim()
                        .orEmpty()
                if (persisted.isNotEmpty()) {
                    persisted
                } else {
                    "android-${UUID.randomUUID()}".also {
                        preferences.edit().putString(KEY_DEVICE_ID, it).apply()
                    }
                }
            }
        }

        override fun id(): String = stableId

        private companion object {
            const val PREFERENCES_NAME = "vaiinilla_device_identity"
            const val KEY_DEVICE_ID = "stable_device_id"
        }
    }
