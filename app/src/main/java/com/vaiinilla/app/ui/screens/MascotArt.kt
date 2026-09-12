package com.vaiinilla.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.R
import kotlinx.coroutines.delay

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val VaiinillaSerif =
    FontFamily(
        Font(
            R.font.fraunces,
            weight = FontWeight.SemiBold,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(600),
                    FontVariation.Setting("SOFT", 40f),
                    FontVariation.Setting("WONK", 1f),
                ),
        ),
        Font(
            R.font.fraunces,
            weight = FontWeight.Bold,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(700),
                    FontVariation.Setting("SOFT", 30f),
                    FontVariation.Setting("WONK", 1f),
                ),
        ),
        Font(
            R.font.fraunces_italic,
            weight = FontWeight.Medium,
            style = FontStyle.Italic,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(560),
                    FontVariation.Setting("SOFT", 70f),
                    FontVariation.Setting("WONK", 1f),
                ),
        ),
    )

private fun pathOf(data: String): Path = PathParser().parsePathString(data).toPath()

private val MASCOT_ARM_LEFT = pathOf("M24.1 52.5L16.49 61.65")
private val MASCOT_ARM_RIGHT = pathOf("M75.9 52.5L83.51 61.65")
private val MASCOT_LEG_LEFT = pathOf("M39.75 70.5L39.75 77.65")
private val MASCOT_LEG_RIGHT = pathOf("M60.25 70.5L60.25 77.65")
private val MASCOT_BODY =
    pathOf(
        "M23.7 18.9Q25.3 17.1 26.19 17.91L29.39 20.85Q30.87 22.2 32.34 20.85L35.55 17.91Q36.43 17.1 " +
            "37.32 17.91L40.53 20.85Q42 22.2 43.47 20.85L46.68 17.91Q47.56 17.1 48.45 17.91L51.66 20.85Q53.13 22.2 " +
            "54.6 20.85L57.81 17.91Q58.7 17.1 59.07 18.24L59.64 19.94Q59.9 20.7 60.46 21.27L77.33 38.14Q77.9 38.7 " +
            "77.9 39.5L77.9 67.3Q77.9 72.5 72.7 72.5L27.3 72.5Q22.1 72.5 22.1 67.3L22.1 25.9Q22.1 20.7 23.7 18.9Z",
    )
private val MASCOT_FOLD = pathOf("M59.9 20.7L59.9 33.1Q59.9 38.7 65.49 38.7L77.9 38.7Z")
private val MASCOT_BAR_TOP =
    pathOf("M30 29.1a1.8 1.8 0 0 1 1.8-1.8h19.21a1.8 1.8 0 0 1 0 3.6H31.8A1.8 1.8 0 0 1 30 29.1Z")
private val MASCOT_BAR_BOTTOM =
    pathOf("M29.85 36.15a1.65 1.65 0 0 1 1.65-1.65h12.09a1.65 1.65 0 0 1 0 3.3H31.5a1.65 1.65 0 0 1-1.65-1.65Z")
private val MASCOT_EYE_LEFT =
    pathOf(
        "M43.1 52.5C43.1 56.54 42.35 57.9 40.1 57.9C37.85 57.9 37.1 56.54 37.1 52.5C37.1 48.46 " +
            "37.85 47.1 40.1 47.1C42.35 47.1 43.1 48.46 43.1 52.5Z",
    )
private val MASCOT_EYE_RIGHT =
    pathOf(
        "M62.9 52.5C62.9 56.54 62.15 57.9 59.9 57.9C57.65 57.9 56.9 56.54 56.9 52.5C56.9 48.46 " +
            "57.65 47.1 59.9 47.1C62.15 47.1 62.9 48.46 62.9 52.5Z",
    )

