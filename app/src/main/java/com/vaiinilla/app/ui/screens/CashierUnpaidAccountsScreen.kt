package com.vaiinilla.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vaiinilla.app.core.venue.LocalVenueContext
import com.vaiinilla.app.domain.admin.AdminUnpaidAccount
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.MoneyInput
import com.vaiinilla.app.domain.model.TipChoice
import com.vaiinilla.app.domain.model.TipPercents
import com.vaiinilla.app.domain.model.amountFor
import com.vaiinilla.app.domain.model.asTipWire
import com.vaiinilla.app.domain.repository.AccountCollection
import com.vaiinilla.app.domain.repository.AccountPaymentMethod
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.operational.CashierUnpaidUiState
import com.vaiinilla.app.ui.operational.unpaidAccountKey
import java.math.BigDecimal

private val UnpaidCoral = Color(0xFFE5645A)

/** Cuánto cambio dar; null si lo recibido no alcanza para lo que falta más la propina, o no es un monto. */
internal fun unpaidChange(
    received: String,
    remaining: String,
    tip: BigDecimal = BigDecimal.ZERO,
): BigDecimal? {
    val paid = MoneyInput.parse(received) ?: return null
    val due = remaining.toBigDecimalOrNull() ?: return null
    return (paid - due - tip).takeIf { it.signum() >= 0 }
    /** La cuenta, el método, lo recibido (solo efectivo) y la propina (`propina`, null sin propina). */
    onCharge: (AdminUnpaidAccount, AccountPaymentMethod, String?, String?) -> Unit,
    onDismissCollected: () -> Unit,
    onClearChargeError: () -> Unit,
) {
    var charging by remember { mutableStateOf<Pair<AdminUnpaidAccount, AccountPaymentMethod>?>(null) }
    BackHandler(onBack = onBack)
    val accounts = state.accounts?.accounts.orEmpty()
    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "encabezado") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.cardBackground)
                                .clickable(onClick = onBack)
                                .semantics { contentDescription = "Volver a Caja" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("CAJA", fontSize = 11.sp, fontWeight = FontWeight.Black, color = colors.textSecondary)
                        Text(
                            "Cuentas por cobrar",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.textPrimary,
                        )
                    }
                }
            }
            item(key = "explica") {
                Text(
                    "Mesas y ${LocalVenueContext.current.payAtEndSpaces} que pidieron pagar al final y no se han cobrado, " +
                        "también las de un turno que " +
                        "ya se cerró. Mientras haya una, la caja no se puede cerrar.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp,
                    color = colors.textSecondary,
                )
            }
            state.lastCollected?.let { collected ->
                item(key = "cobrado") {
                    CollectedCard(collected.spaceName, collected.collection, colors, onDismissCollected)
                }
            }
            state.chargeError?.let { message ->
                item(key = "error-cobro") {
                    Text(
                        message,
                        color = UnpaidCoral,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onClearChargeError),
                    )
                }
            }
            when {
                state.accounts == null && state.error != null ->
                    item(key = "error") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(state.error, color = UnpaidCoral, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            ActionPill(label = "Reintentar", enabled = true, loading = false, colors = colors) {
                                onRefresh()
                            }
                        }
                    }
                state.accounts == null ->
                    item(key = "cargando") {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = colors.textPrimary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                accounts.isEmpty() ->
                    item(key = "vacio") {
                        Text(
                            "No hay cuentas por cobrar. Ya puedes cerrar la caja.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                else ->
                    items(accounts, key = { unpaidAccountKey(it) }) { account ->
                        UnpaidAccountCard(
                            account = account,
                            colors = colors,
                            busy = state.charging == unpaidAccountKey(account),
                            enabled = state.charging == null,
                            onCash = { charging = account to AccountPaymentMethod.CASH },
                            onTerminal = { charging = account to AccountPaymentMethod.TERMINAL },
                        )
                    }
            }
        }
    }
    charging?.let { (account, method) ->
        ChargeUnpaidDialog(
            account = account,
            method = method,
            colors = colors,
            onDismiss = { charging = null },
        ) { received, tip ->
            charging = null
            onCharge(account, method, received, tip)
        }
    }
}

@Composable
private fun UnpaidAccountCard(
    account: AdminUnpaidAccount,
    colors: OperationalColors,
    busy: Boolean,
    enabled: Boolean,
    onCash: () -> Unit,
    onTerminal: () -> Unit,
) {
    val paid = account.paid.toBigDecimalOrNull()?.signum() == 1
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.cardBackground)
                .border(1.dp, if (account.abandoned) UnpaidCoral else colors.cardBorder, RoundedCornerShape(22.dp))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    account.spaceName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        if (account.orders == 1) "1 pedido" else "${account.orders} pedidos",
                        account.orderLines
                            .map { it.folio }
                            .filter { it.isNotBlank() }
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString(", ") { "#$it" },
                        account.hours.takeIf { it > 0 }?.let { "hace $it h" },
                    ).joinToString(" · "),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                )
            }
            if (account.abandoned) {
                Text(
                    "Turno cerrado",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = UnpaidCoral,
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(UnpaidCoral.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
        if (paid) {
            AmountRow("Total", moneyLabel(account.total), colors)
            AmountRow("Ya abonado", moneyLabel(account.paid), colors)
        }
        AmountRow("Por cobrar", moneyLabel(account.remaining), colors, strong = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionPill(
                label = "Efectivo",
                enabled = enabled,
                loading = busy,
                colors = colors,
                modifier = Modifier.weight(1f),
                onClick = onCash,
            )
            ActionPill(
                label = "Terminal",
                enabled = enabled,
                loading = false,
                colors = colors,
                outlined = true,
                modifier = Modifier.weight(1f),
                onClick = onTerminal,
            )
        }
    }
}

@Composable
private fun AmountRow(
    label: String,
    value: String,
    colors: OperationalColors,
    strong: Boolean = false,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            fontSize = if (strong) 15.sp else 13.5.sp,
            fontWeight = if (strong) FontWeight.Black else FontWeight.SemiBold,
            color = if (strong) colors.textPrimary else colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            fontSize = if (strong) 18.sp else 14.sp,
            fontWeight = FontWeight.Black,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun CollectedCard(
    spaceName: String,
    collection: AccountCollection,
    colors: OperationalColors,
    onDismiss: () -> Unit,
) {
    val change = collection.change.toBigDecimalOrNull()?.signum() == 1
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.accentLime)
                .clickable(onClick = onDismiss)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            if (collection.settled) "$spaceName cobrada" else "Cobro registrado en $spaceName",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = colors.accentInk,
        )
        Text(
            buildString {
                append(moneyLabel(collection.total))
                append(if (collection.method == AccountPaymentMethod.TERMINAL) " con terminal" else " en efectivo")
                if (change) append(" · cambio ${moneyLabel(collection.change)}")
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.accentInk,
        )
        Text("Toca para ocultar", fontSize = 12.sp, color = colors.accentInk.copy(alpha = 0.7f))
    }
}

