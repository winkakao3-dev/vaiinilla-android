package com.vaiinilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

@Composable
fun TestOnlyModeCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .then(
                    if (enabled) {
                        Modifier.border(2.dp, colors.accent, shape)
                    } else {
                        Modifier
                    },
                ).clickable { onEnabledChange(!enabled) },
        color = if (enabled) colors.accent.copy(alpha = 0.18f) else colors.paper2,
        shape = shape,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Solo pruebas",
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                )
                Text(
                    "Sin API, Firebase ni red. Solo fixtures locales del dispositivo.",
                    color = colors.muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = colors.paper,
                        checkedTrackColor = colors.ink,
                        uncheckedThumbColor = colors.paper,
                        uncheckedTrackColor = colors.muted.copy(alpha = 0.35f),
                    ),
            )
        }
    }
}

@Composable
fun TestOnlyModeBadge(modifier: Modifier = Modifier) {
    val colors = LocalVaiinillaColors.current
    Text(
        text = "SOLO PRUEBAS",
        color = colors.accentInk,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.8.sp,
        modifier =
            modifier
                .background(colors.accent, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
    )
}
