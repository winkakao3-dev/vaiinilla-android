package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
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
            (fadeIn() + scaleIn(spring(dampingRatio = 0.8f, stiffness = 300f), initialScale = 0.96f)) togetherWith fadeOut()
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
            AuthIconTile(icon = Icons.Outlined.MarkEmailUnread, size = 150.dp, iconSize = 72.dp)
            Spacer(Modifier.height(28.dp))
            AuthHeading(
                cap = "Enlace enviado",
                title = "Revisa tu correo.",
                subtitle = "Si hay una cuenta con este correo, te llega un enlace para crear tu contraseña.",
                centered = true,
                modifier = Modifier.authArrive(1),
            )
            if (state.email.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                AuthChip(icon = Icons.Outlined.Email, text = state.email.trim(), modifier = Modifier.authArrive(2))
            }
            state.errorMessage?.let { error ->
                Spacer(Modifier.height(14.dp))
                AuthBanner(error, AuthBannerTone.Error)
            }
        }
    }
}

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
