package com.vaiinilla.app.ui.parentmode

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PanTool
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.GuardianLinkCodeRules
import com.vaiinilla.app.domain.guardianship.GuardianshipException
import com.vaiinilla.app.domain.guardianship.GuardianshipRepository
import com.vaiinilla.app.domain.guardianship.PersonNames
import com.vaiinilla.app.domain.guardianship.PrivacyNotice
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.SlidingSegments
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.OperationalField
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** El aviso vigente, pedido al abrir la hoja; la app no guarda copia. */
private class NoticeLoader(
    private val guardianship: GuardianshipRepository,
) {
    var notice by mutableStateOf<PrivacyNotice?>(null)
    var error by mutableStateOf<String?>(null)

    suspend fun load() {
        error = null
        withContext(Dispatchers.IO) { guardianship.privacyNotice() }
            .onSuccess { notice = it }
            .onFailure {
                notice = null
                error = it.toUserFacingMessage("No pudimos cargar el aviso de privacidad.")
            }
    }
}

private fun Throwable.isConsentRequired(): Boolean =
    (this as? GuardianshipException)?.code == PrivacyNotice.CONSENT_REQUIRED

/**
 * Agregar a un hijo: sin cuenta, la escuela lo conoce por su matrícula; con cuenta, él te pasa el
 * código de seis letras que sale en su app. En los dos casos el tutor acepta antes el aviso de privacidad.
 */
