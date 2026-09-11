package com.vaiinilla.app.ui.screens

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.auth.student.StudentAuthUiState
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialPrimaryButton
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.delay

private val EaseOutLiz = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) < 0.01f
    }
}

@Composable
private fun MaskedReveal(
    text: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    fontStyle: FontStyle = FontStyle.Normal,
    lineHeight: androidx.compose.ui.unit.TextUnit = fontSize,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp,
    delayMs: Int,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val progress by
        animateFloatAsState(
            targetValue = 1f,
            animationSpec =
                tween(
                    durationMillis = if (reduceMotion) 0 else 620,
                    delayMillis = if (reduceMotion) 0 else delayMs,
                    easing = EaseOutLiz,
                ),
            label = "reveal",
        )
    Text(
        text = text,
        color = color,
        style =
            TextStyle(
                fontFamily = VaiinillaSerif,
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                letterSpacing = letterSpacing,
            ),
        modifier =
            modifier
                .clipToBounds()
                .drawWithContent {
                    val visible = size.height * progress
                    clipRect(top = size.height - visible, bottom = size.height) {
                        this@drawWithContent.drawContent()
                    }
                },
    )
}

@Composable
private fun FadeUp(
    delayMs: Int,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val progress by
        animateFloatAsState(
            targetValue = 1f,
            animationSpec =
                tween(
                    durationMillis = if (reduceMotion) 0 else 520,
                    delayMillis = if (reduceMotion) 0 else delayMs,
                    easing = EaseOutLiz,
                ),
            label = "fade_up",
        )
    Box(
        modifier =
            modifier.graphicsLayer {
                alpha = progress
                translationY = (1f - progress) * 34f
            },
    ) {
        content()
    }
}

