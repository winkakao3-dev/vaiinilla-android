package com.vaiinilla.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.auth.student.StudentAuthUiState
import com.vaiinilla.app.ui.components.AuthAccessField
import com.vaiinilla.app.ui.components.AuthAccessFieldKind
import com.vaiinilla.app.ui.components.AuthInkSubmitButton
import com.vaiinilla.app.ui.components.AuthLegalCheckRow
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

private enum class RegisterStep(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
) {
    Email(
        icon = Icons.Outlined.Email,
        title = "¿Cuál es tu correo?",
        subtitle = "Te enviaremos un enlace para verificarlo.",
    ),
    Password(
        icon = Icons.Outlined.Lock,
        title = "Crea una contraseña",
        subtitle = "Usa al menos 6 caracteres.",
    ),
    Name(
        icon = Icons.Outlined.Person,
        title = "¿Cómo te llamas?",
        subtitle = "Así te llamará la cafetería al entregar tu pedido.",
    ),
    Legal(
        icon = Icons.Outlined.Verified,
        title = "Último paso",
        subtitle = "Acepta los documentos para crear tu cuenta.",
    ),
}

@Composable
fun StudentRegisterScreen(
    state: StudentAuthUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmChange: (String) -> Unit = {},
    onContextualIdChange: (String) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onPrivacyChange: (Boolean) -> Unit = {},
    onRegister: () -> Unit,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    var stepIndex by remember { mutableIntStateOf(0) }
    var movingForward by remember { mutableStateOf(true) }
    var revealsPassword by remember { mutableStateOf(false) }
    val steps = RegisterStep.entries
    val step = steps[stepIndex]
    val keyboardController = LocalSoftwareKeyboardController.current

    val normalizedEmail = state.email.trim()
    val canAdvance =
        when (step) {
            RegisterStep.Email -> normalizedEmail.contains("@") && normalizedEmail.contains(".")
            RegisterStep.Password -> state.password.length >= 6
            RegisterStep.Name -> state.name.trim().isNotEmpty()
            RegisterStep.Legal ->
                state.termsAccepted &&
                    state.privacyAccepted &&
                    (!state.clientIdRequired || state.contextualId.isNotBlank()) &&
                    !state.loading
        }

    // The step flow shows the password once with a reveal toggle instead of
    // asking for it twice; mirroring it into passwordConfirm keeps the
    // existing view model validation (which still compares the two) passing.
    val onPasswordStepChange: (String) -> Unit = { value ->
        onPasswordChange(value)
        onPasswordConfirmChange(value)
    }

    fun goToStep(
        target: Int,
        forward: Boolean,
    ) {
        movingForward = forward
        stepIndex = target
    }

    fun advance() {
        if (!canAdvance) return
        if (stepIndex == steps.lastIndex) {
            keyboardController?.hide()
            onRegister()
        } else {
            goToStep(stepIndex + 1, forward = true)
        }
    }

    fun goBack() {
        if (stepIndex == 0) {
            onBack()
        } else {
            goToStep(stepIndex - 1, forward = false)
        }
    }

    BackHandler(onBack = ::goBack)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .statusBarsPadding(),
    ) {
        RegisterTopBar(
            step = stepIndex,
            stepCount = steps.size,
            colors = colors,
            onBack = ::goBack,
            onLogin = onLogin,
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (movingForward) {
                        (slideInHorizontally(tween(280)) { it } + fadeIn(tween(280))) togetherWith
                            (slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(280)))
                    } else {
                        (slideInHorizontally(tween(280)) { -it } + fadeIn(tween(280))) togetherWith
                            (slideOutHorizontally(tween(280)) { it } + fadeOut(tween(280)))
                    }
                },
                label = "register_step",
            ) { targetStep ->
                RegisterStepContent(
                    step = targetStep,
                    state = state,
                    colors = colors,
                    revealsPassword = revealsPassword,
                    onRevealsPasswordChange = { revealsPassword = it },
                    onEmailChange = onEmailChange,
                    onPasswordChange = onPasswordStepChange,
                    onNameChange = onNameChange,
                    onContextualIdChange = onContextualIdChange,
                    onTermsChange = onTermsChange,
                    onPrivacyChange = onPrivacyChange,
                    onSubmitField = ::advance,
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 12.dp),
        ) {
            AuthInkSubmitButton(
                text =
                    when {
                        step != RegisterStep.Legal -> "Continuar"
                        state.loading -> "Creando…"
                        else -> "Crear cuenta"
                    },
                onClick = ::advance,
                enabled = canAdvance,
            )
        }
    }
}

@Composable
private fun RegisterTopBar(
    step: Int,
    stepCount: Int,
    colors: VaiinillaColors,
    onBack: () -> Unit,
    onLogin: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 20.dp),
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = if (step == 0) "Cerrar" else "Atrás",
                    tint = colors.ink,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (step == 0) {
                TextButton(onClick = onLogin) {
                    Text(
                        "Inicia sesión",
                        color = colors.muted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(stepCount) { index ->
                Surface(
                    color = if (index <= step) colors.accentInk else colors.line,
                    shape = CircleShape,
                    modifier =
                        Modifier
                            .width(if (index == step) 22.dp else 7.dp)
                            .height(7.dp),
                ) {}
            }
        }
    }
}

