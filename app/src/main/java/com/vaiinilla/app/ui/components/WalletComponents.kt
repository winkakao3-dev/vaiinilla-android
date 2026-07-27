package com.vaiinilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

@Composable
fun WalletSubflowTopBar(
    title: String,
    onBack: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(52.dp)
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = colors.ink)
        }
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = colors.ink,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
        )
        trailing?.invoke()
    }
}

@Composable
fun WalletPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .height(52.dp),
        shape = RoundedCornerShape(18.dp),
        colors =
            androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.accentInk,
            ),
    ) {
        Text(text, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}

@Composable
fun WalletSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .height(52.dp),
        shape = RoundedCornerShape(18.dp),
        colors =
            androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = colors.paper2,
                contentColor = colors.ink,
            ),
    ) {
        Text(text, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}

@Composable
fun WalletSectionHead(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = colors.ink,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
        )
        if (action != null && onAction != null) {
            androidx.compose.material3.TextButton(onClick = onAction) {
                Text(action, color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun WalletScreenShell(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    androidx.compose.foundation.layout.Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.paper),
    ) {
        content()
    }
}
