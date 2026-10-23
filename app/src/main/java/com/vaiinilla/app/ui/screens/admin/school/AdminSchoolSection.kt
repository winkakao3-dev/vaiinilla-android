package com.vaiinilla.app.ui.screens.admin.school

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.school.GuardianRequest
import com.vaiinilla.app.domain.school.RosterStudent
import com.vaiinilla.app.domain.school.SchoolGuardianship
import com.vaiinilla.app.domain.school.SchoolSettings
import com.vaiinilla.app.domain.school.SchoolSettingsRules
import com.vaiinilla.app.domain.school.SchoolStudentRequest
import com.vaiinilla.app.ui.admin.school.AdminSchoolUiState
import com.vaiinilla.app.ui.admin.school.AdminSchoolViewModel
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.profile.displayInitials
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField
import com.vaiinilla.app.ui.screens.admin.AdminCard
import com.vaiinilla.app.ui.screens.admin.AdminChoice
import com.vaiinilla.app.ui.screens.admin.AdminCoral
import com.vaiinilla.app.ui.screens.admin.AdminHint
import com.vaiinilla.app.ui.screens.admin.AdminLoadError
import com.vaiinilla.app.ui.screens.admin.AdminSectionPage
import com.vaiinilla.app.ui.screens.admin.AdminSectionTitle
import com.vaiinilla.app.ui.screens.admin.AdminSheetError
import java.time.LocalDate

/** Lo que Negocio › Escuela le pide al ViewModel; separado para probar la pantalla sin red. */
class AdminSchoolActions(
    val onRetry: () -> Unit = {},
    val onSave: (SchoolSettings, Boolean, () -> Unit) -> Unit = { _, _, _ -> },
    val onRemoveJoinCode: () -> Unit = {},
    val onReview: (GuardianRequest, Boolean) -> Unit = { _, _ -> },
    val onRetryRequests: () -> Unit = {},
    val onSearch: (String) -> Unit = {},
    val onRemoveGuardianship: (SchoolGuardianship) -> Unit = {},
    val onMakePrincipal: (SchoolGuardianship) -> Unit = {},
    val onRetryRoster: () -> Unit = {},
    val onSaveRoster: (List<RosterStudent>, () -> Unit) -> Unit = { _, _ -> },
    val onLoadReserved: (LocalDate) -> Unit = {},
    val onSaveMenu: (LocalDate, Set<Int>, () -> Unit) -> Unit = { _, _, _ -> },
    val onCloseDay: (LocalDate, String) -> Unit = { _, _ -> },
    val onReopenDay: (LocalDate) -> Unit = {},
    val onRetryStudentRequests: () -> Unit = {},
    val onResolve: (SchoolStudentRequest) -> Unit = {},
    val onReject: (SchoolStudentRequest, String, () -> Unit) -> Unit = { _, _, _ -> },
)

/** Negocio › Escuela, conectada a su ViewModel. [products] es el catálogo que ya cargó Menú. */
@Composable
internal fun AdminSchoolSection(
    viewModel: AdminSchoolViewModel,
    products: List<Product>?,
    colors: OperationalColors,
    readOnly: Boolean,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState
    AdminSchoolContent(
        state = state,
        products = products,
        colors = colors,
        readOnly = readOnly,
        onBack = onBack,
        actions =
            AdminSchoolActions(
                onRetry = viewModel::refresh,
                onSave = viewModel::save,
                onRemoveJoinCode = viewModel::removeJoinCode,
                onReview = viewModel::review,
                onRetryRequests = viewModel::loadRequests,
                onSearch = viewModel::searchGuardianships,
                onRemoveGuardianship = viewModel::removeGuardianship,
                onMakePrincipal = viewModel::makePrincipal,
                onRetryRoster = viewModel::loadRoster,
                onSaveRoster = viewModel::saveRoster,
                onLoadReserved = viewModel::loadReserved,
                onSaveMenu = viewModel::saveMenu,
                onCloseDay = viewModel::closeDay,
                onReopenDay = viewModel::reopenDay,
                onRetryStudentRequests = viewModel::loadStudentRequests,
                onResolve = viewModel::resolve,
                onReject = viewModel::reject,
            ),
    )
}