@Composable
internal fun AddChildSheet(
    guardianship: GuardianshipRepository,
    onAdded: (Dependent) -> Unit,
    onDismiss: () -> Unit,
    onParentUnlocked: () -> Unit = {},
) {
    val colors = rememberOperationalColors()
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    val scope = rememberCoroutineScope()
    val loader = remember { NoticeLoader(guardianship) }
    // El servidor pidió el código de la escuela: al darlo, se vuelve a mandar lo que ya estaba escrito.
    var asksSchoolCode by remember { mutableStateOf(false) }
    var unlockedByCode by remember { mutableStateOf(false) }
    var withCode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var accepted by remember { mutableStateOf(false) }
    var showsNotice by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var state by remember { mutableStateOf<MorphState>(MorphState.Idle("Agregar")) }
    var sendKey by remember { mutableStateOf(newKey()) }
    LaunchedEffect(Unit) { loader.load() }

    val busy = state.isBusy
    val codeProblem =
        if (GuardianLinkCodeRules.typedLength(code) >= 6 && GuardianLinkCodeRules.normalized(code) == null) {
            "Son seis letras o números, sin I, O, 0 ni 1."
        } else {
            null
        }
    val canSubmit =
        !busy &&
            accepted &&
            loader.notice != null &&
            if (withCode) {
                GuardianLinkCodeRules.normalized(code) != null
            } else {
                name.trim().length >= 2 &&
                    studentId.isNotBlank()
            }

    fun submit() {
        val version = loader.notice?.version ?: return
        error = null
        state = MorphState.Working(if (withCode) "Buscando a tu hijo…" else "Agregando…")
        scope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    if (withCode) {
                        guardianship.link(GuardianLinkCodeRules.normalized(code).orEmpty(), version, sendKey)
                    } else {
                        guardianship.addDependent(
                            name.trim(),
                            studentId.trim(),
                            group.trim().ifEmpty { null },
                            version,
                            sendKey,
                        )
                    }
                }
            result
                .onSuccess { dependent ->
                    haptics.success()
                    state =
                        MorphState.Done(
                            if (dependent.isAwaitingSchool) {
                                "Le avisamos a la escuela"
                            } else {
                                "${dependent.firstName} " +
                                    "ya está contigo"
                            },
                        )
                    onAdded(dependent)
                    delay(if (reduced) 300 else 1000)
                    onDismiss()
                }.onFailure {
                    state = MorphState.Idle("Agregar")
                    sendKey = newKey()
                    error = it.toUserFacingMessage("No pudimos agregarlo. Intenta otra vez.")
                    if (it.isParentCodeRequired()) {
                        error = null
                        asksSchoolCode = true
                    } else if (it.isConsentRequired()) {
                        // El aviso cambió mientras la hoja estaba abierta: se lee y se acepta el nuevo.
                        accepted = false
                        showsNotice = true
                        loader.load()
                    }
                }
        }
    }

    ParentSheet(
        title = "Agregar a tu hijo",
        colors = colors,
        busy = busy,
        onDismiss = onDismiss,
        footer = {
            // Junto al botón: abajo del aviso largo quedaba fuera de la vista.
            error?.let { ParentErrorCard(message = it, colors = colors, onDismiss = { error = null }) }
            MorphingButton(
                state = state,
                enabled = canSubmit,
                colors = colors,
                testTag = "add-child-submit",
                onClick = ::submit,
            )
        },
    ) {
        SlidingSegments(
            labels = listOf("Sin cuenta", "Con código"),
            selectedIndex = if (withCode) 1 else 0,
            onSelect = {
                haptics.selection()
                withCode = it == 1
                error = null
                sendKey = newKey()
            },
            trackColor = colors.cardInner,
            indicatorColor = colors.textPrimary,
            selectedContentColor = colors.background,
            idleContentColor = colors.textPrimary,
            enabled = !busy,
            borderColor = colors.cardBorder,
        )
        if (withCode) {
            OperationalField(
                value = code,
                onValueChange = { code = it.uppercase() },
                label = "Código de tu hijo",
                colors = colors,
                placeholder = "ABC234",
                supporting =
                    codeProblem
                        ?: "Lo saca en su app, en Cartera › Soy alumno › Que mi tutor me agregue (dura 15 minutos). " +
                        "Si ya lo cuida otro tutor, él te pasa uno desde la ficha de tu hijo.",
                isError = codeProblem != null,
                enabled = !busy,
            )
        } else {
            OperationalField(value = name, onValueChange = {
                name = it
            }, label = "Nombre", colors = colors, placeholder = "Como lo conoce la escuela", enabled = !busy)
            OperationalField(
                value = studentId,
                onValueChange = { studentId = it },
                label = "Matrícula",
                colors = colors,
                placeholder = "La de su credencial",
                supporting = "Con ella Caja lo encuentra aunque no traiga teléfono.",
                enabled = !busy,
            )
            OperationalField(value = group, onValueChange = {
                group = it
            }, label = "Grupo", colors = colors, placeholder = "Por ejemplo: 3°B (opcional)", enabled = !busy)
        }
        ConsentBlock(
            loader = loader,
            accepted = accepted,
            showsNotice = showsNotice,
            busy = busy,
            colors = colors,
            onToggle = { accepted = !accepted },
            onToggleNotice = { showsNotice = !showsNotice },
            onRetry = { scope.launch { loader.load() } },
        )
    }

    if (asksSchoolCode) {
        ParentUnlockSheet(
            guardianship = guardianship,
            onUnlocked = {
                unlockedByCode = true
                onParentUnlocked()
            },
            onDismiss = {
                asksSchoolCode = false
                if (unlockedByCode) {
                    unlockedByCode = false
                    submit()
                }
            },
        )
    }
}

@Composable
private fun ConsentBlock(
    loader: NoticeLoader,
    accepted: Boolean,
    showsNotice: Boolean,
    busy: Boolean,
    colors: OperationalColors,
    onToggle: () -> Unit,
    onToggleNotice: () -> Unit,
    onRetry: () -> Unit,
) {
    val loadError = loader.error
    if (loadError != null) {
        ParentErrorCard(message = loadError, colors = colors, onRetry = onRetry)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ConsentCheck(
            checked = accepted,
            enabled = loader.notice != null && !busy,
            colors = colors,
            testTag = "add-child-consent",
            onToggle = onToggle,
        )
        Row(
            modifier =
                Modifier
                    .padding(start = 38.dp)
                    .testTag("add-child-read-notice")
                    .physicalPress(
                        enabled = loader.notice != null,
                        scale = PhysicalPressScale.Small,
                        onClick = onToggleNotice,
                    ).padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (showsNotice) "Ocultar el aviso" else "Leer el aviso",
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(18.dp).rotate(if (showsNotice) 180f else 0f),
            )
        }
        val notice = loader.notice
        AnimatedVisibility(visible = showsNotice && notice != null) {
            if (notice != null) PrivacyNoticeText(notice = notice, colors = colors)
        }
    }
}