@Composable
fun VaiinillaMascot(
    modifier: Modifier = Modifier,
    delayMs: Long = 0,
    paperColor: Color,
    accentColor: Color,
    inkColor: Color,
    reduceMotion: Boolean = false,
    shadowColor: Color = Color(0x1C000000),
) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(delayMs, reduceMotion) {
        if (reduceMotion) {
            appear.snapTo(1f)
        } else {
            appear.snapTo(0f)
            delay(delayMs)
            appear.animateTo(1f, spring(dampingRatio = 0.58f, stiffness = 240f))
        }
    }
    val idle = rememberInfiniteTransition(label = "mascot_idle")
    val breathe by
        idle.animateFloat(
            initialValue = 1f,
            targetValue = 1.018f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "breathe",
        )
    val sway by
        idle.animateFloat(
            initialValue = -1.1f,
            targetValue = 1.1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "sway",
        )
    val blink by
        idle.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        keyframes {
                            durationMillis = 3600
                            1f at 0
                            1f at 3040
                            0.06f at 3120 using FastOutSlowInEasing
                            1f at 3230 using FastOutSlowInEasing
                            1f at 3600
                        },
                    repeatMode = RepeatMode.Restart,
                ),
            label = "blink",
        )

    val resolvedBreathe = if (reduceMotion) 1f else breathe
    val resolvedSway = if (reduceMotion) 0f else sway
    val resolvedBlink = if (reduceMotion) 1f else blink

    Canvas(
        modifier = modifier,
    ) {
        val s = size.minDimension / 100f
        val alpha = appear.value.coerceIn(0f, 1f)
        if (alpha <= 0.001f) return@Canvas
        withTransform({
            scale(alpha, alpha, pivot = Offset(50f * s, 82f * s))
            translate(0f, (1f - alpha) * -26f * s)
            rotate(resolvedSway, pivot = Offset(50f * s, 82f * s))
            scale(1f, resolvedBreathe, pivot = Offset(50f * s, 82f * s))
        }) {
            withTransform({ scale(s, s, pivot = Offset.Zero) }) {
                withTransform({ translate(0f, 1.5f) }) {
                    drawPath(MASCOT_BODY, shadowColor)
                }
                drawPath(MASCOT_ARM_LEFT, paperColor, style = Stroke(7f, cap = StrokeCap.Round))
                drawPath(MASCOT_ARM_RIGHT, paperColor, style = Stroke(7f, cap = StrokeCap.Round))
                drawPath(MASCOT_LEG_LEFT, paperColor, style = Stroke(11.7f, cap = StrokeCap.Round))
                drawPath(MASCOT_LEG_RIGHT, paperColor, style = Stroke(11.7f, cap = StrokeCap.Round))
                drawPath(MASCOT_BODY, paperColor)
                drawPath(MASCOT_FOLD, accentColor)
                drawPath(MASCOT_BAR_TOP, accentColor)
                drawPath(MASCOT_BAR_BOTTOM, accentColor)
                withTransform({
                    scale(1f, resolvedBlink, pivot = Offset(40.1f, 52.5f))
                }) {
                    drawPath(MASCOT_EYE_LEFT, inkColor)
                }
                withTransform({
                    scale(1f, resolvedBlink, pivot = Offset(59.9f, 52.5f))
                }) {
                    drawPath(MASCOT_EYE_RIGHT, inkColor)
                }
            }
        }
    }
}

enum class VaiinillaGlyphKind { Cup, Leaf, Bean, Spark, Cube, Note }

private val GLYPH_CUP_BODY =
    pathOf("M22 34h44a6 6 0 0 1 6 6v12a24 24 0 0 1-24 24H40a24 24 0 0 1-24-24V40a6 6 0 0 1 6-6Z")
private val GLYPH_CUP_HANDLE = pathOf("M72 42h8a10 10 0 0 1 0 20h-6")
private val GLYPH_CUP_STEAM = pathOf("M28 24c0-5 5-6 5-11M46 24c0-5 5-6 5-11M64 24c0-5 5-6 5-11")
private val GLYPH_LEAF_BODY = pathOf("M24 76C24 40 44 22 80 20c2 38-16 58-56 56Z")
private val GLYPH_LEAF_VEIN = pathOf("M30 70c14-14 28-26 44-42")
private val GLYPH_BEAN_BODY = pathOf("M50 20a22 30 0 1 0 0 60a22 30 0 1 0 0-60Z")
private val GLYPH_BEAN_VEIN = pathOf("M40 30c12 10 8 30 20 40")
private val GLYPH_SPARK = pathOf("M50 14c4 18 14 28 32 32-18 4-28 14-32 32-4-18-14-28-32-32 18-4 28-14 32-32Z")
private val GLYPH_CUBE =
    pathOf("M36 24h28a12 12 0 0 1 12 12v28a12 12 0 0 1-12 12H36a12 12 0 0 1-12-12V36a12 12 0 0 1 12-12Z")