@Composable
internal fun AdminSchoolContent(
    state: AdminSchoolUiState,
    products: List<Product>?,
    colors: OperationalColors,
    readOnly: Boolean,
    actions: AdminSchoolActions,
    onBack: () -> Unit,
) {
    val enabled = !readOnly && !state.saving
    AdminSectionPage(
        title = "Escuela",
        colors = colors,
        notice = state.notice,
        noticeIsError = state.noticeIsError,
        onBack = onBack,
    ) {
        val settings = state.settings
        if (settings == null) {
            item {
                val error = state.loadError
                if (error !=
                    null
                ) {
                    AdminLoadError(error, colors, actions.onRetry)
                } else {
                    AdminHint("Cargando la escuela…", colors)
                }
            }
            return@AdminSectionPage
        }
        item(key = "tutores") { GuardianRequestsCard(state, settings, colors, enabled, actions) }
        item(key = "quien-cuida") { GuardianshipsCard(state, colors, enabled, actions) }
        item(key = "devoluciones") { StudentRequestsCard(state, colors, enabled, actions) }
        item(key = "codigo") { JoinCodeCard(settings, colors, enabled, actions) }
        item(key = "padron") { RosterCard(state, colors, enabled, actions) }
        item(key = "reglas") {
            SchoolRulesCard(settings, colors, enabled) { draft, done -> actions.onSave(draft, false, done) }
        }
        item(key = "cada-dia") { MenuDaysCard(state, products, colors, !readOnly, actions) }
    }
}

/** El código que la escuela reparte para que los papás y alumnos entren. */
@Composable
private fun JoinCodeCard(
    settings: SchoolSettings,
    colors: OperationalColors,
    enabled: Boolean,
    actions: AdminSchoolActions,
) {
    var confirmsRegenerate by remember { mutableStateOf(false) }
    AdminCard(colors, Modifier.animateContentSize(schoolSpring())) {
        AdminSectionTitle("Unirse a la escuela", colors)
        AdminChoice(
            question = "¿Quién puede entrar?",
            options = listOf(false to "Cualquiera con el QR", true to "Solo con el código"),
            selected = settings.joinRequiresCode,
            colors = colors,
            enabled = enabled,
        ) { requires ->
            if (requires !=
                settings.joinRequiresCode
            ) {
                actions.onSave(settings.copy(joinRequiresCode = requires), false) {}
            }
        }
        val code = settings.joinCode
        if (code != null) {
            Text(
                code,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp,
                color = colors.textPrimary,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            AdminHint(
                "Los papás lo escriben para agregar a sus hijos. Repártelo en la circular. " +
                    "Al cambiarlo, el anterior deja de servir; quien ya entró se queda.",
                colors,
            )
            if (confirmsRegenerate) {
                InlineConfirm(
                    title = "¿Cambiar el código?",
                    message = "El código de ahora deja de servir para entrar.",
                    confirmLabel = "Cambiar",
                    colors = colors,
                    enabled = enabled,
                    onCancel = { confirmsRegenerate = false },
                ) {
                    confirmsRegenerate = false
                    actions.onSave(settings, true) {}
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SchoolButton("Cambiar código", colors, enabled) { confirmsRegenerate = true }
                    if (!settings.joinRequiresCode) {
                        SchoolButton(
                            "Quitar código",
                            colors,
                            enabled,
                            destructive = true,
                            onClick = actions.onRemoveJoinCode,
                        )
                    }
                }
            }
        } else {
            AdminHint("Sin código, entra cualquiera que tenga el QR de la escuela.", colors)
            SchoolButton("Crear código", colors, enabled) { actions.onSave(settings, true) {} }
        }
    }
}

/** Recarga con tarjeta y reglas para apartar: se editan juntas y se guardan con un botón. */
@Composable
private fun SchoolRulesCard(
    saved: SchoolSettings,
    colors: OperationalColors,
    enabled: Boolean,
    onSave: (SchoolSettings, () -> Unit) -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    var draft by remember(saved) { mutableStateOf<SchoolSettings?>(null) }
    val current = draft ?: saved
    val problem = SchoolSettingsRules.problem(current)
    val edit: ((SchoolSettings) -> SchoolSettings) -> Unit = { change -> draft = change(draft ?: saved) }
    AdminCard(colors) {
        AdminSectionTitle("Recarga con tarjeta", colors)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OperationalField(
                value = current.reloadMinimum,
                onValueChange = { text -> edit { it.copy(reloadMinimum = text.take(10)) } },
                label = "Mínimo",
                colors = colors,
                placeholder = "50",
                enabled = enabled,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
            OperationalField(
                value = current.reloadMaximum,
                onValueChange = { text -> edit { it.copy(reloadMaximum = text.take(10)) } },
                label = "Máximo",
                colors = colors,
                placeholder = "2000",
                enabled = enabled,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
        }

        AdminSectionTitle("Apartar comida", colors, Modifier.padding(top = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    daysLabel(current.daysAhead),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Text("Con 0, solo se pide para hoy.", fontSize = 12.sp, color = colors.textSecondary)
            }
            StepperButton("−", "Un día menos", colors, enabled && current.daysAhead > 0) {
                haptics.selection()
                edit { it.copy(daysAhead = (it.daysAhead - 1).coerceAtLeast(0)) }
            }
            Text(
                current.daysAhead.toString(),
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            StepperButton(
                "+",
                "Un día más",
                colors,
                enabled && current.daysAhead < SchoolSettingsRules.MAX_DAYS_AHEAD,
            ) {
                haptics.selection()
                edit { it.copy(daysAhead = (it.daysAhead + 1).coerceAtMost(SchoolSettingsRules.MAX_DAYS_AHEAD)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OperationalField(
                value = current.orderCutoff,
                onValueChange = { text -> edit { it.copy(orderCutoff = text.take(5)) } },
                label = "Pedir hasta",
                colors = colors,
                placeholder = "Todo el día",
                supporting = "Del mismo día",
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            OperationalField(
                value = current.cancelCutoff,
                onValueChange = { text -> edit { it.copy(cancelCutoff = text.take(5)) } },
                label = "Cancelar hasta",
                colors = colors,
                placeholder = "Hasta que empiece",
                supporting = "El saldo regresa",
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }
        AdminSheetError(problem.takeIf { draft != null })
        ActionPill(
            label = "Guardar",
            enabled = enabled && draft != null && draft != saved && problem == null,
            loading = false,
            colors = colors,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            haptics.impact()
            val ready = draft ?: return@ActionPill
            onSave(ready) { draft = null }
        }
    }
}

private fun daysLabel(days: Int): String =
    when (days) {
        0 -> "Solo hoy"
        1 -> "Hasta 1 día antes"
        else -> "Hasta $days días antes"
    }

@Composable
private fun StepperButton(
    symbol: String,
    description: String,
    colors: OperationalColors,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onClick)
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.cardInner)
                .semantics {
                    role = Role.Button
                    this.contentDescription = description
                },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) colors.textPrimary else colors.textMuted,
        )
    }
}

// region Piezas compartidas de la sección

/** Botón plano de las tarjetas; el lima queda para el "Guardar" de cada una. */
@Composable
internal fun SchoolButton(
    label: String,
    colors: OperationalColors,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier =
            modifier
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onClick)
                .clip(shape)
                .background(colors.cardInner)
                .semantics { role = Role.Button }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color =
                when {
                    !enabled -> colors.textMuted
                    destructive -> AdminCoral
                    else -> colors.textPrimary
                },
        )
    }
}

