package com.vaiinilla.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.auth.student.StudentAuthUiState
import com.vaiinilla.app.ui.components.AuthBanner
import com.vaiinilla.app.ui.components.AuthBannerTone
import com.vaiinilla.app.ui.components.AuthBoxField
import com.vaiinilla.app.ui.components.AuthCheckCard
import com.vaiinilla.app.ui.components.AuthGhostButton
import com.vaiinilla.app.ui.components.AuthHeading
import com.vaiinilla.app.ui.components.AuthLimeButton
import com.vaiinilla.app.ui.components.AuthStage
import com.vaiinilla.app.ui.components.AuthStageColors
import com.vaiinilla.app.ui.components.AuthStageProgress
import com.vaiinilla.app.ui.components.AuthTextLink
import com.vaiinilla.app.ui.components.authArrive
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

private enum class RegisterStep { Email, Password, Name, Legal }

@Composable
fun StudentRegisterScreen(
    state: StudentAuthUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmChange: (String) -> Unit = {},
    onTermsChange: (Boolean) -> Unit,
    onPrivacyChange: (Boolean) -> Unit = {},
    onRegister: () -> Unit,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    var movingForward by remember { mutableStateOf(true) }
    val steps = RegisterStep.entries
    val step = steps[stepIndex]
    val keyboardController = LocalSoftwareKeyboardController.current

    val normalizedEmail = state.email.trim()
    val emailLooksValid = normalizedEmail.contains("@") && normalizedEmail.substringAfter("@").contains(".")
    val canAdvance =
        when (step) {
            RegisterStep.Email -> emailLooksValid
            RegisterStep.Password -> state.password.length >= MIN_PASSWORD
            RegisterStep.Name -> state.name.trim().isNotEmpty()
            RegisterStep.Legal -> state.termsAccepted && state.privacyAccepted && !state.loading
        }

    // Se pide la contraseña una sola vez con el botón para verla; copiarla a passwordConfirm mantiene la
    // validación del view model, que todavía compara las dos.
    val onPasswordStepChange: (String) -> Unit = { value ->
        onPasswordChange(value)
        onPasswordConfirmChange(value)
    }

    fun goToStep(target: Int) {
        movingForward = target > stepIndex
        stepIndex = target
    }

    fun advance() {
        if (!canAdvance) return
        if (stepIndex == steps.lastIndex) {
            keyboardController?.hide()
            onRegister()
        } else {
            goToStep(stepIndex + 1)
        }
    }

    fun goBack() {
        if (stepIndex == 0) onBack() else goToStep(stepIndex - 1)
    }

    BackHandler(onBack = ::goBack)

    if (state.emailExistsSuggestion) {
        EmailAlreadyRegisteredStage(
            email = normalizedEmail,
            onBack = ::goBack,
            onLogin = onLogin,
            onForgotPassword = onForgotPassword,
            onUseAnotherEmail = {
                onEmailChange("")
                goToStep(0)
            },
        )
        return
    }

    AuthStage(
        loading = state.loading,
        onBack = ::goBack,
        backDescription = if (stepIndex == 0) "Cerrar" else "Atrás",
        progress = AuthStageProgress(step = stepIndex, count = steps.size),
        contentTopPadding = 34.dp,
        actions = {
            if (step == RegisterStep.Legal && !canAdvance && !state.loading) {
                Text(
                    "Acepta los dos para crear tu cuenta",
                    color = AuthStageColors.Faint,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            AuthLimeButton(
                text =
                    when {
                        step != RegisterStep.Legal -> "Continuar"
                        state.loading -> "Creando…"
                        else -> "Crear cuenta"
                    },
                onClick = ::advance,
                enabled = canAdvance,
                showArrow = step != RegisterStep.Legal,
            )
        },
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val direction = if (movingForward) 1 else -1
                (slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 380f)) { it / 3 * direction } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(200)) { -it / 4 * direction } + fadeOut(tween(160)))
            },
            label = "registerStep",
        ) { targetStep ->
            Column(modifier = Modifier.fillMaxWidth()) {
                RegisterStepContent(
                    step = targetStep,
                    state = state,
                    emailLooksValid = emailLooksValid,
                    onEmailChange = onEmailChange,
                    onPasswordChange = onPasswordStepChange,
                    onNameChange = onNameChange,
                    onTermsChange = onTermsChange,
                    onPrivacyChange = onPrivacyChange,
                    onEditStep = { goToStep(it.ordinal) },
                    onLogin = onLogin,
                    onSubmitField = ::advance,
                )
            }
        }
    }
}

