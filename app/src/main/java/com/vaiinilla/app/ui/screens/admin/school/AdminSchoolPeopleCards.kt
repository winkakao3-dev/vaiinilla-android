package com.vaiinilla.app.ui.screens.admin.school

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.school.GuardianRequest
import com.vaiinilla.app.domain.school.RefundMethod
import com.vaiinilla.app.domain.school.SchoolGuardianship
import com.vaiinilla.app.domain.school.SchoolSettings
import com.vaiinilla.app.domain.school.SchoolStudentRequest
import com.vaiinilla.app.domain.school.StudentRequestKind
import com.vaiinilla.app.ui.admin.school.AdminSchoolUiState
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField
import com.vaiinilla.app.ui.screens.admin.AdminCard
import com.vaiinilla.app.ui.screens.admin.AdminChoice
import com.vaiinilla.app.ui.screens.admin.AdminCoral
import com.vaiinilla.app.ui.screens.admin.AdminHint
import com.vaiinilla.app.ui.screens.admin.AdminSectionTitle

/** Los tutores que esperan a la escuela, con lo que hace falta para decidir. */
@Composable
internal fun GuardianRequestsCard(
    state: AdminSchoolUiState,
    settings: SchoolSettings,
    colors: OperationalColors,
    enabled: Boolean,
    actions: AdminSchoolActions,
) {
    var rejecting by remember { mutableStateOf<String?>(null) }
    AdminCard(colors, Modifier.animateContentSize(schoolSpring())) {
        AdminSectionTitle("Tutores", colors)
        AdminChoice(
            question = "¿Quién decide quién cuida a cada alumno?",
            options = listOf(true to "La escuela aprueba", false to "Entran solos"),
            selected = settings.guardiansNeedApproval,
            colors = colors,
            enabled = enabled,
        ) { needsApproval ->
            if (needsApproval != settings.guardiansNeedApproval) {
                actions.onSave(settings.copy(guardiansNeedApproval = needsApproval), false) {}
            }
        }
        val requests = state.requests
        when {
            requests == null && state.requestsError != null ->
                SectionRetry(state.requestsError, colors, actions.onRetryRequests)
            requests == null -> AdminHint("Cargando solicitudes…", colors)
            requests.isEmpty() ->
                AdminHint(
                    if (settings.guardiansNeedApproval) {
                        "Nadie espera. Cuando un tutor agregue a su hijo, aparece aquí para que lo apruebes."
                    } else {
                        "Los tutores que agregan a su hijo lo ven y le recargan de inmediato, sin pasar por aquí."
                    },
                    colors,
                )
            else ->
                requests.forEachIndexed { index, request ->
                    GuardianRequestRow(
                        request = request,
                        hasRoster = state.hasRoster,
                        rejecting = rejecting == request.id,
                        colors = colors,
                        enabled = enabled,
                        onApprove = { actions.onReview(request, true) },
                        onAskReject = { rejecting = request.id },
                        onCancelReject = { rejecting = null },
                        onReject = {
                            rejecting = null
                            actions.onReview(request, false)
                        },
                    )
                    if (index < requests.lastIndex) RowDivider(colors)
                }
        }
    }
}

