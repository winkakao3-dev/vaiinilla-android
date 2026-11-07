package com.vaiinilla.app.ui.parentmode

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.GuardianLinkCodeRules
import com.vaiinilla.app.domain.guardianship.GuardianshipRepository
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.SlidingSegments
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.discovery.rememberHostSystemBars
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class AddChildStep { SCHOOL_CODE, WHO, ADDED }

/** Lo que se escribe en el paso 2. Se queda al ir por el código de la escuela, para mandarlo después tal cual. */
private class ChildForm {
    var withCode by mutableStateOf(false)
    var name by mutableStateOf("")
    var studentId by mutableStateOf("")
    var group by mutableStateOf("")
    var code by mutableStateOf("")
    var accepted by mutableStateOf(false)
    var showsNotice by mutableStateOf(false)

    val codeProblem: String?
        get() =
            if (GuardianLinkCodeRules.typedLength(code) >= 6 && GuardianLinkCodeRules.normalized(code) == null) {
                "Son seis letras o números, sin I, O, 0 ni 1."
            } else {
                null
            }

    val isComplete: Boolean
        get() =
            if (withCode) {
                GuardianLinkCodeRules.normalized(code) != null
            } else {
                name.trim().length >= 2 && studentId.isNotBlank()
            }
}

/**
 * Agregar a un hijo en tres pasos, a pantalla completa: el código de la escuela (la app siempre lo pide, F17; el
 * servidor lo valida en POST /tutelas/habilitar), quién es (sus datos o el código que saca él en su app, con el aviso de privacidad) y qué sigue: su primera
 * recarga, o esperar a que la escuela apruebe al tutor.
 */