/** Pide la propina y lo recibido en efectivo (con el cambio), o confirma el cobro con terminal. */
@Composable
private fun ChargeUnpaidDialog(
    account: AdminUnpaidAccount,
    method: AccountPaymentMethod,
    colors: OperationalColors,
    onDismiss: () -> Unit,
    onConfirm: (received: String?, tip: String?) -> Unit,
) {
    var received by remember { mutableStateOf("") }
    var tipChoice by remember { mutableStateOf<TipChoice>(TipChoice.None) }
    val cash = method == AccountPaymentMethod.CASH
    val remaining = account.remaining.toBigDecimalOrNull() ?: BigDecimal.ZERO
    // El servidor rechaza propina sin nada que cobrar: con la cuenta ya cubierta por abonos no se ofrece.
    val allowTip = remaining.signum() > 0
    val tipProblem = if (allowTip) unpaidTipProblem(tipChoice, account.remaining) else null
    val tipAmount = if (allowTip) tipChoice.amountFor(remaining) else BigDecimal.ZERO
    // Lo que paga la mesa: lo que falta más la propina. `total_esperado` sigue siendo solo lo que falta.
    val chargeLabel = Money.format(remaining + tipAmount)
    val change = if (cash) unpaidChange(received, account.remaining, tipAmount) else null
    val ready = tipProblem == null && (!cash || change != null)
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = colors.background, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Cobrar ${account.spaceName}",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                )
                Text(
                    when {
                        cash && tipAmount.signum() > 0 ->
                            "Por cobrar: ${moneyLabel(account.remaining)} más ${moneyLabel(Money.format(tipAmount))} " +
                                "de propina, ${moneyLabel(chargeLabel)} en efectivo."
                        cash -> "Por cobrar: ${moneyLabel(account.remaining)} en efectivo."
                        else -> "Cobra ${moneyLabel(chargeLabel)} en la terminal y confirma cuando pase."
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                )
                if (allowTip) {
                    Text("Propina", fontSize = 13.sp, fontWeight = FontWeight.Black, color = colors.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        ChipButton(
                            label = "Sin",
                            enabled = true,
                            colors = colors,
                            modifier = Modifier.weight(1f),
                            selected = tipChoice == TipChoice.None,
                        ) { tipChoice = TipChoice.None }
                        TipPercents.forEach { percent ->
                            ChipButton(
                                label = "$percent%",
                                enabled = true,
                                colors = colors,
                                modifier = Modifier.weight(1f),
                                selected = tipChoice == TipChoice.Percent(percent),
                            ) { tipChoice = TipChoice.Percent(percent) }
                        }
                        ChipButton(
                            label = "Otro",
                            enabled = true,
                            colors = colors,
                            modifier = Modifier.weight(1f),
                            selected = tipChoice is TipChoice.Custom,
                        ) { tipChoice = TipChoice.Custom((tipChoice as? TipChoice.Custom)?.amount.orEmpty()) }
                    }
                    val custom = tipChoice as? TipChoice.Custom
                    if (custom != null) {
                        OperationalField(
                            value = custom.amount,
                            onValueChange = { tipChoice = TipChoice.Custom(MoneyInput.sanitize(it)) },
                            label = "Monto de la propina",
                            colors = colors,
                            keyboardType = KeyboardType.Decimal,
                            isError = tipProblem != null,
                            supporting = tipProblem,
                        )
                    }
                }
                if (cash) {
                    OperationalField(
                        value = received,
                        onValueChange = { received = MoneyInput.sanitize(it) },
                        label = "Recibido",
                        colors = colors,
                        placeholder = moneyLabel(chargeLabel),
                        keyboardType = KeyboardType.Decimal,
                        isError = received.isNotEmpty() && change == null,
                        supporting =
                            when {
                                change != null -> "Cambio: ${moneyLabel(change.toPlainString())}"
                                received.isNotEmpty() && tipAmount.signum() > 0 ->
                                    "No alcanza para lo que falta más la propina."
                                received.isNotEmpty() -> "No alcanza para lo que falta."
                                else -> null
                            },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionPill(
                        label = "Cancelar",
                        enabled = true,
                        loading = false,
                        colors = colors,
                        outlined = true,
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                    )
                    ActionPill(
                        label = if (cash) "Cobrar" else "Ya pasó",
                        enabled = ready,
                        loading = false,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                    ) { onConfirm(if (cash) MoneyInput.toWire(received) else null, tipAmount.asTipWire()) }
                }
            }
        }
    }
}