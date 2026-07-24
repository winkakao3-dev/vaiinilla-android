package com.vaiinilla.app.core.config

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EffectiveDataSourceResolver @Inject constructor(
    private val environment: AppEnvironment,
    @ApplicationContext private val context: Context,
) {
    var isTestOnlyMode: Boolean
        get() = TestOnlyPreferences.isEnabled(context)
        set(value) {
            TestOnlyPreferences.setEnabled(context, value)
        }

    val configuredMode: DataSourceMode
        get() = environment.dataSourceMode

    fun effectiveMode(): DataSourceMode =
        if (isTestOnlyMode || configuredMode == DataSourceMode.MOCK) {
            DataSourceMode.MOCK
        } else {
            DataSourceMode.REMOTE
        }

    fun usesNetwork(): Boolean = effectiveMode() == DataSourceMode.REMOTE
}
