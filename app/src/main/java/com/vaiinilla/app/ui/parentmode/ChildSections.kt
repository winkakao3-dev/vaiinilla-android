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
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.SlidingSegments
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Quién lo cuida

/**
 * "Quién lo cuida" en la ficha del hijo. El principal invita a otro tutor, lo quita o le pasa el cargo;
 * los demás solo ven quién pone los controles.
 */
@Composable
internal fun ChildGuardiansSection(
    child: Dependent,
    busyGuardianId: String?,
    onInvite: () -> Unit,
    onRemove: (ChildGuardian) -> Unit,
    onMakePrincipal: (ChildGuardian) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    var removing by remember { mutableStateOf<ChildGuardian?>(null) }
    var promoting by remember { mutableStateOf<ChildGuardian?>(null) }
    Column(modifier = Modifier.padding(top = 30.dp).testTag("child-guardians")) {
        Text(
            "Quién lo cuida",
            color = colors.ink,
            fontWeight = FontWeight.Black,
            fontSize = 19.sp,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        child.guardians.forEach { guardian ->
            GuardianRow(
                child = child,
                guardian = guardian,
                busy = busyGuardianId == guardian.id,
                onRemove = { removing = guardian },
                onPromote = { promoting = guardian },
            )
        }
        if (child.isPrincipal) {
            QuietRow(
                icon = Icons.Outlined.PersonAddAlt,
                title = "Invitar a otro tutor",
                detail = "Ve, recarga y aparta; los controles los sigues poniendo tú.",
                ink = colors.ink,
                muted = colors.ink,
                modifier = Modifier.padding(top = 4.dp).testTag("child-invite-guardian"),
                onClick = onInvite,
            )
        } else {
            child.principalFirstName?.let { principal ->
                Text(
                    "$principal pone el límite, los bloqueos y las alergias. Tú ves, recargas y apartas.",
                    color = colors.muted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
    removing?.let { guardian ->
        ParentConfirmDialog(
            title = "¿Quitar a ${guardian.firstName}?",
            message = "Deja de ver a ${child.firstName}, de recargarle y de apartarle comida.",
            confirmLabel = "Quitar",
            destructive = true,
            onConfirm = { onRemove(guardian) },
            onDismiss = { removing = null },
        )
    }
    promoting?.let { guardian ->
        ParentConfirmDialog(
            title = "¿${guardian.firstName} pone los controles?",
            message =
                "Decidirá el límite, los bloqueos y las alergias, e invitará " +
                    "o quitará tutores. Tú sigues viendo y recargando.",
            confirmLabel = "Sí, que los ponga ${guardian.firstName}",
            destructive = false,
            onConfirm = { onMakePrincipal(guardian) },
            onDismiss = { promoting = null },
        )
    }
}

@Composable
private fun GuardianRow(
    child: Dependent,
    guardian: ChildGuardian,
    busy: Boolean,
    onRemove: () -> Unit,
    onPromote: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    var menuOpen by remember { mutableStateOf(false) }
    Column {
        Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(PersonNames.initials(guardian.name), colors.paper2, colors.ink, colors.line)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 13.dp)) {
                Text(
                    if (guardian.isYou) "${guardian.name} (tú)" else guardian.name,
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                )
                if (guardian.isPrincipal) Text("Pone los controles", color = colors.muted, fontSize = 12.sp)
            }
            when {
                busy ->
                    CircularProgressIndicator(
                        color = colors.ink,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                child.isPrincipal && !guardian.isYou ->
                    Box {
                        Box(
                            modifier =
                                Modifier
                                    .physicalPress(scale = PhysicalPressScale.Small) { menuOpen = true }
                                    .size(40.dp)
                                    .semantics { contentDescription = "Opciones de ${guardian.firstName}" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.MoreHoriz, contentDescription = null, tint = colors.ink)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Que ponga los controles") },
                                leadingIcon = { Icon(Icons.Outlined.Star, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    onPromote()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Quitar") },
                                leadingIcon = { Icon(Icons.Outlined.PersonRemove, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    onRemove()
                                },
                            )
                        }
                    }
                guardian.isPrincipal ->
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = "Principal",
                        tint = colors.ink,
                        modifier = Modifier.size(18.dp),
                    )
            }
        }
        HorizontalDivider(color = colors.line, thickness = 1.dp, modifier = Modifier.padding(start = 57.dp))
    }
}

// En Caja: PIN y credencial

/** "En Caja", solo para el tutor principal: el PIN que Caja pide antes de cobrarle y, si perdió su credencial, apagarla. */
@Composable
internal fun ChildCounterSecuritySection(
    child: Dependent,
    isBlocking: Boolean,
    onPin: () -> Unit,
    onBlockCredential: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    var confirmsBlock by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text("En Caja", color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        QuietRow(
            icon = if (child.hasPin) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
            title = if (child.hasPin) "Cambiar su PIN" else "Ponerle PIN",
            detail =
                if (child.hasPin) {
                    "Caja se lo pide antes de cobrarle con su saldo."
                } else {
                    "Sin PIN le cobran con su " +
                        "nombre o su credencial."
                },
            ink = colors.ink,
            muted = colors.muted,
            modifier = Modifier.testTag("child-pin"),
            onClick = onPin,
        )
        if (child.hasCredential) {
            QuietRow(
                icon = Icons.Outlined.CreditCardOff,
                title = "Bloquear su credencial",
                detail = "Si la perdió. En Caja le dan otra.",
                ink = colors.ink,
                muted = colors.muted,
                busy = isBlocking,
                modifier = Modifier.testTag("child-block-credential"),
            ) { confirmsBlock = true }
        }
    }
    if (confirmsBlock) {
        ParentConfirmDialog(
            title = "¿Bloquear la credencial de ${child.firstName}?",
            message =
                "Su QR y su tarjeta dejan de servir al momento. Hasta " +
                    "que Caja le dé otra, lo encuentran por su nombre.",
            confirmLabel = "Bloquear",
            destructive = true,
            onConfirm = onBlockCredential,
            onDismiss = { confirmsBlock = false },
        )
    }
}

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
                    sendKey = newKey()
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
                            .padding(vertical = 12.dp),
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

// Si deja la escuela

/** "Si deja la escuela", solo para el principal: pedir su saldo de vuelta o que borren sus datos. */
@Composable
internal fun StudentRequestsSection(
    balance: BigDecimal,
    requests: List<StudentRequest>,
    modifier: Modifier = Modifier,
    onOpen: (StudentRequestKind) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = modifier.testTag("child-requests")) {
        Text("Si deja la escuela", color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        RequestRow(
            kind = StudentRequestKind.REFUND,
            requests = requests,
            title = "Pedir que le devuelvan su saldo",
            idle = "La escuela te entrega lo que le quede, en efectivo o por transferencia.",
            onOpen = onOpen,
        )
        RequestRow(
            kind = StudentRequestKind.DATA_REMOVAL,
            requests = requests,
            title = "Borrar sus datos",
            idle =
                if (balance.signum() > 0) {
                    "Primero pide que te devuelvan su saldo."
                } else {
                    "La escuela deja de tenerlo en su lista y borra lo que lo identifica."
                },
            onOpen = onOpen,
        )
    }
}

@Composable
private fun RequestRow(
    kind: StudentRequestKind,
    requests: List<StudentRequest>,
    title: String,
    idle: String,
    onOpen: (StudentRequestKind) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val latest = StudentRequest.latest(kind, requests)
    val pending = latest?.status == StudentRequestStatus.PENDING
    QuietRow(
        icon = if (kind == StudentRequestKind.REFUND) Icons.Outlined.Undo else Icons.Outlined.PersonRemove,
        title = title,
        detail = latest?.let(::requestStatusLine) ?: idle,
        ink = colors.ink,
        muted = colors.muted,
        enabled = !pending,
        trailing = if (pending) Icons.Outlined.Schedule else Icons.Outlined.ChevronRight,
        trailingDescription = if (pending) "Esperando a la escuela" else null,
        modifier = Modifier.testTag("child-request-${kind.wireValue}"),
    ) { onOpen(kind) }
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
                                ).atZoneSameInstant(ZoneId.systemDefault())
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
                sendKey = newKey()
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
