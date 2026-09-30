package com.vaiinilla.app.ui.screens

import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.R
import com.vaiinilla.app.ui.auth.student.StudentAuthUiState
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.delay

private val EaseOutLiz = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Composable
internal fun rememberReduceMotion(): Boolean {
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
private fun WelcomeEntrance(
    delayMs: Int,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    travelDp: Float = 12f,
    content: @Composable () -> Unit,
) {
    val progress = remember(reduceMotion) { Animatable(if (reduceMotion) 1f else 0f) }
    val travelPx = with(LocalDensity.current) { travelDp.dp.toPx() }
    LaunchedEffect(delayMs, reduceMotion) {
        if (reduceMotion) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            delay(delayMs.toLong())
            progress.animateTo(
                1f,
                tween(durationMillis = 460, easing = EaseOutLiz),
            )
        }
    }
    val blurPx = with(LocalDensity.current) { 7.dp.toPx() }
    Box(
        modifier =
            modifier.graphicsLayer {
                alpha = progress.value
                translationY = (1f - progress.value) * travelPx
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val radius = (1f - progress.value) * blurPx
                    renderEffect = if (radius > 0.5f) BlurEffect(radius, radius, TileMode.Decal) else null
                }
            },
    ) {
        content()
    }
}

@Composable
private fun WelcomeTopBar(
    onBack: (() -> Unit)?,
    onExplore: () -> Unit,
    enabled: Boolean,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp),
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
            }
        }
        Text(
            text = "Vaiinilla.",
            color = colors.ink,
            fontFamily = VaiinillaSerif,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.35).sp,
            modifier = Modifier.align(Alignment.Center),
        )
        TextButton(
            onClick = onExplore,
            enabled = enabled,
            modifier = Modifier.align(Alignment.CenterEnd).heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            Text(
                "Explorar",
                color = colors.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun StudentAuthLandingScreen(
    state: StudentAuthUiState,
    onBack: (() -> Unit)?,
    onRegister: () -> Unit,
    onLogin: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onExplore: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val reduceMotion = rememberReduceMotion()
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(colors.paper),
    ) {
        val compact = maxHeight < 820.dp
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
        ) {
            WelcomeEntrance(delayMs = 0, reduceMotion = reduceMotion, travelDp = 6f) {
                WelcomeTopBar(
                    onBack = onBack,
                    onExplore = onExplore,
                    enabled = !state.loading,
                )
            }
            WelcomeHero(
                reduceMotion = reduceMotion,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 6.dp),
            )
            WelcomeEntrance(
                delayMs = 380,
                reduceMotion = reduceMotion,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 4.dp, bottom = if (compact) 16.dp else 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Tu cafetería,",
                        color = colors.ink,
                        fontFamily = VaiinillaSerif,
                        fontSize = if (compact) 36.sp else 41.sp,
                        lineHeight = if (compact) 38.sp else 43.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-1.1).sp,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "a tu ritmo.",
                        color = colors.ink,
                        fontFamily = VaiinillaSerif,
                        fontSize = if (compact) 36.sp else 41.sp,
                        lineHeight = if (compact) 38.sp else 43.sp,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = (-1.0).sp,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Pide, sigue tu pedido y paga desde un solo lugar.",
                        color = colors.muted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = if (compact) 8.dp else 12.dp),
                    )
                }
            }
            WelcomeEntrance(
                delayMs = 520,
                reduceMotion = reduceMotion,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(
                                start = 20.dp,
                                end = 20.dp,
                                bottom = if (compact) 10.dp else 18.dp,
                            ),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    state.errorMessage?.let { error ->
                        AuthErrorBanner(error)
                    }
                    EditorialAccentButton(
                        text = "Crear cuenta",
                        onClick = onRegister,
                        enabled = !state.loading,
                    )
                    GooglePillSignInButton(
                        onClick = onGoogleSignIn,
                        enabled = !state.loading,
                        background = colors.paper2,
                        border = colors.line,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "¿Ya tienes cuenta?",
                            color = colors.muted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        TextButton(
                            onClick = onLogin,
                            enabled = !state.loading,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        ) {
                            Text(
                                "Iniciar sesión",
                                color = colors.ink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
        if (state.loading) {
            Box(
                modifier = Modifier.fillMaxSize().background(colors.paper.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = colors.accent,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

/** Full-width secondary auth surface with the same height and radius as the primary CTA. */
@Composable
private fun AuthPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    background: Color,
    border: Color,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .physicalPress(enabled = enabled, onClick = onClick)
                .clip(shape)
                .background(background)
                .border(1.dp, border, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Full-width Google option using the official multicolor mark and explicit action label. */
@Composable
private fun GooglePillSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    background: Color,
    border: Color,
) {
    AuthPillButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        background = background,
        border = border,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_google_logo),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                "Continuar con Google",
                color = LocalVaiinillaColors.current.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Preview(name = "Bienvenida V2 · claro", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun StudentAuthLandingScreenLightPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        StudentAuthLandingScreen(
            state = StudentAuthUiState(),
            onBack = null,
            onRegister = {},
            onLogin = {},
            onGoogleSignIn = {},
            onExplore = {},
        )
    }
}

@Preview(name = "Bienvenida V2 · oscuro", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun StudentAuthLandingScreenDarkPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Dark) {
        StudentAuthLandingScreen(
            state = StudentAuthUiState(),
            onBack = {},
            onRegister = {},
            onLogin = {},
            onGoogleSignIn = {},
            onExplore = {},
        )
    }
}
