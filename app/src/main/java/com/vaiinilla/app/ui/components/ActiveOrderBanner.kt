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
import com.vaiinilla.app.ui.theme.Cream
import com.vaiinilla.app.ui.theme.Ink
import com.vaiinilla.app.ui.theme.MutedInk

@Composable
fun ActiveOrderBanner(
    folio: String,
    statusLabel: String,
    itemCount: Int,
    destination: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .physicalPress(onClick = onClick),
        color = Ink,
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
                        text = "PEDIDO ACTIVO",
                        color = Color(0xFFAAAAAA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                    )
                    Text(
                        text = "#$folio",
                        color = Cream,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Surface(
                    color = Color(0x33FFFFFF),
                    shape = RoundedCornerShape(99.dp),
                ) {
                    Text(
                        text = statusLabel,
                        color = Cream,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("$itemCount productos", color = MutedInk, fontSize = 12.sp)
                Text(destination, color = MutedInk, fontSize = 12.sp)
            }
        }
    }
}
