package com.vaiinilla.app.ui.notices

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vaiinilla.app.core.notifications.NoticeExplanation
import com.vaiinilla.app.core.notifications.NoticeLookup
import com.vaiinilla.app.core.notifications.ReceivedNotice
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.components.EditorialSecondaryButton
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.WalletScreenShell
import com.vaiinilla.app.ui.components.WalletSubflowTopBar
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaColors
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Abre «Avisos recibidos» desde el menú del avatar del personal o desde Cuenta; `null` si no hay navegación. */
val LocalOpenReceivedNotices = compositionLocalOf<(() -> Unit)?> { null }

private val TIME = DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.forLanguageTag("es-MX"))

/**
 * Historial de los avisos que llegaron a este teléfono, con lo que explica cada uno: qué es, quién lo provocó, a
 * quién va y por qué le llegó a esta cuenta (AVISOS-01, igual que el panel de iOS). No cambia nada en el servidor.
 */
@Composable
fun ReceivedNoticesScreen(
    viewModel: ReceivedNoticesViewModel,
    modes: List<AuthorizedMode>,
    onBack: () -> Unit,
) {
    val notices by viewModel.notices.collectAsStateWithLifecycle()
    val searching by viewModel.searching.collectAsStateWithLifecycle()
    ReceivedNoticesContent(
        notices = notices,
        searching = searching,
        onBack = onBack,
        onLookUp = { viewModel.lookUp(it, modes) },
        onClear = viewModel::clear,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReceivedNoticesContent(
    notices: List<ReceivedNotice>,
    searching: Set<String>,
    onBack: () -> Unit,
    onLookUp: (ReceivedNotice) -> Unit,
    onClear: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    WalletScreenShell(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            WalletSubflowTopBar(
                title = "Avisos recibidos",
                onBack = {
                    haptics.click()
                    onBack()
                },
            )
            if (notices.isEmpty()) {
                EmptyNotices(colors)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(20.dp),
                ) {
                    item {
                        Text(
                            "Cada aviso que llegó a este teléfono. Tócalo para ver qué es, de qué negocio y pedido, " +
                                "quién lo provocó y por qué te llegó.",
                            color = colors.muted,
                            fontSize = 14.sp,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp).arrive(index = 0),
                        )
                    }
                    itemsIndexed(notices, key = { _, notice -> notice.id }) { index, notice ->
                        NoticeRow(
                            notice = notice,
                            colors = colors,
                            modifier = Modifier.arrive(index = (index + 1).coerceAtMost(8), key = notice.id),
                            onClick = {
                                haptics.click()
                                openId = notice.id
                            },
                        )
                    }
                    item {
                        TextButton(
                            onClick = { confirmClear = true },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) {
                            Text(
                                if (confirmClear) "Toca otra vez para borrar el historial" else "Borrar el historial",
                                color = if (confirmClear) colors.coral else colors.muted,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                            )
                        }
                        if (confirmClear) {
                            TextButton(
                                onClick = {
                                    haptics.click()
                                    confirmClear = false
                                    onClear()
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Sí, borrar", color = colors.coral, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    val open = notices.firstOrNull { it.id == openId }
    if (open != null) {
        ModalBottomSheet(
            onDismissRequest = { openId = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.paper,
        ) {
            NoticeDetail(
                notice = open,
                searching = open.id in searching,
                colors = colors,
                onLookUp = {
                    haptics.click()
                    onLookUp(open)
                },
            )
        }
    }
}

@Composable
private fun EmptyNotices(colors: VaiinillaColors) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.arrive(index = 0)) {
            Text("Sin avisos todavía", color = colors.ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(
                "Aquí se guarda cada aviso que llega a este teléfono desde ahora, para que veas de dónde viene.",
                color = colors.muted,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun NoticeRow(
    notice: ReceivedNotice,
    colors: VaiinillaColors,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val explanation = remember(notice.id) { NoticeExplanation.explain(notice.payload, notice.isRemote) }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.paper2)
                .physicalPress(scale = PhysicalPressScale.Nav, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                explanation.kind.uppercase(),
                color = colors.muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.weight(1f),
            )
            Text(time(notice.receivedAtMillis), color = colors.muted, fontSize = 12.sp)
        }
        Text(
            notice.title,
            color = colors.ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (notice.body.isNotBlank()) {
            Text(notice.body, color = colors.ink2, fontSize = 14.sp, lineHeight = 18.sp)
        }
        Text(
            notice.arrivals.joinToString(" · ") { it.label },
            color = colors.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun NoticeDetail(
    notice: ReceivedNotice,
    searching: Boolean,
    colors: VaiinillaColors,
    onLookUp: () -> Unit,
) {
    val explanation = remember(notice.id) { NoticeExplanation.explain(notice.payload, notice.isRemote) }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(explanation.kind.uppercase(), color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                notice.title,
                color = colors.ink,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (notice.body.isNotBlank()) Text(notice.body, color = colors.ink2, fontSize = 15.sp)
            Text(
                "${time(notice.receivedAtMillis)} · ${if (notice.isRemote) "del servidor" else "de esta app"}",
                color = colors.muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Section("Qué es", explanation.what, colors)
        Section("Quién lo provocó", explanation.trigger, colors)
        Section("A quién va", explanation.audience, colors)
        val phone = notice.phone
        Section(
            "Por qué llegó a este teléfono",
            buildString {
                append(explanation.reason(phone?.memberships.orEmpty(), phone?.openMode))
                phone?.account?.let { append("\nCuenta: $it.") }
                if (notice.phoneSeenLater) append("\nLa cuenta se anotó al abrir la app, no cuando llegó el aviso.")
                if (phone == null) append("\nLlegó sin sesión abierta en este teléfono.")
            },
            colors,
        )
        Section("Cómo llegó", notice.arrivals.joinToString("\n") { it.label }, colors)
        if (explanation.orderId != null) {
            OrderSection(notice.lookup, searching, colors, onLookUp)
        }
        Section(
            "Datos del aviso",
            notice.payload.entries
                .sortedBy { it.key }
                .joinToString("\n") { "${it.key}: ${it.value}" }
                .ifBlank { "—" },
            colors,
            monospace = true,
        )
    }
}

@Composable
private fun OrderSection(
    lookup: NoticeLookup?,
    searching: Boolean,
    colors: VaiinillaColors,
    onLookUp: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("El pedido", color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        AnimatedContent(
            targetState = if (searching) "searching" else lookup?.let { it::class.simpleName } ?: "none",
            transitionSpec = {
                fadeIn(spring(stiffness = 300f)) togetherWith fadeOut(spring(stiffness = 300f))
            },
            label = "pedido-del-aviso",
        ) { stateKey ->
            when {
                stateKey == "searching" ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = colors.ink,
                            modifier = Modifier.padding(end = 10.dp).size(20.dp),
                        )
                        Text("Buscando con los modos de esta cuenta…", color = colors.ink2, fontSize = 14.sp)
                    }
                lookup is NoticeLookup.Found -> {
                    val order = lookup.order
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.paper2)
                                .padding(14.dp),
                    ) {
                        Text(
                            "#${order.folio} · ${order.establishmentName}",
                            color = colors.ink,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            listOfNotNull(
                                order.status,
                                order.total,
                                order.paymentMethod,
                                order.destination,
                                order.space,
                            ).joinToString(" · "),
                            color = colors.ink2,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        order.customerName?.let { Text("Lo pidió $it", color = colors.ink2, fontSize = 14.sp) }
                        order.items.forEach { Text(it, color = colors.ink, fontSize = 14.sp) }
                        Text(
                            "Leído como ${order.readAs}",
                            color = colors.muted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                lookup is NoticeLookup.Unavailable ->
                    Column {
                        Text(lookup.reason, color = colors.ink2, fontSize = 14.sp, lineHeight = 19.sp)
                        EditorialSecondaryButton(text = "Buscar otra vez", onClick = onLookUp)
                    }
                else -> EditorialSecondaryButton(text = "Buscar el pedido", onClick = onLookUp)
            }
        }
    }
}

@Composable
private fun Section(
    title: String,
    body: String,
    colors: VaiinillaColors,
    monospace: Boolean = false,
) {
    Column {
        Text(title, color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(
            body,
            color = colors.ink,
            fontSize = if (monospace) 12.sp else 15.sp,
            lineHeight = if (monospace) 17.sp else 20.sp,
            fontFamily = if (monospace) FontFamily.Monospace else null,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private fun time(millis: Long): String = BusinessTime.format(Instant.ofEpochMilli(millis), TIME)
