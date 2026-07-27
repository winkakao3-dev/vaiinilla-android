package com.vaiinilla.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.WalletPrimaryButton
import com.vaiinilla.app.ui.components.WalletScreenShell
import com.vaiinilla.app.ui.components.WalletSectionHead
import com.vaiinilla.app.ui.components.WalletSubflowTopBar
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.wallet.WalletUiState

private enum class AddMoneyMethod { Card, Spei }

@Composable
fun WalletAddMoneyScreen(
    walletState: WalletUiState,
    initialMethod: String = "card",
    onBack: () -> Unit,
    onCreditBalance: (Int) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val context = LocalContext.current
    var amount by rememberSaveable { mutableIntStateOf(100) }
    var method by rememberSaveable {
        mutableStateOf(if (initialMethod == "spei") AddMoneyMethod.Spei else AddMoneyMethod.Card)
    }
    val chipAmounts = listOf(50, 100, 200, 500)

    WalletScreenShell(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
        ) {
            WalletSubflowTopBar(title = "Añadir dinero", onBack = onBack)

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
            ) {
                Text(
                    "Monto a agregar",
                    color = colors.muted,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                )
                Text(
                    "$$amount",
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 48.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 6.dp),
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    items(chipAmounts) { chip ->
                        AmountChip(
                            label = "$$chip",
                            selected = amount == chip,
                            onClick = { amount = chip },
                        )
                    }
                }

                WalletSectionHead(title = "¿Cómo quieres agregarlo?")

                PaymentChoiceRow(
                    brand = "VISA",
                    title = "Tarjeta •••• 4242",
                    subtitle = "Acreditación inmediata en la demo",
                    selected = method == AddMoneyMethod.Card,
                    onClick = { method = AddMoneyMethod.Card },
                )
                Spacer(Modifier.height(8.dp))
                PaymentChoiceRow(
                    brand = "SPEI",
                    title = "Transferencia",
                    subtitle = "Usa tu CLABE y referencia personal",
                    selected = method == AddMoneyMethod.Spei,
                    brandUsesAccent = true,
                    onClick = { method = AddMoneyMethod.Spei },
                )

                if (method == AddMoneyMethod.Card) {
                    CardPreviewStrip(
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    WalletPrimaryButton(
                        text = "Agregar al saldo",
                        onClick = {
                            onCreditBalance(amount)
                            onBack()
                        },
                        modifier = Modifier.padding(top = 20.dp, bottom = 24.dp),
                    )
                } else {
                    SpeiDetails(
                        context = context,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    WalletPrimaryButton(
                        text = "Simular transferencia recibida",
                        onClick = {
                            onCreditBalance(amount)
                            onBack()
                        },
                        modifier = Modifier.padding(top = 20.dp, bottom = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        onClick = onClick,
        color = if (selected) colors.accent else colors.paper2,
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = if (selected) colors.accentInk else colors.ink,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun PaymentChoiceRow(
    brand: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    brandUsesAccent: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (selected) {
                        Modifier.border(2.dp, colors.accent, RoundedCornerShape(18.dp))
                    } else {
                        Modifier
                    },
                ),
        onClick = onClick,
        color = if (selected) colors.paper2 else colors.paper,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(42.dp)
                        .background(
                            if (brandUsesAccent) colors.accent else colors.ink,
                            RoundedCornerShape(12.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    brand.take(4),
                    color = if (brandUsesAccent) colors.accentInk else colors.paper,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(subtitle, color = colors.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
            }
            if (selected) {
                Text("✓", color = colors.accent, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun CardPreviewStrip(modifier: Modifier = Modifier) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.ink, RoundedCornerShape(20.dp))
                .padding(18.dp),
    ) {
        Column {
            Text("VISA", color = colors.paper, fontWeight = FontWeight.Black, fontSize = 11.sp)
            Text(
                "DANI ÁLVAREZ · •••• 4242 · vence 08/29",
                color = colors.paper.copy(alpha = 0.75f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SpeiDetails(
    context: Context,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val clabe = "646180157034852019"
    val reference = "UTCH241087"

    Column(modifier = modifier) {
        SpeiField(label = "Banco receptor", value = "STP")
        Spacer(Modifier.height(10.dp))
        SpeiField(
            label = "CLABE",
            value = clabe,
            onCopy = { copyToClipboard(context, "CLABE", clabe) },
        )
        Spacer(Modifier.height(10.dp))
        SpeiField(
            label = "Referencia",
            value = reference,
            onCopy = { copyToClipboard(context, "Referencia", reference) },
        )
        Text(
            "La transferencia no paga el producto directamente. Primero se acredita al saldo y después eliges Saldo al confirmar.",
            color = colors.muted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun SpeiField(
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.paper2,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    value,
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (onCopy != null) {
                TextButton(onClick = onCopy) {
                    Text("Copiar", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun copyToClipboard(
    context: Context,
    label: String,
    value: String,
) {
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(context, "$label copiado", Toast.LENGTH_SHORT).show()
    }
}
