package com.vaiinilla.app.ui.screens.admin

import com.vaiinilla.app.ui.components.physicalPress
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.ToastPill
import com.vaiinilla.app.ui.components.VaiinillaQrCode
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.ChipButton
import com.vaiinilla.app.ui.screens.OperationalColors

private const val SECTION_TITLE_FITS = 18

/** Una sección de "Negocio": título con regresar, contenido con scroll y su aviso flotante. */
@Composable
internal fun AdminSectionPage(
    title: String,
    colors: OperationalColors,
    notice: String?,
    noticeIsError: Boolean,
    onBack: () -> Unit,
    subtitle: String? = null,
    eyebrow: String? = null,
    trailing: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AdminRoundButton(Icons.AutoMirrored.Outlined.ArrowBack, "Regresar", colors, onBack)
                androidx.compose.foundation.layout
                    .Spacer(Modifier.weight(1f))
                trailing()
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // El título grande de la sección, como el de la navegación de iOS.
                item(key = "section-title") {
                    Column(Modifier.padding(horizontal = 4.dp)) {
                        eyebrow?.let { AdminCapsLabel(it, colors) }
                        val compact = eyebrow != null || title.length > SECTION_TITLE_FITS
                        Text(
                            title,
                            fontWeight = FontWeight.Black,
                            fontSize = if (compact) 28.sp else 34.sp,
                            lineHeight = if (compact) 32.sp else 38.sp,
                            letterSpacing = (-1).sp,
                            color = colors.textPrimary,
                            modifier = Modifier.semantics { heading() },
                        )
                        subtitle?.let {
                            Text(
                                it,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
                content()
            }
        }
        ToastPill(
            message = notice,
            containerColor = colors.textPrimary,
            contentColor = colors.background,
            badgeColor = if (noticeIsError) AdminCoral else colors.accentLime,
            badgeContentColor = if (noticeIsError) colors.background else colors.accentInk,
            icon = if (noticeIsError) Icons.Rounded.PriorityHigh else Icons.Rounded.Check,
        )
    }
}

/** Texto de ayuda gris, como los de la web debajo de cada opción. */
@Composable
internal fun AdminHint(
    text: String,
    colors: OperationalColors,
) {
    Text(text, fontSize = 12.sp, color = colors.textSecondary)
}

/** Error dentro de una hoja: el aviso flotante queda detrás de ella. */
@Composable
internal fun AdminSheetError(text: String?) {
    if (text == null) return
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = AdminCoral,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** Dos o más opciones excluyentes como chips, para las preguntas de "¿Cuándo pagan?" y similares. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> AdminChoice(
    question: String,
    options: List<Pair<T, String>>,
    selected: T?,
    colors: OperationalColors,
    enabled: Boolean,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(question, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                ChipButton(
                    label = label,
                    enabled = enabled,
                    colors = colors,
                    selected = value == selected,
                ) { onSelect(value) }
            }
        }
    }
}

/** Confirmación antes de algo que no se deshace fácil. */
@Composable
internal fun AdminConfirmSheet(
    title: String,
    message: String,
    confirmLabel: String,
    colors: OperationalColors,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    // Lo que no se deshace fácil va en el aviso centrado con botón rojo (marco 54), no en una hoja.
    androidx.compose.ui.window.Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Column(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(colors.cardBackground)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.4).sp,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                message,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
            )
            AdminPrimaryButton(
                title = confirmLabel,
                colors = colors,
                modifier = Modifier.padding(top = 6.dp),
                busy = busy,
                fill = AdminCoral,
                contentColor = Color.White,
                onClick = onConfirm,
            )
            AdminSecondaryButton(title = "Cancelar", colors = colors, enabled = !busy, onClick = onDismiss)
        }
    }
}

