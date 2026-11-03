package com.vaiinilla.app.ui.support

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.ClientCap
import com.vaiinilla.app.ui.components.ClientCard
import com.vaiinilla.app.ui.components.ClientCardDivider
import com.vaiinilla.app.ui.components.ClientPageHeader
import com.vaiinilla.app.ui.components.EditorialSearchField
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.WalletScreenShell
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.walletTile
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

data class SupportCard(
    val title: String,
    val body: String,
    /** La línea corta bajo el título, con el tema cerrado. */
    val summary: String = "",
)

object SupportCopy {
    const val EMAIL = "equipo@vaiinilla.app"
    const val MAILTO = "mailto:$EMAIL"
    const val INTRO =
        "Estamos para ayudarte con tu pedido, tu saldo, un pago o tu cuenta. " +
            "Escríbenos y te respondemos lo antes posible."
    const val CONTACT_TITLE = "Contacto directo"
    const val CONTACT_BODY = "Escríbenos con el correo con el que entras y te respondemos lo antes posible."
    const val LEGAL_NOTE = "Los documentos legales vigentes están en:"

    val cards =
        listOf(
            SupportCard(
                title = "Pedidos y saldo",
                summary = "Si no aparece, tardó o el saldo no se ve",
                body =
                    "Si tu pedido no aparece, tardó más de lo indicado o tu saldo no se refleja " +
                        "después de una recarga, escríbenos con la hora aproximada " +
                        "y el establecimiento donde lo hiciste.",
            ),
            SupportCard(
                title = "Pagos",
                summary = "Cargos con tarjeta y reembolsos",
                body =
                    "Los pagos con tarjeta los procesa Stripe de forma segura. Si un cargo se ve " +
                        "duplicado o no se completó, mándanos el correo de tu cuenta y la fecha del intento.",
            ),
            SupportCard(
                title = "Tu cuenta",
                summary = "Cómo eliminarla desde la app o la web",
                body =
                    "Para eliminar tu cuenta puedes hacerlo desde la app en Configuración, " +
                        "o seguir la guía en app.vaiinilla.app/eliminar-cuenta.",
            ),
        )
}

/** Lo mínimo del último pedido para empezar por ahí: casi toda la ayuda es sobre un pedido. */
data class SupportLastOrder(
    val folio: Int,
    val state: String,
    val total: String,
)

/** Temas cuyo título o texto contienen la búsqueda; sin búsqueda, todos. */
internal fun supportTopicsMatching(query: String): List<SupportCard> {
    val needle = query.trim()
    if (needle.isEmpty()) return SupportCopy.cards
    return SupportCopy.cards.filter {
        it.title.contains(needle, ignoreCase = true) || it.body.contains(needle, ignoreCase = true)
    }
}

private const val LEGAL_TOPIC = "Privacidad y términos"