/** Confirmación dentro de la tarjeta, junto a lo que se confirma; sin hojas encima. */
@Composable
internal fun InlineConfirm(
    title: String,
    message: String,
    confirmLabel: String,
    colors: OperationalColors,
    enabled: Boolean,
    destructive: Boolean = false,
    onCancel: () -> Unit,
    extra: @Composable () -> Unit = {},
    confirmEnabled: Boolean = true,
    onConfirm: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.cardInner)
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
        Text(message, fontSize = 13.sp, color = colors.textPrimary)
        extra()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SchoolConfirmButton(confirmLabel, colors, enabled && confirmEnabled, destructive, onConfirm)
            SchoolButton("No", colors, enabled, onClick = onCancel)
        }
    }
}

/** El "sí" de una confirmación va en tinta: es lo elegido, no la acción principal de la pantalla. */
@Composable
private fun SchoolConfirmButton(
    label: String,
    colors: OperationalColors,
    enabled: Boolean,
    destructive: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier =
            Modifier
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onClick)
                .clip(shape)
                .background(
                    if (!enabled) {
                        colors.pillBackground
                    } else if (destructive) {
                        AdminCoral
                    } else {
                        colors.textPrimary
                    },
                ).semantics { role = Role.Button }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (!enabled) colors.textMuted else colors.background,
        )
    }
}

/** Avatar con iniciales y el bloque de texto de un alumno o tutor. */
@Composable
internal fun PersonRow(
    name: String,
    colors: OperationalColors,
    content: @Composable () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(colors.cardInner),
            contentAlignment = Alignment.Center,
        ) {
            Text(displayInitials(name), fontSize = 15.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { content() }
    }
}

/** Los botones de una fila, alineados con el texto (a la derecha del avatar). */
@Composable
internal fun RowActions(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.padding(start = 56.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
internal fun RowDivider(colors: OperationalColors) {
    Box(
        Modifier
            .padding(start = 56.dp, top = 4.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.cardBorder),
    )
}

@Composable
internal fun SectionRetry(
    message: String,
    colors: OperationalColors,
    onRetry: () -> Unit,
) {
    AdminHint(message, colors)
    SchoolButton("Reintentar", colors, enabled = true, onClick = onRetry)
}

internal fun <T> schoolSpring() =
    spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

/** Se vuelve a pedir lo apartado cada que se elige otro día. */
@Composable
internal fun LoadOnSelect(
    date: LocalDate?,
    load: (LocalDate) -> Unit,
) {
    LaunchedEffect(date) { date?.let(load) }
}

// endregion
