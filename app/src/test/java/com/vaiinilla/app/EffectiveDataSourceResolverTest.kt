package com.vaiinilla.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vaiinilla.app.core.config.AppEnvironment
import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.core.config.EffectiveDataSourceResolver
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EffectiveDataSourceResolverTest {
    @Test
    fun `solo pruebas uses fixtures even when APK is configured remote`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resolver =
            EffectiveDataSourceResolver(
                environment = AppEnvironment(DataSourceMode.REMOTE, "https://example.invalid/api/v1/"),
                context = context,
            )

        resolver.isTestOnlyMode = true
        assertEquals(DataSourceMode.MOCK, resolver.effectiveMode())

        resolver.isTestOnlyMode = false
        assertEquals(DataSourceMode.REMOTE, resolver.effectiveMode())
    }
}
