package com.vaiinilla.app.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

/*
 * Liquid glass surfaces for the student bottom nav, built on Kyant's Backdrop library.
 * Backdrop blur needs RenderEffect (API 31) and the refraction lens needs RuntimeShader (API 33);
 * the library skips whichever effect the device lacks, and below API 31 (or without a backdrop)
 * these surfaces fall back to the solid theme colors the nav used before.
 */

internal val LiquidGlassCapsule = RoundedCornerShape(percent = 50)

/** How much of the screen shows through the dock; high enough to keep labels legible on photos. */
internal const val GLASS_TINT_ALPHA = 0.72f

internal const val LENS_TINT_ALPHA = 0.10f

private val DockBlurRadius = 8.dp
private val DockLensHeight = 16.dp
private val DockLensAmount = 20.dp
private val LensHeight = 10.dp
private val LensAmount = 14.dp
private val LensShadowRadius = 8.dp

private const val TAG = "LiquidGlass"

@Volatile
private var refractionRejected = false

/**
 * Refraction lens that gives up for good if this device's shader compiler rejects the AGSL
 * (some GPU drivers do), leaving the blurred glass instead of crashing the screen.
 */
private fun BackdropEffectScope.refract(
    height: Float,
    amount: Float,
) {
    if (refractionRejected) return
    try {
        lens(height, amount)
    } catch (error: IllegalArgumentException) {
        refractionRejected = true
        Log.w(TAG, "Shader de refracción rechazado; el vidrio sigue sin refracción", error)
    }
}

// Same AGSL features the library's rim highlight needs (a color uniform); tried once per process.
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

private fun rimHighlight(): Highlight = if (agslCompiles) Highlight.Default else Highlight.Plain

private fun isGlassSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * The dock: blurs and refracts what is behind it, and exports its own result to [dockBackdrop]
 * so the lens can refract the glass itself instead of the raw screen.
 */
internal fun Modifier.liquidGlassDock(
    backdrop: Backdrop?,
    dockBackdrop: LayerBackdrop,
    tint: Color,
    solid: Color,
    border: Color,
    elevation: Dp,
): Modifier =
    if (backdrop != null && isGlassSupported()) {
        drawBackdrop(
            backdrop = backdrop,
            shape = { LiquidGlassCapsule },
            effects = {
                vibrancy()
                blur(DockBlurRadius.toPx())
                refract(DockLensHeight.toPx(), DockLensAmount.toPx())
            },
            highlight = ::rimHighlight,
            exportedBackdrop = dockBackdrop,
            onDrawSurface = { drawRect(tint) },
        )
    } else {
        this
            .shadow(elevation, LiquidGlassCapsule)
            .clip(LiquidGlassCapsule)
            .background(solid)
            .border(1.dp, border, LiquidGlassCapsule)
    }

/** The moving capsule over the active tab. It draws behind the tabs and never takes touches. */
internal fun Modifier.liquidGlassLens(
    source: Backdrop?,
    tint: Color,
    solid: Color,
    layerBlock: GraphicsLayerScope.() -> Unit,
): Modifier =
    if (source != null && isGlassSupported()) {
        drawBackdrop(
            backdrop = source,
            shape = { LiquidGlassCapsule },
            effects = { refract(LensHeight.toPx(), LensAmount.toPx()) },
            highlight = ::rimHighlight,
            shadow = { Shadow(radius = LensShadowRadius) },
            layerBlock = layerBlock,
            onDrawSurface = { drawRect(tint) },
        )
    } else {
        this
            .graphicsLayer(layerBlock)
            .clip(LiquidGlassCapsule)
            .background(solid)
    }
