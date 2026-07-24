package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.VaiinillaBottomNav
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.cartItemCount
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

@Composable
fun WalletScreen(
    state: OrderFlowUiState,
    balance: Int,
    onAddMoney: () -> Unit,
    onPaymentMethods: () -> Unit,
    onAccount: () -> Unit,
    onMenu: () -> Unit,
    onAssistant: () -> Unit,
    onOrders: () -> Unit,
    onCart: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("Cartera", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 22.sp)
            }

            item {
                BalanceCard(balance = balance, colors = colors, onAddMoney = onAddMoney)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    WalletActionCard(
                        icon = Icons.Outlined.Add,
                        title = "Añadir dinero",
                        subtitle = "Tarjeta o transferencia",
                        modifier = Modifier.weight(1f),
                        onClick = onAddMoney,
                    )
                    WalletActionCard(
                        icon = Icons.Outlined.CreditCard,
                        title = "Métodos de pago",
                        subtitle = "Tarjetas y SPEI",
                        modifier = Modifier.weight(1f),
                        onClick = onPaymentMethods,
                    )
                    WalletActionCard(
                        icon = Icons.Outlined.Person,
                        title = "Mi cuenta",
                        subtitle = "Perfil y matrícula",
                        modifier = Modifier.weight(1f),
                        onClick = onAccount,
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MiniStatCard(value = "$29", label = "Cashback acumulado", modifier = Modifier.weight(1f))
                    MiniStatCard(value = "1", label = "Pedidos realizados", modifier = Modifier.weight(1f))
                }
            }

            item {
                SectionHead(title = "Métodos de pago", action = "Administrar", onAction = onPaymentMethods)
            }

            item {
                PaymentMethodRow(
                    brand = "VISA",
                    title = "•••• 4242",
                    subtitle = "Vence 08/29 · disponible para pagar pedidos",
                    selected = true,
                )
                Spacer(Modifier.height(8.dp))
                PaymentMethodRow(
                    brand = "SPEI",
                    title = "Transferencia bancaria",
                    subtitle = "Sólo para añadir dinero al saldo",
                    selected = false,
                    brandColor = LocalVaiinillaColors.current.paper2,
                )
            }

            item {
                SectionHead(title = "Movimientos recientes", action = null)
            }

            item {
                MovementRow(
                    icon = "↗",
                    title = "Recarga",
                    subtitle = "Hoy, 09:20",
                    amount = "+$200",
                    positive = true,
                )
                Spacer(Modifier.height(8.dp))
                MovementRow(
                    icon = "↙",
                    title = "Pedido #3472",
                    subtitle = "Hoy, 11:42",
                    amount = "-$101",
                    positive = false,
                )
            }
        }

        VaiinillaBottomNav(
            activeTab = StudentTab.WALLET,
            cartCount = state.cartItemCount,
            onMenu = onMenu,
            onAssistant = onAssistant,
            onOrders = onOrders,
            onWallet = {},
            onCart = onCart,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun BalanceCard(
    balance: Int,
    colors: com.vaiinilla.app.ui.theme.VaiinillaColors,
    onAddMoney: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(colors.accent),
    ) {
        Text(
            text = "$",
            color = colors.accentInk.copy(alpha = 0.08f),
            fontSize = 120.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = (-12).dp),
        )
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "Saldo disponible",
                color = colors.accentInk.copy(alpha = 0.65f),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
            )
            Text(
                "$$balance",
                color = colors.accentInk,
                fontWeight = FontWeight.Black,
                fontSize = 50.sp,
                lineHeight = 52.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "UTCH-241087",
                color = colors.accentInk.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            Button(
                onClick = onAddMoney,
                colors = ButtonDefaults.buttonColors(containerColor = colors.paper, contentColor = colors.accentInk),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(48.dp),
            ) {
                Text("Añadir dinero", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun WalletActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = modifier,
        onClick = onClick,
        color = colors.paper2,
        shape = RoundedCornerShape(19.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(20.dp))
            Text(title, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            Text(subtitle, color = colors.muted, fontSize = 10.sp, lineHeight = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun MiniStatCard(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = modifier,
        color = colors.paper2,
        shape = RoundedCornerShape(19.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(value, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(label, color = colors.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun SectionHead(title: String, action: String?, onAction: (() -> Unit)? = null) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(action, color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PaymentMethodRow(
    brand: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    brandColor: Color = LocalVaiinillaColors.current.ink,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.paper2,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brandColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(brand.take(4), color = if (brandColor == colors.ink) colors.paper else colors.ink, fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(subtitle, color = colors.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
            }
            if (selected) {
                Text("✓", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun MovementRow(
    icon: String,
    title: String,
    subtitle: String,
    amount: String,
    positive: Boolean,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.paper2,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accent.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(icon, color = colors.ink, fontWeight = FontWeight.Black)
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(subtitle, color = colors.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
            }
            Text(
                amount,
                color = if (positive) Color(0xFF3D5A1E) else colors.ink,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
            )
        }
    }
}
