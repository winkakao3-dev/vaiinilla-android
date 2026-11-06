package com.vaiinilla.app.ui.hotel.guest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/** Cabecera del menú de una habitación (H02): dónde se entrega y «Room service» en grande, sin portada ni buscador. */
@Composable
fun HotelMenuHeader(
    roomName: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier = modifier.fillMaxWidth().statusBarsPadding().padding(top = 12.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "$roomName · se lleva a tu puerta",
            color = colors.muted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
        )
        Text(
            "Room service",
            color = colors.ink,
            fontSize = 36.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp,
        )
    }
}

/** La barra lima de abajo con lo que lleva el pedido (H02): «Ver pedido · 2» y el total. */
@Composable
fun HotelCartBar(
    count: Int,
    total: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val shape = RoundedCornerShape(29.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .height(58.dp)
                .testTag("hotel_cart_bar")
                .physicalPress(onClick = {
                    haptics.click()
                    onClick()
                })
                .clip(shape)
                .background(colors.accent)
                .semantics {
                    role = Role.Button
                    contentDescription = "Ver pedido: $count, $total"
                }.padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Ver pedido · $count", color = colors.accentInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Text(
            total,
            color = colors.accentInk,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
    }
}
