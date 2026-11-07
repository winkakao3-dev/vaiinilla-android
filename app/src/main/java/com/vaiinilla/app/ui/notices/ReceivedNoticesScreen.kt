package com.vaiinilla.app.ui.notices

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.DeliveryDining
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RoomService
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.vaiinilla.app.ui.components.ClientCap
import com.vaiinilla.app.ui.components.ClientCard
import com.vaiinilla.app.ui.components.ClientCardDivider
import com.vaiinilla.app.ui.components.ClientPageHeader
import com.vaiinilla.app.ui.screens.walletTile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.vaiinilla.app.ui.components.ChoiceChip
import com.vaiinilla.app.ui.components.EditorialSecondaryButton
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.WalletScreenShell
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaColors

/** Abre «Avisos recibidos» desde el menú del avatar del personal o desde Cuenta; `null` si no hay navegación. */
val LocalOpenReceivedNotices = compositionLocalOf<(() -> Unit)?> { null }

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
        onResolve = { viewModel.resolveIfNeeded(it, modes) },
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
    onResolve: (ReceivedNotice) -> Unit = {},
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var business by rememberSaveable { mutableStateOf<String?>(null) }
    val chips = remember(notices) { NoticePanel.chips(notices) }
    // Si el negocio elegido ya no tiene avisos (se borró el historial), se ven todos.
    val activeBusiness = business?.takeIf { chosen -> chips.any { it.first == chosen } }
    val shown = remember(notices, activeBusiness) { NoticePanel.ordered(NoticePanel.filter(notices, activeBusiness)) }
    val (pending, earlier) = remember(shown) { shown.partition(NoticePanel::isPending) }

    WalletScreenShell(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
        ) {
            item(key = "head") {
                ClientPageHeader(
                    title = "Avisos recibidos",
                    subtitle = "Cada aviso que llegó a este teléfono",
                    onBack = {
                        haptics.click()
                        onBack()
                    },
                    modifier = Modifier.arrive(index = 0),
                )
            }
            if (notices.isEmpty()) {
                item(key = "empty") { EmptyNotices(colors) }
            }
            if (chips.size > 1) {
                item(key = "chips") {
                    BusinessChips(
                        chips = chips,
                        total = notices.size,
                        selected = activeBusiness,
                        onSelect = {
                            haptics.click()
                            business = it
                        },
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
            val groups = listOf("Pendientes" to pending, "Anteriores" to earlier).filter { it.second.isNotEmpty() }
            groups.forEachIndexed { groupIndex, (label, group) ->
                item(key = "group-$label") {
                    Column(modifier = Modifier.padding(top = 18.dp).arrive(index = groupIndex + 1, key = label)) {
                        // Siempre con su nombre: sin él, «Anteriores» se leía como una lista suelta (en vivo, W04).
                        ClientCap(label, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
                        ClientCard {
                            group.forEachIndexed { index, notice ->
                                LaunchedEffect(notice.id) { onResolve(notice) }
                                if (index > 0) ClientCardDivider()
                                NoticeRow(
                                    notice = notice,
                                    pending = label == "Pendientes",
                                    searching = notice.id in searching,
                                    colors = colors,
                                    onClick = {
                                        haptics.click()
                                        openId = notice.id
                                    },
                                )
                            }
                        }
                    }
                }
            }
            if (notices.isNotEmpty()) {
                item(key = "clear") {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        TextButton(onClick = { confirmClear = !confirmClear }) {
                            Text(
                                if (confirmClear) "Toca otra vez para borrar el historial" else "Borrar el historial",
                                color = if (confirmClear) colors.coral else colors.muted,
                                fontWeight = FontWeight.Bold,
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
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 48.dp).arrive(index = 1)) {
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

@Composable
private fun BusinessChips(
    chips: List<Pair<String, Int>>,
    total: Int,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).arrive(index = 1),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChoiceChip(label = "Todos · $total", selected = selected == null, onClick = { onSelect(null) })
        chips.forEach { (name, count) ->
            ChoiceChip(label = "$name · $count", selected = selected == name, onClick = { onSelect(name) })
        }
    }
}

/** El ícono de la fila según lo que es el aviso. */
private fun noticeIcon(notice: ReceivedNotice): ImageVector {
    val alert = notice.payload["alert"].orEmpty()
    return when (notice.payload["type"]) {
        "staff_alert" ->
            when (alert) {
                "por_cobrar" -> Icons.Outlined.PointOfSale
                "nueva_comanda" -> Icons.Outlined.Restaurant
                "listo_mesero", "listo_barra" -> Icons.Outlined.RoomService
                "llamada_mesa" -> Icons.Outlined.NotificationsActive
                "turno_por_terminar" -> Icons.Outlined.Schedule
                "pedido_domicilio" -> Icons.Outlined.DeliveryDining
                else -> Icons.Outlined.ShoppingBag
            }
        "order_state" -> Icons.AutoMirrored.Outlined.ReceiptLong
        "saldo_bajo", "recarga_acreditada" -> Icons.Outlined.AccountBalanceWallet
        "mesero_en_camino" -> Icons.AutoMirrored.Outlined.DirectionsWalk
        else -> Icons.Outlined.Notifications
    }
}

/** Una fila del lienzo L11: el ícono en su cuadro, el aviso, su negocio y lo que dice, y la hora. */
@Composable
private fun NoticeRow(
    notice: ReceivedNotice,
    pending: Boolean,
    searching: Boolean,
    colors: VaiinillaColors,
    onClick: () -> Unit,
) {
    val business = NoticePanel.business(notice)
    val subtitle =
        listOfNotNull(business, notice.body.takeIf { it.isNotBlank() })
            .joinToString(" · ")
            .ifEmpty { if (searching) "Buscando el pedido…" else NoticePanel.explanation(notice).kind }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (pending) colors.accent.copy(alpha = 0.18f) else walletTile(colors)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                noticeIcon(notice),
                contentDescription = null,
                tint = if (pending) colors.accent else colors.ink2,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                notice.title,
                color = colors.ink,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                color = colors.muted,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Row(
            modifier = Modifier.padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(NoticePanel.shortDate(notice.receivedAtMillis), color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (pending) {
                Box(
                    modifier =
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(colors.coral)
                            .semantics { contentDescription = "Pendiente" },
                )
            }
        }
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
            if (notice.lookup !is NoticeLookup.Found) {
                NoticePanel.customer(notice)?.let { Text("Lo pidió $it", color = colors.ink2, fontSize = 15.sp) }
            }
            val source = if (notice.isRemote) "del servidor" else "de esta app"
            Text(
                "${NoticePanel.date(notice.receivedAtMillis)} · $source",
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
