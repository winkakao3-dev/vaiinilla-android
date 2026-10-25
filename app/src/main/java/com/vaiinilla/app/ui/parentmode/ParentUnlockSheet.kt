package com.vaiinilla.app.ui.parentmode

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.guardianship.GuardianshipException
import com.vaiinilla.app.domain.guardianship.GuardianshipRepository
import com.vaiinilla.app.domain.guardianship.SchoolJoinCodeRules
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

internal fun Throwable.isParentCodeRequired(): Boolean =
    (this as? GuardianshipException)?.code == GuardianshipException.PARENT_CODE_REQUIRED

/** Lo que se le dice al tutor cuando el código no pasó. */
internal fun parentUnlockMessage(error: Throwable): String {
    val guardian = error as? GuardianshipException
    if (guardian?.code == GuardianshipException.RATE_LIMITED || guardian?.httpStatus == 429) {
        // Retry-After viene en segundos; se redondea hacia arriba para no decir "0 minutos".
        val minutes = (guardian.cause as? ApiClientException)?.retryAfterSeconds?.let { (it + 59) / 60 }
        val wait =
            when {
                minutes == null || minutes <= 0 -> "unos minutos"
                minutes == 1L -> "un minuto"
                else -> "$minutes minutos"
            }
        return "Fueron muchos intentos. Espera $wait y vuelve a probar."
    }
    if (guardian?.code == GuardianshipException.JOIN_CODE_INVALID) {
        return "Ese no es el código de esta escuela. Revisa la circular o pídeselo a la escuela."
    }
    return error.toUserFacingMessage("No pudimos revisar el código. Intenta otra vez.")
}

/**
 * El tutor que entró a la escuela sin código lo escribe aquí: es lo que le deja agregar o vincular hijos.
 * Quien entró con "Usar código" ya llega habilitado y nunca ve esta hoja.
 */
@Composable
internal fun ParentUnlockSheet(
    guardianship: GuardianshipRepository,
    onUnlocked: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = rememberOperationalColors()
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var state by remember { mutableStateOf<MorphState>(MorphState.Idle("Validar código")) }
    var sendKey by remember { mutableStateOf(newKey()) }
    var shakes by remember { mutableIntStateOf(0) }
    val canSubmit = !state.isBusy && SchoolJoinCodeRules.isComplete(code)

    fun submit() {
        if (state.isBusy || !SchoolJoinCodeRules.isComplete(code)) return
        error = null
        state = MorphState.Working("Revisando…")
        scope.launch {
            withContext(Dispatchers.IO) { guardianship.unlockParent(code, sendKey) }
                .onSuccess {
                    haptics.success()
                    state = MorphState.Done("Listo, ya puedes agregarlo")
                    onUnlocked()
                    delay(if (reduced) 300 else 900)
                    onDismiss()
                }.onFailure {
                    state = MorphState.Idle("Validar código")
                    sendKey = keyAfterFailure(it, sendKey)
                    haptics.impact()
                    error = parentUnlockMessage(it)
                    shakes++
                }
        }
    }

    ParentSheet(
        title = "El código de la escuela",
        subtitle = "El que la escuela les da a los tutores",
        colors = colors,
        busy = state.isBusy,
        onDismiss = onDismiss,
        footer = {
            error?.let {
                Box(Modifier.testTag("parent-unlock-error")) {
                    ParentErrorCard(message = it, colors = colors, onDismiss = { error = null })
                }
            }
            MorphingButton(
                state = state,
                enabled = canSubmit,
                colors = colors,
                testTag = "parent-unlock-submit",
                onClick = ::submit,
            )
        },
    ) {
        SchoolCodeTiles(
            code = code,
            hasError = error != null,
            shakes = shakes,
            enabled = !state.isBusy,
            colors = colors,
            onCodeChange = {
                code = SchoolJoinCodeRules.sanitized(it)
                error = null
            },
            onGo = ::submit,
        )
        Text(
            "Con él puedes agregar a tus hijos. Solo se pide una vez.",
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Seis fichas como mínimo (los códigos van de 5 a 8); la que sigue se marca para que se vea dónde se
 * escribe. El campo de texto es el que recibe el teclado; las fichas son su dibujo.
 */
@Composable
private fun SchoolCodeTiles(
    code: String,
    hasError: Boolean,
    shakes: Int,
    enabled: Boolean,
    colors: OperationalColors,
    onCodeChange: (String) -> Unit,
    onGo: () -> Unit,
) {
    val reduced = reducedMotion()
    val focus = remember { FocusRequester() }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LaunchedEffect(shakes) {
        if (shakes == 0 || reduced) return@LaunchedEffect
        shake.snapTo(0f)
        shake.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 260f))
    }
    val tileCount = maxOf(6, minOf(SchoolJoinCodeRules.MAX_LENGTH, code.length + 1))
    BasicTextField(
        value = code,
        onValueChange = onCodeChange,
        enabled = enabled,
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Go,
            ),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .testTag("parent-unlock-field")
                .semantics {
                    contentDescription =
                        if (code.isEmpty()) "Código de la escuela" else "Código ${code.toList().joinToString(" ")}"
                },
        decorationBox = { innerTextField ->
            // El campo real queda invisible bajo las fichas: recibe el teclado, el pegado y la selección.
            Box(Modifier.size(1.dp).alpha(0f)) { innerTextField() }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .offset {
                            // Un meneo corto de lado a lado, para un código que no pasó.
                            IntOffset((8.dp.toPx() * sin(shake.value * PI * 4)).roundToInt(), 0)
                        },
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                repeat(tileCount) { index ->
                    val filled = index < code.length
                    val current = index == code.length
                    val border: Color =
                        when {
                            hasError -> ErrorCoral
                            filled -> colors.textPrimary
                            current -> colors.textSecondary
                            else -> colors.cardBorder
                        }
                    Box(
                        modifier =
                            Modifier
                                .size(width = 38.dp, height = 54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (filled) colors.cardBackground else colors.cardInner)
                                .border(
                                    if (filled || current) 2.dp else 1.dp,
                                    border,
                                    RoundedCornerShape(14.dp),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            code.getOrNull(index)?.toString().orEmpty(),
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 24.sp,
                        )
                    }
                }
            }
        },
    )
}
