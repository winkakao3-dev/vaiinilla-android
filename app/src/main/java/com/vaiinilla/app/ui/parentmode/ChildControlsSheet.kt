package com.vaiinilla.app.ui.parentmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.DependentControls
import com.vaiinilla.app.domain.guardianship.TypedAmount
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.school.SchoolAllergen
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.OperationalField
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Las reglas puras de la hoja de controles: qué se valida y qué se manda. */
internal object ChildControlsRules {
    const val NOTES_LIMIT = 500

    /** Vacío no es problema (quita el límite); algo que no es monto, sí. */
    fun amountProblem(text: String): String? =
        if (text.isBlank() || TypedAmount.parse(text) != null) null else "Escribe un monto como 60 o 60.50."

    /** Un monto en cero es válido pero no hace lo que parece: se dice antes de guardar. */
    fun zeroNote(
        text: String,
        meaning: String,
    ): String? = if (TypedAmount.parse(text)?.signum() == 0) meaning else null

    /** Los bloqueos de productos sueltos se conservan aunque esta hoja solo edite categorías. */
    fun edited(
        current: DependentControls,
        dailyLimit: String,
        lowBalance: String,
        notes: String,
        allergens: Set<SchoolAllergen>,
        blockedCategories: Set<Int>,
    ): DependentControls =
        current.copy(
            dailyLimit = TypedAmount.parse(dailyLimit),
            lowBalanceAlert = TypedAmount.parse(lowBalance),
            notes = notes,
            allergens = allergens,
            blockedCategoryIds = blockedCategories,
        )
}

/**
 * Lo que el tutor decide sobre lo que su hijo compra sin él. Solo el principal cambia el límite, los
 * bloqueos, las alergias y la nota; los demás los ven de solo lectura y ponen su propio aviso de saldo
 * bajo (el servidor responde GUARDIAN_NOT_PRINCIPAL si alguien más lo intenta).
 */