@Composable
internal fun AddChildFlow(
    guardianship: GuardianshipRepository,
    onAdded: (Dependent) -> Unit,
    onDismiss: () -> Unit,
    onParentUnlocked: () -> Unit = {},
    /** Los hijos que ya cuidas: el código de uno de ellos no agrega a nadie y hay que decirlo (QAIOS-31). */
    knownChildIds: Set<String> = emptySet(),
    /** Empieza por el código de la escuela; Cartera y Opciones de la escuela siempre lo piden (F17). */
    startsWithSchoolCode: Boolean = false,
    schoolName: String? = null,
    /** Paso 3: abre la ficha del hijo recién agregado; con un monto, directo en recargarle eso. */
    onOpenChild: (Dependent, Int?) -> Unit = { _, _ -> onDismiss() },
) {
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    val scope = rememberCoroutineScope()
    val loader = remember { NoticeLoader(guardianship) }
    val form = remember { ChildForm() }
    var step by remember { mutableStateOf(if (startsWithSchoolCode) AddChildStep.SCHOOL_CODE else AddChildStep.WHO) }
    // El código se validó en este mismo flujo: el paso 2 lo confirma arriba.
    var codeConfirmed by remember { mutableStateOf(false) }
    // El servidor pidió el código al agregar: al darlo, se vuelve a mandar lo que ya estaba escrito.
    var resubmitAfterCode by remember { mutableStateOf(false) }
    var codeBusy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var state by remember { mutableStateOf<MorphState>(MorphState.Idle("Continuar")) }
    var sendKey by remember { mutableStateOf(newKey()) }
    var added by remember { mutableStateOf<Dependent?>(null) }
    LaunchedEffect(Unit) { loader.load() }

    val busy = state.isBusy || codeBusy
    val canSubmit = !state.isBusy && form.accepted && loader.notice != null && form.isComplete

    fun submit() {
        val version = loader.notice?.version ?: return
        error = null
        state = MorphState.Working(if (form.withCode) "Buscando a tu hijo…" else "Agregando…")
        scope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    if (form.withCode) {
                        guardianship.link(GuardianLinkCodeRules.normalized(form.code).orEmpty(), version, sendKey)
                    } else {
                        guardianship.addDependent(
                            form.name.trim(),
                            form.studentId.trim(),
                            form.group.trim().ifEmpty { null },
                            version,
                            sendKey,
                        )
                    }
                }
            result
                .onSuccess { dependent ->
                    haptics.success()
                    val alreadyCared = dependent.userId in knownChildIds
                    state =
                        MorphState.Done(
                            if (alreadyCared) "Ya cuidas a ${dependent.firstName}" else "${dependent.firstName} ya está contigo",
                        )
                    onAdded(dependent)
                    if (alreadyCared) {
                        delay(if (reduced) 300 else 2000)
                        onDismiss()
                    } else {
                        delay(if (reduced) 0 else 450)
                        added = dependent
                        step = AddChildStep.ADDED
                    }
                }.onFailure {
                    state = MorphState.Idle("Continuar")
                    sendKey = keyAfterFailure(it, sendKey)
                    when {
                        it.isParentCodeRequired() -> {
                            resubmitAfterCode = true
                            step = AddChildStep.SCHOOL_CODE
                        }
                        it.isConsentRequired() -> {
                            // El aviso cambió mientras el flujo estaba abierto: se lee y se acepta el nuevo.
                            error = it.toUserFacingMessage("El aviso de privacidad cambió. Léelo y acéptalo otra vez.")
                            form.accepted = false
                            form.showsNotice = true
                            loader.load()
                        }
                        else -> error = it.toUserFacingMessage("No pudimos agregarlo. Intenta otra vez.")
                    }
                }
        }
    }

    fun back() {
        if (busy) return
        if (step == AddChildStep.SCHOOL_CODE && resubmitAfterCode) {
            resubmitAfterCode = false
            step = AddChildStep.WHO
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = ::back,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnClickOutside = false,
            ),
    ) {
        val colors = LocalVaiinillaColors.current
        val bars = rememberHostSystemBars()
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(colors.paper)
                    .windowInsetsPadding(bars.only(WindowInsetsSides.Top))
                    .testTag("add-child-flow"),
        ) {
            StepHeader(
                step = step,
                awaiting = added?.isAwaitingSchool == true,
                backEnabled = !busy,
                onBack = if (step == AddChildStep.ADDED) null else ::back,
            )
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (reduced) {
                        fadeIn(snap()) togetherWith fadeOut(snap())
                    } else {
                        val forward = targetState.ordinal > initialState.ordinal
                        (
                            slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 400f)) {
                                if (forward) it / 4 else -it / 4
                            } + fadeIn(spring(stiffness = 300f))
                        ) togetherWith
                            (
                                slideOutHorizontally(spring(stiffness = 500f)) { if (forward) -it / 6 else it / 6 } +
                                    fadeOut(spring(stiffness = 600f))
                            )
                    }
                },
                modifier = Modifier.weight(1f),
                label = "add-child-step",
            ) { current ->
                when (current) {
                    AddChildStep.SCHOOL_CODE ->
                        SchoolCodeStep(
                            guardianship = guardianship,
                            schoolName = schoolName,
                            onBusyChange = { codeBusy = it },
                            onUnlocked = {
                                onParentUnlocked()
                                codeConfirmed = true
                                step = AddChildStep.WHO
                                if (resubmitAfterCode) {
                                    resubmitAfterCode = false
                                    submit()
                                }
                            },
                        )
                    AddChildStep.WHO ->
                        WhoStep(
                            form = form,
                            loader = loader,
                            state = state,
                            error = error,
                            canSubmit = canSubmit,
                            confirmedSchool = if (codeConfirmed) schoolName ?: "La escuela" else null,
                            onFormChanged = {
                                error = null
                                sendKey = newKey()
                            },
                            onRetryNotice = { scope.launch { loader.load() } },
                            onSubmit = ::submit,
                        )
                    AddChildStep.ADDED ->
                        added?.let { child ->
                            AddedChildStep(
                                child = child,
                                schoolName = schoolName,
                                onOpenChild = onOpenChild,
                                onDone = onDismiss,
                            )
                        }
                }
            }
        }
    }
}

/** El regreso y la barra de tres partes; la tercera va en amarillo mientras la escuela revisa al tutor. */
@Composable
private fun StepHeader(
    step: AddChildStep,
    awaiting: Boolean,
    backEnabled: Boolean,
    onBack: (() -> Unit)?,
) {
    val colors = LocalVaiinillaColors.current
    val number = step.ordinal + 1
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(start = if (onBack != null) 12.dp else 20.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            // El círculo se ve de 40 dp; el toque es de 48.
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .physicalPress(enabled = backEnabled, scale = PhysicalPressScale.Small, onClick = onBack)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Regresar"
                        },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.paper2),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = colors.ink,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
        }
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Paso $number de 3" },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(3) { index ->
                val target =
                    when {
                        index == 2 && step == AddChildStep.ADDED && awaiting -> colors.yolk
                        index < number -> colors.accent
                        else -> colors.line
                    }
                val fill by animateColorAsState(target, spring(stiffness = 300f), label = "step-$index")
                Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(fill))
            }
        }
        Text(
            "$number de 3",
            color = colors.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** El cuerpo de un paso con scroll y los botones abajo, que suben con el teclado. */
@Composable
internal fun AddChildStepPage(
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val bars = rememberHostSystemBars()
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 18.dp, bottom = 20.dp),
            content = content,
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime.union(bars.only(WindowInsetsSides.Bottom)))
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = bottom,
        )
    }
}

