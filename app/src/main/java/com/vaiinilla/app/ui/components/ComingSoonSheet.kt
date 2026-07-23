package com.vaiinilla.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.theme.Cream
import com.vaiinilla.app.ui.theme.Ink
import com.vaiinilla.app.ui.theme.MutedInk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoonSheet(
    title: String,
    description: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Cream,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                color = Ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Próximamente",
                color = MutedInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
            )
            Text(
                text = description,
                color = MutedInk,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
