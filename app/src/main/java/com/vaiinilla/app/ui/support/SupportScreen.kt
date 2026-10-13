package com.vaiinilla.app.ui.support

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.EditorialPrimaryButton
import com.vaiinilla.app.ui.components.EditorialSearchField
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.SettingsGroup
import com.vaiinilla.app.ui.components.SettingsRow
import com.vaiinilla.app.ui.components.WalletScreenShell
import com.vaiinilla.app.ui.components.WalletSubflowTopBar
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.physicalPress
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

/** Lo mínimo del último pedido para empezar por ahí: casi toda la ayuda es sobre un pedido. */
data class SupportLastOrder(
    val title: String,
    val detail: String,
)

/** Temas cuyo título o texto contienen la búsqueda; sin búsqueda, todos. */
internal fun supportTopicsMatching(query: String): List<SupportCard> {
    val needle = query.trim()
    if (needle.isEmpty()) return SupportCopy.cards
    return SupportCopy.cards.filter {
        it.title.contains(needle, ignoreCase = true) || it.body.contains(needle, ignoreCase = true)
    }
}

@Composable
fun SupportScreen(
    onBack: () -> Unit,
    termsUrl: String = "",
    privacyUrl: String = "",
    lastOrder: SupportLastOrder? = null,
    onOpenLastOrder: () -> Unit = {},
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val uriHandler = LocalUriHandler.current
    var query by rememberSaveable { mutableStateOf("") }
    var openTopic by rememberSaveable { mutableStateOf<String?>(null) }
    val topics = supportTopicsMatching(query)

    WalletScreenShell(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp),
        ) {
            WalletSubflowTopBar(
                title = "Ayuda",
                onBack = {
                    haptics.click()
                    onBack()
                },
            )
            Column(
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                // Casi todo lo que se pregunta es de un pedido: el último va primero, a un toque.
                if (lastOrder != null) {
                    SettingsGroup(title = "Tu último pedido", modifier = Modifier.arrive(index = 0)) {
                        SettingsRow(
                            title = lastOrder.title,
                            subtitle = lastOrder.detail,
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                            onClick = onOpenLastOrder,
                            showDivider = false,
                        )
                    }
                }
                EditorialSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Buscar en la ayuda",
                    modifier = Modifier.arrive(index = 1),
                )
                SettingsGroup(title = "Temas", modifier = Modifier.arrive(index = 2)) {
                    topics.forEachIndexed { index, card ->
                        SupportTopic(
                            card = card,
                            expanded = openTopic == card.title,
                            onToggle = {
                                haptics.selection()
                                openTopic = if (openTopic == card.title) null else card.title
                            },
                            showDivider = index != topics.lastIndex,
                        )
                    }
                    if (topics.isEmpty()) {
                        Text(
                            "No hay un tema con esa palabra. Escríbenos abajo.",
                            color = colors.muted,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                Column(modifier = Modifier.arrive(index = 3), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "¿No lo resolviste?",
                        color = colors.ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(SupportCopy.CONTACT_BODY, color = colors.muted, fontSize = 14.sp, lineHeight = 20.sp)
                    EditorialPrimaryButton(
                        text = SupportCopy.EMAIL,
                        onClick = { runCatching { uriHandler.openUri(SupportCopy.MAILTO) } },
                    )
                    Text(
                        SupportCopy.LEGAL_NOTE,
                        color = colors.muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp),
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

/** Un tema que se abre en su lugar: la respuesta aparece debajo, sin otra pantalla. */
@Composable
private fun SupportTopic(
    card: SupportCard,
    expanded: Boolean,
    onToggle: () -> Unit,
    showDivider: Boolean,
) {
    val colors = LocalVaiinillaColors.current
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
        label = "support-topic-chevron",
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .physicalPress(scale = PhysicalPressScale.Nav, onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                card.title,
                color = colors.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(dampingRatio = 0.9f, stiffness = 400f)) + fadeIn(),
            exit = shrinkVertically(spring(dampingRatio = 1f, stiffness = 600f)) + fadeOut(),
        ) {
            Text(
                card.body,
                color = colors.muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
            )
        }
        if (showDivider) {
            HorizontalDivider(color = colors.line, thickness = 1.dp, modifier = Modifier.padding(start = 16.dp))
        }
    }
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
