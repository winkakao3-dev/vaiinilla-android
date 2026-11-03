package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.MoneyInput
import com.vaiinilla.app.domain.model.WalletClient
import com.vaiinilla.app.domain.model.WalletReloadReceipt
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.RollingText
import com.vaiinilla.app.ui.components.animateSelectionColor
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.operational.PendingReloadUi
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

/** Billetes con los que suele recargar la gente. */
private val ReloadQuickAmounts = listOf(50, 100, 200, 500)

private const val ReloadSearchMinLength = 2
private const val ReloadSearchDebounceMs = 350L
private const val ReloadScrollDelayMs = 280L

/** "Recargar $100 en efectivo": a quién va lo dice la fila marcada, cuánto lo repite el botón. */
internal fun walletReloadLabel(amount: BigDecimal?): String =
    if (amount == null || amount.signum() <= 0) {
        "Recargar en efectivo"
    } else {
        "Recargar ${moneyLabel(Money.format(amount))} en efectivo"
    }

private val LastOrderDay = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-MX"))

/** "Pidió hoy", "Pidió ayer" o "Pidió el 28 oct", con el día del negocio; null si nunca ha pedido aquí. */
internal fun walletClientLastOrder(
    lastOrderAt: String?,
    today: LocalDate = BusinessTime.today(),
): String? {
    val instant =
        lastOrderAt?.let {
            runCatching { Instant.parse(it) }.getOrNull()
                ?: runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull()
        } ?: return null
    return when (val day = BusinessTime.at(instant).toLocalDate()) {
        today -> "Pidió hoy"
        today.minusDays(1) -> "Pidió ayer"
        else -> "Pidió el ${LastOrderDay.format(day).trimEnd('.')}"
    }
}

/** Bajo el nombre: identificador, correo enmascarado y último pedido, para no recargarle al homónimo. */
internal fun walletClientDetail(
    client: WalletClient,
    today: LocalDate = BusinessTime.today(),
): String? =
    listOfNotNull(client.contextualId, client.maskedEmail, walletClientLastOrder(client.lastOrderAt, today))
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { null }

/** La fila del cliente: lo que lo distingue de un homónimo y su saldo antes de recargar. */
internal fun walletClientRowDetail(
    client: WalletClient,
    today: LocalDate = BusinessTime.today(),
): String? =
    listOfNotNull(
        walletClientDetail(client, today),
        client.balance?.toBigDecimalOrNull()?.let { "saldo ${moneyLabel(Money.format(it))}" },
    ).joinToString(" · ").ifBlank { null }

/** Lo escrito como monto, o null si no es un monto positivo. */
internal fun walletReloadAmount(text: String): BigDecimal? =
    text
        .trim()
        .toBigDecimalOrNull()
        ?.takeIf { it.signum() > 0 && it.scale() <= 2 }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WalletReloadSheet(
    clients: List<WalletClient>,
    searching: Boolean,
    receipt: WalletReloadReceipt?,
    canReload: Boolean,
    colors: OperationalColors,
    onSearch: (String) -> Unit,
    onScan: () -> Unit,
    onReload: (userId: String, amount: String) -> Unit,
    onDismiss: () -> Unit,
    blockedReason: String? = null,
    reloading: Boolean = false,
    errorMessage: String? = null,
    pendingReload: PendingReloadUi? = null,
    onConfirmPending: () -> Unit = {},
    onDiscardPending: () -> Unit = {},
    onDismissError: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        WalletReloadContent(
            clients = clients,
            searching = searching,
            receipt = receipt,
            canReload = canReload,
            colors = colors,
            onSearch = onSearch,
            onScan = onScan,
            onReload = onReload,
            onDone = onDismiss,
            blockedReason = blockedReason,
            reloading = reloading,
            errorMessage = errorMessage,
            pendingReload = pendingReload,
            onConfirmPending = onConfirmPending,
            onDiscardPending = onDiscardPending,
            onDismissError = onDismissError,
        )
    }
}

/**
 * Recarga en una sola hoja: se busca al cliente (o se escanea su QR) y, al marcarlo, aparece el
 * monto debajo. Así no se escribe dinero antes de saber a quién va.
 */
