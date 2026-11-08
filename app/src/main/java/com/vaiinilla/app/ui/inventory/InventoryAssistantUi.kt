package com.vaiinilla.app.ui.inventory

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaiinilla.app.data.inventory.InventoryAlertSupply
import com.vaiinilla.app.data.inventory.InventoryAlerts
import com.vaiinilla.app.data.inventory.InventoryUnit
import com.vaiinilla.app.ui.admin.AdminInventoryViewModel
import com.vaiinilla.app.ui.screens.ChipButton
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.admin.AdminInventorySection
import com.vaiinilla.app.ui.screens.admin.AdminPanel
import com.vaiinilla.app.ui.screens.admin.AdminPrimaryButton
import com.vaiinilla.app.ui.screens.admin.AdminSecondaryButton
import com.vaiinilla.app.ui.screens.amberInk
import java.util.Locale

private val Coral = Color(0xFFF15B55)
private val CoralLight = Color(0xFFFF8A84)

/** "640 ml", "2,400 ml", "1.5 kg". */
internal fun inventoryAmount(
    quantity: Double,
    unit: InventoryUnit,
): String {
    val number =
        if (quantity % 1.0 == 0.0) {
            "%,d".format(Locale.US, quantity.toLong())
        } else {
            "%,.2f".format(Locale.US, quantity).trimEnd('0').trimEnd('.')
        }
    return "$number ${unit.short}"
}

/** "Clamato", "Clamato y Corona", "Clamato, Corona y 2 más". */
internal fun namesLabel(names: List<String>): String =
    when (names.size) {
        0 -> ""
        1 -> names[0]
        2 -> "${names[0]} y ${names[1]}"
        else -> "${names[0]}, ${names[1]} y ${names.size - 2} más"
    }

/** El título del aviso de Caja y Cocina: lo más urgente primero. */
internal fun alertsHeadline(alerts: InventoryAlerts): String {
    val missing = alerts.soldOut.flatMap { it.missing }.distinct()
    return when {
        alerts.soldOut.isNotEmpty() -> "Se acabó: ${namesLabel(missing.ifEmpty { alerts.soldOut.map { it.name } })}"
        alerts.tomorrow.size == 1 -> "${alerts.tomorrow[0].name} no alcanza para mañana"
        alerts.tomorrow.isNotEmpty() -> "${alerts.tomorrow.size} cosas no alcanzan para mañana"
        alerts.belowMinimum.size == 1 -> "${alerts.belowMinimum[0].name} está bajo el mínimo"
        else -> "${alerts.belowMinimum.size} insumos bajo el mínimo"
    }
}

internal fun alertsDetail(alerts: InventoryAlerts): String =
    buildList {
        if (alerts.soldOut.isNotEmpty()) {
            val products = alerts.soldOut.map { it.name }
            add("${namesLabel(products)} ya ${if (products.size == 1) "salió" else "salieron"} del menú.")
            if (alerts.tomorrow.isNotEmpty()) add("Para mañana falta ${namesLabel(alerts.tomorrow.map { it.name })}.")
        } else if (alerts.tomorrow.size > 1) {
            add("${namesLabel(alerts.tomorrow.map { it.name })}.")
        } else if (alerts.tomorrow.size == 1) {
            val s = alerts.tomorrow[0]
            add(
                "Tienes ${inventoryAmount(
                    s.stock,
                    s.unit,
                )}; mañana se usan unos ${inventoryAmount(s.dailyUse, s.unit)}.",
            )
        }
        if (alerts.belowMinimum.isNotEmpty() && (alerts.soldOut.isNotEmpty() || alerts.tomorrow.isNotEmpty())) {
            add("${alerts.belowMinimum.size} bajo el mínimo.")
        }
    }.joinToString(" ")

internal fun supplierLine(supply: InventoryAlertSupply): String? =
    listOfNotNull(supply.supplierName, supply.supplierPhone).takeIf { it.isNotEmpty() }?.joinToString(" · ")

internal fun dialPhone(
    context: Context,
    phone: String,
) {
    val digits = phone.filter { it.isDigit() || it == '+' }
    if (digits.isEmpty()) return
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))) }
}

/**
 * El aviso de Caja (ámbar) y Cocina (coral si algo ya se agotó): qué falta, quién lo surte y la entrada al asistente.
 * No se pinta si no hay nada que avisar.
 */
