package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.auth.student.StudentAuthUiState
import com.vaiinilla.app.ui.components.AuthBanner
import com.vaiinilla.app.ui.components.AuthBannerTone
import com.vaiinilla.app.ui.components.AuthBoxField
import com.vaiinilla.app.ui.components.AuthChip
import com.vaiinilla.app.ui.components.AuthGhostButton
import com.vaiinilla.app.ui.components.AuthHeading
import com.vaiinilla.app.ui.components.AuthIconTile
import com.vaiinilla.app.ui.components.AuthLimeButton
import com.vaiinilla.app.ui.components.AuthNumberedSteps
import com.vaiinilla.app.ui.components.AuthStage
import com.vaiinilla.app.ui.components.AuthStageColors
import com.vaiinilla.app.ui.components.AuthTextLink
import com.vaiinilla.app.ui.components.authArrive
import com.vaiinilla.app.ui.theme.Lime
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import com.vaiinilla.app.ui.theme.Yolk
import kotlinx.coroutines.delay

@Composable
fun StudentForgotPasswordScreen(
    state: StudentAuthUiState,
    onBack: () -> Unit,
    onEmailChange: (String) -> Unit,
    onSendReset: () -> Unit,
) {
    AnimatedContent(
        targetState = state.passwordResetSent,
        transitionSpec = {
            (fadeIn() + scaleIn(spring(dampingRatio = 0.8f, stiffness = 300f), initialScale = 0.96f)) togetherWith
                fadeOut()
        },
        label = "forgotPasswordStage",
    ) { sent ->
        if (sent) {
            ResetLinkSentStage(state = state, onBack = onBack, onSendAgain = onSendReset)
        } else {
            ResetRequestStage(state = state, onBack = onBack, onEmailChange = onEmailChange, onSendReset = onSendReset)
        }
    }
}