@Composable
internal fun WalletReloadContent(
    clients: List<WalletClient>,
    searching: Boolean,
    receipt: WalletReloadReceipt?,
    canReload: Boolean,
    colors: OperationalColors,
    onSearch: (String) -> Unit,
    onScan: () -> Unit,
    onReload: (userId: String, amount: String) -> Unit,
    onDone: () -> Unit,
    blockedReason: String? = null,
    reloading: Boolean = false,
    errorMessage: String? = null,
    pendingReload: PendingReloadUi? = null,
    onConfirmPending: () -> Unit = {},
    onDiscardPending: () -> Unit = {},
    onDismissError: () -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    var searched by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<WalletClient?>(null) }
    var amountText by remember { mutableStateOf("") }
    // Un recibo de antes de abrir la hoja no es el de esta recarga.
    val receiptAtOpen = remember { receipt?.movementId }
    val done = receipt?.takeIf { it.movementId != receiptAtOpen }
    val reduced = reducedMotion()

    val scroll = rememberScrollState()
    // El monto y el botón aparecen debajo de los resultados: la hoja baja sola para que no queden escondidos.
    LaunchedEffect(selected?.userId, amountText.isEmpty()) {
        if (selected == null) return@LaunchedEffect
        delay(ReloadScrollDelayMs)
        scroll.animateScrollTo(scroll.maxValue)
    }

    LaunchedEffect(query) {
        if (query.trim().length < ReloadSearchMinLength) return@LaunchedEffect
        delay(ReloadSearchDebounceMs)
        searched = true
        onSearch(query)
    }

    AnimatedContent(
        targetState = done != null,
        transitionSpec = {
            if (reduced) {
                fadeIn(snap()) togetherWith fadeOut(snap())
            } else {
                (
                    slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 500f)) { it / 5 } +
                        fadeIn(spring(stiffness = 400f))
                ) togetherWith fadeOut(spring(stiffness = 600f))
            }
        },
        label = "wallet-reload-step",
    ) { finished ->
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .imePadding()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (finished) {
                done?.let { ReloadDone(it, colors, onDone) }
            } else {
                ReloadHeader(colors)
                if (pendingReload != null) {
                    PendingReloadCard(pendingReload, reloading, colors, onConfirmPending, onDiscardPending)
                }
                ClientSearchField(
                    query = query,
                    onQueryChange = {
                        query = it
                        searched = false
                    },
                    onSubmit = {
                        if (query.trim().length >= ReloadSearchMinLength) {
                            searched = true
                            onSearch(query)
                        }
                    },
                    onScan = onScan,
                    colors = colors,
                )
                ClientResults(
                    clients = clients,
                    selected = selected,
                    searched = searched,
                    searching = searching,
                    colors = colors,
                    onPick = {
                        onDismissError()
                        selected = it
                    },
                )
                if (selected == null) ReloadError(errorMessage)
                AnimatedContent(
                    targetState = selected,
                    contentKey = { it != null },
                    transitionSpec = {
                        if (reduced) {
                            fadeIn(snap()) togetherWith fadeOut(snap())
                        } else {
                            fadeIn(spring(stiffness = 400f)) togetherWith fadeOut(spring(stiffness = 600f))
                        }
                    },
                    label = "wallet-reload-amount",
                ) { client ->
                    if (client != null) {
                        AmountSection(
                            client = client,
                            amountText = amountText,
                            onAmountChange = { amountText = it },
                            canReload = canReload,
                            blockedReason = blockedReason,
                            reloading = reloading,
                            errorMessage = errorMessage,
                            pendingReload = pendingReload,
                            colors = colors,
                            onReload = { amount -> onReload(client.userId, Money.format(amount)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReloadHeader(colors: OperationalColors) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Recargar saldo", fontSize = 28.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
        Text(
            "El cliente te da el efectivo y se le abona a su cartera al momento.",
            fontSize = 14.sp,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun ClientSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onScan: () -> Unit,
    colors: OperationalColors,
) {
    val shape = CircleShape
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(shape)
                .background(colors.cardBackground)
                .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Outlined.Search,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary),
            cursorBrush = SolidColor(colors.textPrimary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            modifier = Modifier.weight(1f),
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text("Nombre o identificador", fontSize = 15.sp, color = colors.textSecondary, maxLines = 1)
                    }
                    field()
                }
            },
        )
        Row(
            modifier =
                Modifier
                    .physicalPress(scale = PhysicalPressScale.Small, onClick = onScan)
                    .semantics(mergeDescendants = true) { contentDescription = "Escanear QR del cliente" }
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(colors.textPrimary)
                    .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Outlined.QrCodeScanner,
                contentDescription = null,
                tint = colors.background,
                modifier = Modifier.size(18.dp),
            )
            Text("Su QR", fontSize = 13.sp, fontWeight = FontWeight.Black, color = colors.background, maxLines = 1)
        }
    }
}

@Composable
private fun ClientResults(
    clients: List<WalletClient>,
    selected: WalletClient?,
    searched: Boolean,
    searching: Boolean,
    colors: OperationalColors,
    onPick: (WalletClient) -> Unit,
) {
    // Una búsqueda nueva no le quita la marca al cliente ya elegido: el botón ya no dice su nombre.
    val shown =
        selected
            ?.takeIf { pick -> clients.none { it.userId == pick.userId } }
            ?.let { listOf(it) + clients }
            ?: clients
    if (searching) {
        Text("Buscando…", fontSize = 13.sp, color = colors.textSecondary)
    } else if (searched && clients.isEmpty()) {
        Text(
            "Nadie con ese nombre en este lugar. Prueba con el identificador o escanea su QR.",
            fontSize = 13.sp,
            color = colors.textSecondary,
        )
    }
    if (shown.isEmpty()) return
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.cardBackground)
                .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        shown.forEachIndexed { index, client ->
            key(client.userId) {
                if (index > 0) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                                .height(1.dp)
                                .background(colors.cardBorder),
                    )
                }
                ClientRow(client, isSelected = client.userId == selected?.userId, colors) { onPick(client) }
            }
        }
    }
    Text(
        "Ves parte del correo para no confundir a dos personas con el mismo nombre.",
        fontSize = 12.sp,
        color = colors.textSecondary,
    )
}