@Composable
private fun RegisterStepContent(
    step: RegisterStep,
    state: StudentAuthUiState,
    emailLooksValid: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onPrivacyChange: (Boolean) -> Unit,
    onEditStep: (RegisterStep) -> Unit,
    onLogin: () -> Unit,
    onSubmitField: () -> Unit,
) {
    // Cada página animada tiene su requester: durante el deslizamiento conviven la que sale y la que entra.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(step) {
        if (step != RegisterStep.Legal) runCatching { focusRequester.requestFocus() }
    }
    when (step) {
        RegisterStep.Email -> {
            AuthHeading(
                cap = "Crea tu cuenta",
                title = "¿Cuál es\ntu correo?",
                subtitle = "Te mandamos un enlace para verificarlo.",
            )
            Spacer(Modifier.height(24.dp))
            AuthBoxField(
                value = state.email,
                onValueChange = onEmailChange,
                label = "Correo",
                placeholder = "tu@correo.com",
                icon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                onImeAction = onSubmitField,
                isValid = emailLooksValid,
                focusRequester = focusRequester,
                modifier = Modifier.authArrive(1),
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿Ya tienes cuenta?", color = AuthStageColors.Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                AuthTextLink("Entra", onClick = onLogin, color = AuthStageColors.Ink, modifier = Modifier.padding(start = 6.dp))
            }
        }
        RegisterStep.Password -> {
            AuthHeading(
                cap = "Crea tu cuenta",
                title = "Crea una\ncontraseña.",
                subtitle = "Usa al menos $MIN_PASSWORD caracteres. Toca el ojo para ver lo que escribes.",
            )
            Spacer(Modifier.height(24.dp))
            AuthBoxField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = "Contraseña",
                placeholder = "Tu contraseña",
                icon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onImeAction = onSubmitField,
                password = true,
                focusRequester = focusRequester,
                modifier = Modifier.authArrive(1),
            )
            Spacer(Modifier.height(16.dp))
            PasswordStrength(password = state.password, modifier = Modifier.authArrive(2))
        }
        RegisterStep.Name -> {
            AuthHeading(
                cap = "Crea tu cuenta",
                title = "¿Cómo te\nllamas?",
                subtitle = "Así te llaman cuando tu pedido está listo.",
            )
            Spacer(Modifier.height(24.dp))
            AuthBoxField(
                value = state.name,
                onValueChange = onNameChange,
                label = "Nombre",
                placeholder = "Tu nombre",
                icon = Icons.Outlined.Person,
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
                onImeAction = onSubmitField,
                focusRequester = focusRequester,
                modifier = Modifier.authArrive(1),
            )
            Spacer(Modifier.height(24.dp))
            NameCallPreview(
                firstName = state.name.trim().substringBefore(" "),
                placeName = state.guestVenue?.establishment?.name,
                modifier = Modifier.authArrive(2),
            )
        }
        RegisterStep.Legal -> {
            AuthHeading(
                cap = "Crea tu cuenta",
                title = "Último paso.",
                subtitle = "Revisa tus datos y acepta los documentos.",
            )
            Spacer(Modifier.height(22.dp))
            RegisterSummary(
                email = state.email.trim(),
                name = state.name.trim(),
                onEditEmail = { onEditStep(RegisterStep.Email) },
                onEditName = { onEditStep(RegisterStep.Name) },
                modifier = Modifier.authArrive(1),
            )
            Spacer(Modifier.height(14.dp))
            AuthCheckCard(
                checked = state.termsAccepted,
                onCheckedChange = onTermsChange,
                label = "Acepto los términos",
                linkLabel = "Leer",
                url = state.termsUrl,
                modifier = Modifier.authArrive(2),
            )
            Spacer(Modifier.height(10.dp))
            AuthCheckCard(
                checked = state.privacyAccepted,
                onCheckedChange = onPrivacyChange,
                label = "Acepto el aviso de privacidad",
                linkLabel = "Leer",
                url = state.privacyUrl,
                modifier = Modifier.authArrive(3),
            )
            state.errorMessage?.let { error ->
                Spacer(Modifier.height(14.dp))
                AuthBanner(error, AuthBannerTone.Error)
            }
        }
    }
}

@Composable
private fun PasswordStrength(
    password: String,
    modifier: Modifier = Modifier,
) {
    val longEnough = password.length >= MIN_PASSWORD
    val mixesLettersAndDigits = password.any(Char::isLetter) && password.any(Char::isDigit)
    val score =
        listOf(
            password.isNotEmpty(),
            longEnough,
            mixesLettersAndDigits,
            password.length >= 10,
        ).count { it }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { index ->
                val color by androidx.compose.animation.animateColorAsState(
                    if (index < score) AuthStageColors.Accent else AuthStageColors.Paper3,
                    spring(stiffness = 400f),
                    label = "passwordMeter$index",
                )
                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(6.dp)).background(color))
            }
        }
        Spacer(Modifier.height(14.dp))
        PasswordRule(text = "Al menos $MIN_PASSWORD caracteres", met = longEnough)
        Spacer(Modifier.height(8.dp))
        PasswordRule(text = "Mejor si mezclas letras y números", met = mixesLettersAndDigits)
    }
}