@Composable
internal fun StepTitle(
    title: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 32,
) {
    Text(
        title,
        color = LocalVaiinillaColors.current.ink,
        fontSize = fontSize.sp,
        lineHeight = (fontSize + 2).sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-1.3).sp,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

/** El rojo de los errores, más claro en oscuro para que se lea sobre el papel. */
internal val VaiinillaColors.errorText: Color
    get() = if (isDark) Color(0xFFFF8B86) else coral

/** La píldora de tinta de los pasos: se vuelve el progreso del envío y luego la confirmación. */
@Composable
internal fun FlowPillButton(
    state: MorphState,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    val active = enabled && state is MorphState.Idle
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(testTag)
                .height(56.dp)
                .graphicsLayer { alpha = if (active || state !is MorphState.Idle) 1f else 0.45f }
                .physicalPress(enabled = active, scale = PhysicalPressScale.Small) {
                    haptics.click()
                    onClick()
                }.clip(RoundedCornerShape(percent = 50))
                .background(colors.ink)
                .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                if (reduced) {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                } else {
                    (fadeIn(spring(stiffness = 300f)) + scaleIn(spring(dampingRatio = 0.7f, stiffness = 400f), 0.92f)) togetherWith
                        (fadeOut(spring(stiffness = 600f)) + scaleOut(spring(stiffness = 600f), 0.96f))
                }
            },
            label = "flow-pill",
        ) { current ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                when (current) {
                    is MorphState.Working ->
                        CircularProgressIndicator(
                            color = colors.paper,
                            strokeWidth = 2.dp,
                            modifier = Modifier.padding(end = 10.dp).size(18.dp),
                        )
                    is MorphState.Done ->
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = colors.paper,
                            modifier = Modifier.padding(end = 8.dp).size(20.dp),
                        )
                    is MorphState.Idle -> Unit
                }
                val label =
                    when (current) {
                        is MorphState.Idle -> current.label
                        is MorphState.Working -> current.label
                        is MorphState.Done -> current.label
                    }
                Text(label, color = colors.paper, fontSize = 16.5.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
        }
    }
}

