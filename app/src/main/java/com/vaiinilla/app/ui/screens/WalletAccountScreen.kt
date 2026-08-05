package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.WalletScreenShell
import com.vaiinilla.app.ui.components.WalletSectionHead
import com.vaiinilla.app.ui.components.WalletSubflowTopBar
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

@Composable
fun WalletAccountScreen(onBack: () -> Unit) {
    val colors = LocalVaiinillaColors.current

    WalletScreenShell(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
        ) {
            WalletSubflowTopBar(title = "Mi cuenta", onBack = onBack)

            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .size(56.dp)
                                .background(colors.ink, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("DA", color = colors.paper, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text("Dani", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text("Cuenta de estudiante activa", color = colors.muted, fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(20.dp))

                AccountField(label = "Matrícula", value = "UTCH-241087")
                AccountField(label = "Correo", value = "dani.alvarez@utch.mx")
                AccountField(label = "Tel", value = "614 555 0187")
                AccountField(label = "Plantel", value = "Campus Chihuahua")

                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    color = colors.paper2,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Código para Caja", color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            "UTCH-241087",
                            color = colors.ink,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }

                WalletSectionHead(title = "Actividad reciente")

                ActivityRow(
                    title = "SPEI +$100",
                    subtitle = "Recarga al saldo",
                    positive = true,
                )
                Spacer(Modifier.height(8.dp))
                ActivityRow(
                    title = "Pedido #3411 −$42",
                    subtitle = "Pago con saldo",
                    positive = false,
                )
            }
        }
    }
}

@Preview(
    name = "Mi cuenta · claro",
    showBackground = true,
    widthDp = 411,
    heightDp = 891,
)
@Composable
private fun WalletAccountScreenLightPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        WalletAccountScreen(onBack = {})
    }
}

@Preview(
    name = "Mi cuenta · oscuro",
    showBackground = true,
    widthDp = 411,
    heightDp = 891,
)
@Composable
private fun WalletAccountScreenDarkPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Dark) {
        WalletAccountScreen(onBack = {})
    }
}

@Composable
private fun AccountField(
    label: String,
    value: String,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text(
            value,
            color = colors.ink,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ActivityRow(
    title: String,
    subtitle: String,
    positive: Boolean,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.paper2,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(colors.accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (positive) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                    contentDescription = null,
                    tint = colors.ink,
                    modifier = Modifier.size(21.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(subtitle, color = colors.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}