@Composable
private fun TypewriterLine(
    phrases: List<String>,
    color: Color,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(if (reduceMotion) phrases.first() else "") }
    if (!reduceMotion) {
        LaunchedEffect(Unit) {
            while (true) {
                for (phrase in phrases) {
                    for (index in 1..phrase.length) {
                        text = phrase.take(index)
                        delay(46)
                    }
                    delay(1500)
                    for (index in phrase.length downTo 0) {
                        text = phrase.take(index)
                        delay(14)
                    }
                    delay(200)
                }
            }
        }
    }
    val cursor by
        rememberInfiniteTransition(label = "cursor")
            .animateFloat(
                initialValue = 0.15f,
                targetValue = 0.75f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 520),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "cursor_alpha",
            )
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            color = color,
            fontSize = 11.5.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
        )
        Text(
            "▍",
            color = color.copy(alpha = if (reduceMotion) 0.5f else cursor),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun StudentAuthLandingScreen(
    state: StudentAuthUiState,
    onBack: (() -> Unit)?,
    onRegister: () -> Unit,
    onLogin: () -> Unit,
    onExplore: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val reduceMotion = rememberReduceMotion()
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
        ) {
            FadeUp(delayMs = 60, reduceMotion = reduceMotion) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Vaiinilla.",
                        color = colors.ink,
                        style =
                            TextStyle(
                                fontFamily = VaiinillaSerif,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.4).sp,
                            ),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "EDICIÓN 01",
                        color = colors.muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.8.sp,
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FadeUp(delayMs = 140, reduceMotion = reduceMotion) {
                    Text(
                        "TU CAFETERÍA, A TU RITMO",
                        color = if (colors.isDark) colors.accent else colors.accentInk,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.2.sp,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                MaskedReveal(
                    text = "Come bien.",
                    color = colors.ink,
                    fontSize = 46.sp,
                    lineHeight = 50.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-1.4).sp,
                    delayMs = 200,
                    reduceMotion = reduceMotion,
                )
                MaskedReveal(
                    text = "Sigue tu pedido.",
                    color = colors.ink,
                    fontSize = 46.sp,
                    lineHeight = 52.sp,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-1.2).sp,
                    delayMs = 340,
                    reduceMotion = reduceMotion,
                )
            }

            Spacer(Modifier.weight(1f))

            Box(modifier = Modifier.fillMaxWidth().height(286.dp)) {
                val accent = colors.accent
                val ink = colors.ink
                FloatingGlyph(
                    kind = VaiinillaGlyphKind.Leaf,
                    color = ink,
                    accent = accent,
                    sizeDp = 46.dp,
                    phase = 0.10f,
                    amplitudeDp = 5f,
                    durationMs = 5600,
                    modifier = Modifier.align(Alignment.TopStart).offset(x = 34.dp, y = 26.dp).size(46.dp),
                )
                FloatingGlyph(
                    kind = VaiinillaGlyphKind.Cup,
                    color = ink,
                    accent = accent,
                    sizeDp = 54.dp,
                    phase = 0.55f,
                    amplitudeDp = 6f,
                    durationMs = 6400,
                    modifier = Modifier.align(Alignment.CenterStart).offset(x = 34.dp, y = 10.dp).size(54.dp),
                )
                FloatingGlyph(
                    kind = VaiinillaGlyphKind.Bean,
                    color = ink,
                    accent = accent,
                    sizeDp = 42.dp,
                    phase = 0.30f,
                    amplitudeDp = 5f,
                    durationMs = 6000,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = (-48).dp, y = 30.dp).size(42.dp),
                )
                FloatingGlyph(
                    kind = VaiinillaGlyphKind.Spark,
                    color = ink,
                    accent = accent,
                    sizeDp = 26.dp,
                    phase = 0.80f,
                    amplitudeDp = 4f,
                    durationMs = 5200,
                    modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-36).dp, y = (-44).dp).size(26.dp),
                )
                FloatingGlyph(
                    kind = VaiinillaGlyphKind.Cube,
                    color = ink,
                    accent = accent,
                    sizeDp = 40.dp,
                    phase = 0.65f,
                    amplitudeDp = 5f,
                    durationMs = 6800,
                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-52).dp, y = (-38).dp).size(40.dp),
                )
                FloatingGlyph(
                    kind = VaiinillaGlyphKind.Note,
                    color = ink,
                    accent = accent,
                    sizeDp = 36.dp,
                    phase = 0.42f,
                    amplitudeDp = 5f,
                    durationMs = 6200,
                    modifier = Modifier.align(Alignment.BottomStart).offset(x = 74.dp, y = (-26).dp).size(36.dp),
                )

                Box(
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    FadeUp(delayMs = 430, reduceMotion = reduceMotion) {
                        VaiinillaMascot(
                            modifier = Modifier.size(176.dp),
                            delayMs = 430,
                            paperColor = Color(0xFFF5ECDA),
                            accentColor = colors.accent,
                            inkColor = Color(0xFF1D1C18),
                        )
                    }
                }

                FadeUp(
                    delayMs = 760,
                    reduceMotion = reduceMotion,
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    TypewriterLine(
                        phrases = listOf("cargando tu cafetería…", "preparando tu menú…", "casi listo"),
                        color = colors.muted,
                        reduceMotion = reduceMotion,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            FadeUp(delayMs = 880, reduceMotion = reduceMotion) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    EditorialAccentButton(
                        text = "Comenzar",
                        onClick = onRegister,
                        enabled = !state.loading,
                    )
                    EditorialPrimaryButton(
                        text = "Ya tengo una cuenta",
                        onClick = onLogin,
                        enabled = !state.loading,
                        background = colors.paper2,
                        contentColor = colors.ink,
                    )
                    TextButton(
                        onClick = onExplore,
                        enabled = !state.loading,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(
                            "Explorar sin cuenta",
                            color = colors.muted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier =
                    Modifier
                        .statusBarsPadding()
                        .padding(start = 8.dp, top = 4.dp),
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
            }
        }

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent)
            }
        }
    }
}

@Preview(name = "Bienvenida v2", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun StudentAuthLandingScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        StudentAuthLandingScreen(
            state = StudentAuthUiState(),
            onBack = null,
            onRegister = {},
            onLogin = {},
            onExplore = {},
        )
    }
}