/** Paso 2: quién es el hijo, con sus datos o con el código que saca en su app. */
@Composable
private fun WhoStep(
    form: ChildForm,
    loader: NoticeLoader,
    state: MorphState,
    error: String?,
    canSubmit: Boolean,
    /** La escuela cuyo código se acaba de validar; `null` si no se pidió en este flujo. */
    confirmedSchool: String?,
    onFormChanged: () -> Unit,
    onRetryNotice: () -> Unit,
    onSubmit: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val busy = state.isBusy
    AddChildStepPage(
        bottom = {
            AnimatedVisibility(visible = error != null) {
                Text(
                    error.orEmpty(),
                    color = colors.errorText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).testTag("add-child-error"),
                )
            }
            FlowPillButton(state = state, enabled = canSubmit, testTag = "add-child-submit", onClick = onSubmit)
        },
    ) {
        StepTitle("¿Quién es tu hijo?", Modifier.arrive(0))
        if (confirmedSchool != null) {
            Row(
                modifier =
                    Modifier
                        .arrive(1)
                        .padding(start = 4.dp, top = 8.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(colors.accent.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
                Text(
                    "$confirmedSchool · código correcto",
                    color = colors.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        SlidingSegments(
            labels = listOf("Con sus datos", "Con su código"),
            selectedIndex = if (form.withCode) 1 else 0,
            onSelect = {
                haptics.selection()
                form.withCode = it == 1
                onFormChanged()
            },
            trackColor = colors.paper2,
            indicatorColor = colors.ink,
            selectedContentColor = colors.paper,
            idleContentColor = colors.muted,
            enabled = !busy,
            modifier = Modifier.arrive(2).padding(top = 22.dp),
        )
        Column(
            modifier = Modifier.arrive(3).padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (form.withCode) {
                FlowField(
                    value = form.code,
                    onValueChange = {
                        form.code = it.uppercase()
                        onFormChanged()
                    },
                    label = "Código de tu hijo",
                    placeholder = "ABC234",
                    enabled = !busy,
                    isError = form.codeProblem != null,
                    capitalization = KeyboardCapitalization.Characters,
                )
                FieldNote(
                    form.codeProblem
                        ?: "Lo saca en su app, en Cartera › Soy alumno › Que mi tutor me agregue (dura 15 minutos). " +
                        "Si ya lo cuida otro tutor, él te pasa uno desde la ficha de tu hijo.",
                    isError = form.codeProblem != null,
                )
            } else {
                FlowField(
                    value = form.name,
                    onValueChange = {
                        form.name = it
                        onFormChanged()
                    },
                    label = "Nombre completo",
                    placeholder = "Como lo conoce la escuela",
                    enabled = !busy,
                    capitalization = KeyboardCapitalization.Words,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowField(
                        value = form.studentId,
                        onValueChange = {
                            form.studentId = it
                            onFormChanged()
                        },
                        label = "Matrícula",
                        placeholder = "De su credencial",
                        enabled = !busy,
                        capitalization = KeyboardCapitalization.Characters,
                        modifier = Modifier.weight(1.3f),
                    )
                    FlowField(
                        value = form.group,
                        onValueChange = {
                            form.group = it
                            onFormChanged()
                        },
                        label = "Grupo",
                        placeholder = "Opcional",
                        enabled = !busy,
                        capitalization = KeyboardCapitalization.Characters,
                        modifier = Modifier.weight(1f),
                    )
                }
                FieldNote("La matrícula viene en su credencial. Con ella Caja lo encuentra aunque no traiga teléfono.")
            }
        }
        Column(modifier = Modifier.arrive(4).padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ConsentCard(
                loader = loader,
                checked = form.accepted,
                enabled = loader.notice != null && !busy,
                onToggle = {
                    haptics.selection()
                    form.accepted = !form.accepted
                },
                onToggleNotice = { form.showsNotice = !form.showsNotice },
                onRetry = onRetryNotice,
            )
            val notice = loader.notice
            AnimatedVisibility(visible = form.showsNotice && notice != null) {
                if (notice != null) PrivacyNoticeText(notice = notice, colors = rememberOperationalColors())
            }
        }
    }
}

@Composable
private fun FieldNote(
    text: String,
    isError: Boolean = false,
) {
    val colors = LocalVaiinillaColors.current
    Text(
        text,
        color = if (isError) colors.errorText else colors.muted,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

/** Un campo del lienzo: la etiqueta adentro, arriba del valor, y el borde lima mientras se escribe. */
@Composable
private fun FlowField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
) {
    val colors = LocalVaiinillaColors.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val border by animateColorAsState(
        when {
            isError -> colors.coral
            focused -> colors.accent
            else -> Color.Transparent
        },
        spring(stiffness = 400f),
        label = "flow-field-border",
    )
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(color = colors.ink, fontSize = 16.5.sp, fontWeight = FontWeight.ExtraBold),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(capitalization = capitalization, autoCorrectEnabled = false),
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth(),
        decorationBox = { field ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.paper2)
                    .border(1.5.dp, border, shape)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(label, color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Box(Modifier.padding(top = 3.dp)) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            color = colors.muted.copy(alpha = 0.55f),
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                        )
                    }
                    field()
                }
            }
        },
    )
}

/** El permiso del tutor; «aviso de privacidad» abre el aviso completo debajo. */
@Composable
private fun ConsentCard(
    loader: NoticeLoader,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onToggleNotice: () -> Unit,
    onRetry: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val loadError = loader.error
    if (loadError != null) {
        ParentErrorCard(message = loadError, colors = rememberOperationalColors(), onRetry = onRetry)
        return
    }
    val box by animateColorAsState(if (checked) colors.accent else Color.Transparent, spring(stiffness = 500f), label = "consent-box")
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("add-child-consent")
                .alpha(if (loader.notice == null) 0.5f else 1f)
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onToggle)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.paper2)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .semantics {
                    role = Role.Checkbox
                    stateDescription = if (checked) "Aceptado" else "Sin aceptar"
                },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier =
                Modifier
                    .padding(top = 1.dp)
                    .size(24.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(box)
                    .border(1.5.dp, if (checked) Color.Transparent else colors.muted, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = checked,
                enter = scaleIn(spring(dampingRatio = 0.55f, stiffness = 600f)) + fadeIn(),
                exit = scaleOut(spring(stiffness = 800f)) + fadeOut(),
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accentInk, modifier = Modifier.size(15.dp))
            }
        }
        Text(
            buildAnnotatedString {
                append("Soy su madre, padre o tutor y acepto el ")
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "aviso",
                        styles =
                            TextLinkStyles(
                                SpanStyle(color = colors.ink, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline),
                            ),
                    ) { if (loader.notice != null) onToggleNotice() },
                ) { append("aviso de privacidad") }
                append(" para menores.")
            },
            color = colors.ink2,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
    }
}