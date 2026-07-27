package com.vaiinilla.app.core.config

import com.vaiinilla.app.BuildConfig

/**
 * Demo-only surfaces (role switcher interno, galería, wallet, asistente).
 *
 * Technically gated to **debug builds** and unlocked only when **Solo pruebas** is on.
 * Release APKs never expose these tools.
 */
object DemoFeatures {
    val toolsAvailable: Boolean
        get() = BuildConfig.ALLOW_DEMO_TOOLS

    fun isUnlocked(testOnlyMode: Boolean): Boolean = toolsAvailable && testOnlyMode
}