/** El correo a soporte; con pedido, el asunto ya dice cuál es. */
internal fun supportMailto(folio: Int? = null): String =
    folio?.let { "${SupportCopy.MAILTO}?subject=${Uri.encode("Ayuda con el pedido #$it")}" } ?: SupportCopy.MAILTO

/** Ayuda (Doop L12): el último pedido primero, los temas que se abren en su lugar y el correo a soporte. */
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
    var mailFailed by rememberSaveable { mutableStateOf(false) }
    val topics = supportTopicsMatching(query)
    val needle = query.trim()
    val hasLegal = termsUrl.isNotBlank() || privacyUrl.isNotBlank()
    val showLegal = hasLegal && (needle.isEmpty() || "$LEGAL_TOPIC legales".contains(needle, ignoreCase = true))

    fun writeTo(mailto: String) {
        haptics.click()
        mailFailed = runCatching { uriHandler.openUri(mailto) }.isFailure
    }

    fun toggle(topic: String) {
        haptics.selection()
        openTopic = if (openTopic == topic) null else topic
    }

    WalletScreenShell(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 28.dp),
        ) {
            ClientPageHeader(
                title = "Ayuda",
                subtitle = "Con tu pedido, tu saldo, un pago o tu cuenta",
                onBack = {
                    haptics.click()
                    onBack()
                },
                modifier = Modifier.arrive(index = 0),
            )
            EditorialSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Buscar en la ayuda",
                modifier = Modifier.padding(top = 18.dp).arrive(index = 1),
            )
            if (lastOrder != null) {
                ClientCap("Tu último pedido", modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp).arrive(index = 2))
                LastOrderCard(
                    order = lastOrder,
                    onOpen = {
                        haptics.click()
                        onOpenLastOrder()
                    },
                    onAskForHelp = { writeTo(supportMailto(lastOrder.folio)) },
                    modifier = Modifier.arrive(index = 2),
                )
            }
            ClientCap("Temas", modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp).arrive(index = 3))
            ClientCard(modifier = Modifier.arrive(index = 3)) {
                topics.forEachIndexed { index, card ->
                    if (index > 0) ClientCardDivider()
                    SupportTopic(
                        title = card.title,
                        summary = card.summary,
                        expanded = openTopic == card.title,
                        onToggle = { toggle(card.title) },
                    ) {
                        Text(
                            card.body,
                            color = colors.muted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                        )
                    }
                }
                if (showLegal) {
                    if (topics.isNotEmpty()) ClientCardDivider()
                    SupportTopic(
                        title = LEGAL_TOPIC,
                        summary = null,
                        expanded = openTopic == LEGAL_TOPIC,
                        onToggle = { toggle(LEGAL_TOPIC) },
                    ) {
                        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                            Text(SupportCopy.LEGAL_NOTE, color = colors.muted, fontSize = 14.sp, lineHeight = 20.sp)
                            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                LegalLink(label = "Términos", url = termsUrl)
                                LegalLink(label = "Privacidad", url = privacyUrl)
                            }
                        }
                    }
                }
                if (topics.isEmpty() && !showLegal) {
                    Text(
                        "No hay un tema con esa palabra. Escríbenos abajo.",
                        color = colors.muted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            ContactCard(
                mailFailed = mailFailed,
                onWrite = { writeTo(supportMailto()) },
                modifier = Modifier.padding(top = 22.dp).arrive(index = 4),
            )
        }
    }
}

@Composable
private fun LastOrderCard(
    order: SupportLastOrder,
    onOpen: () -> Unit,
    onAskForHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    ClientCard(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .physicalPress(scale = PhysicalPressScale.Small, onClick = onOpen)
                    .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "#${order.folio} · ${order.state} · ${order.total}",
                    color = colors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "¿Algo salió mal? Te ayudamos con este pedido",
                    color = colors.muted,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box(
                modifier =
                    Modifier
                        .heightIn(min = 40.dp)
                        .physicalPress(scale = PhysicalPressScale.Small, onClick = onAskForHelp)
                        .clip(CircleShape)
                        .background(colors.ink)
                        .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Pedir ayuda", color = colors.onInk, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun ContactCard(
    mailFailed: Boolean,
    onWrite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    ClientCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("¿No lo resolviste?", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                SupportCopy.CONTACT_BODY,
                color = colors.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
            Box(
                modifier =
                    Modifier
                        .padding(top = 14.dp)
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .physicalPress(scale = PhysicalPressScale.Small, onClick = onWrite)
                        .clip(RoundedCornerShape(24.dp))
                        .background(walletTile(colors)),
                contentAlignment = Alignment.Center,
            ) {
                Text("Escribir a soporte", color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
            AnimatedVisibility(visible = mailFailed, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Text(
                    "No se abrió el correo. Escríbenos a ${SupportCopy.EMAIL}",
                    color = colors.ink2,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

/** Un tema que se abre en su lugar: la respuesta aparece debajo, sin otra pantalla. */
@Composable
private fun SupportTopic(
    title: String,
    summary: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    answer: @Composable () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = if (reducedMotion()) snap() else spring(dampingRatio = 0.8f, stiffness = 500f),
        label = "support-topic-chevron",
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
                    .physicalPress(scale = PhysicalPressScale.Small, onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                if (!summary.isNullOrBlank()) {
                    Text(
                        summary,
                        color = colors.muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = rotation },
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(dampingRatio = 0.9f, stiffness = 400f)) + fadeIn(),
            exit = shrinkVertically(spring(dampingRatio = 1f, stiffness = 600f)) + fadeOut(),
        ) {
            answer()
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
