package com.vaiinilla.app.ui.parentmode

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.vaiinilla.app.domain.guardianship.compactMoney
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import java.math.BigDecimal
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
import com.vaiinilla.app.ui.components.minTouchTarget
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
    var showAllAllergens by remember { mutableStateOf(false) }
    var lastThreshold by remember { mutableStateOf(current.lowBalanceAlert) }
    val vColors = LocalVaiinillaColors.current
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
        title = "Controles de ${child.firstName}",
        subtitle = "La caja los revisa en cada compra",
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
        ControlCard(Modifier.alpha(if (principal) 1f else 0.6f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    ControlTitle("Límite por día")
                    ControlNote(
                        limitProblem
                            ?: ChildControlsRules.zeroNote(dailyLimit, "Con \$0 ${child.firstName} no puede comprar nada sin ti.")
                            ?: "Lo más que puede gastar al día",
                        isError = limitProblem != null,
                    )
                }
                MoneyStepper(
                    value = TypedAmount.parse(dailyLimit),
                    emptyLabel = "Sin límite",
                    enabled = principal,
                    tag = "controls-limit",
                ) {
                    dailyLimit = TypedAmount.editable(it)
                    touched()
                }
            }
        }
        ControlCard(Modifier.alpha(if (principal) 1f else 0.6f)) {
            ControlTitle("Alergias")
            ControlNote("Se le muestran bloqueadas con el motivo. No se le vende ni se le aparta nada que las tenga.")
            ChipFlow {
                allergens.sortedBy { it.label }.forEach { allergen ->
                    PillChip("${allergen.label} ×", PillStyle.Allergy, enabled = principal, description = "Quitar ${allergen.label}") {
                        allergens = allergens - allergen
                        touched()
                    }
                }
                if (showAllAllergens) {
                    SchoolAllergen.entries.filterNot { it in allergens }.forEach { allergen ->
                        PillChip("+ ${allergen.label}", PillStyle.Off, enabled = principal, description = "Agregar ${allergen.label}") {
                            allergens = allergens + allergen
                            touched()
                        }
                    }
                } else if (principal && allergens.size < SchoolAllergen.entries.size) {
                    PillChip("+ Otra", PillStyle.Off, enabled = true, description = "Agregar otra alergia", tag = "controls-more-allergens") {
                        showAllAllergens = true
                    }
                }
                if (allergens.isEmpty() && !principal) ControlNote("Ninguna anotada")
            }
        }
        if (categories.isNotEmpty()) {
            ControlCard(Modifier.alpha(if (principal) 1f else 0.6f)) {
                ControlTitle("No puede comprar")
                ChipFlow {
                    categories.forEach { category ->
                        val on = category.id in blockedCategories
                        PillChip(category.name, if (on) PillStyle.On else PillStyle.Off, enabled = principal, selected = on) {
                            blockedCategories = if (on) blockedCategories - category.id else blockedCategories + category.id
                            touched()
                        }
                    }
                }
            }
        }
        ControlCard {
            val threshold = TypedAmount.parse(lowBalance)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    ControlTitle(if (threshold != null) "Avísame si baja de ${compactMoney(threshold)}" else "Avísame si su saldo baja")
                    ControlNote(
                        lowProblem
                            ?: ChildControlsRules.zeroNote(lowBalance, "Con \$0 no te avisamos nunca; apágalo si no quieres aviso.")
                            ?: "Te llega un aviso para recargarle",
                        isError = lowProblem != null,
                    )
                }
                Switch(
                    checked = threshold != null,
                    onCheckedChange = { on ->
                        lowBalance = if (on) TypedAmount.editable(lastThreshold ?: DEFAULT_LOW_BALANCE) else ""
                        if (!on) lastThreshold = threshold
                        touched()
                    },
                    colors =
                        SwitchDefaults.colors(
                            checkedTrackColor = vColors.accent,
                            checkedThumbColor = vColors.accentInk,
                            checkedBorderColor = vColors.accent,
                            uncheckedTrackColor = vColors.line,
                            uncheckedThumbColor = vColors.muted,
                            uncheckedBorderColor = vColors.line,
                        ),
                    modifier = Modifier.testTag("controls-low-switch"),
                )
            }
            AnimatedVisibility(
                visible = threshold != null,
                enter = if (reduced) fadeIn(snap()) else fadeIn() + expandVertically(spring(dampingRatio = 0.86f, stiffness = 380f)),
                exit = if (reduced) fadeOut(snap()) else fadeOut() + shrinkVertically(spring(stiffness = 500f)),
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
                    MoneyStepper(value = threshold, emptyLabel = "—", enabled = true, tag = "controls-low", allowEmpty = false) {
                        lowBalance = TypedAmount.editable(it)
                        touched()
                    }
                }
            }
        }
        ControlCard(Modifier.alpha(if (principal) 1f else 0.6f)) {
            ControlTitle("Nota para la cafetería")
            OperationalField(
                value = notes,
                onValueChange = { notes = it },
                label = "",
                colors = colors,
                placeholder = "Por ejemplo: que siempre se lleve una fruta",
                supporting =
                    if (notes.length > ChildControlsRules.NOTES_LIMIT) "Hasta ${ChildControlsRules.NOTES_LIMIT} letras." else null,
                isError = notes.length > ChildControlsRules.NOTES_LIMIT,
                singleLine = false,
                minLines = 2,
                enabled = principal,
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("controls-unlink")
                    .physicalPress(enabled = !unlinking) { confirmsUnlink = true }
                    .padding(vertical = 10.dp)
                    .minTouchTarget(),
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

private val DEFAULT_LOW_BALANCE = BigDecimal("50")
private val STEP = BigDecimal("10")

/** − $60 +, de 10 en 10. Por debajo de $10 queda sin monto (sin límite) si [allowEmpty]. */
@Composable
private fun MoneyStepper(
    value: BigDecimal?,
    emptyLabel: String,
    enabled: Boolean,
    tag: String,
    allowEmpty: Boolean = true,
    onChange: (BigDecimal?) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    Row(
        modifier = Modifier.height(40.dp).clip(RoundedCornerShape(percent = 50)).background(colors.line).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton("−", "Bajar", enabled && (value != null && (allowEmpty || value > STEP))) {
            haptics.selection()
            val next = value?.subtract(STEP)
            onChange(if (next == null || next < STEP) (if (allowEmpty) null else STEP) else next)
        }
        AnimatedContent(
            targetState = value?.let(::compactMoney) ?: emptyLabel,
            transitionSpec = {
                if (reduced) {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                } else {
                    (fadeIn(spring(stiffness = 500f)) + slideInVertically(spring(dampingRatio = 0.7f, stiffness = 500f)) { it / 2 }) togetherWith
                        (fadeOut(spring(stiffness = 800f)) + slideOutVertically(spring(stiffness = 800f)) { -it / 2 })
                }
            },
            label = "stepper",
        ) { text ->
            Text(
                text,
                color = colors.ink,
                fontWeight = FontWeight.Black,
                fontSize = if (value == null) 13.sp else 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.widthIn(min = 62.dp).padding(horizontal = 4.dp),
            )
        }
        StepButton("+", "Subir", enabled) {
            haptics.selection()
            onChange(value?.add(STEP) ?: STEP)
        }
    }
}

@Composable
private fun StepButton(
    symbol: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onClick)
                .alpha(if (enabled) 1f else 0.35f)
                .semantics {
                    contentDescription = description
                    role = Role.Button
                },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = colors.ink2, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

private enum class PillStyle { On, Off, Allergy }

/** Las pastillas de Doop: lo elegido en tinta, la alergia en coral con su ×, lo que se puede agregar en papel 3. */
@Composable
private fun PillChip(
    text: String,
    style: PillStyle,
    enabled: Boolean,
    selected: Boolean = false,
    description: String? = null,
    tag: String? = null,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val background by animateColorAsState(
        when (style) {
            PillStyle.On -> colors.ink
            PillStyle.Off -> colors.line
            PillStyle.Allergy -> colors.coral.copy(alpha = 0.18f)
        },
        spring(stiffness = 600f),
        label = "pill-bg",
    )
    val ink by animateColorAsState(
        when (style) {
            PillStyle.On -> colors.paper
            PillStyle.Off -> colors.ink2
            PillStyle.Allergy -> colors.errorText
        },
        spring(stiffness = 600f),
        label = "pill-ink",
    )
    Box(
        modifier =
            Modifier
                .let { if (tag != null) it.testTag(tag) else it }
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small) {
                    haptics.selection()
                    onClick()
                }.height(34.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(background)
                .semantics {
                    role = Role.Checkbox
                    this.selected = selected || style == PillStyle.Allergy
                    if (description != null) contentDescription = description
                }.padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, maxLines = 1)
    }
}

@Composable
private fun ControlCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(LocalVaiinillaColors.current.paper2)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun ControlTitle(text: String) {
    Text(text, color = LocalVaiinillaColors.current.ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
}

@Composable
private fun ControlNote(
    text: String,
    isError: Boolean = false,
) {
    val colors = LocalVaiinillaColors.current
    Text(
        text,
        color = if (isError) colors.errorText else colors.muted,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 17.sp,
    )
}