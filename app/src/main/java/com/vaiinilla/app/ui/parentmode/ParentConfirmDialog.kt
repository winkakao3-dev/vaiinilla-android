package com.vaiinilla.app.ui.parentmode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.screens.rememberOperationalColors

/**
 * La confirmación de algo que no se deshace solo (quitar, bloquear, cancelar). La acción va en tinta o en
 * coral si destruye; "No" queda como texto, así el lima de la pantalla sigue siendo uno.
 */
@Composable
internal fun ParentConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = rememberOperationalColors()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = colors.background,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().testTag("parent-confirm"),
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(title, color = colors.textPrimary, fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text(
                    message,
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 18.dp),
                )
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .physicalPress {
                                onDismiss()
                                onConfirm()
                            }.height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(if (destructive) ErrorCoral else colors.textPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        confirmLabel,
                        color = if (destructive) colors.accentInk else colors.background,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                    )
                }
                Box(
                    modifier = Modifier.fillMaxWidth().physicalPress(onClick = onDismiss).height(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
