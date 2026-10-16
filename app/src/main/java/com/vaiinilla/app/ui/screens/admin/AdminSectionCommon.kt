package com.vaiinilla.app.ui.screens.admin

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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ToastPill
import com.vaiinilla.app.ui.components.VaiinillaQrCode
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.screens.ActionPill
import com.vaiinilla.app.ui.screens.ChipButton
import com.vaiinilla.app.ui.screens.OperationalColors

/** Una sección de "Negocio": título con regresar, contenido con scroll y su aviso flotante. */
@Composable
internal fun AdminSectionPage(
    title: String,
    colors: OperationalColors,
    notice: String?,
    noticeIsError: Boolean,
    onBack: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .physicalPress(scale = PhysicalPressScale.Small, onClick = onBack)
                            .size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Regresar",
                        tint = colors.textPrimary,
                    )
                }
                Text(title, fontWeight = FontWeight.Black, fontSize = 20.sp, color = colors.textPrimary)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
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
    AdminSheet(title = title, colors = colors, busy = busy, onDismiss = onDismiss) {
        Text(message, fontSize = 15.sp, color = colors.textPrimary)
        ActionPill(label = confirmLabel, enabled = true, loading = busy, colors = colors, onClick = onConfirm)
        ActionPill(
            label = "Cancelar",
            enabled = !busy,
            loading = false,
            colors = colors,
            primary = false,
            onClick = onDismiss,
        )
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
            if (!shareQr(context, url, name)) onNotice("No se pudo preparar el QR.", true)
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