@Composable
fun InventoryAlertsCard(
    alerts: InventoryAlerts?,
    colors: OperationalColors,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (alerts == null || alerts.total == 0) return
    val context = LocalContext.current
    val urgent = alerts.soldOut.isNotEmpty()
    val tint = if (urgent) Coral.copy(alpha = 0.16f) else colors.amberInk.copy(alpha = 0.14f)
    val titleInk = if (urgent) (if (colors.isDark) CoralLight else Coral) else colors.amberInk
    val supplier = alerts.firstSupplier
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(tint)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(alertsHeadline(alerts), fontSize = 15.sp, fontWeight = FontWeight.Black, color = titleInk)
        Text(
            alertsDetail(alerts),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            modifier = Modifier.padding(top = 4.dp),
        )
        supplier?.let { s ->
            Text(
                supplierLine(s).orEmpty(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            supplier?.supplierPhone?.let { phone ->
                AdminPrimaryButton(title = "Llamar", colors = colors, fullWidth = false) { dialPhone(context, phone) }
            }
            AdminSecondaryButton(title = "Ver insumos", colors = colors, outlined = true, onClick = onOpen)
        }
    }
}

/** El Inventario completo de Cocina: sin costos ni reporte; editar depende del permiso del admin. */
@Composable
fun KitchenInventoryDialog(
    inventory: AdminInventoryViewModel,
    assistant: InventoryAssistantViewModel,
    colors: OperationalColors,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            AdminInventorySection(
                viewModel = inventory,
                catalog = null,
                colors = colors,
                readOnly = false,
                onBack = onDismiss,
                assistant = assistant,
                canCount = true,
                showCosts = false,
                ownerControls = false,
            )
        }
    }
}

/** La entrada de Cocina a su Inventario completo. */
@Composable
fun InventoryEntryRow(
    colors: OperationalColors,
    onOpen: () -> Unit,
) {
    AdminPanel(colors, modifier = Modifier.padding(vertical = 6.dp).clickable(onClick = onOpen)) {
        Text("Inventario", fontSize = 15.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
        Text(
            "Existencias, recetas, mermas, conteo y el asistente",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
        )
    }
}

/** Una fila de insumo en «Cómo vas»: cuánto hay, cuánto se usa y el proveedor con «Llamar». */
@Composable
private fun AlertSupplyRow(
    supply: InventoryAlertSupply,
    detail: String,
    colors: OperationalColors,
) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(supply.name, fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
            Text(detail, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
            Text(
                supplierLine(supply) ?: "Sin proveedor: agrégalo al editar el insumo",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color =
                    if (supply.supplierName != null ||
                        supply.supplierPhone != null
                    ) {
                        colors.textPrimary
                    } else {
                        colors.textMuted
                    },
            )
        }
        supply.supplierPhone?.let { phone ->
            Box(Modifier.padding(start = 10.dp)) {
                AdminPrimaryButton(title = "Llamar", colors = colors, fullWidth = false) { dialPhone(context, phone) }
            }
        }
    }
}

@Composable
private fun OverviewCaption(
    text: String,
    color: Color,
) {
    Text(
        text.uppercase(Locale.forLanguageTag("es-MX")),
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.4.sp,
        color = color,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp).semantics { heading() },
    )
}