@Composable
private fun PasswordRule(
    text: String,
    met: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (met) AuthStageColors.Accent else AuthStageColors.Paper3),
            contentAlignment = Alignment.Center,
        ) {
            if (met) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = AuthStageColors.AccentInk, modifier = Modifier.size(13.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            color = if (met) AuthStageColors.Ink2 else AuthStageColors.Muted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun NameCallPreview(
    firstName: String,
    placeName: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(AuthStageColors.Ink)
                .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            "ASÍ LO VERÁS",
            color = Color(0xFF7A786E),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.8.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (firstName.isBlank()) "¡Tu pedido está listo!" else "¡$firstName, tu pedido está listo!",
            color = AuthStageColors.Paper,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            placeName ?: "Vaiinilla",
            color = Color(0xFF5C5B54),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RegisterSummary(
    email: String,
    name: String,
    onEditEmail: () -> Unit,
    onEditName: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(AuthStageColors.Paper2),
    ) {
        SummaryRow(label = "Correo", value = email, onEdit = onEditEmail)
        Box(Modifier.fillMaxWidth().height(1.dp).background(AuthStageColors.Line))
        SummaryRow(label = "Nombre", value = name, onEdit = onEditName)
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = AuthStageColors.Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(72.dp))
        Text(
            value,
            color = AuthStageColors.Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        AuthTextLink("Cambiar", onClick = onEdit, modifier = Modifier.padding(horizontal = 8.dp))
    }
}

/** Sin el nombre ni las iniciales del dueño: no se revela de quién es un correo registrado. */
@Composable
private fun EmailAlreadyRegisteredStage(
    email: String,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onUseAnotherEmail: () -> Unit,
) {
    AuthStage(
        loading = false,
        onBack = onBack,
        progress = AuthStageProgress(step = 0, count = RegisterStep.entries.size),
        contentTopPadding = 34.dp,
        actions = {
            AuthLimeButton(text = "Entrar con este correo", onClick = onLogin)
            Spacer(Modifier.height(10.dp))
            AuthGhostButton(text = "Crear una contraseña nueva", onClick = onForgotPassword)
            AuthTextLink(
                "Usar otro correo",
                onClick = onUseAnotherEmail,
                color = AuthStageColors.Muted,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        AuthHeading(
            cap = "Crea tu cuenta",
            title = "Ya tienes\nuna cuenta.",
            subtitle = "Con este correo ya se creó una cuenta en Vaiinilla. Entra con ella.",
            modifier = Modifier.authArrive(0),
        )
        Spacer(Modifier.height(22.dp))
        AuthBoxField(
            value = email,
            onValueChange = {},
            label = "Correo",
            placeholder = "",
            icon = Icons.Outlined.Email,
            enabled = false,
            isError = true,
            modifier = Modifier.authArrive(1),
        )
        Spacer(Modifier.height(10.dp))
        AuthBanner("Este correo ya está registrado.", AuthBannerTone.Warning, modifier = Modifier.authArrive(2))
        Spacer(Modifier.height(22.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .authArrive(3)
                    .clip(RoundedCornerShape(22.dp))
                    .background(AuthStageColors.Ink)
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(AuthStageColors.Accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Key, contentDescription = null, tint = AuthStageColors.AccentInk, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("¿Es tu correo?", color = AuthStageColors.Paper, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Entra con tu contraseña. Si no la recuerdas, crea una nueva.",
                    color = Color(0xFF5C5B54),
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private const val MIN_PASSWORD = 6

@Preview(name = "Auth · registro", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StudentRegisterScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        StudentRegisterScreen(
            state = StudentAuthUiState(name = "Dani", email = "dani@correo.com"),
            onBack = {},
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onPasswordConfirmChange = {},
            onTermsChange = {},
            onPrivacyChange = {},
            onRegister = {},
            onLogin = {},
            onForgotPassword = {},
        )
    }
}

@Composable
internal fun AuthErrorBanner(message: String) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.coral.copy(alpha = 0.22f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(message, color = colors.ink, modifier = Modifier.padding(14.dp), fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
internal fun AuthNoticeBanner(message: String) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.accent.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(message, color = colors.ink, modifier = Modifier.padding(14.dp), fontSize = 14.sp, lineHeight = 20.sp)
    }
}
