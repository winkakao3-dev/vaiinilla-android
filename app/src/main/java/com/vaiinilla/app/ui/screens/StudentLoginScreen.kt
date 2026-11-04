package com.vaiinilla.app.ui.screens

import kotlinx.coroutines.delay
import com.vaiinilla.app.ui.components.AuthGhostButton
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.R
import com.vaiinilla.app.domain.auth.student.StudentAuthMfaChallenge
import com.vaiinilla.app.ui.auth.student.StudentAuthUiState
import com.vaiinilla.app.ui.components.AuthBackCircle
import com.vaiinilla.app.ui.components.AuthBanner
import com.vaiinilla.app.ui.components.AuthBannerTone
import com.vaiinilla.app.ui.components.AuthBoxField
import com.vaiinilla.app.ui.components.AuthChip
import com.vaiinilla.app.ui.components.AuthHeading
import com.vaiinilla.app.ui.components.AuthIconTile
import com.vaiinilla.app.ui.components.AuthLegalLinks
import com.vaiinilla.app.ui.components.AuthLimeButton
import com.vaiinilla.app.ui.components.AuthStage
import com.vaiinilla.app.ui.components.AuthStageColors
import com.vaiinilla.app.ui.components.AuthTextLink
import com.vaiinilla.app.ui.components.authArrive
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

@Composable
fun StudentLoginScreen(
    state: StudentAuthUiState,
    onBack: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onContextualIdChange: (String) -> Unit = {},
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
    onMfaCodeChange: (String) -> Unit = {},
    onMfaFactorSelected: (String) -> Unit = {},
    onSubmitMfa: () -> Unit = {},
    onCancelMfa: () -> Unit = {},
    showBack: Boolean = true,
    existingVerifiedSession: Boolean = false,
) {
    val mfaChallenge = state.mfaChallenge
    when {
        mfaChallenge != null ->
            MfaChallengeStage(
                state = state,
                challenge = mfaChallenge,
                onCodeChange = onMfaCodeChange,
                onFactorSelected = onMfaFactorSelected,
                onSubmit = onSubmitMfa,
                onCancel = onCancelMfa,
            )
        existingVerifiedSession ->
            LinkVenueStage(
                state = state,
                showBack = showBack,
                onBack = onBack,
                onContextualIdChange = onContextualIdChange,
                onContinue = onLogin,
            )
        else ->
            EmailLoginStage(
                state = state,
                showBack = showBack,
                onBack = onBack,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onLogin = onLogin,
                onForgotPassword = onForgotPassword,
                onRegister = onRegister,
            )
    }
}

