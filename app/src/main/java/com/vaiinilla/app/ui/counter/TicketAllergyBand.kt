package com.vaiinilla.app.ui.counter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.counter.StudentAllergens

private val TicketCoral = Color(0xFFF15B55)
private val TicketCoralInk = Color(0xFF1D1C18)

/** Las alergias del alumno, en coral y arriba del ticket: lo primero que ve quien prepara o entrega. */
@Composable
fun TicketAllergyBand(
    allergies: List<String>,
    modifier: Modifier = Modifier,
) {
    val known = StudentAllergens.known(allergies)
    if (known.isEmpty()) return
    val labels = known.map(StudentAllergens::label)
    Row(
        modifier =
            modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TicketCoral)
                .padding(horizontal = 12.dp, vertical = 9.dp)
                .clearAndSetSemantics { contentDescription = "Alergia: " + labels.joinToString(", ") },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = TicketCoralInk, modifier = Modifier.size(15.dp))
        Text(
            "ALERGIA · " + labels.joinToString(", ") { it.uppercase() },
            color = TicketCoralInk,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp,
        )
    }
}
