package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import com.vaiinilla.app.ui.components.RollingText
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.moneyLabel

/** N11 «Movimientos del cajón»: cuánto debe haber y qué entró o salió del cajón hoy. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashDrawerSheet(
    expectedCash: String?,
    colors: OperationalColors,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        CashDrawerContent(expectedCash = expectedCash, colors = colors, modifier = Modifier.navigationBarsPadding())
    }
}

/**
 * Lo que debe haber viene de `efectivo_esperado`. El servidor todavía no guarda entradas ni salidas del cajón con
 * su motivo, así que no hay botones para sacar o meter dinero: serían una promesa que la app no cumple.
 */
@Composable
internal fun CashDrawerContent(
    expectedCash: String?,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        Text(
            "Movimientos del cajón",
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.8).sp,
            lineHeight = 32.sp,
            color = colors.textPrimary,
        )
        Column(
            modifier =
                Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .arrive(0)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.cardBackground)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text("Debe haber en el cajón", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
            if (expectedCash != null) {
                RollingText(
                    text = moneyLabel(expectedCash),
                    style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.2).sp),
                    color = colors.textPrimary,
                )
                Text(
                    "Lo calcula el servidor; compáralo con lo que cuentes.",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            } else {
                Text("—", fontSize = 34.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
                Text(
                    "El servidor aún no dijo cuánto debe haber.",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                )
            }
        }
        Text(
            "HOY · MÁS NUEVO ARRIBA",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .arrive(1)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.cardBackground)
                    .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(colors.cardInner),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.SwapHoriz, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Todavía no se pueden anotar entradas ni salidas",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                )
                Text(
                    "Si sacas dinero para una compra o para cambio, apúntalo aparte con su motivo para explicarlo al cerrar caja.",
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
