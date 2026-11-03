package com.vaiinilla.app.ui.parentmode

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.guardianship.GuardianshipException
import com.vaiinilla.app.domain.guardianship.GuardianshipRepository
import com.vaiinilla.app.domain.guardianship.SchoolJoinCodeRules
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
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
 * Paso 1 de agregar a un hijo: el tutor que entró a la escuela sin código lo escribe aquí, y es lo que le deja
 * agregar o vincular hijos. Quien entró con "Usar código" ya llega habilitado y se salta este paso.
 */
@Composable
internal fun SchoolCodeStep(
    guardianship: GuardianshipRepository,
    schoolName: String?,
    onBusyChange: (Boolean) -> Unit,
    onUnlocked: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var state by remember { mutableStateOf<MorphState>(MorphState.Idle("Validar código")) }
    var sendKey by remember { mutableStateOf(newKey()) }
    var shakes by remember { mutableIntStateOf(0) }
    val canSubmit = !state.isBusy && SchoolJoinCodeRules.isComplete(code)
    val busyChange by rememberUpdatedState(onBusyChange)
    LaunchedEffect(state.isBusy) { busyChange(state.isBusy) }
    DisposableEffect(Unit) { onDispose { busyChange(false) } }

    fun submit() {
        if (state.isBusy || !SchoolJoinCodeRules.isComplete(code)) return
        error = null
        state = MorphState.Working("Revisando…")
        scope.launch {
            withContext(Dispatchers.IO) { guardianship.unlockParent(code, sendKey) }
                .onSuccess {
                    haptics.success()
                    state = MorphState.Done("Código correcto")
                    delay(if (reduced) 0 else 600)
                    onUnlocked()
                }.onFailure {
                    state = MorphState.Idle("Validar código")
                    sendKey = keyAfterFailure(it, sendKey)
                    haptics.impact()
                    error = parentUnlockMessage(it)
                    shakes++
                }
        }
    }

    AddChildStepPage(
        bottom = {
            FlowPillButton(state = state, enabled = canSubmit, testTag = "parent-unlock-submit", onClick = ::submit)
        },
    ) {
        StepTitle("El código de la escuela", Modifier.arrive(0))
        Text(
            "El que ${schoolName ?: "la escuela"} les da a los tutores en la circular. Solo se pide una vez.",
            color = colors.muted,
            fontSize = 14.5.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.arrive(1).padding(start = 4.dp, end = 4.dp, top = 10.dp),
        )
        SchoolCodeTiles(
            code = code,
            hasError = error != null,
            shakes = shakes,
            enabled = !state.isBusy,
            onCodeChange = {
                code = SchoolJoinCodeRules.sanitized(it)
                error = null
            },
            onGo = ::submit,
            modifier = Modifier.arrive(2).padding(top = 30.dp),
        )
        AnimatedVisibility(visible = error != null) {
            Text(
                error.orEmpty(),
                color = colors.errorText,
                fontSize = 14.sp,
                lineHeight = 19.6.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(top = 16.dp)
                        .testTag("parent-unlock-error"),
            )
        }
        AttemptsNote(Modifier.arrive(3).padding(top = 26.dp))
        WhereTheCodeComes(schoolName, Modifier.arrive(4).padding(top = 20.dp))
    }
}

@Composable
private fun AttemptsNote(modifier: Modifier = Modifier) {
    val colors = LocalVaiinillaColors.current
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = colors.ink, fontWeight = FontWeight.Black)) {
                append("Cuidado con los intentos. ")
            }
            append("Si fallas varias veces, tendrás que esperar un rato para volver a probar.")
        },
        color = colors.ink2,
        fontSize = 13.5.sp,
        lineHeight = 19.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.paper2)
                .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

/** Una circular de muestra, para que el tutor sepa qué buscar en el papel que le dio la escuela. */
@Composable
private fun WhereTheCodeComes(
    schoolName: String?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    // La circular es papel impreso: se ve igual con el tema claro o el oscuro.
    val paper = Color(0xFFF5F1E5)
    val paperInk = Color(0xFF1D1E1C)
    val paperLine = Color(0xFFDEDBD0)
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "DÓNDE VIENE",
            color = colors.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.2.sp,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .rotate(-1.2f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(paper)
                    .padding(16.dp)
                    .clearAndSetSemantics {
                        contentDescription = "En la circular de la escuela viene como «Código para tutores en Vaiinilla»"
                    },
        ) {
            Text(
                schoolName?.let { "${it.uppercase(Locale.forLanguageTag("es-MX"))} · CIRCULAR" } ?: "CIRCULAR DE LA ESCUELA",
                color = Color(0xFF77786F),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                maxLines = 1,
            )
            Box(Modifier.padding(top = 8.dp).fillMaxWidth(0.92f).height(7.dp).clip(RoundedCornerShape(4.dp)).background(paperLine))
            Box(Modifier.padding(top = 6.dp).fillMaxWidth(0.78f).height(7.dp).clip(RoundedCornerShape(4.dp)).background(paperLine))
            Text(
                "Código para tutores en Vaiinilla:",
                color = paperInk,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                "ABC234",
                color = paperInk,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                letterSpacing = 3.sp,
                modifier =
                    Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFB9D86D))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
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
    onCodeChange: (String) -> Unit,
    onGo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
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
            modifier
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
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val gap = 8.dp
                // Con ocho fichas no caben de 44 en un teléfono angosto: se encogen y conservan la proporción.
                val width = min(44.dp, (maxWidth - gap * (tileCount - 1)) / tileCount)
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .offset {
                                // Un meneo corto de lado a lado, para un código que no pasó.
                                IntOffset((6.dp.toPx() * sin(shake.value * PI * 4)).roundToInt(), 0)
                            },
                    horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                ) {
                    repeat(tileCount) { index ->
                        val current = index == code.length
                        val border by animateColorAsState(
                            when {
                                hasError -> colors.coral.copy(alpha = 0.6f)
                                current && enabled -> colors.accent
                                else -> Color.Transparent
                            },
                            spring(stiffness = 400f),
                            label = "code-tile-$index",
                        )
                        Box(
                            modifier =
                                Modifier
                                    .size(width = width, height = width * 58f / 44f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.paper2)
                                    .border(1.5.dp, border, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                code.getOrNull(index)?.toString().orEmpty(),
                                color = colors.ink,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                            )
                        }
                    }
                }
            }
        },
    )
}