@Composable
private fun ClientRow(
    client: WalletClient,
    isSelected: Boolean,
    colors: OperationalColors,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val border = animateSelectionColor(if (isSelected) colors.accentLime else Color.Transparent, "reload-client-border")
    val tint =
        animateSelectionColor(if (isSelected) colors.accentLime.copy(alpha = 0.08f) else Color.Transparent, "reload-client-tint")
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { if (isSelected) selected = true }
                .physicalPress(scale = PhysicalPressScale.Nav, onClick = onClick)
                .clip(shape)
                .background(tint)
                .border(2.dp, border, shape)
                .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ClientInitials(client.name)
        Column(modifier = Modifier.weight(1f)) {
            Text(client.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
            walletClientRowDetail(client)?.let {
                Text(it, fontSize = 12.sp, color = colors.textSecondary, maxLines = 2)
            }
        }
        if (isSelected) {
            Box(
                modifier = Modifier.size(22.dp).clip(CircleShape).background(colors.accentLime),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = colors.accentInk,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun AmountSection(
    client: WalletClient,
    amountText: String,
    onAmountChange: (String) -> Unit,
    canReload: Boolean,
    blockedReason: String?,
    reloading: Boolean,
    errorMessage: String?,
    pendingReload: PendingReloadUi?,
    colors: OperationalColors,
    onReload: (BigDecimal) -> Unit,
) {
    val amount = walletReloadAmount(amountText)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "¿CUÁNTO RECARGA?",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 4.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ReloadQuickAmounts.forEach { bill ->
                val value = bill.toBigDecimal()
                ReloadAmountChip(
                    label = moneyLabel(bill),
                    isSelected = amount != null && amount.compareTo(value) == 0,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                ) { onAmountChange(bill.toString()) }
            }
        }
        OperationalField(
            value = amountText,
            onValueChange = { raw -> onAmountChange(MoneyInput.sanitize(raw)) },
            label = "Otro monto",
            colors = colors,
            placeholder = "0.00",
            keyboardType = KeyboardType.Decimal,
        )
        val before = client.balance?.toBigDecimalOrNull()
        if (before != null && amount != null) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.cardBackground)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Saldo de ${client.name.substringBefore(' ')} después",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    moneyLabel(Money.format(before + amount)),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                )
            }
        }
        if (!canReload && blockedReason != null) {
            Text(blockedReason, fontSize = 13.sp, color = colors.textSecondary)
        }
        ReloadError(errorMessage)
        ActionPill(
            label = walletReloadLabel(amount),
            enabled = canReload && amount != null && pendingReload == null,
            loading = reloading,
            colors = colors,
            inverted = true,
            shape = CircleShape,
            height = 58.dp,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
        ) { amount?.let(onReload) }
    }
}

