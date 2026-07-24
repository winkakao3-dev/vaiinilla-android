package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.WalletPrimaryButton
import com.vaiinilla.app.ui.components.WalletScreenShell
import com.vaiinilla.app.ui.components.WalletSubflowTopBar
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.wallet.SavedCard
import com.vaiinilla.app.ui.wallet.WalletUiState

@Composable
fun WalletAddCardScreen(
    walletState: WalletUiState,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    var holder by rememberSaveable { mutableStateOf("DANI ÁLVAREZ") }
    var number by rememberSaveable { mutableStateOf("4242 4242 4242 4242") }
    var expiry by rememberSaveable { mutableStateOf("08/29") }
    var cvv by rememberSaveable { mutableStateOf("123") }

    WalletScreenShell(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            WalletSubflowTopBar(title = "Agregar tarjeta", onBack = onBack)

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.ink, RoundedCornerShape(24.dp))
                        .padding(22.dp),
                ) {
                    Column {
                        Text("VAIINILLA · VISA", color = colors.paper, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        Text(
                            "•••• •••• •••• ${number.filter { it.isDigit() }.takeLast(4).ifBlank { "4242" }}",
                            color = colors.paper,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 18.dp),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(holder.uppercase(), color = colors.paper.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text(expiry, color = colors.paper.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                WalletField(label = "Nombre del titular", value = holder, onValueChange = { holder = it })
                Spacer(Modifier.height(12.dp))
                WalletField(label = "Número de tarjeta", value = number, onValueChange = { number = it })
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WalletField(
                        label = "Vencimiento",
                        value = expiry,
                        onValueChange = { expiry = it },
                        modifier = Modifier.weight(1f),
                    )
                    WalletField(
                        label = "CVV",
                        value = cvv,
                        onValueChange = { cvv = it },
                        modifier = Modifier.weight(1f),
                    )
                }

                Text(
                    "Interfaz demostrativa. Los datos no se procesan ni se envían a una pasarela bancaria.",
                    color = colors.muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 16.dp),
                )

                WalletPrimaryButton(
                    text = "Guardar tarjeta",
                    onClick = {
                        val lastFour = number.filter { it.isDigit() }.takeLast(4).ifBlank { "4242" }
                        val newCard = SavedCard(
                            brand = "VISA",
                            lastFour = lastFour,
                            holder = holder.uppercase(),
                            expiry = expiry,
                        )
                        if (walletState.cards.none { it.lastFour == newCard.lastFour }) {
                            walletState.cards = walletState.cards + newCard
                        }
                        onSaved()
                    },
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun WalletField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = modifier) {
        Text(label, color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.paper2,
                unfocusedContainerColor = colors.paper2,
                focusedTextColor = colors.ink,
                unfocusedTextColor = colors.ink,
                focusedBorderColor = colors.line,
                unfocusedBorderColor = colors.line,
            ),
        )
    }
}
