package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.arrive
import java.time.DayOfWeek
import java.time.LocalDate

private val WeekLetters = listOf("L", "M", "M", "J", "V", "S", "D")

/** El día de la semana de hoy, de 0 (lunes) a 6 (domingo), para marcarlo en la gráfica. */
internal fun weekdayIndex(date: LocalDate): Int = date.dayOfWeek.value - DayOfWeek.MONDAY.value

/** N07 «Mi turno»: el resumen del turno y las propinas de quien usa el teléfono. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StaffShiftSheet(
    modeLabel: String,
    placeName: String,
    colors: OperationalColors,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        StaffShiftContent(
            modeLabel = modeLabel,
            placeName = placeName,
            today = LocalDate.now(),
            colors = colors,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

/**
 * El servidor todavía no guarda quién cobró cada propina ni un resumen por persona y turno, así que la tarjeta
 * se dibuja como en Doop pero dice que no hay datos, en vez de inventar cifras.
 */
@Composable
internal fun StaffShiftContent(
    modeLabel: String,
    placeName: String,
    today: LocalDate,
    colors: OperationalColors,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        Text("Mi turno", fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.9).sp, color = colors.textPrimary)
        Text(
            listOf(modeLabel, placeName).filter { it.isNotBlank() }.joinToString(" · "),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 6.dp),
        )
        Column(
            modifier =
                Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .arrive(0)
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.accentLime)
                    .padding(18.dp),
        ) {
            Text("Propinas de hoy", fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = colors.accentInk.copy(alpha = 0.75f))
            Text(
                "Sin datos",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.4).sp,
                color = colors.accentInk,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                "Todavía no se guarda quién cobró cada propina, así que aquí no hay cifras tuyas que mostrar.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.accentInk.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(modifier = Modifier.padding(top = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Mesas", "Entregas", "Respuesta").forEach { label ->
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.accentInk.copy(alpha = 0.1f))
                                .padding(10.dp),
                    ) {
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = colors.accentInk.copy(alpha = 0.7f))
                        Text("—", fontSize = 16.sp, fontWeight = FontWeight.Black, color = colors.accentInk)
                    }
                }
            }
        }
        Column(
            modifier =
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .arrive(1)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.cardBackground)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "ESTA SEMANA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                Text("—", fontSize = 15.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
            }
            val todayIndex = weekdayIndex(today)
            Row(
                modifier = Modifier.padding(top = 10.dp).fillMaxWidth().height(90.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                WeekLetters.indices.forEach { i ->
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(0.08f)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(if (i == todayIndex) colors.accentLime.copy(alpha = 0.6f) else colors.cardInner),
                    )
                }
            }
            Row(modifier = Modifier.padding(top = 6.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                WeekLetters.forEachIndexed { i, letter ->
                    Text(
                        letter,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = if (i == todayIndex) colors.accentLime else colors.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