/** «Cómo vas» de Inventario y la parte de arriba del asistente: lo que no alcanza, lo agotado y lo bajo. */
internal fun LazyListScope.inventoryOverview(
    state: InventoryAssistantState,
    colors: OperationalColors,
    onRetry: () -> Unit,
) {
    val alerts = state.alerts
    if (alerts == null) {
        item(key = "overview-status") {
            Text(
                state.alertsError?.takeUnless { state.alertsLoading } ?: "Revisando tu inventario…",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier =
                    Modifier
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                        .then(if (state.alertsError != null) Modifier.clickable(onClick = onRetry) else Modifier),
            )
        }
        return
    }
    if (alerts.total == 0) {
        item(key = "overview-ok") {
            AdminPanel(colors, modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    "Todo alcanza para mañana",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                )
                Text(
                    "Se calcula con lo que se vendió los últimos ${alerts.days} días. Nada está agotado ni bajo el mínimo.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        return
    }
    if (alerts.tomorrow.isNotEmpty()) {
        item(key = "overview-tomorrow") {
            Column {
                OverviewCaption(
                    "No alcanza para mañana · ${alerts.tomorrow.size}",
                    if (colors.isDark) CoralLight else Coral,
                )
                AdminPanel(colors, padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) {
                    alerts.tomorrow.forEach { s ->
                        AlertSupplyRow(
                            s,
                            "Tienes ${inventoryAmount(
                                s.stock,
                                s.unit,
                            )} · mañana se usan unos ${inventoryAmount(s.dailyUse, s.unit)}",
                            colors,
                        )
                    }
                }
            }
        }
    }
    if (alerts.soldOut.isNotEmpty()) {
        item(key = "overview-soldout") {
            Column {
                OverviewCaption("Ya no se puede preparar · ${alerts.soldOut.size}", colors.textSecondary)
                AdminPanel(colors, padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) {
                    alerts.soldOut.forEach { p ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 11.dp)) {
                            Text(
                                p.name,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.textPrimary,
                            )
                            Text(
                                if (p.missing.isEmpty()) {
                                    "Agotado en el menú"
                                } else {
                                    "Agotado en el menú: falta ${namesLabel(p.missing)}"
                                },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
    if (alerts.belowMinimum.isNotEmpty()) {
        item(key = "overview-minimum") {
            Column {
                OverviewCaption("Bajo el mínimo · ${alerts.belowMinimum.size}", colors.textSecondary)
                AdminPanel(colors, padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) {
                    alerts.belowMinimum.forEach { s ->
                        val days = s.daysLeft
                        AlertSupplyRow(
                            s,
                            "Tienes ${inventoryAmount(s.stock, s.unit)}" +
                                when {
                                    days == null -> " · mínimo ${inventoryAmount(s.minimum, s.unit)}"
                                    days == 1 -> " · alcanza para 1 día"
                                    else -> " · alcanza para $days días"
                                },
                            colors,
                        )
                    }
                }
            }
        }
    }
}

/** El asistente de inventario a pantalla completa: arriba cómo vas, abajo la plática. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InventoryAssistantDialog(
    viewModel: InventoryAssistantViewModel,
    colors: OperationalColors,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { viewModel.refreshAlerts() }
    LaunchedEffect(state.messages.size, state.thinking) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(
                listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1,
            )
        }
    }
    val send = {
        if (viewModel.ask(draft)) draft = ""
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.cardBackground)
                        .clickable(onClick = onDismiss)
                        .semantics { contentDescription = "Regresar" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = colors.textPrimary)
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        "Asistente de inventario",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.textPrimary,
                    )
                    Text(
                        "Ve tus existencias, recetas y ventas",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                    )
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                inventoryOverview(state, colors, viewModel::refreshAlerts)
                item(key = "chat-gap") { Spacer(Modifier.height(4.dp)) }
                items(state.messages.size, key = { "m$it" }) { index ->
                    val message = state.messages[index]
                    ChatBubble(message.text, message.fromStaff, colors)
                }
                if (state.thinking) {
                    item(key = "thinking") { ChatBubble("Revisando tu inventario…", false, colors, muted = true) }
                }
                state.error?.let { error ->
                    item(key = "error") {
                        Column {
                            Text(
                                error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary,
                            )
                            AdminSecondaryButton(
                                title = "Reintentar",
                                colors = colors,
                                outlined = true,
                                modifier = Modifier.padding(top = 8.dp),
                                onClick = viewModel::retry,
                            )
                        }
                    }
                }
            }
            if (state.options.isNotEmpty() && state.canSend) {
                FlowRow(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.options.forEach { option ->
                        ChipButton(label = option, enabled = state.canSend, colors = colors) { viewModel.ask(option) }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.cardBackground)
                    .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f).padding(vertical = 10.dp)) {
                    if (draft.isEmpty()) {
                        Text(
                            if (state.canSend) "Escribe tu pregunta" else "Esta plática ya está llena",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMuted,
                        )
                    }
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it.take(INVENTORY_QUESTION_MAX_LETTERS) },
                        enabled = state.canSend,
                        textStyle =
                            TextStyle(
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                            ),
                        cursorBrush = SolidColor(colors.textPrimary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { send() }),
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (draft.isNotBlank() && state.canSend) colors.accentLime else colors.cardInner)
                        .clickable(enabled = draft.isNotBlank() && state.canSend) { send() }
                        .semantics { contentDescription = "Preguntar" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, tint = colors.accentInk)
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    text: String,
    fromStaff: Boolean,
    colors: OperationalColors,
    muted: Boolean = false,
) {
    Box(Modifier.fillMaxWidth(), contentAlignment = if (fromStaff) Alignment.CenterEnd else Alignment.CenterStart) {
        Text(
            text,
            fontSize = 14.5.sp,
            lineHeight = 20.sp,
            fontWeight = if (fromStaff) FontWeight.Bold else FontWeight.SemiBold,
            color =
                when {
                    fromStaff -> colors.background
                    muted -> colors.textSecondary
                    else -> colors.textPrimary
                },
            modifier =
                Modifier
                    .widthIn(max = 320.dp)
                    .clip(
                        if (fromStaff) {
                            RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)
                        } else {
                            RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
                        },
                    ).background(if (fromStaff) colors.textPrimary else colors.cardBackground)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
        )
    }
}
