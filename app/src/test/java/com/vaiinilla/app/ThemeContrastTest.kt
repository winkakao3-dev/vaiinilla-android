package com.vaiinilla.app

import androidx.compose.ui.graphics.Color
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ThemeContrastTest {
    @Test
    fun `core theme text pairs stay readable in every supported mode`() {
        listOf(
            VaiinillaThemeMode.Light,
            VaiinillaThemeMode.Dark,
            VaiinillaThemeMode.Amoled,
        ).forEach { mode ->
            val colors = mode.resolveColors()
            assertContrast(mode, colors.paper, colors.ink)
            assertContrast(mode, colors.paper, colors.muted)
            assertContrast(mode, colors.paper2, colors.muted)
            assertContrast(mode, colors.accent, colors.accentInk)
        }
    }

    private fun assertContrast(
        mode: VaiinillaThemeMode,
        background: Color,
        foreground: Color,
    ) {
        assertTrue(
            "Contrast below AA for $mode: ${contrastRatio(background, foreground)}",
            contrastRatio(background, foreground) >= 4.5,
        )
    }

    private fun contrastRatio(
        background: Color,
        foreground: Color,
    ): Double {
        val backgroundLuminance = relativeLuminance(background)
        val foregroundLuminance = relativeLuminance(foreground)
        val lighter = maxOf(backgroundLuminance, foregroundLuminance)
        val darker = minOf(backgroundLuminance, foregroundLuminance)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun linearize(channel: Float): Double {
            val value = channel.toDouble()
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }

        return (0.2126 * linearize(color.red)) +
            (0.7152 * linearize(color.green)) +
            (0.0722 * linearize(color.blue))
    }
}
