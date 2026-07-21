package com.vaiinilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.theme.Coral
import com.vaiinilla.app.ui.theme.Ink
import com.vaiinilla.app.ui.theme.Lime

@Composable
fun VaiinillaBottomNav(
    activeTab: StudentTab,
    cartCount: Int,
    onMenu: () -> Unit,
    onCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            color = Ink,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
        ) {
            Row(
                modifier = Modifier.padding(7.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                NavItem(
                    modifier = Modifier.weight(1f),
                    label = "Menú",
                    icon = Icons.Outlined.Home,
                    active = activeTab == StudentTab.MENU,
                    onClick = onMenu,
                )
                NavItem(
                    modifier = Modifier.weight(1f),
                    label = "Asistente",
                    icon = Icons.Outlined.AutoAwesome,
                    active = false,
                    onClick = null,
                )
                NavItem(
                    modifier = Modifier.weight(1f),
                    label = "Pedidos",
                    icon = Icons.Outlined.ReceiptLong,
                    active = false,
                    onClick = null,
                )
                NavItem(
                    modifier = Modifier.weight(1f),
                    label = "Cartera",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    active = false,
                    onClick = null,
                )
                NavItem(
                    modifier = Modifier.weight(1f),
                    label = "Carrito",
                    icon = Icons.Outlined.ShoppingCart,
                    active = activeTab == StudentTab.CART,
                    badge = cartCount,
                    onClick = onCart,
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    badge: Int = 0,
) {
    val foreground = if (active) Ink else Color(0xFFA8AAA3)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .background(if (active) Lime else Color.Transparent)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = foreground,
                    modifier = Modifier.size(21.dp),
                )
                if (badge > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(start = 13.dp)
                            .size(17.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Coral),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = badge.coerceAtMost(99).toString(),
                            color = Ink,
                            fontSize = 9.sp,
                            lineHeight = 9.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
            Text(
                text = label,
                color = foreground,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

enum class StudentTab {
    MENU,
    CART,
}
