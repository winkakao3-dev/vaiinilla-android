package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.VaiinillaQrCode
import com.vaiinilla.app.ui.components.WalletScreenShell
import com.vaiinilla.app.ui.components.WalletSubflowTopBar
import com.vaiinilla.app.ui.discovery.QrPayloadParser
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

@Composable
fun WalletAccountScreen(
    onBack: () -> Unit,
    displayName: String = "",
    email: String = "",
    userId: String? = null,
) {
    val colors = LocalVaiinillaColors.current
    val qrValue = userId?.trim()?.takeIf { it.isNotEmpty() }?.let(QrPayloadParser::encodeUser)
    WalletScreenShell(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp),
        ) {
            WalletSubflowTopBar(title = "Mi cuenta", onBack = onBack)
            Column(modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 28.dp)) {
                Text(
                    "Código personal",
                    color = colors.muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    displayName.ifBlank { "Tu cuenta" },
                    color = colors.ink,
                    fontSize = 36.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.6).sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (email.isNotBlank()) {
                    Text(
                        email,
                        color = colors.muted,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(colors.paper2)
                            .border(1.dp, colors.line, RoundedCornerShape(32.dp))
                            .padding(24.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 34.dp, y = (-34).dp)
                                .size(118.dp)
                                .rotate(18f)
                                .clip(RoundedCornerShape(38.dp))
                                .background(colors.accent.copy(alpha = 0.95f)),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Text(
                            "Para recargar saldo",
                            color = colors.ink,
                            fontSize = 24.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.7).sp,
                        )
                        Text(
                            "Muestra este código en Caja para encontrar tu cuenta.",
                            color = colors.muted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp, end = 48.dp),
                        )
                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 16.dp,
                                        shape = RoundedCornerShape(24.dp),
                                        ambientColor = Color(0x17171717),
                                        spotColor = Color(0x17171717),
                                    ),
                            color = Color.White,
                            shape = RoundedCornerShape(24.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (qrValue != null) {
                                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                        VaiinillaQrCode(
                                            value = qrValue,
                                            qrSize = minOf(maxWidth, 240.dp),
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.padding(top = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.accent.copy(alpha = 0.22f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(colors.accent),
                                            )
                                        }
                                        Text(
                                            "Listo para escanear",
                                            color = colors.muted,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.4.sp,
                                        )
                                    }
                                } else {
                                    Text(
                                        "Inicia sesión para ver tu código.",
                                        color = colors.muted,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Text(
                "También pueden buscarte por nombre en Caja.",
                color = colors.muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
            )
        }
    }
}

@Preview(name = "Mi cuenta · claro", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun WalletAccountScreenLightPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        WalletAccountScreen(
            onBack = {},
            displayName = "David Ramirez",
            email = "keinkao@gmail.com",
            userId = "u-preview",
        )
    }
}

@Preview(name = "Mi cuenta · oscuro", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun WalletAccountScreenDarkPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Dark) {
        WalletAccountScreen(
            onBack = {},
            displayName = "David Ramirez",
            email = "keinkao@gmail.com",
            userId = "u-preview",
        )
    }
}
