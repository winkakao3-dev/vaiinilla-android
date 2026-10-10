package com.vaiinilla.app.ui.support

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.EditorialPrimaryButton
import com.vaiinilla.app.ui.components.WalletScreenShell
import com.vaiinilla.app.ui.components.WalletSubflowTopBar
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

data class SupportCard(
    val title: String,
    val body: String,
)

object SupportCopy {
    const val EMAIL = "equipo@vaiinilla.app"
    const val MAILTO = "mailto:$EMAIL"
    const val INTRO =
        "Estamos para ayudarte con tu pedido, tu saldo, un pago o tu cuenta. " +
            "Escríbenos y te respondemos lo antes posible."
    const val CONTACT_TITLE = "Contacto directo"
    const val CONTACT_BODY =
        "La vía más rápida es el correo. Incluye el correo con el que entras y, " +
            "si tu duda es de un pedido, el número o folio que aparece en tu ticket."
    const val LEGAL_NOTE = "Los documentos legales vigentes están en:"

    val cards =
        listOf(
            SupportCard(
                title = "Pedidos y saldo",
                body =
                    "Si tu pedido no aparece, tardó más de lo indicado o tu saldo no se refleja " +
                        "después de una recarga, escríbenos con la hora aproximada " +
                            "y el establecimiento donde lo hiciste.",
            ),
            SupportCard(
                title = "Pagos",
                body =
                    "Los pagos con tarjeta los procesa Stripe de forma segura. Si un cargo se ve " +
                        "duplicado o no se completó, mándanos el correo de tu cuenta y la fecha del intento.",
            ),
            SupportCard(
                title = "Tu cuenta",
                body =
                    "Para eliminar tu cuenta puedes hacerlo desde la app en Configuración, " +
                        "o seguir la guía en app.vaiinilla.app/eliminar-cuenta.",
            ),
        )
}

@Composable
fun SupportScreen(
    onBack: () -> Unit,
    termsUrl: String = "",
    privacyUrl: String = "",
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val uriHandler = LocalUriHandler.current

    WalletScreenShell(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp),
        ) {
            WalletSubflowTopBar(
                title = "Ayuda",
                onBack = {
                    haptics.click()
                    onBack()
                },
            )
            Column(modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 28.dp)) {
                Text(
                    "¿Necesitas ayuda?",
                    color = colors.ink,
                    fontSize = 36.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.6).sp,
                )
                Text(
                    SupportCopy.INTRO,
                    color = colors.muted,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SupportPanel {
                    Text(
                        SupportCopy.CONTACT_TITLE,
                        color = colors.ink,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        SupportCopy.CONTACT_BODY,
                        color = colors.muted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                    EditorialPrimaryButton(
                        text = SupportCopy.EMAIL,
                        onClick = { runCatching { uriHandler.openUri(SupportCopy.MAILTO) } },
                    )
                }
                SupportCopy.cards.dropLast(1).forEach { card ->
                    SupportPanel { SupportCardText(card) }
                }
                SupportPanel {
                    SupportCardText(SupportCopy.cards.last())
                    Text(
                        SupportCopy.LEGAL_NOTE,
                        color = colors.muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        LegalLink(label = "Términos", url = termsUrl)
                        LegalLink(label = "Privacidad", url = privacyUrl)
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportCardText(card: SupportCard) {
    val colors = LocalVaiinillaColors.current
    Text(
        card.title,
        color = colors.ink,
        fontSize = 19.sp,
        fontWeight = FontWeight.Black,
    )
    Text(
        card.body,
        color = colors.muted,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
}

@Composable
private fun SupportPanel(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.paper2)
                .border(1.dp, colors.line, RoundedCornerShape(22.dp))
                .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun LegalLink(
    label: String,
    url: String,
) {
    val colors = LocalVaiinillaColors.current
    val uriHandler = LocalUriHandler.current
    Text(
        text = label,
        modifier =
            if (url.isNotBlank()) {
                Modifier.clickable { uriHandler.openUri(url) }
            } else {
                Modifier
            },
        color = colors.ink,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
}