@Composable
internal fun ChildControlsSheet(
    child: Dependent,
    services: ParentModeServices,
    onSaved: (Dependent) -> Unit,
    onUnlinked: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = rememberOperationalColors()
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    val scope = rememberCoroutineScope()
    val current = child.controls ?: DependentControls()
    val principal = child.isPrincipal
    var dailyLimit by remember { mutableStateOf(TypedAmount.editable(current.dailyLimit)) }
    var lowBalance by remember { mutableStateOf(TypedAmount.editable(current.lowBalanceAlert)) }
    var notes by remember { mutableStateOf(current.notes) }
    var allergens by remember { mutableStateOf(current.allergens) }
    var blockedCategories by remember { mutableStateOf(current.blockedCategoryIds) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var state by remember { mutableStateOf<MorphState>(MorphState.Idle("Guardar")) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmsUnlink by remember { mutableStateOf(false) }
    var unlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { services.catalog.getCatalog() }
            .onSuccess { catalog -> categories = catalog.categories.sortedBy { it.order } }
    }
    val limitProblem = ChildControlsRules.amountProblem(dailyLimit)
    val lowProblem = ChildControlsRules.amountProblem(lowBalance)
    val canSave =
        !state.isBusy && limitProblem == null && lowProblem == null && notes.length <= ChildControlsRules.NOTES_LIMIT

    fun touched() {
        haptics.selection()
        if (state is MorphState.Done) state = MorphState.Idle("Guardar")
    }

    fun save() {
        state = MorphState.Working("Guardando…")
        error = null
        val edited = ChildControlsRules.edited(current, dailyLimit, lowBalance, notes, allergens, blockedCategories)
        scope.launch {
            withContext(Dispatchers.IO) {
                if (principal) {
                    services.guardianship.updateControls(child.userId, edited, newKey())
                } else {
                    services.guardianship.updateLowBalanceAlert(child.userId, edited.lowBalanceAlert, newKey())
                }
            }.onSuccess {
                state = MorphState.Done("Guardado")
                onSaved(it)
                delay(if (reduced) 300 else 900)
                onDismiss()
            }.onFailure {
                state = MorphState.Idle("Guardar")
                error = it.toUserFacingMessage("No se guardaron los controles.")
            }
        }
    }

    ParentSheet(
        title = "Controles",
        subtitle = child.name,
        colors = colors,
        busy = state.isBusy || unlinking,
        onDismiss = onDismiss,
        footer = {
            error?.let { ParentErrorCard(message = it, colors = colors, onDismiss = { error = null }) }
            MorphingButton(
                state = state,
                enabled = canSave,
                colors = colors,
                testTag = "controls-save",
                onClick = ::save,
            )
        },
    ) {
        child.principalFirstName?.let {
            Text(
                "Los controles los pone $it. Tú puedes cambiar cuándo te avisamos que su saldo bajó.",
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.testTag("controls-read-only"),
            )
        }
        OperationalField(
            value = dailyLimit,
            onValueChange = { dailyLimit = it },
            label = "Límite por día",
            colors = colors,
            placeholder = "Sin límite",
            supporting =
                limitProblem
                    ?: ChildControlsRules.zeroNote(
                        dailyLimit,
                        "Con \$0 ${child.firstName} no puede comprar nada sin ti.",
                    )
                    ?: "Lo más que ${child.firstName} gasta en un día sin ti.",
            isError = limitProblem != null,
            keyboardType = KeyboardType.Decimal,
            enabled = principal,
        )
        OperationalField(
            value = lowBalance,
            onValueChange = { lowBalance = it },
            label = "Avísame si su saldo baja de",
            colors = colors,
            placeholder = "Sin aviso",
            supporting =
                lowProblem
                    ?: ChildControlsRules.zeroNote(
                        lowBalance,
                        "Con \$0 no te avisamos nunca; deja el campo vacío si no quieres aviso.",
                    )
                    ?: "Te llega una notificación para recargarle.",
            isError = lowProblem != null,
            keyboardType = KeyboardType.Decimal,
        )
        Column(
            modifier = Modifier.alpha(if (principal) 1f else 0.6f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SheetSectionTitle(
                "Alergias",
                colors,
                "No se le vende ni se le aparta nada que las tenga, ni lo que la escuela no ha revisado.",
            )
            ChipFlow {
                SchoolAllergen.entries.forEach { allergen ->
                    ToggleChip(allergen.label, allergen in allergens, colors, enabled = principal) {
                        allergens = if (allergen in allergens) allergens - allergen else allergens + allergen
                        touched()
                    }
                }
            }
        }
        if (categories.isNotEmpty()) {
            Column(
                modifier = Modifier.alpha(if (principal) 1f else 0.6f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SheetSectionTitle("No puede comprar", colors, "Lo de estas categorías no se le cobra.")
                ChipFlow {
                    categories.forEach { category ->
                        ToggleChip(category.name, category.id in blockedCategories, colors, enabled = principal) {
                            blockedCategories =
                                if (category.id in
                                    blockedCategories
                                ) {
                                    blockedCategories - category.id
                                } else {
                                    blockedCategories +
                                        category.id
                                }
                            touched()
                        }
                    }
                }
            }
        }
        OperationalField(
            value = notes,
            onValueChange = { notes = it },
            label = "Nota para la cafetería",
            colors = colors,
            placeholder = "Por ejemplo: sin picante",
            supporting =
                if (notes.length >
                    ChildControlsRules.NOTES_LIMIT
                ) {
                    "Hasta ${ChildControlsRules.NOTES_LIMIT} letras."
                } else {
                    null
                },
            isError = notes.length > ChildControlsRules.NOTES_LIMIT,
            singleLine = false,
            minLines = 2,
            enabled = principal,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("controls-unlink")
                    .physicalPress(enabled = !unlinking) { confirmsUnlink = true }
                    .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (unlinking) {
                CircularProgressIndicator(
                    color = ErrorCoral,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp).padding(end = 8.dp),
                )
            }
            Text(
                "Quitar a ${child.firstName} de mi cuenta",
                color = ErrorCoral,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }
    }
    if (confirmsUnlink) {
        ParentConfirmDialog(
            title = "¿Quitar a ${child.firstName}?",
            message = "Su cuenta y su saldo se quedan en la escuela; tú dejas de verlo y de recargarle.",
            confirmLabel = "Quitar",
            destructive = true,
            onConfirm = {
                unlinking = true
                scope.launch {
                    withContext(Dispatchers.IO) { services.guardianship.unlink(child.userId, newKey()) }
                        .onSuccess { onUnlinked() }
                        .onFailure { error = it.toUserFacingMessage("No se pudo quitar.") }
                    unlinking = false
                }
            },
            onDismiss = { confirmsUnlink = false },
        )
    }
}
