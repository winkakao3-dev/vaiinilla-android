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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.MoneyInput
import com.vaiinilla.app.domain.model.WalletClient
import com.vaiinilla.app.domain.model.WalletReloadReceipt
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.RollingText
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import java.math.BigDecimal

/** Billetes con los que suele recargar la gente. */
private val ReloadQuickAmounts = listOf(50, 100, 200, 500)

/** "Recargar $100 a Ana": el botón repite a quién y cuánto antes de mover el dinero. */
internal fun walletReloadLabel(
    amount: BigDecimal?,
    client: WalletClient,
): String =
    if (amount == null || amount.signum() <= 0) {
        "Recargar a ${client.name}"
    } else {
        "Recargar ${moneyLabel(Money.format(amount))} a ${client.name}"
    }

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
        )
    }
}

/**
 * Recarga en dos pasos: primero se encuentra al cliente (buscar o escanear su QR) y después
 * aparece el monto. Así no se escribe dinero antes de saber a quién va.
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
) {
    var query by remember { mutableStateOf("") }
    var searched by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<WalletClient?>(null) }
    var amountText by remember { mutableStateOf("") }
    // Un recibo de antes de abrir la hoja no es el de esta recarga.
    val receiptAtOpen = remember { receipt?.movementId }
    val done = receipt?.takeIf { it.movementId != receiptAtOpen }
    val reduced = reducedMotion()
    val step =
        when {
            done != null -> 2
            selected != null -> 1
            else -> 0
        }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            if (reduced) {
                fadeIn(snap()) togetherWith fadeOut(snap())
            } else {
                val direction = if (targetState > initialState) 1 else -1
                (
                    slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 500f)) { it / 5 * direction } +
                        fadeIn(spring(stiffness = 400f))
                ) togetherWith fadeOut(spring(stiffness = 600f))
            }
        },
        label = "wallet-reload-step",
    ) { current ->
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (current) {
                0 ->
                    FindClientStep(
                        query = query,
                        onQueryChange = {
                            query = it
                            searched = false
                        },
                        searched = searched,
                        searching = searching,
                        clients = clients,
                        colors = colors,
                        onSearch = {
                            searched = true
                            onSearch(query)
                        },
                        onScan = onScan,
                        onPick = { selected = it },
                    )
                1 ->
                    selected?.let { client ->
                        AmountStep(
                            client = client,
                            amountText = amountText,
                            onAmountChange = { amountText = it },
                            canReload = canReload,
                            colors = colors,
                            onChangeClient = {
                                selected = null
                                amountText = ""
                            },
                            onReload = { amount -> onReload(client.userId, Money.format(amount)) },
                        )
                    }
                else ->
                    done?.let { ReloadDone(it, colors, onDone) }
            }
        }
    }
}

@Composable
private fun FindClientStep(
    query: String,
    onQueryChange: (String) -> Unit,
    searched: Boolean,
    searching: Boolean,
    clients: List<WalletClient>,
    colors: OperationalColors,
    onSearch: () -> Unit,
    onScan: () -> Unit,
    onPick: (WalletClient) -> Unit,
) {
    Column {
        Text("Recargar saldo", fontSize = 13.sp, color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
        Text("¿A quién?", fontSize = 30.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OperationalField(
            value = query,
            onValueChange = onQueryChange,
            label = "Nombre o identificador",
            colors = colors,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier =
                Modifier
                    .physicalPress(scale = PhysicalPressScale.Small, onClick = onScan)
                    .semantics { contentDescription = "Escanear QR del cliente" }
                    .size(58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.cardInner)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = colors.textPrimary)
        }
    }
    ChipButton(
        label = if (searching) "Buscando…" else "Buscar",
        enabled = !searching && query.trim().length >= 2,
        colors = colors,
        modifier = Modifier.fillMaxWidth(),
        onClick = onSearch,
    )
    if (searched && !searching && clients.isEmpty()) {
        Text(
            "Nadie con ese nombre en este lugar. Prueba con el identificador o escanea su QR.",
            fontSize = 13.sp,
            color = colors.textSecondary,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        clients.forEach { client ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .physicalPress(scale = PhysicalPressScale.Nav) { onPick(client) }
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.cardInner)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(client.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                    client.contextualId?.let {
                        Text(it, fontSize = 12.sp, color = colors.textSecondary, maxLines = 1)
                    }
                }
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun AmountStep(
    client: WalletClient,
    amountText: String,
    onAmountChange: (String) -> Unit,
    canReload: Boolean,
    colors: OperationalColors,
    onChangeClient: () -> Unit,
    onReload: (BigDecimal) -> Unit,
) {
    val amount = walletReloadAmount(amountText)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Recargar a", fontSize = 13.sp, color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
            Text(client.name, fontSize = 24.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
            client.contextualId?.let { Text(it, fontSize = 12.sp, color = colors.textSecondary) }
        }
        Text(
            "Cambiar",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = colors.textPrimary,
            modifier =
                Modifier
                    .physicalPress(scale = PhysicalPressScale.Small, onClick = onChangeClient)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ReloadQuickAmounts.forEach { bill ->
            val value = bill.toBigDecimal()
            ChipButton(
                label = "$$bill",
                enabled = true,
                colors = colors,
                modifier = Modifier.weight(1f),
                selected = amount != null && amount.compareTo(value) == 0,
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
    if (!canReload) {
        Text(
            "Abre la caja para registrar recargas en efectivo.",
            fontSize = 13.sp,
            color = colors.textSecondary,
        )
    }
    ActionPill(
        label = walletReloadLabel(amount, client),
        enabled = canReload && amount != null,
        loading = false,
        colors = colors,
    ) { amount?.let(onReload) }
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