@Composable
private fun GuardianRequestRow(
    request: GuardianRequest,
    hasRoster: Boolean,
    rejecting: Boolean,
    colors: OperationalColors,
    enabled: Boolean,
    onApprove: () -> Unit,
    onAskReject: () -> Unit,
    onCancelReject: () -> Unit,
    onReject: () -> Unit,
) {
    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PersonRow(request.studentName, colors) {
            Text(request.studentName, fontSize = 15.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
            if (request.studentLine.isNotEmpty()) {
                Text(
                    request.studentLine,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                )
            }
            Text(
                "Lo pide ${request.guardianName}${request.guardianEmail?.let { " · $it" }.orEmpty()}",
                fontSize = 13.sp,
                color = colors.textPrimary,
            )
            Text(
                request.facts(hasRoster),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                // Lo que no cuadra con el padrón sale en coral: es lo que la escuela debe revisar.
                color = if (hasRoster && !request.inRoster) AdminCoral else colors.textSecondary,
            )
        }
        if (rejecting) {
            InlineConfirm(
                title = "¿Rechazar a ${request.guardianName}?",
                message =
                    if (request.studentHasAccount) {
                        "No verá ni cuidará a ${request.studentName}."
                    } else {
                        "No verá ni cuidará a ${request.studentName}. " +
                            "Si nadie más lo cuida, su matrícula queda libre para su verdadero tutor."
                    },
                confirmLabel = "Rechazar",
                colors = colors,
                enabled = enabled,
                destructive = true,
                onCancel = onCancelReject,
                onConfirm = onReject,
            )
        } else {
            RowActions {
                SchoolButton("Aprobar", colors, enabled, onClick = onApprove)
                SchoolButton("Rechazar", colors, enabled, destructive = true, onClick = onAskReject)
            }
        }
    }
}

/**
 * Buscar a un alumno para ver quién lo cuida, quitar a un tutor o cambiar al que pone los controles (por ejemplo,
 * cuando los tutores se separan o uno ya no debe verlo).
 */
@Composable
internal fun GuardianshipsCard(
    state: AdminSchoolUiState,
    colors: OperationalColors,
    enabled: Boolean,
    actions: AdminSchoolActions,
) {
    var query by rememberSaveable { mutableStateOf("") }
    // "q<id>" = confirmar quitar; "p<id>" = confirmar que ponga los controles.
    var confirming by remember { mutableStateOf<String?>(null) }
    AdminCard(colors, Modifier.animateContentSize(schoolSpring())) {
        AdminSectionTitle("Quién cuida a cada alumno", colors)
        OperationalField(
            value = query,
            onValueChange = {
                query = it
                actions.onSearch(it)
            },
            label = "Alumno",
            colors = colors,
            placeholder = "Nombre o matrícula",
            enabled = enabled,
        )
        val found = state.guardianships
        when {
            state.guardianshipsError != null -> AdminHint(state.guardianshipsError, colors)
            found == null -> AdminHint("Escribe al menos dos letras del nombre o la matrícula.", colors)
            found.isEmpty() -> AdminHint("Nadie cuida a un alumno con ese nombre o matrícula.", colors)
            else -> {
                val students = SchoolGuardianship.byStudent(found)
                students.forEachIndexed { index, rows ->
                    StudentGuardians(
                        rows = rows,
                        confirming = confirming,
                        colors = colors,
                        enabled = enabled,
                        onAsk = { confirming = it },
                        onRemove = {
                            confirming = null
                            actions.onRemoveGuardianship(it)
                        },
                        onPromote = {
                            confirming = null
                            actions.onMakePrincipal(it)
                        },
                    )
                    if (index < students.lastIndex) RowDivider(colors)
                }
            }
        }
    }
}