@Composable
private fun ReloadAmountChip(
    label: String,
    isSelected: Boolean,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val container = animateSelectionColor(if (isSelected) colors.accentLime else colors.cardBackground, "reload-chip")
    val ink = animateSelectionColor(if (isSelected) colors.accentInk else colors.textPrimary, "reload-chip-ink")
    Box(
        modifier =
            modifier
                .semantics { if (isSelected) selected = true }
                .physicalPress(onClick = onClick)
                .height(52.dp)
                .clip(shape)
                .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontWeight = FontWeight.Black, fontSize = 17.sp, color = ink, maxLines = 1)
    }
}
/** El error de la recarga, dentro de la hoja: detrás de ella la pantalla de Caja no se ve. */
@Composable
private fun ReloadError(message: String?) {
    if (message == null) return
    Text(
        message,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = ReloadErrorInk,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive },
    )
}

private val ReloadErrorInk = Color(0xFFE5645A)

/** Una recarga que quizá se abonó y quizá no: Caja la muestra con cliente y monto para que el cajero decida. */
@Composable
private fun PendingReloadCard(
    pending: PendingReloadUi,
    busy: Boolean,
    colors: OperationalColors,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.cardInner)
                .border(1.dp, colors.highlightBorder, RoundedCornerShape(16.dp))
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Recarga sin confirmar",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = colors.textPrimary,
        )
        Text(
            moneyLabel(pending.amount) + pending.userName?.let { " a $it" }.orEmpty() +
                ". Quizá no llegó al servidor. Confírmala para abonarla una sola vez, " +
                "o descártala si el cliente ya no la quiere.",
            fontSize = 13.sp,
            color = colors.textSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ChipButton(
                label = "Descartar",
                enabled = !busy,
                colors = colors,
                modifier = Modifier.weight(1f),
                onClick = onDiscard,
            )
            ActionPill(
                label = if (busy) "Confirmando…" else "Confirmar",
                enabled = !busy,
                loading = busy,
                colors = colors,
                modifier = Modifier.weight(1f),
                onClick = onConfirm,
            )
        }
    }
}

@Composable
private fun ReloadDone(
    receipt: WalletReloadReceipt,
    colors: OperationalColors,
    onDone: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(colors.accentLime),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = colors.accentInk,
                modifier = Modifier.size(18.dp),
            )
        }
        Text("Recarga registrada", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
    }
    Column {
        Text("Saldo nuevo", fontSize = 13.sp, color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
        RollingText(
            text = moneyLabel(receipt.newBalance),
            style = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.4).sp),
            color = colors.textPrimary,
        )
        Text(
            "${moneyLabel(receipt.previousBalance)} + ${moneyLabel(receipt.amount)}",
            fontSize = 13.sp,
            color = colors.textSecondary,
        )
    }
    ActionPill("Listo", enabled = true, loading = false, colors = colors, primary = false, onClick = onDone)
}

// Mismo nombre, mismo color: el cajero distingue a dos Sofías de un vistazo.
private val InitialsPalette =
    listOf(
        Color(0xFFE9B8C8) to Color(0xFF3A1622),
        Color(0xFFB8D4E9) to Color(0xFF10283A),
        Color(0xFFF4C98A) to Color(0xFF3A2410),
        Color(0xFFD5E8A6) to Color(0xFF182008),
    )

@Composable
private fun ClientInitials(name: String) {
    val initials =
        name
            .split(' ')
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "?" }
    val (bg, fg) = InitialsPalette[Math.floorMod(name.hashCode(), InitialsPalette.size)]
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = fg, fontWeight = FontWeight.Black, fontSize = 14.sp)
    }
}