private val GLYPH_CUBE_CROSS = pathOf("M24 44h52M44 24v52")
private val GLYPH_NOTE_BODY =
    pathOf(
        "M26 34.5 32 30l6 4.5L44 30l6 4.5L56 30l6 4.5L68 30l6 4.5V76a6 6 0 0 1-6 6H32a6 6 0 0 1-6-6V34.5Z",
    )
private val GLYPH_NOTE_FOLD = pathOf("M58 30h10a6 6 0 0 1 6 6v10L58 30Z")
private val GLYPH_NOTE_LINES = pathOf("M36 46h20M36 56h14")

fun DrawScope.drawVaiinillaGlyph(
    kind: VaiinillaGlyphKind,
    color: Color,
    accent: Color,
    strokeScale: Float = 1f,
) {
    val s = size.minDimension / 100f
    val w = 6.5f * strokeScale

    fun str(path: Path) = drawPath(path, color, style = Stroke(w, cap = StrokeCap.Round))
    withTransform({ scale(s, s, pivot = Offset.Zero) }) {
        when (kind) {
            VaiinillaGlyphKind.Cup -> {
                str(GLYPH_CUP_BODY)
                str(GLYPH_CUP_HANDLE)
                str(GLYPH_CUP_STEAM)
            }

            VaiinillaGlyphKind.Leaf -> {
                str(GLYPH_LEAF_BODY)
                str(GLYPH_LEAF_VEIN)
            }

            VaiinillaGlyphKind.Bean -> {
                withTransform({ rotate(-24f, pivot = Offset(50f, 50f)) }) {
                    str(GLYPH_BEAN_BODY)
                    str(GLYPH_BEAN_VEIN)
                }
            }

            VaiinillaGlyphKind.Spark -> {
                drawPath(GLYPH_SPARK, accent)
            }

            VaiinillaGlyphKind.Cube -> {
                str(GLYPH_CUBE)
                str(GLYPH_CUBE_CROSS)
            }

            VaiinillaGlyphKind.Note -> {
                str(GLYPH_NOTE_BODY)
                drawPath(GLYPH_NOTE_FOLD, accent)
                str(GLYPH_NOTE_LINES)
            }
        }
    }
}

@Composable
fun FloatingGlyph(
    kind: VaiinillaGlyphKind,
    color: Color,
    accent: Color,
    sizeDp: androidx.compose.ui.unit.Dp,
    phase: Float,
    amplitudeDp: Float,
    durationMs: Int,
    reduceMotion: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (reduceMotion) {
        Canvas(modifier = modifier) {
            drawVaiinillaGlyph(kind, color, accent)
        }
        return
    }
    val transition = rememberInfiniteTransition(label = "glyph_$kind")
    val float by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "float_$kind",
        )
    val density = androidx.compose.ui.platform.LocalDensity.current
    val angle = (float + phase) * 2f * Math.PI.toFloat()
    val dx = with(density) { (kotlin.math.sin(angle) * amplitudeDp).dp.toPx() }
    val dy = with(density) { (kotlin.math.cos(angle * 0.8f) * amplitudeDp * 0.9f).dp.toPx() }
    val rot = kotlin.math.sin(angle * 0.6f) * 9f

    Canvas(
        modifier =
            modifier.graphicsLayer(
                translationX = dx,
                translationY = dy,
                rotationZ = rot,
            ),
    ) {
        drawVaiinillaGlyph(kind, color, accent)
    }
}
