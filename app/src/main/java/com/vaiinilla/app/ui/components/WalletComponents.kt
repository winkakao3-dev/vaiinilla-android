package com.vaiinilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
    onBack: (() -> Unit)?,
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
        // Sin pantalla detrás (la primera de la app) no hay flecha: tocarla no haría nada.
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = colors.ink)
            }
        } else {
            Spacer(Modifier.width(12.dp))
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

@Composable
fun WalletUnavailableScreen(
    title: String,
    onBack: () -> Unit,
    description: String,
) {
    val colors = LocalVaiinillaColors.current
    WalletScreenShell(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            WalletSubflowTopBar(title = title, onBack = onBack)
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                color = colors.paper2,
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Función pendiente",
                        color = colors.ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                    )
                    Text(
                        description,
                        color = colors.muted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