@Composable
private fun ResetRequestStage(
    state: StudentAuthUiState,
    onBack: () -> Unit,
    onEmailChange: (String) -> Unit,
    onSendReset: () -> Unit,
) {
    val secondsLeft = rememberSecondsUntil(state.resetLockedUntilMs)
    val email = state.email.trim()
    val canSend = !state.loading && secondsLeft == 0L && email.contains("@")
    AuthStage(
        loading = state.loading,
        onBack = onBack,
        contentTopPadding = 28.dp,
        actions = {
            AuthLimeButton(
                text = if (secondsLeft > 0) "Mandar otro en ${formatCountdown(secondsLeft)}" else "Mandar enlace",
                onClick = onSendReset,
                enabled = canSend,
            )
            Spacer(Modifier.height(4.dp))
            AuthTextLink(
                "Ya me acordé · Volver a entrar",
                onClick = onBack,
                color = AuthStageColors.Muted,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        AuthIconTile(icon = Icons.Outlined.Key, modifier = Modifier.authArrive(0))
        Spacer(Modifier.height(22.dp))
        AuthHeading(
            cap = "Recupera tu acceso",
            title = "Crea una\ncontraseña nueva.",
            subtitle = "Te mandamos un enlace a tu correo para que la elijas.",
            modifier = Modifier.authArrive(1),
        )
        Spacer(Modifier.height(22.dp))
        AuthBoxField(
            value = state.email,
            onValueChange = onEmailChange,
            label = "Correo",
            placeholder = "tu@correo.com",
            icon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
            onImeAction = { if (canSend) onSendReset() },
            isError = state.errorMessage != null,
            modifier = Modifier.authArrive(2),
        )
        state.errorMessage?.let { error ->
            Spacer(Modifier.height(12.dp))
            AuthBanner(error, AuthBannerTone.Error)
        }
        Spacer(Modifier.height(18.dp))
        AuthNumberedSteps(
            steps =
                listOf(
                    "Escribe el correo de tu cuenta",
                    "Abre el enlace que te llega",
                    "Elige tu contraseña nueva",
                ),
            modifier = Modifier.authArrive(3),
        )
    }
}

@Composable
private fun ResetLinkSentStage(
    state: StudentAuthUiState,
    onBack: () -> Unit,
    onSendAgain: () -> Unit,
) {
    val secondsLeft = rememberSecondsUntil(state.resetLockedUntilMs)
    AuthStage(
        loading = state.loading,
        contentTopPadding = 72.dp,
        actions = {
            AuthLimeButton(text = "Volver a entrar", onClick = onBack)
            Spacer(Modifier.height(10.dp))
            AuthGhostButton(
                text = if (secondsLeft > 0) "Mandar otra vez en ${formatCountdown(secondsLeft)}" else "Mandar otra vez",
                onClick = onSendAgain,
                enabled = !state.loading && secondsLeft == 0L,
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            SentEnvelopeArt()
            Spacer(Modifier.height(24.dp))
            AuthHeading(
                cap = "",
                title = "Revisa tu correo.",
                subtitle = "Si hay una cuenta con este correo, ya te llegó el enlace para crear tu contraseña nueva.",
                centered = true,
                modifier = Modifier.authArrive(1),
            )
            if (state.email.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                AuthChip(icon = null, text = state.email.trim(), modifier = Modifier.authArrive(2))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "¿No lo ves? Busca en Spam o Promociones.",
                color = AuthStageColors.Faint,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
            state.errorMessage?.let { error ->
                Spacer(Modifier.height(14.dp))
                AuthBanner(error, AuthBannerTone.Error)
            }
        }
    }
}

/** El sobre con la carta asomando: se dibuja aquí porque el lienzo lo pide sin marco ni foto. */
@Composable
private fun SentEnvelopeArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 196.dp, height = 156.dp)) {
        val w = size.width
        val h = size.height
        val top = h * 0.32f
        val radius = CornerRadius(w * 0.1f)
        drawRoundRect(
            Yolk.copy(alpha = 0.72f),
            topLeft = Offset(0f, top),
            size = Size(w, h - top),
            cornerRadius = radius,
        )
        val letterX = w * 0.135f
        val letterW = w * 0.73f
        drawRoundRect(
            AuthPaperCream,
            topLeft = Offset(letterX, 0f),
            size = Size(letterW, h * 0.76f),
            cornerRadius = CornerRadius(w * 0.055f),
        )
        val lineX = letterX + w * 0.075f
        drawRoundRect(Lime, Offset(lineX, h * 0.1f), Size(letterW * 0.5f, h * 0.05f), CornerRadius(h))
        drawRoundRect(AuthLetterLine, Offset(lineX, h * 0.2f), Size(letterW * 0.8f, h * 0.04f), CornerRadius(h))
        drawRoundRect(AuthLetterLine, Offset(lineX, h * 0.29f), Size(letterW * 0.62f, h * 0.04f), CornerRadius(h))
        val front =
            Path().apply {
                addRoundRect(RoundRect(Rect(0f, top, w, h), radius))
                val notch =
                    Path().apply {
                        moveTo(-1f, top - 1f)
                        lineTo(w + 1f, top - 1f)
                        lineTo(w + 1f, top + 1f)
                        lineTo(w / 2f, h * 0.62f)
                        lineTo(-1f, top + 1f)
                        close()
                    }
                op(this, notch, PathOperation.Difference)
            }
        drawPath(front, Yolk)
    }
}

private val AuthPaperCream = Color(0xFFF3EFE3)
private val AuthLetterLine = Color(0xFFD8D3C4)

/** Segundos que faltan para [untilMs], contados en vivo. */
@Composable
internal fun rememberSecondsUntil(untilMs: Long): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(untilMs) {
        now = System.currentTimeMillis()
        while (untilMs > now) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    return ((untilMs - now + 999) / 1_000).coerceAtLeast(0L)
}

internal fun formatCountdown(seconds: Long): String = "%d:%02d".format(seconds / 60, seconds % 60)

@Preview(name = "Auth · recuperar contraseña", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StudentForgotPasswordScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        StudentForgotPasswordScreen(
            state = StudentAuthUiState(email = "dani@utch.mx"),
            onBack = {},
            onEmailChange = {},
            onSendReset = {},
        )
    }
}
