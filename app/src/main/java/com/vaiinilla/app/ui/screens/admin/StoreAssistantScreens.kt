package com.vaiinilla.app.ui.screens.admin

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.admin.AssistantChange
import com.vaiinilla.app.domain.admin.AssistantLabels
import com.vaiinilla.app.domain.admin.AssistantMessage
import com.vaiinilla.app.domain.admin.AssistantPhoto
import com.vaiinilla.app.domain.admin.AssistantProposal
import com.vaiinilla.app.domain.admin.AssistantUpload
import com.vaiinilla.app.ui.admin.AdminStoreViewModel
import com.vaiinilla.app.ui.admin.StoreAssistantState
import com.vaiinilla.app.ui.screens.OperationalColors
import com.vaiinilla.app.ui.screens.PreparedProductImage
import com.vaiinilla.app.ui.screens.prepareProductImage
import com.vaiinilla.app.ui.screens.prepareStoreLogo

/**
 * Bienvenida del admin nuevo (marco AS1): la tienda todavía no está lista y se ofrece armarla platicando con el
 * asistente o hacerlo a mano en Flujo de mi tienda. Se ve una sola vez por tienda; "Ahora no" también la cierra.
 */
@Composable
internal fun StoreWelcome(
    placeName: String,
    colors: OperationalColors,
    onAssistant: () -> Unit,
    onManual: () -> Unit,
    onLater: () -> Unit,
) {
    BackHandler(onBack = onLater)
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AdminRoundButton(Icons.Rounded.Close, "Ahora no", colors, onLater)
            }
            Text(
                "Bienvenido a Vaiinilla",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (colors.isDark) colors.accentLime else colors.accentInk,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                if (placeName.isBlank()) "Armemos tu tienda" else "Armemos tu tienda, $placeName",
                fontSize = 32.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.8).sp,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 4.dp).semantics { heading() },
            )
            Text(
                "Antes de vender hay que decirle a la app cómo trabajas: cómo te pagan, cómo entregas y qué ve " +
                    "tu cliente. Te toma unos 5 minutos.",
                fontSize = 14.5.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 10.dp),
            )
            Column(
                Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.accentLime)
                    .clickable(onClick = onAssistant)
                    .padding(18.dp),
            ) {
                Text(
                    "Recomendado",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.accentInk.copy(alpha = 0.85f),
                )
                Text(
                    "Platícalo con el asistente",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.accentInk,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    "Te hace unas preguntas sobre tu negocio y te deja todo armado. Tú lo revisas y lo aceptas.",
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accentInk.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Row(
                Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.cardBackground)
                    .clickable(onClick = onManual)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Hacerlo yo paso a paso",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                    )
                    Text(
                        "Eliges cada opción en Flujo de mi tienda",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMuted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                "Lo que vamos a dejar listo",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 2.dp, top = 24.dp, bottom = 8.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Tipo de negocio", "Cuándo pagan", "Cómo se entrega", "Horarios", "Cocina y barra").forEach {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Check,
                            null,
                            tint = if (colors.isDark) colors.accentLime else colors.accentInk,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            it,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                }
            }
            Text(
                "Puedes cambiar todo después en Negocio › Flujo de mi tienda",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 16.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * El asistente (marcos AS2 y AS3): la plática y, cuando ya hay propuesta, la revisión con "Aceptar y guardar".
 * Nada se guarda hasta aceptar; [onClose] regresa a donde se abrió.
 */
@Composable
internal fun StoreAssistantScreen(
    viewModel: AdminStoreViewModel,
    colors: OperationalColors,
    readOnly: Boolean,
    onClose: () -> Unit,
) {
    val state by viewModel.assistant
    LaunchedEffect(Unit) { viewModel.startAssistant() }
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        val proposal = state.proposal
        if (state.reviewing && proposal != null) {
            BackHandler { viewModel.reviewProposal(false) }
            AssistantReview(
                proposal = proposal,
                state = state,
                colors = colors,
                readOnly = readOnly,
                onBack = { viewModel.reviewProposal(false) },
                onAccept = { viewModel.acceptProposal(onClose) },
            )
        } else {
            BackHandler(onBack = onClose)
            AssistantChat(viewModel, state, colors, onClose)
        }
    }
}

@Composable
private fun AssistantChat(
    viewModel: AdminStoreViewModel,
    state: StoreAssistantState,
    colors: OperationalColors,
    onClose: () -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    // Lo que el servidor rechazó por largo vuelve al campo para mandarlo en partes.
    LaunchedEffect(state.draftToRestore) {
        state.draftToRestore?.let {
            if (draft.isBlank()) draft = it
            viewModel.draftRestored()
        }
    }
    val list = rememberLazyListState()
    val rows = state.messages.size + 2
    LaunchedEffect(state.messages.size, state.thinking, state.error, state.proposal) {
        list.animateScrollToItem((rows - 1).coerceAtLeast(0))
    }
    Column(Modifier.fillMaxSize()) {
        AssistantHeader(
            title = "Asistente",
            subtitle = "Armando tu tienda",
            backLabel = "Regresar",
            colors = colors,
            onBack = onClose,
            trailing = {
                if (state.messages.size > 1) {
                    AdminRoundButton(Icons.Rounded.Refresh, "Empezar de nuevo", colors, viewModel::restartAssistant)
                } else {
                    Spacer(Modifier.size(44.dp))
                }
            },
        )
        LazyColumn(
            state = list,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(state.messages, key = { index, _ -> index }) { _, message ->
                Bubble(message, colors, photos = state.photos)
            }
            item(key = "estado") {
                when {
                    state.thinking ->
                        Bubble(
                            AssistantMessage(fromOwner = false, text = "Escribiendo…"),
                            colors,
                            muted = true,
                        )
                    state.error != null ->
                        Column {
                            Text(
                                state.error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary,
                            )
                            if (state.retryable) {
                                Box(Modifier.padding(top = 8.dp)) {
                                    AdminFilterChip(
                                        "Reintentar",
                                        selected = false,
                                        colors = colors,
                                        onClick = viewModel::retryAssistant,
                                    )
                                }
                            }
                        }
                    else -> Spacer(Modifier.height(1.dp))
                }
            }
            item(key = "propuesta") {
                val proposal = state.proposal
                if (proposal != null && !state.thinking) {
                    ProposalSoFar(proposal, colors)
                } else {
                    Spacer(Modifier.height(1.dp))
                }
            }
        }
        val context = LocalContext.current
        val picker =
            rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
                if (uris.isEmpty()) return@rememberLauncherForActivityResult
                // Varias fotos a la vez: todas van con el siguiente mensaje, hasta las que quepan.
                val left = viewModel.assistant.value.photosLeft
                uris.take(left).forEach { uri ->
                    val image = runCatching { prepareProductImage(context, uri) }.getOrNull()
                    val logo = runCatching { prepareStoreLogo(context, uri) }.getOrNull()
                    if (image == null || logo == null) {
                        viewModel.photoPreparationFailed()
                    } else {
                        viewModel.addAssistantPhoto(image.toUpload(), logo.toUpload())
                    }
                }
                if (uris.size > left) viewModel.tooManyPhotos(left)
            }
        val hasPhoto = state.pendingPhotoIds.isNotEmpty()
        val tooLong = draft.trim().length > state.lettersLeftForText
        val send = {
            if (state.canSend && !tooLong && (draft.isNotBlank() || hasPhoto)) {
                if (viewModel.sendToAssistant(draft)) draft = ""
            }
        }
        if (!state.canSend && !state.thinking && !state.applying) {
            Text(
                "Ya platicamos mucho. Revisa la propuesta o empieza de nuevo.",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        val proposal = state.proposal
        if (proposal != null && !state.thinking) {
            AdminPrimaryButton(
                title = "Revisar la propuesta · ${countLabel(proposal.itemCount, "cambio", "cambios")}",
                colors = colors,
                fill = colors.accentLime,
                contentColor = colors.accentInk,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                onClick = { viewModel.reviewProposal(true) },
            )
        }
        if (!state.thinking && state.error == null && state.options.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                state.options.forEach { option ->
                    AdminFilterChip(option, selected = false, colors = colors, onClick = {
                        viewModel.sendToAssistant(option)
                    })
                }
            }
        }
        if (hasPhoto) {
            Column(Modifier.padding(top = 6.dp)) {
                Text(
                    if (state.pendingPhotoIds.size == 1) {
                        "Se manda con tu mensaje. Dile qué es: tu logo, tu local o qué producto."
                    } else {
                        "Se mandan ${state.pendingPhotoIds.size} fotos con tu mensaje. Dile qué es cada una, " +
                            "en orden: tu logo, tu local o qué producto."
                    },
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.pendingPhotoIds.forEachIndexed { index, id ->
                        PendingPhoto(
                            photo = state.photos[id],
                            position = index + 1,
                            colors = colors,
                            onRemove = { viewModel.removeAssistantPhoto(id) },
                        )
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(colors.cardBackground)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(enabled = state.canAddPhoto) { picker.launch("image/*") }
                    .semantics { contentDescription = "Mandar una foto" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.AddPhotoAlternate,
                    null,
                    tint = if (state.canAddPhoto) colors.textPrimary else colors.textMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
            BasicTextField(
                value = draft,
                // Sin recortar: si pasa del tope se avisa abajo y no se deja mandar.
                onValueChange = { draft = it },
                enabled = state.canSend,
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(colors.textPrimary),
                keyboardOptions =
                    KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                maxLines = 4,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .semantics { contentDescription = "Escribe tu respuesta" },
                decorationBox = { field ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (draft.isEmpty()) {
                            Text(
                                "Escribe tu respuesta…",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textMuted,
                            )
                        }
                        field()
                    }
                },
            )
            val canSend = state.canSend && !tooLong && (draft.isNotBlank() || hasPhoto)
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (canSend) colors.accentLime else colors.cardBorder)
                    .clickable(enabled = canSend, onClick = send)
                    .semantics { contentDescription = "Enviar" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.ArrowUpward,
                    null,
                    tint = if (canSend) colors.accentInk else colors.textMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        LetterCounter(draft.trim().length, state.lettersLeftForText, colors)
    }
}

/** Encabezado de la plática y de la revisión: regresar a la izquierda, título al centro y [trailing] a la derecha. */
@Composable
private fun AssistantHeader(
    title: String,
    subtitle: String,
    backLabel: String,
    colors: OperationalColors,
    onBack: () -> Unit,
    trailing: @Composable () -> Unit = { Spacer(Modifier.size(44.dp)) },
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AdminRoundButton(Icons.AutoMirrored.Outlined.ArrowBack, backLabel, colors, onBack)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Text(subtitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted)
        }
        trailing()
    }
}

/** Lo que ya quedó en la propuesta, para ver avanzar la tienda mientras se platica (marco AS2c). */
@Composable
private fun ProposalSoFar(
    proposal: AssistantProposal,
    colors: OperationalColors,
) {
    Column(Modifier.padding(top = 4.dp)) {
        Text(
            "Hasta ahora llevas",
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        AdminPanel(colors, padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) {
            val rows =
                buildList {
                    if (proposal.changes.isNotEmpty()) {
                        add("Ajustes" to proposal.changes.joinToString(" · ") { it.title })
                    }
                    proposal.spaces.forEach { group ->
                        add(
                            AssistantLabels.spaceType(group.type) to
                                "${group.prefix} 1 a ${group.prefix} ${group.count}",
                        )
                    }
                    if (proposal.productCount > 0) {
                        add("Menú" to countLabel(proposal.productCount, "producto", "productos"))
                    }
                    val photoCount = proposal.productPhotos.size + proposal.storePhotos.size
                    if (photoCount > 0) add("Fotos" to countLabel(photoCount, "foto", "fotos"))
                    if (proposal.invites.isNotEmpty()) {
                        add("Tu equipo" to countLabel(proposal.invites.size, "invitación", "invitaciones"))
                    }
                    proposal.courtHours?.let { add("Horario de las pistas" to AssistantLabels.courtHours(it)) }
                    if (proposal.supplies.isNotEmpty()) {
                        add("Insumos" to countLabel(proposal.supplies.size, "insumo", "insumos"))
                    }
                    if (proposal.recipes.isNotEmpty()) {
                        add("Recetas" to countLabel(proposal.recipes.size, "receta", "recetas"))
                    }
                }
            rows.forEachIndexed { index, (title, detail) ->
                if (index > 0) AdminRowDivider(colors)
                ItemRow(title, detail, colors)
            }
        }
    }
}

private fun countLabel(
    count: Int,
    one: String,
    many: String,
) = "$count ${if (count == 1) one else many}"

/** Cuenta de letras cuando el mensaje ya es largo; si pasa del tope, dice que se mande en partes. */
@Composable
private fun LetterCounter(
    length: Int,
    max: Int,
    colors: OperationalColors,
) {
    if (length < max * COUNTER_FROM_PERCENT / 100) return
    val over = length > max
    Text(
        if (over) {
            "${formatLetters(length)} de ${formatLetters(max)} letras: es mucho para un mensaje. " +
                "Mándalo en dos partes."
        } else {
            "${formatLetters(length)} de ${formatLetters(max)} letras"
        },
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = if (over) FontWeight.ExtraBold else FontWeight.SemiBold,
        color = if (over) colors.textPrimary else colors.textMuted,
        textAlign = TextAlign.End,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
    )
}

private fun formatLetters(value: Int): String = "%,d".format(java.util.Locale.US, value)

/** Una foto que se manda con el siguiente mensaje, con su número y una cruz para quitarla. */
@Composable
private fun PendingPhoto(
    photo: AssistantPhoto?,
    position: Int,
    colors: OperationalColors,
    onRemove: () -> Unit,
) {
    Box(Modifier.size(60.dp)) {
        PhotoThumb(photo, colors, size = 56, modifier = Modifier.align(Alignment.BottomStart))
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .clip(CircleShape)
                .background(colors.textPrimary)
                .clickable(onClick = onRemove)
                .semantics { contentDescription = "Quitar la foto $position" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, null, tint = colors.background, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun Bubble(
    message: AssistantMessage,
    colors: OperationalColors,
    muted: Boolean = false,
    photos: Map<String, AssistantPhoto> = emptyMap(),
) {
    val mine = message.fromOwner
    val photoIds = PHOTO_TOKEN.findAll(message.text).map { it.groupValues[1] }.toList()
    val text =
        message.text
            .replace(PHOTO_TOKEN, "")
            .replace(Regex(" {2,}"), " ")
            .trim()
    if (mine) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            photoIds.forEach { id ->
                PhotoThumb(photos[id], colors, size = 150, modifier = Modifier.padding(bottom = 4.dp))
            }
            if (text.isNotEmpty()) {
                Text(
                    text,
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.background,
                    modifier =
                        Modifier
                            .widthIn(max = 290.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp,
                                    bottomStart = 20.dp,
                                    bottomEnd = 6.dp,
                                ),
                            ).background(colors.textPrimary)
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                )
            }
        }
    } else {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val tint = if (colors.isDark) colors.accentLime else colors.accentInk
                Icon(Icons.Rounded.AutoAwesome, null, tint = tint, modifier = Modifier.size(14.dp))
                Text(
                    "Asistente",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tint,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            photoIds.forEach { id ->
                PhotoThumb(photos[id], colors, size = 150, modifier = Modifier.padding(top = 6.dp))
            }
            if (text.isNotEmpty()) {
                Text(
                    text,
                    fontSize = 16.5.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (muted) colors.textMuted else colors.textPrimary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun AssistantReview(
    proposal: AssistantProposal,
    state: StoreAssistantState,
    colors: OperationalColors,
    readOnly: Boolean,
    onBack: () -> Unit,
    onAccept: () -> Unit,
) {
    val changes = proposal.changes
    val reasons = proposal.sectionReasons
    Column(Modifier.fillMaxSize()) {
        AssistantHeader(
            title = "Revisa tu tienda",
            subtitle = "Asistente",
            backLabel = "Seguir platicando",
            colors = colors,
            onBack = onBack,
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            item {
                Text(
                    "Con lo que me contaste, así quedaría tu tienda. Nada se guarda hasta que lo aceptes.",
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
            if (state.failures.isNotEmpty()) {
                item {
                    ReviewSection("No se pudo guardar todo", colors) {
                        state.failures.forEachIndexed { index, (label, reason) ->
                            if (index > 0) AdminRowDivider(colors)
                            ItemRow(label, reason, colors, mark = false)
                        }
                    }
                }
            }
            if (changes.isNotEmpty()) {
                item {
                    ReviewSection("Ajustes", colors) {
                        changes.forEachIndexed { index, change ->
                            if (index > 0) AdminRowDivider(colors)
                            ChangeRow(change, colors)
                        }
                    }
                }
            }
            if (proposal.spaces.isNotEmpty()) {
                item {
                    ReviewSection("Mesas y lugares", colors, reasons["espacios"]) {
                        proposal.spaces.forEachIndexed { index, group ->
                            if (index > 0) AdminRowDivider(colors)
                            ItemRow(
                                AssistantLabels.spaceType(group.type),
                                "${group.prefix} 1 a ${group.prefix} ${group.count} · cada uno con su QR" +
                                    group.hourlyPrice?.let { " · $$it la hora" }.orEmpty(),
                                colors,
                            )
                        }
                    }
                }
            }
            proposal.menu.forEachIndexed { index, category ->
                item(key = "menu-$index") {
                    ReviewSection(
                        if (index == 0) "Menú" else null,
                        colors,
                        if (index == 0) reasons["menu"] else null,
                    ) {
                        Text(
                            if (category.existingId != null) "${category.name} · ya la tienes" else category.name,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                        category.products.forEach { product ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                product.photoId?.let { PhotoThumb(state.photos[it], colors, size = 40) }
                                Column(
                                    Modifier.weight(1f).padding(
                                        start =
                                            if (product.photoId !=
                                                null
                                            ) {
                                                10.dp
                                            } else {
                                                0.dp
                                            },
                                    ),
                                ) {
                                    Text(
                                        product.name,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                    )
                                    Text(
                                        listOfNotNull(product.description, AssistantLabels.station(product.station))
                                            .joinToString(" · "),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textMuted,
                                    )
                                }
                                Text(
                                    AssistantLabels.money(product.price),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (colors.isDark) colors.accentLime else colors.textPrimary,
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
            proposal.courtHours?.let { hours ->
                item {
                    ReviewSection(
                        "Horario de las pistas",
                        colors,
                        reasons["horario_pistas"] ?: "Cuándo se pueden rentar. Es aparte del horario de pedidos.",
                    ) {
                        ItemRow("Se rentan", AssistantLabels.courtHours(hours), colors)
                    }
                }
            }
            if (proposal.supplies.isNotEmpty()) {
                item {
                    ReviewSection("Insumos", colors, reasons["insumos"] ?: "Se dan de alta en tu inventario.") {
                        proposal.supplies.forEachIndexed { index, supply ->
                            if (index > 0) AdminRowDivider(colors)
                            ItemRow(supply.name, AssistantLabels.supply(supply), colors)
                        }
                    }
                }
            }
            if (proposal.recipes.isNotEmpty()) {
                item {
                    ReviewSection(
                        "Recetas",
                        colors,
                        reasons["recetas"]
                            ?: "Cada venta descuenta esto de tu inventario. Lo demás de cada receta se queda igual.",
                    ) {
                        proposal.recipes.forEachIndexed { index, recipe ->
                            if (index > 0) AdminRowDivider(colors)
                            ItemRow(recipe.productName, AssistantLabels.recipe(recipe), colors)
                        }
                    }
                }
            }
            if (proposal.productPhotos.isNotEmpty()) {
                item {
                    ReviewSection(
                        "Fotos de productos",
                        colors,
                        reasons["fotos_productos"] ?: "Se ponen a productos que ya tienes en el menú.",
                    ) {
                        proposal.productPhotos.forEach { productPhoto ->
                            Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                PhotoThumb(state.photos[productPhoto.photoId], colors, size = 52)
                                Text(
                                    productPhoto.name,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.textPrimary,
                                    modifier = Modifier.padding(start = 12.dp),
                                )
                            }
                        }
                    }
                }
            }
            if (proposal.storePhotos.isNotEmpty()) {
                item {
                    ReviewSection("Fotos de tu tienda", colors, reasons["fotos"]) {
                        proposal.storePhotos.forEach { (kind, id) ->
                            Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                PhotoThumb(state.photos[id], colors, size = 52)
                                Text(
                                    if (kind == "logo") "Logo" else "Foto del local",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.textPrimary,
                                    modifier = Modifier.padding(start = 12.dp),
                                )
                            }
                        }
                    }
                }
            }
            if (proposal.invites.isNotEmpty()) {
                item {
                    ReviewSection(
                        "Tu equipo",
                        colors,
                        reasons["personal"] ?: "A cada uno le llega una invitación por correo.",
                    ) {
                        proposal.invites.forEachIndexed { index, invite ->
                            if (index > 0) AdminRowDivider(colors)
                            ItemRow(invite.email, AssistantLabels.role(invite.role), colors)
                        }
                    }
                }
            }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                "${proposal.itemCount} ${if (proposal.itemCount == 1) "cosa" else "cosas"} por guardar · " +
                    "Lo demás se queda como está",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                textAlign = TextAlign.Center,
            )
            (state.progress ?: state.error)?.let {
                Text(
                    it,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                )
            }
            if (readOnly) {
                Text(
                    "Tu tienda está en solo consulta: no se pueden guardar cambios.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                )
            }
            AdminPrimaryButton(
                title = if (state.failures.isEmpty()) "Aceptar y guardar" else "Reintentar lo que faltó",
                colors = colors,
                busy = state.applying,
                enabled = !readOnly && !state.applying,
                fill = colors.accentLime,
                contentColor = colors.accentInk,
                onClick = onAccept,
            )
            AdminSecondaryButton(
                title = "Seguir platicando",
                colors = colors,
                filled = false,
                enabled = !state.applying,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                onClick = onBack,
            )
        }
    }
}

@Composable
private fun ReviewSection(
    title: String?,
    colors: OperationalColors,
    note: String? = null,
    content: @Composable () -> Unit,
) {
    Column(Modifier.padding(top = if (title != null) 14.dp else 8.dp)) {
        title?.let {
            Text(
                it,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp).semantics { heading() },
            )
        }
        AdminPanel(colors, padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) { content() }
        note?.let {
            Text(
                it,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
        }
    }
}

@Composable
private fun ItemRow(
    title: String,
    detail: String,
    colors: OperationalColors,
    mark: Boolean = true,
) {
    ChangeRow(AssistantChange(field = "", title = title, value = detail, reason = null), colors, mark)
}

@Composable
private fun PhotoThumb(
    photo: AssistantPhoto?,
    colors: OperationalColors,
    size: Int,
    modifier: Modifier = Modifier,
) {
    val bitmap: ImageBitmap? =
        remember(photo) {
            photo?.image?.bytes?.let { bytes ->
                runCatching {
                    BitmapFactory
                        .decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = 4 })
                        ?.asImageBitmap()
                }.getOrNull()
            }
        }
    Box(
        modifier
            .size(size.dp)
            .clip(RoundedCornerShape(if (size > 60) 16.dp else 10.dp))
            .background(colors.cardBackground),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap,
                contentDescription = "Foto",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(Icons.Rounded.AddPhotoAlternate, null, tint = colors.textMuted, modifier = Modifier.size(18.dp))
        }
    }
}

private const val COUNTER_FROM_PERCENT = 80

private val PHOTO_TOKEN = Regex("""\[(foto\d+)]""")

private fun PreparedProductImage.toUpload() = AssistantUpload(bytes, filename, mimeType)

private val AssistantProposal.itemCount: Int
    get() =
        changes.size + spaces.size + productCount + productPhotos.size + storePhotos.size + invites.size +
            supplies.size + recipes.size + (if (courtHours != null) 1 else 0)

@Composable
private fun ChangeRow(
    change: AssistantChange,
    colors: OperationalColors,
    mark: Boolean = true,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp)) {
        Box(
            Modifier
                .padding(top = 1.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (mark) colors.accentLime else colors.cardBorder),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (mark) Icons.Rounded.Check else Icons.Rounded.Close,
                null,
                tint = if (mark) colors.accentInk else colors.textPrimary,
                modifier = Modifier.size(13.dp),
            )
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(change.title, fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
            Text(
                change.value,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (colors.isDark) colors.accentLime else colors.textPrimary,
                modifier = Modifier.padding(top = 2.dp),
            )
            change.reason?.let {
                Text(
                    it,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/** En Flujo de mi tienda y en «Para dejar lista tu tienda»: abre el asistente. */
@Composable
internal fun AssistantEntryCard(
    colors: OperationalColors,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.accentLime)
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.AutoAwesome, null, tint = colors.accentInk, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                "Armarlo con el asistente",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = colors.accentInk,
            )
            Text(
                "Te pregunta cómo trabajas y te deja todo listo para revisar",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.accentInk.copy(alpha = 0.8f),
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.ArrowForwardIos,
            null,
            tint = colors.accentInk,
            modifier = Modifier.size(14.dp),
        )
    }
}
