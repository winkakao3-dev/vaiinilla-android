package com.vaiinilla.app.ui.screens.admin.school

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.school.RosterStudent
import com.vaiinilla.app.domain.school.SchoolRosterParser
import com.vaiinilla.app.ui.admin.school.AdminSchoolUiState
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField
import com.vaiinilla.app.ui.screens.admin.AdminCard
import com.vaiinilla.app.ui.screens.admin.AdminCoral
import com.vaiinilla.app.ui.screens.admin.AdminHint
import com.vaiinilla.app.ui.screens.admin.AdminSectionTitle

/** El padrón de la escuela: con él, los tutores solo dan de alta a alumnos que de verdad son de aquí. */
@Composable
internal fun RosterCard(
    state: AdminSchoolUiState,
    colors: OperationalColors,
    enabled: Boolean,
    actions: AdminSchoolActions,
) {
    var pasting by rememberSaveable { mutableStateOf(false) }
    var confirmsRemove by remember { mutableStateOf(false) }
    AdminCard(colors, Modifier.animateContentSize(schoolSpring())) {
        AdminSectionTitle("Padrón de alumnos", colors)
        val roster = state.roster
        when {
            roster == null && state.rosterError != null ->
                SectionRetry(
                    state.rosterError,
                    colors,
                    actions.onRetryRoster,
                )
            roster == null -> AdminHint("Cargando el padrón…", colors)
            pasting ->
                RosterPaste(
                    replacesCount = roster.size,
                    colors = colors,
                    enabled = enabled,
                    onCancel = { pasting = false },
                ) { students -> actions.onSaveRoster(students) { pasting = false } }
            roster.isEmpty() -> {
                AdminHint(
                    "Sin padrón, cualquier tutor con el código puede dar de alta la matrícula que escriba. " +
                        "Pega la lista de tus alumnos para que solo entren ellos.",
                    colors,
                )
                SchoolButton("Pegar lista", colors, enabled) { pasting = true }
            }
            else -> {
                Text(
                    if (roster.size == 1) "1 alumno" else "${roster.size} alumnos",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                )
                SchoolRosterParser.groupsLine(roster)?.let {
                    Text(it, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                }
                AdminHint("Los tutores solo dan de alta matrículas de esta lista, y el grupo sale de aquí.", colors)
                if (confirmsRemove) {
                    InlineConfirm(
                        title = "¿Quitar el padrón?",
                        message =
                            "Cualquier tutor con el código podrá dar de alta la matrícula que escriba. Los hijos ya " +
                                "dados de alta se quedan.",
                        confirmLabel = "Quitar",
                        colors = colors,
                        enabled = enabled,
                        destructive = true,
                        onCancel = { confirmsRemove = false },
                    ) {
                        confirmsRemove = false
                        actions.onSaveRoster(emptyList()) {}
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SchoolButton("Cambiar lista", colors, enabled) { pasting = true }
                        SchoolButton("Quitar padrón", colors, enabled, destructive = true) { confirmsRemove = true }
                    }
                }
            }
        }
    }
}

/** Pegar el padrón desde la hoja de cálculo: se lee al momento y se dice qué líneas no sirven antes de guardar. */
@Composable
private fun RosterPaste(
    replacesCount: Int,
    colors: OperationalColors,
    enabled: Boolean,
    onCancel: () -> Unit,
    onSave: (List<RosterStudent>) -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    var text by rememberSaveable { mutableStateOf("") }
    val result = remember(text) { SchoolRosterParser.parse(text) }
    AdminHint(
        "Copia de tu hoja de cálculo las columnas matrícula, nombre y grupo, y pégalas aquí: una fila por alumno." +
            if (replacesCount > 0) " Reemplaza la lista de ahora ($replacesCount)." else "",
        colors,
    )
    OperationalField(
        value = text,
        onValueChange = { text = it },
        label = "Alumnos",
        colors = colors,
        placeholder = "A-001, Ana López, 3°B",
        enabled = enabled,
        singleLine = false,
        minLines = 6,
    )
    if (result.students.isNotEmpty() || result.problems.isNotEmpty()) {
        Column(
            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                if (result.students.size == 1) "Se leyó 1 alumno" else "Se leyeron ${result.students.size} alumnos",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            result.problems.take(3).forEach {
                Text(it.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AdminCoral)
            }
            if (result.problems.size > 3) {
                Text(
                    "y ${result.problems.size - 3} líneas más con problema",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AdminCoral,
                )
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ActionPill(
            label = "Guardar padrón",
            enabled = enabled && result.canSave,
            loading = false,
            colors = colors,
            modifier = Modifier.weight(1f),
        ) {
            haptics.impact()
            onSave(result.students)
        }
        SchoolButton("Cancelar", colors, enabled, onClick = onCancel)
    }
}
