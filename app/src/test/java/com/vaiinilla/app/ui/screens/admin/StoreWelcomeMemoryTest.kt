package com.vaiinilla.app.ui.screens.admin

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Y02: la marca no guarda fecha, así que ninguna hora ni cambio de día la borra; una instancia nueva la sigue viendo. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StoreWelcomeMemoryTest {
    private val prefs =
        ApplicationProvider
            .getApplicationContext<Context>()
            .getSharedPreferences("bienvenida", Context.MODE_PRIVATE)

    @Test
    fun `la bienvenida vista no vuelve al abrir la app otro dia`() {
        val today = StoreWelcomeMemory(prefs, "vista_1")
        assertFalse(today.seen)

        today.markSeen()

        assertTrue(StoreWelcomeMemory(prefs, "vista_1").seen)
        assertFalse(StoreWelcomeMemory(prefs, "vista_2").seen)
    }
}
