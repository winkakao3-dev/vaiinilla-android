package com.vaiinilla.app.ui.parentmode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CreditCardOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.guardianship.ChildGuardian
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.GuardianshipRepository
import com.vaiinilla.app.domain.guardianship.PersonNames
import com.vaiinilla.app.domain.guardianship.RefundMethod
import com.vaiinilla.app.domain.guardianship.StudentRequest
import com.vaiinilla.app.domain.guardianship.StudentRequestKind
import com.vaiinilla.app.domain.guardianship.StudentRequestStatus
import com.vaiinilla.app.domain.guardianship.compactMoney
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.SlidingSegments
import com.vaiinilla.app.ui.components.minTouchTarget
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.OperationalField
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Poner, cambiar o quitar el PIN de 4 dígitos que Caja le pide al alumno. */
@Composable
internal fun ChildPinSheet(
    child: Dependent,
    guardianship: GuardianshipRepository,
    onSaved: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = rememberOperationalColors()
    val reduced = reducedMotion()
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<MorphState>(MorphState.Idle("Guardar PIN")) }
    var removing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var sendKey by remember { mutableStateOf(newKey()) }
    val focus = remember { FocusRequester() }
    val busy = removing || state.isBusy
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    fun save(newPin: String?) {
        error = null
        if (newPin == null) removing = true else state = MorphState.Working("Guardando…")
        scope.launch {
            withContext(Dispatchers.IO) { guardianship.setPin(child.userId, newPin, sendKey) }
                .onSuccess {
                    removing = false
                    state = MorphState.Done(if (newPin == null) "Sin PIN" else "PIN guardado")
                    onSaved(newPin != null)
                    delay(if (reduced) 300 else 900)
                    onDismiss()
                }.onFailure {
                    removing = false
                    state = MorphState.Idle("Guardar PIN")
                    sendKey = keyAfterFailure(it, sendKey)
                    error = it.toUserFacingMessage("No se guardó el PIN. Intenta otra vez.")
                }
        }
    }

    ParentSheet(
        title = if (child.hasPin) "Cambiar su PIN" else "PIN para Caja",
        subtitle = child.name,
        colors = colors,
        busy = busy,
        onDismiss = onDismiss,
        footer = {
            error?.let { ParentErrorCard(message = it, colors = colors, onDismiss = { error = null }) }
            if (child.hasPin) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .physicalPress(
                                enabled = !busy,
                            ) { save(null) }
                            .padding(vertical = 12.dp)
                            .minTouchTarget(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (removing) {
                        CircularProgressIndicator(
                            color = colors.textPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        "Quitar el PIN",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            MorphingButton(
                state = state,
                enabled = Dependent.isValidPin(pin) && !busy,
                colors = colors,
                testTag = "child-pin-save",
            ) {
                save(pin)
            }
        },
    ) {
        // Cuatro puntos que se llenan; el campo real está oculto debajo.
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
            BasicTextField(
                value = pin,
                onValueChange = { typed ->
                    pin = typed.filter(Char::isDigit).take(4)
                    error = null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier =
                    Modifier
                        .alpha(0.02f)
                        .focusRequester(focus)
                        .testTag("child-pin-field")
                        .semantics { contentDescription = "PIN de 4 dígitos" },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                repeat(4) { index ->
                    val filled = index < pin.length
                    Box(
                        modifier =
                            Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (filled) colors.textPrimary else Color.Transparent)
                                .border(2.dp, colors.textPrimary, CircleShape)
                                .physicalPress(
                                    scale = PhysicalPressScale.Small,
                                ) { runCatching { focus.requestFocus() } },
                    )
                }
            }
        }
        Text(
            "Cuatro números que ${child.firstName} le dice a Caja antes de que le cobren con su saldo. Si se equivocan 5 veces, se bloquea 15 minutos.",
            color = colors.textPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Text("Lo que le apartas desde aquí no lo pide.", color = colors.textSecondary, fontSize = 13.sp)
    }
}

/** Lo que pasó con la última solicitud de ese tipo, en una línea. */
internal fun requestStatusLine(request: StudentRequest): String =
    when (request.status) {
        StudentRequestStatus.PENDING ->
            if (request.kind == StudentRequestKind.REFUND && request.refundMethod != null) {
                "Esperando a la escuela · ${request.refundMethod.label.lowercase()}"
            } else {
                "Esperando a la escuela"
            }
        StudentRequestStatus.REJECTED -> "La escuela dijo: ${request.rejectionReason ?: "no procede"}"
        StudentRequestStatus.RESOLVED -> {
            val day =
                request.resolvedAt
                    ?.let {
                        runCatching {
                            OffsetDateTime
                                .parse(
                                    it,
                                ).atZoneSameInstant(BusinessTime.zone)
                        }.getOrNull()
                    }?.let {
                        " el " +
                            it.format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-MX"))).trimEnd('.')
                    }.orEmpty()
            if (request.kind == StudentRequestKind.REFUND && request.refunded != null) {
                "Te devolvieron ${compactMoney(request.refunded)}$day."
            } else {
                "Resuelta$day."
            }
        }
    }

/** Pedirle a la escuela la devolución de su saldo o que borre sus datos. */
@Composable
internal fun StudentRequestSheet(
    kind: StudentRequestKind,
    child: Dependent,
    balance: BigDecimal,
    guardianship: GuardianshipRepository,
    onSent: (StudentRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = rememberOperationalColors()
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    val scope = rememberCoroutineScope()
    val idleTitle =
        if (kind ==
            StudentRequestKind.REFUND
        ) {
            "Pedir ${compactMoney(balance)}"
        } else {
            "Pedir que borren sus datos"
        }
    var method by remember { mutableStateOf(RefundMethod.CASH) }
    var note by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<MorphState>(MorphState.Idle(idleTitle)) }
    var error by remember { mutableStateOf<String?>(null) }
    var sendKey by remember { mutableStateOf(newKey()) }
    var confirmsRemoval by remember { mutableStateOf(false) }
    val noteTooLong = note.length > StudentRequest.NOTE_LIMIT
    val canSubmit =
        !state.isBusy &&
            !noteTooLong &&
            if (kind == StudentRequestKind.REFUND) balance.signum() > 0 else balance.signum() == 0

    fun submit() {
        error = null
        state = MorphState.Working("Avisando a la escuela…")
        scope.launch {
            withContext(Dispatchers.IO) {
                guardianship.requestForStudent(
                    child.userId,
                    kind,
                    if (kind ==
                        StudentRequestKind.REFUND
                    ) {
                        method
                    } else {
                        null
                    },
                    note,
                    sendKey,
                )
            }.onSuccess {
                state = MorphState.Done("Le avisamos a la escuela")
                onSent(it)
                delay(if (reduced) 300 else 1000)
                onDismiss()
            }.onFailure {
                state = MorphState.Idle(idleTitle)
                sendKey = keyAfterFailure(it, sendKey)
                error = it.toUserFacingMessage("No pudimos avisarle a la escuela. Intenta otra vez.")
            }
        }
    }

    ParentSheet(
        title = if (kind == StudentRequestKind.REFUND) "Devolver su saldo" else "Borrar sus datos",
        subtitle = child.name,
        colors = colors,
        busy = state.isBusy,
        onDismiss = onDismiss,
        footer = {
            error?.let { ParentErrorCard(message = it, colors = colors, onDismiss = { error = null }) }
            MorphingButton(state = state, enabled = canSubmit, colors = colors, testTag = "student-request-submit") {
                if (kind == StudentRequestKind.DATA_REMOVAL) confirmsRemoval = true else submit()
            }
        },
    ) {
        if (kind == StudentRequestKind.REFUND) {
            Column {
                Text("Tiene", color = colors.textSecondary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    compactMoney(balance),
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 40.sp,
                    letterSpacing = (-1.2).sp,
                )
            }
            SlidingSegments(
                labels = RefundMethod.entries.map { it.label },
                selectedIndex = RefundMethod.entries.indexOf(method),
                onSelect = {
                    haptics.selection()
                    method = RefundMethod.entries[it]
                    sendKey = newKey()
                },
                trackColor = colors.cardInner,
                indicatorColor = colors.textPrimary,
                selectedContentColor = colors.background,
                idleContentColor = colors.textPrimary,
                borderColor = colors.cardBorder,
                enabled = !state.isBusy,
            )
            Text(
                if (method == RefundMethod.CASH) {
                    "La escuela te avisa cuándo pasar por él. Te entrega todo lo que le quede ese día."
                } else {
                    "La escuela te contacta para pedirte tu cuenta. Te transfiere todo lo que le quede ese día."
                },
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        } else {
            val identity =
                if (child.hasOwnAccount) {
                    "Su cuenta de Google " +
                        "sigue siendo suya."
                } else {
                    "También su nombre, grupo y matrícula."
                }
            Text(
                "La escuela quita a ${child.firstName} de su lista, apaga su credencial y borra sus controles y alergias. $identity",
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            Text(
                "Lo que compró y sus recargas se quedan porque son las cuentas de la escuela.",
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            if (balance.signum() > 0) {
                Text(
                    "Todavía tiene ${compactMoney(balance)}. Primero pide que te lo devuelvan.",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        }
        OperationalField(
            value = note,
            onValueChange = { note = it },
            label = "Nota para la escuela",
            colors = colors,
            placeholder =
                if (kind ==
                    StudentRequestKind.REFUND
                ) {
                    "Por ejemplo: se cambia de escuela (opcional)"
                } else {
                    "Opcional"
                },
            supporting = if (noteTooLong) "Hasta ${StudentRequest.NOTE_LIMIT} letras." else null,
            isError = noteTooLong,
            singleLine = false,
            minLines = 2,
            enabled = !state.isBusy,
        )
    }
    if (confirmsRemoval) {
        ParentConfirmDialog(
            title = "¿Pedir que borren los datos de ${child.firstName}?",
            message =
                "Cuando la escuela lo resuelva, ${child.firstName} " +
                    "desaparece de tu cuenta y no se puede deshacer.",
            confirmLabel = "Sí, pedirlo",
            destructive = true,
            onConfirm = ::submit,
            onDismiss = { confirmsRemoval = false },
        )
    }
}
