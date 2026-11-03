package com.vaiinilla.app.ui.parentmode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.GuardianshipException
import com.vaiinilla.app.domain.guardianship.GuardianshipRepository
import com.vaiinilla.app.domain.guardianship.PersonNames
import com.vaiinilla.app.domain.guardianship.PrivacyNotice
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** El aviso vigente, pedido al abrir la hoja; la app no guarda copia. */
internal class NoticeLoader(
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

internal fun Throwable.isConsentRequired(): Boolean =
    (this as? GuardianshipException)?.code == PrivacyNotice.CONSENT_REQUIRED

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
                            sendKey = keyAfterFailure(it, sendKey)
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