@Composable
private fun EmailLoginStage(
    state: StudentAuthUiState,
    showBack: Boolean,
    onBack: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
) {
    val error = state.errorMessage
    var shakes by remember { mutableIntStateOf(0) }
    LaunchedEffect(error) { if (error != null) shakes++ }
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val wrongCredentials = error == "Correo o contraseña incorrectos."
    val canLogin = state.email.isNotBlank() && state.password.isNotBlank() && !state.loading

    AuthStage(loading = state.loading, contentTopPadding = 0.dp) {
        Box(modifier = Modifier.fillMaxWidth()) {
            androidx.compose.animation.AnimatedVisibility(
                visible = !keyboardOpen && error == null,
                enter = fadeIn() + expandVertically(spring(dampingRatio = 0.85f, stiffness = 300f)),
                exit = fadeOut() + shrinkVertically(spring(dampingRatio = 0.9f, stiffness = 400f)),
            ) {
                LoginPhotoStack(modifier = Modifier.authArrive(0))
            }
            if (showBack) {
                Box(Modifier.padding(top = 8.dp)) {
                    AuthBackCircle(onClick = onBack, contentDescription = "Volver")
                }
            }
        }
        Spacer(Modifier.height(if (keyboardOpen || error != null) 64.dp else 4.dp))
        AuthHeading(
            cap = "Tu cuenta Vaiinilla",
            title = "Qué bueno\nverte de nuevo.",
            subtitle = "Tu menú, tus pedidos y tu saldo, donde los dejaste.",
            modifier = Modifier.authArrive(1),
        )
        state.noticeMessage?.let { notice ->
            Spacer(Modifier.height(16.dp))
            AuthBanner(notice, AuthBannerTone.Notice)
        }
        if (error != null) {
            Spacer(Modifier.height(16.dp))
            if (wrongCredentials) {
                AuthBanner(
                    message = "Revisa que el correo esté bien escrito. Si no recuerdas tu contraseña, puedes crear una nueva.",
                    tone = AuthBannerTone.Error,
                    title = "El correo o la contraseña no coinciden",
                )
            } else {
                AuthBanner(error, AuthBannerTone.Error)
            }
        }
        Spacer(Modifier.height(if (error != null) 12.dp else 22.dp))
        AuthBoxField(
            value = state.email,
            onValueChange = onEmailChange,
            label = "Correo",
            placeholder = "tu@correo.com",
            icon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            modifier = Modifier.authArrive(2),
        )
        Spacer(Modifier.height(10.dp))
        AuthBoxField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = "Contraseña",
            placeholder = "Tu contraseña",
            icon = Icons.Outlined.Lock,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = { if (canLogin) onLogin() },
            password = true,
            isError = error != null,
            shakeKey = shakes,
            modifier = Modifier.authArrive(3),
        )
        if (error == null) {
            Row(
                modifier = Modifier.fillMaxWidth().authArrive(4),
                horizontalArrangement = Arrangement.End,
            ) {
                AuthTextLink(text = "¿Olvidaste tu contraseña?", onClick = onForgotPassword)
            }
        }
        Spacer(Modifier.height(12.dp))
        AuthLimeButton(
            text = if (error != null) "Intentar de nuevo" else "Entrar",
            onClick = onLogin,
            enabled = canLogin,
            showArrow = error == null,
            modifier = Modifier.authArrive(5),
        )
        Spacer(Modifier.height(10.dp))
        if (error != null) {
            AuthGhostButton(text = "Crear una contraseña nueva", onClick = onForgotPassword, modifier = Modifier.authArrive(6))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().authArrive(6),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("¿Primera vez aquí?", color = AuthStageColors.Muted, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                AuthTextLink("Crea tu cuenta", onClick = onRegister, color = AuthStageColors.Ink, modifier = Modifier.padding(start = 6.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
        AuthLegalLinks(privacyUrl = state.privacyUrl, termsUrl = state.termsUrl)
    }
}

@Composable
private fun LoginPhotoStack(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().height(250.dp).padding(top = 64.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        StackPhoto(R.drawable.waffle, rotation = -9f, modifier = Modifier.offset(x = (-54).dp, y = 16.dp))
        StackPhoto(R.drawable.torta, rotation = 8f, modifier = Modifier.offset(x = 54.dp, y = 12.dp))
    }
}

@Composable
private fun StackPhoto(
    res: Int,
    rotation: Float,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    Image(
        painter = painterResource(res),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .size(width = 126.dp, height = 162.dp)
                .graphicsLayer { rotationZ = rotation }
                .shadow(16.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(shape)
                .border(3.dp, AuthStageColors.Paper2, shape),
    )
}

/** La sesión ya existe y está verificada; falta lo que pide este negocio para darle acceso (su matrícula). */
@Composable
private fun LinkVenueStage(
    state: StudentAuthUiState,
    showBack: Boolean,
    onBack: () -> Unit,
    onContextualIdChange: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val label = state.clientIdLabel
    val needsId = state.clientIdRequired
    val error = state.errorMessage?.takeUnless { needsId && state.contextualId.isBlank() && it.startsWith("Ingresa tu") }
    val canContinue = !state.loading && (!needsId || state.contextualId.isNotBlank())
    AuthStage(
        loading = state.loading,
        onBack = if (showBack) onBack else null,
        contentTopPadding = 40.dp,
        actions = {
            AuthLimeButton(text = "Continuar", onClick = onContinue, enabled = canContinue, showArrow = true)
        },
    ) {
        AuthIconTile(icon = Icons.Outlined.Badge, modifier = Modifier.authArrive(0))
        Spacer(Modifier.height(22.dp))
        AuthHeading(
            cap = state.guestVenue?.establishment?.name ?: "Tu lugar",
            title = if (needsId) "Solo falta tu\n${label.lowercase()}." else "Completa tu acceso.",
            subtitle =
                if (needsId) {
                    "Este lugar la pide para darte acceso a su menú."
                } else {
                    "Tu cuenta ya está verificada. Solo falta vincularla a este lugar."
                },
            modifier = Modifier.authArrive(1),
        )
        (state.session?.email ?: state.email).takeIf { it.isNotBlank() }?.let { email ->
            Spacer(Modifier.height(16.dp))
            AuthChip(icon = Icons.Outlined.Verified, text = email, modifier = Modifier.authArrive(2))
        }
        if (needsId) {
            Spacer(Modifier.height(22.dp))
            AuthBoxField(
                value = state.contextualId,
                onValueChange = onContextualIdChange,
                label = label,
                placeholder = "Tu ${label.lowercase()}",
                icon = Icons.Outlined.Badge,
                imeAction = ImeAction.Done,
                onImeAction = { if (canContinue) onContinue() },
                isError = error != null,
                modifier = Modifier.authArrive(3),
            )
        }
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            AuthBanner(error, AuthBannerTone.Error)
        }
    }
}

@Composable
private fun MfaChallengeStage(
    state: StudentAuthUiState,
    challenge: StudentAuthMfaChallenge,
    onCodeChange: (String) -> Unit,
    onFactorSelected: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    val canSubmit = !state.loading && state.mfaCode.length == 6 && state.mfaFactorUid != null
    AuthStage(
        loading = state.loading,
        onBack = onCancel,
        contentTopPadding = 28.dp,
        actions = {
            AuthLimeButton(text = "Confirmar", onClick = onSubmit, enabled = canSubmit)
            Spacer(Modifier.height(4.dp))
            AuthTextLink(
                "Cancelar y volver",
                onClick = onCancel,
                color = AuthStageColors.Muted,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        AuthIconTile(icon = Icons.Outlined.VerifiedUser, size = 58.dp, iconSize = 27.dp, modifier = Modifier.authArrive(0))
        Spacer(Modifier.height(18.dp))
        AuthHeading(
            cap = "Verificación en dos pasos",
            title = "Confirma que\neres tú.",
            subtitle = "Abre tu app autenticadora y escribe el código de 6 dígitos de Vaiinilla.",
            modifier = Modifier.authArrive(1),
        )
        Spacer(Modifier.height(24.dp))
        OneTimeCodeBoxes(
            code = state.mfaCode,
            onCodeChange = onCodeChange,
            onDone = { if (canSubmit) onSubmit() },
            isError = state.errorMessage != null,
            modifier = Modifier.authArrive(2),
        )
        Spacer(Modifier.height(14.dp))
        CodeExpiryLine(modifier = Modifier.authArrive(3))
        state.errorMessage?.let { error ->
            Spacer(Modifier.height(12.dp))
            AuthBanner(error, AuthBannerTone.Error)
        }
        Spacer(Modifier.height(16.dp))
        MfaFactorPicker(state = state, challenge = challenge, onFactorSelected = onFactorSelected)
    }
}

@Composable
private fun CodeExpiryLine(modifier: Modifier = Modifier) {
    val fraction by produceState(initialValue = totpFractionLeft()) {
        while (true) {
            delay(1_000)
            value = totpFractionLeft()
        }
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(16.dp)) {
            val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(AuthStageColors.Line, style = Stroke(width = 2.2.dp.toPx()))
            drawArc(AuthStageColors.Accent, -90f, 360f * fraction, useCenter = false, style = stroke)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "El código cambia cada 30 segundos",
            color = AuthStageColors.Muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun totpFractionLeft(): Float = 1f - (System.currentTimeMillis() % 30_000L) / 30_000f

/** La app autenticadora en uso; con más de una, «Cambiar» abre la lista para elegir. */
@Composable
private fun MfaFactorPicker(
    state: StudentAuthUiState,
    challenge: StudentAuthMfaChallenge,
    onFactorSelected: (String) -> Unit,
) {
    var choosing by remember { mutableStateOf(false) }
    val current = challenge.factors.firstOrNull { it.uid == state.mfaFactorUid } ?: challenge.factors.firstOrNull()
    if (!choosing && current != null) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(AuthStageColors.Paper2)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(AuthStageColors.Paper3),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.PhoneAndroid, contentDescription = null, tint = AuthStageColors.Ink2, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    current.displayName?.takeIf { it.isNotBlank() } ?: "Aplicación autenticadora",
                    color = AuthStageColors.Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text("La que activaste en tu cuenta", color = AuthStageColors.Muted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
            if (challenge.factors.size > 1) {
                AuthTextLink("Cambiar", onClick = { choosing = true }, enabled = !state.loading)
            }
        }
        return
    }
    Text("Elige tu aplicación", color = AuthStageColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
    challenge.factors.forEach { factor ->
        val selected = factor.uid == state.mfaFactorUid
        Spacer(Modifier.height(8.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(AuthStageColors.Paper2)
                    .border(
                        1.5.dp,
                        if (selected) AuthStageColors.Accent else AuthStageColors.Line,
                        RoundedCornerShape(18.dp),
                    ).physicalPress(enabled = !state.loading) {
                        onFactorSelected(factor.uid)
                        choosing = false
                    }.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                factor.displayName?.takeIf { it.isNotBlank() } ?: "Aplicación autenticadora",
                color = AuthStageColors.Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Text("En uso", color = AuthStageColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
@Composable
private fun OneTimeCodeBoxes(
    code: String,
    onCodeChange: (String) -> Unit,
    onDone: () -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    BasicTextField(
        value = code,
        onValueChange = { value -> onCodeChange(value.filter(Char::isDigit).take(6)) },
        modifier =
            modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .semantics { contentDescription = "Código de verificación" },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { _ ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(6) { index ->
                    if (index == 3) Spacer(Modifier.width(8.dp))
                    val digit = code.getOrNull(index)
                    val current = index == code.length
                    val shape = RoundedCornerShape(16.dp)
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(58.dp)
                                .clip(shape)
                                .background(AuthStageColors.Paper2)
                                .border(
                                    1.5.dp,
                                    when {
                                        isError -> AuthStageColors.Danger
                                        current -> AuthStageColors.Ink
                                        else -> AuthStageColors.Line
                                    },
                                    shape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (digit != null) {
                            Text(
                                digit.toString(),
                                color = AuthStageColors.Ink,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                            )
                        } else if (current) {
                            Box(Modifier.width(2.dp).height(22.dp).background(AuthStageColors.Accent))
                        }
                    }
                }
            }
        },
    )
}

@Preview(name = "Auth · login", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StudentLoginScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        StudentLoginScreen(
            state = StudentAuthUiState(email = "dani@correo.com"),
            onBack = {},
            onEmailChange = {},
            onPasswordChange = {},
            onLogin = {},
            onForgotPassword = {},
            onRegister = {},
        )
    }
}