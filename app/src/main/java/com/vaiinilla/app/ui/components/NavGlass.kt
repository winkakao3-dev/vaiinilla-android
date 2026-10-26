package com.vaiinilla.app.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/*
 * El vidrio de la barra del alumno y del círculo de perfil. Desenfoca y dobla lo que pasa
 * por detrás (blur desde API 31, refracción de orilla desde API 33); sin eso queda una
 * superficie lechosa sólida con la misma forma.
 */

private val Capsule = RoundedCornerShape(percent = 50)
private val GlassBlur = 20.dp
private val EdgeLensHeight = 12.dp
private val EdgeLensAmount = 22.dp
private val GlassShadowRadius = 22.dp

private const val TAG = "NavGlass"

/** Colores del vidrio; los de iOS 26 sobre claro y sobre oscuro. */
internal data class NavGlassStyle(
    val tint: Color,
    val solid: Color,
    val hairline: Color,
    val lensRest: Color,
    val lensLifted: Color,
    val lensShadow: Color,
    val rim: Color,
    val badgeRing: Color,
)

@Composable
internal fun navGlassStyle(): NavGlassStyle =
    if (LocalVaiinillaColors.current.isDark) {
        NavGlassStyle(
            tint = Color(0xA61C1C1E),
            solid = Color(0xF21C1C1E),
            hairline = Color(0x1FFFFFFF),
            lensRest = Color(0x24FFFFFF),
            lensLifted = Color(0x14FFFFFF),
            lensShadow = Color(0x59000000),
            rim = Color(0x8CFFFFFF),
            badgeRing = Color(0xFF1C1C1E),
        )
    } else {
        NavGlassStyle(
            tint = Color(0xBDFFFFFF),
            solid = Color(0xF7F9F9FA),
            hairline = Color(0x14000000),
            lensRest = Color(0x14000000),
            lensLifted = Color(0x66FFFFFF),
            lensShadow = Color(0x1F000000),
            rim = Color(0xE6FFFFFF),
            badgeRing = Color.White,
        )
    }

@Volatile
private var refractionRejected = false

/** Algunos drivers rechazan el AGSL de la refracción: entonces se queda el blur y no truena. */
private fun BackdropEffectScope.edgeLens() {
    if (refractionRejected) return
    try {
        lens(EdgeLensHeight.toPx(), EdgeLensAmount.toPx())
    } catch (error: IllegalArgumentException) {
        refractionRejected = true
        Log.w(TAG, "Shader de refracción rechazado; el vidrio sigue sin refracción", error)
    }
}

private const val PROBE_AGSL =
    "uniform shader content; layout(color) uniform half4 tint; half4 main(float2 p) { return content.eval(p) * tint; }"

private val agslCompiles: Boolean by lazy {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && probeAgsl()
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun probeAgsl(): Boolean =
    try {
        RuntimeShader(PROBE_AGSL)
        true
    } catch (error: IllegalArgumentException) {
        Log.w(TAG, "Este dispositivo no compila AGSL; el vidrio usa borde plano", error)
        false
    }

internal fun Modifier.navGlass(
    backdrop: Backdrop?,
    style: NavGlassStyle,
): Modifier =
    if (backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        drawBackdrop(
            backdrop = backdrop,
            shape = { Capsule },
            effects = {
                vibrancy()
                blur(GlassBlur.toPx())
                edgeLens()
            },
            highlight = { if (agslCompiles) Highlight.Default else Highlight.Plain },
            shadow = { Shadow(radius = GlassShadowRadius) },
            onDrawSurface = { drawRect(style.tint) },
        )
    } else {
        this
            .shadow(14.dp, Capsule, ambientColor = Color(0x33000000), spotColor = Color(0x33000000))
            .clip(Capsule)
            .background(style.solid)
            .border(0.5.dp, style.hairline, Capsule)
    }
