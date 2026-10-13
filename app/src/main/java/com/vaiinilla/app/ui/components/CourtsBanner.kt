package com.vaiinilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.repository.SpaceAvailability
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/**
 * Las canchas dentro del menú: una franja de una línea con cuántas están libres. El menú es de comida; el detalle de
 * cada cancha vive en la renta, que se abre al tocar la franja.
 */
@Composable
fun CourtsBanner(
    courts: List<SpaceAvailability>,
    modifier: Modifier = Modifier,
    /** Abre la renta de canchas; sin él, la franja solo informa. */
    onOpen: (() -> Unit)? = null,
) {
    if (courts.isEmpty()) return
    val colors = LocalVaiinillaColors.current
    val free = courts.count { it.isFreeNow }
    val dot = animateSelectionColor(if (free > 0) colors.accent2 else colors.muted, "courts-banner-dot")
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.paper2)
                .physicalPress(enabled = onOpen != null) { onOpen?.invoke() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(10.dp))
        NumberTicker(value = free, label = "courts-banner-free") { count ->
            Text("$count", color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
        Text(
            when {
                free == 1 -> " cancha libre"
                else -> " canchas libres"
            } + " de ${courts.size}",
            color = colors.ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (onOpen != null) {
            Text("Rentar", color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.ink,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