/** Marco 49: el QR en grande sobre su tarjeta clara, el enlace y las tres cosas que se hacen con él. */
@Composable
internal fun AdminBigQr(
    url: String,
    name: String,
    colors: OperationalColors,
    onNotice: (String, Boolean) -> Unit,
    code: String? = null,
) {
    val context = LocalContext.current
    var writingNfc by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFFF5F0E3))
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            VaiinillaQrCode(value = url, qrSize = 196.dp)
            Text(name, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B1B18))
            Text(
                code?.let { "Código $it · Vaiinilla" } ?: "Vaiinilla",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF5E5C52),
            )
        }
        AdminPanel(colors, Modifier.physicalPress { openLink(context, url) }) {
            Text(
                "Enlace de $name · abre la app o la web",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QrAction("Compartir QR", Icons.Outlined.Share, colors, Modifier.weight(1f), highlighted = false) {
                if (!shareQr(context, url, name, url)) onNotice("No se pudo preparar el QR.", true)
            }
            QrAction("Copiar enlace", Icons.Outlined.ContentCopy, colors, Modifier.weight(1f), highlighted = false) {
                copyLink(context, name, url)
                onNotice("Enlace copiado", false)
            }
            QrAction("Grabar en NFC", Icons.Outlined.Nfc, colors, Modifier.weight(1f), highlighted = true) { writingNfc = true }
        }
    }
    if (writingNfc) {
        AdminNfcSheet(url = url, colors = colors, onDone = { ok, message ->
            writingNfc = false
            onNotice(message, !ok)
        }, onDismiss = { writingNfc = false })
    }
}

@Composable
private fun QrAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colors: OperationalColors,
    modifier: Modifier,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val fg = if (highlighted) colors.accentInk else colors.textPrimary
    Column(
        modifier
            .physicalPress(onClick = onClick)
            .clip(RoundedCornerShape(20.dp))
            .background(if (highlighted) colors.accentLime else colors.cardBackground)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1)
    }
}
/** El QR de un enlace con lo que se hace con él: compartirlo como imagen, copiar el enlace, abrirlo y grabarlo en NFC. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AdminQrBlock(
    url: String,
    name: String,
    colors: OperationalColors,
    onNotice: (String, Boolean) -> Unit,
    code: String? = null,
    shareMessage: String = url,
) {
    val context = LocalContext.current
    var writingNfc by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        VaiinillaQrCode(value = url, qrSize = 120.dp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            if (code != null) {
                Text("Código", fontSize = 12.sp, color = colors.textSecondary)
                Text(code, fontSize = 22.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
            }
            Text(url, fontSize = 11.sp, color = colors.textSecondary, maxLines = 2)
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChipButton(label = "Compartir QR", enabled = true, colors = colors) {
            if (!shareQr(context, url, name, shareMessage)) onNotice("No se pudo preparar el QR.", true)
        }
        ChipButton(label = "Copiar enlace", enabled = true, colors = colors) {
            copyLink(context, name, url)
            onNotice("Enlace copiado", false)
        }
        ChipButton(label = "Abrir", enabled = true, colors = colors) { openLink(context, url) }
        ChipButton(label = "Grabar en NFC", enabled = true, colors = colors) { writingNfc = true }
    }
    if (writingNfc) {
        AdminNfcSheet(url = url, colors = colors, onDone = { ok, message ->
            writingNfc = false
            onNotice(message, !ok)
        }, onDismiss = { writingNfc = false })
    }
}

@Composable
private fun AdminNfcSheet(
    url: String,
    colors: OperationalColors,
    onDone: (Boolean, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val writer = rememberNfcLinkWriter()
    LaunchedEffect(url) { writer.start(url, onDone) }
    AdminSheet(title = "Grabar en NFC", colors = colors, busy = false, onDismiss = {
        writer.stop()
        onDismiss()
    }) {
        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(colors.cardBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Nfc,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Text(
            "Acerca una etiqueta NFC a la parte de atrás del teléfono y déjala quieta hasta que diga que quedó grabada.",
            fontSize = 15.sp,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.height(4.dp))
    }
}