/** Arriba de Mis hijos mientras falte aceptar el aviso de privacidad de algún hijo. */
@Composable
internal fun PendingConsentCard(
    children: List<Dependent>,
    ink: androidx.compose.ui.graphics.Color,
    muted: androidx.compose.ui.graphics.Color,
    surface: androidx.compose.ui.graphics.Color,
    line: androidx.compose.ui.graphics.Color,
    onOpen: () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("pending-consent")
                .physicalPress(scale = PhysicalPressScale.Small) {
                    haptics.selection()
                    onOpen()
                }.clip(RoundedCornerShape(16.dp))
                .background(surface)
                .border(1.dp, line, RoundedCornerShape(16.dp))
                .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.PanTool, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                "Falta tu permiso para los datos de ${consentNames(children)}",
                color = ink,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Text("Lee el aviso de privacidad y acéptalo", color = muted, fontSize = 13.sp)
        }
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            tint = muted,
            modifier = Modifier.size(12.dp).padding(top = 3.dp),
        )
    }
}

internal fun consentNames(children: List<Dependent>): String =
    PersonNames
        .list(
            children.map {
                it.firstName
            },
        ).ifEmpty { "tu hijo" }

/**
 * Para quien dio de alta a sus hijos antes de que existiera el aviso, o cuando el aviso cambió: lo lee
 * completo y lo acepta una vez para todos. No bloquea recargas ni controles.
 */
@Composable
internal fun PrivacyConsentSheet(
    children: ChildrenState,
    onDismiss: () -> Unit,
) {
    val colors = rememberOperationalColors()
    val reduced = reducedMotion()
    val scope = rememberCoroutineScope()
    val loader = remember { NoticeLoader(children.services.guardianship) }
    // Los nombres se fijan al abrir: al aceptar la lista se vacía y el subtítulo no debe saltar.
    val names = remember { consentNames(children.childrenNeedingConsent) }
    var accepted by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var state by remember { mutableStateOf<MorphState>(MorphState.Idle("Aceptar")) }
    var sendKey by remember { mutableStateOf(newKey()) }
    LaunchedEffect(Unit) { loader.load() }

    ParentSheet(
        title = "Aviso de privacidad",
        subtitle = "Falta tu permiso para $names",
        colors = colors,
        busy = state.isBusy,
        onDismiss = onDismiss,
        footer = {
            error?.let { ParentErrorCard(message = it, colors = colors, onDismiss = { error = null }) }
            MorphingButton(
                state = state,
                enabled = accepted && loader.notice != null,
                colors = colors,
                testTag = "pending-consent-accept",
            ) {
                val version = loader.notice?.version ?: return@MorphingButton
                error = null
                state = MorphState.Working("Guardando…")
                scope.launch {
                    children
                        .acceptPrivacyNotice(version, sendKey)
                        .onSuccess {
                            state = MorphState.Done("Gracias")
                            delay(if (reduced) 300 else 900)
                            onDismiss()
                        }.onFailure {
                            state = MorphState.Idle("Aceptar")
                            sendKey = newKey()
                            error = it.toUserFacingMessage("No se guardó. Intenta otra vez.")
                            if (it.isConsentRequired()) {
                                // El aviso cambió mientras lo leía: se carga el nuevo y se vuelve a marcar.
                                accepted = false
                                loader.load()
                            }
                        }
                }
            }
        },
    ) {
        val loadError = loader.error
        val notice = loader.notice
        when {
            loadError != null ->
                ParentErrorCard(
                    message = loadError,
                    colors = colors,
                    onRetry = { scope.launch { loader.load() } },
                )
            notice != null -> {
                PrivacyNoticeText(notice = notice, colors = colors)
                ConsentCheck(
                    checked = accepted,
                    enabled = !state.isBusy,
                    colors = colors,
                    testTag = "pending-consent-check",
                ) {
                    accepted = !accepted
                }
            }
            else -> SkeletonRows(4)
        }
    }
}