@Composable
private fun StudentGuardians(
    rows: List<SchoolGuardianship>,
    confirming: String?,
    colors: OperationalColors,
    enabled: Boolean,
    onAsk: (String?) -> Unit,
    onRemove: (SchoolGuardianship) -> Unit,
    onPromote: (SchoolGuardianship) -> Unit,
) {
    val first = rows.firstOrNull() ?: return
    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PersonRow(first.studentName, colors) {
            Text(first.studentName, fontSize = 15.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
            if (first.studentLine.isNotEmpty()) {
                Text(
                    first.studentLine,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                )
            }
        }
        rows.forEach { guardianship ->
            Column(Modifier.padding(start = 56.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    guardianship.guardianName + guardianship.guardianEmail?.let { " · $it" }.orEmpty(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                if (guardianship.isPrincipal) Text("Pone los controles", fontSize = 12.sp, color = colors.textSecondary)
                when (confirming) {
                    "q${guardianship.id}" ->
                        InlineConfirm(
                            title = "¿Quitar a ${guardianship.guardianName}?",
                            message =
                                if (guardianship.isPrincipal) {
                                    "Deja de ver a ${guardianship.studentName}. Si tiene otro tutor, ese pasa a poner los controles."
                                } else {
                                    "Deja de ver a ${guardianship.studentName}, de recargarle y de apartarle comida."
                                },
                            confirmLabel = "Quitar",
                            colors = colors,
                            enabled = enabled,
                            destructive = true,
                            onCancel = { onAsk(null) },
                        ) { onRemove(guardianship) }
                    "p${guardianship.id}" ->
                        InlineConfirm(
                            title = "¿${guardianship.guardianName} pone los controles?",
                            message =
                                "Decidirá el límite, los bloqueos y las alergias de ${guardianship.studentName}. " +
                                    "Los demás tutores siguen viéndolo.",
                            confirmLabel = "Sí, que los ponga",
                            colors = colors,
                            enabled = enabled,
                            onCancel = { onAsk(null) },
                        ) { onPromote(guardianship) }
                    else ->
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (rows.size > 1 && !guardianship.isPrincipal) {
                                SchoolButton(
                                    "Que ponga los controles",
                                    colors,
                                    enabled,
                                ) { onAsk("p${guardianship.id}") }
                            }
                            SchoolButton("Quitar", colors, enabled, destructive = true) { onAsk("q${guardianship.id}") }
                        }
                }
            }
        }
    }
}

/**
 * Lo que piden los tutores cuando su hijo se va: que les devuelvan el saldo o que la escuela borre sus datos. En
 * efectivo el dinero sale de la caja abierta y queda en su corte; por transferencia solo se marca.
 */
@Composable
internal fun StudentRequestsCard(
    state: AdminSchoolUiState,
    colors: OperationalColors,
    enabled: Boolean,
    actions: AdminSchoolActions,
) {
    // "r<id>" = confirmar resolver; "x<id>" = rechazar con motivo.
    var confirming by remember { mutableStateOf<String?>(null) }
    AdminCard(colors, Modifier.animateContentSize(schoolSpring())) {
        AdminSectionTitle("Devoluciones y bajas", colors)
        val requests = state.studentRequests
        when {
            requests == null && state.studentRequestsError != null ->
                SectionRetry(state.studentRequestsError, colors, actions.onRetryStudentRequests)
            requests == null -> AdminHint("Cargando solicitudes…", colors)
            requests.isEmpty() ->
                AdminHint(
                    "Nadie ha pedido nada. Si un tutor pide que le devuelvan el saldo de su hijo o que borren sus " +
                        "datos, aparece aquí.",
                    colors,
                )
            else ->
                requests.forEachIndexed { index, request ->
                    StudentRequestRow(
                        request = request,
                        confirming = confirming,
                        colors = colors,
                        enabled = enabled,
                        onAsk = { confirming = it },
                        onResolve = {
                            confirming = null
                            actions.onResolve(request)
                        },
                        onReject = { reason -> actions.onReject(request, reason) { confirming = null } },
                    )
                    if (index < requests.lastIndex) RowDivider(colors)
                }
        }
    }
}

@Composable
private fun StudentRequestRow(
    request: SchoolStudentRequest,
    confirming: String?,
    colors: OperationalColors,
    enabled: Boolean,
    onAsk: (String?) -> Unit,
    onResolve: () -> Unit,
    onReject: (String) -> Unit,
) {
    val refund = request.kind == StudentRequestKind.REFUND
    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PersonRow(request.studentName, colors) {
            Text(request.studentName, fontSize = 15.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
            request.studentLine?.let {
                Text(it, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
            }
            Text(
                studentRequestWhat(request),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            Text(
                "Lo pide ${request.guardianName}${request.guardianEmail?.let { " · $it" }.orEmpty()}",
                fontSize = 13.sp,
                color = colors.textPrimary,
            )
            request.note?.let { Text("“$it”", fontSize = 13.sp, color = colors.textSecondary) }
            studentRequestWarning(request)?.let {
                Text(it, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AdminCoral)
            }
        }
        when (confirming) {
            "r${request.id}" ->
                InlineConfirm(
                    title = studentRequestConfirmTitle(request),
                    message = studentRequestConfirmMessage(request),
                    confirmLabel = if (refund) "Sí, ya se lo entregué" else "Dar de baja",
                    colors = colors,
                    enabled = enabled,
                    destructive = !refund,
                    onCancel = { onAsk(null) },
                    onConfirm = onResolve,
                )
            "x${request.id}" -> {
                var reason by rememberSaveable(request.id) { mutableStateOf("") }
                InlineConfirm(
                    title = "¿Por qué no procede?",
                    message = "${request.guardianName} lo verá en la ficha de ${request.studentName}.",
                    confirmLabel = "Rechazar",
                    colors = colors,
                    enabled = enabled,
                    destructive = true,
                    onCancel = { onAsk(null) },
                    confirmEnabled = reason.trim().length >= SchoolStudentRequest.MIN_REJECTION_REASON,
                    extra = {
                        OperationalField(
                            value = reason,
                            onValueChange = { reason = it.take(300) },
                            label = "Motivo",
                            colors = colors,
                            placeholder = "Se lo decimos a ${request.guardianName}",
                            enabled = enabled,
                            singleLine = false,
                        )
                    },
                ) { onReject(reason) }
            }
            else ->
                RowActions {
                    SchoolButton(
                        if (refund) "Ya se lo entregué" else "Dar de baja",
                        colors,
                        enabled && request.canResolve,
                    ) { onAsk("r${request.id}") }
                    SchoolButton("Rechazar", colors, enabled, destructive = true) { onAsk("x${request.id}") }
                }
        }
    }
}

/** "Devolver $150 en efectivo" o "Borrar sus datos". */
internal fun studentRequestWhat(request: SchoolStudentRequest): String {
    if (request.kind != StudentRequestKind.REFUND) return "Borrar sus datos"
    val method = request.refundMethod?.let { " ${it.label.lowercase()}" }.orEmpty()
    return "Devolver ${moneyLabel(request.amountToRefund)}$method"
}

internal fun studentRequestWarning(request: SchoolStudentRequest): String? =
    when (request.kind) {
        StudentRequestKind.REFUND ->
            when {
                !request.hasBalance -> "Ya no le queda saldo: recházala."
                request.balanceChanged -> "Al pedirla tenía ${moneyLabel(
                    request.balanceAtRequest,
                )}; se entrega lo de hoy."
                else -> null
            }
        StudentRequestKind.DATA_REMOVAL ->
            if (request.hasBalance) {
                "Todavía tiene ${moneyLabel(request.currentBalance)}: primero devuélveselo a su tutor."
            } else {
                null
            }
    }

internal fun studentRequestConfirmTitle(request: SchoolStudentRequest): String =
    if (request.kind == StudentRequestKind.REFUND) {
        "¿Ya le entregaste ${moneyLabel(request.amountToRefund)} a ${request.guardianName}?"
    } else {
        "¿Dar de baja a ${request.studentName}?"
    }

internal fun studentRequestConfirmMessage(request: SchoolStudentRequest): String =
    when {
        request.kind == StudentRequestKind.DATA_REMOVAL ->
            "Deja de estar en la escuela: se apaga su credencial y se borran sus controles, alergias y lo que lo " +
                "identifica. No se puede deshacer."
        request.refundMethod == RefundMethod.CASH ->
            "Sale de la caja abierta y queda en su corte. Su saldo queda en cero; lo que gaste después ya no entra " +
                "en esta solicitud."
        else ->
            "Su saldo queda en cero. Si sigue comprando, lo que quede después ya no se le devuelve con esta solicitud."
    }