@Composable
private fun RegisterStepContent(
    step: RegisterStep,
    state: StudentAuthUiState,
    colors: VaiinillaColors,
    revealsPassword: Boolean,
    onRevealsPasswordChange: (Boolean) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onContextualIdChange: (String) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onPrivacyChange: (Boolean) -> Unit,
    onSubmitField: () -> Unit,
) {
    // Each animated page owns its requester: during the slide the outgoing and
    // incoming fields coexist, and a shared one could focus the wrong page.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(step) {
        if (step != RegisterStep.Legal) focusRequester.requestFocus()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        RegisterStepHeader(step = step, colors = colors)
        Spacer(modifier = Modifier.weight(1f, fill = false).height(24.dp))

        when (step) {
            RegisterStep.Email ->
                HeroTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    placeholder = "Correo",
                    colors = colors,
                    focusRequester = focusRequester,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    onSubmit = onSubmitField,
                )
            RegisterStep.Password ->
                HeroPasswordField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    colors = colors,
                    focusRequester = focusRequester,
                    revealsPassword = revealsPassword,
                    onRevealsPasswordChange = onRevealsPasswordChange,
                    onSubmit = onSubmitField,
                )
            RegisterStep.Name ->
                HeroTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    placeholder = "Tu nombre",
                    colors = colors,
                    focusRequester = focusRequester,
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                    onSubmit = onSubmitField,
                )
            RegisterStep.Legal ->
                LegalStepContent(
                    state = state,
                    colors = colors,
                    onContextualIdChange = onContextualIdChange,
                    onTermsChange = onTermsChange,
                    onPrivacyChange = onPrivacyChange,
                )
        }

        Spacer(modifier = Modifier.weight(1f, fill = false).height(24.dp))
    }
}

@Composable
private fun RegisterStepHeader(
    step: RegisterStep,
    colors: VaiinillaColors,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .background(
                        color = colors.accent.copy(alpha = if (colors.isDark) 0.18f else 0.22f),
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = step.icon,
                contentDescription = null,
                tint = colors.accentInk,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = step.title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = step.subtitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.muted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HeroTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    colors: VaiinillaColors,
    focusRequester: FocusRequester,
    keyboardOptions: KeyboardOptions,
    onSubmit: () -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        textStyle =
            TextStyle(
                color = colors.ink,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            ),
        singleLine = true,
        cursorBrush =
            androidx.compose.ui.graphics
                .SolidColor(colors.accentInk),
        keyboardOptions = keyboardOptions,
        keyboardActions =
            KeyboardActions(
                onNext = { onSubmit() },
                onDone = { onSubmit() },
            ),
        visualTransformation = visualTransformation,
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.muted.copy(alpha = 0.45f),
                        textAlign = TextAlign.Center,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun HeroPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    colors: VaiinillaColors,
    focusRequester: FocusRequester,
    revealsPassword: Boolean,
    onRevealsPasswordChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        HeroTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = "Contraseña",
            colors = colors,
            focusRequester = focusRequester,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            onSubmit = onSubmit,
            visualTransformation = if (revealsPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.padding(horizontal = 40.dp),
        )
        IconButton(onClick = { onRevealsPasswordChange(!revealsPassword) }) {
            Icon(
                imageVector = if (revealsPassword) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                contentDescription = if (revealsPassword) "Ocultar contraseña" else "Mostrar contraseña",
                tint = colors.muted,
            )
        }
    }
}

@Composable
private fun LegalStepContent(
    state: StudentAuthUiState,
    colors: VaiinillaColors,
    onContextualIdChange: (String) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onPrivacyChange: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (state.clientIdRequired) {
            AuthAccessField(
                value = state.contextualId,
                onValueChange = onContextualIdChange,
                label = state.clientIdLabel,
                placeholder = state.clientIdLabel,
                kind = AuthAccessFieldKind.Id,
                imeAction = ImeAction.Done,
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.paper2,
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                AuthLegalCheckRow(
                    checked = state.termsAccepted,
                    onCheckedChange = onTermsChange,
                    label = "Acepto los términos y condiciones",
                    linkLabel = "Leer términos",
                    url = state.termsUrl,
                )
                AuthLegalCheckRow(
                    checked = state.privacyAccepted,
                    onCheckedChange = onPrivacyChange,
                    label = "Acepto el aviso de privacidad",
                    linkLabel = "Leer privacidad",
                    url = state.privacyUrl,
                )
            }
        }

        state.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(14.dp))
            AuthErrorBanner(error)
        }

        if (state.emailExistsSuggestion) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.yolk.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Este correo ya está registrado.", color = colors.ink, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(name = "Auth · registro", showBackground = true, widthDp = 411, heightDp = 891)
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
            onContextualIdChange = {},
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
