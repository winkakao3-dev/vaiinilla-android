package com.vaiinilla.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

@Composable
fun ActiveOrderBanner(
    folio: String,
    statusLabel: String,
    itemCount: Int,
    destination: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val isPreparing = statusLabel.equals("Preparando", ignoreCase = true)
    val badgeBackground = if (isPreparing) colors.yolk else Color.White.copy(alpha = 0.33f)
    val badgeText = if (isPreparing) colors.ink else colors.paper
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .physicalPress(onClick = onClick),
        color = colors.ink,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = "Pedido activo",
                        color = colors.muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp,
                    )
                    Text(
                        text = "#$folio",
                        color = colors.paper,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Surface(
                    color = badgeBackground,
                    shape = RoundedCornerShape(99.dp),
                ) {
                    Text(
                        text = statusLabel.uppercase(),
                        color = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("$itemCount productos", color = colors.muted, fontSize = 12.sp)
                Text(destination, color = colors.muted, fontSize = 12.sp)
            }
        }
    }
}
