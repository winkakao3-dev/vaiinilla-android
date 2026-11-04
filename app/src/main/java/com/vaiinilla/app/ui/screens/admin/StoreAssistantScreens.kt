package com.vaiinilla.app.ui.screens.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
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
import com.vaiinilla.app.domain.admin.AssistantMessage
import com.vaiinilla.app.ui.admin.AdminStoreViewModel
import com.vaiinilla.app.ui.admin.StoreAssistantState
import com.vaiinilla.app.ui.screens.OperationalColors

/**
 * Bienvenida del admin nuevo (marco AS1): la tienda todavía no está lista y se ofrece armarla platicando con el
 * asistente o hacerlo a mano en Flujo de mi tienda. Se ve una sola vez por tienda; "Ahora no" también la cierra.
 */
@OptIn(ExperimentalLayoutApi::class)
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
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.accentLime),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = colors.accentInk, modifier = Modifier.size(34.dp))
            }
            AdminCapsLabel("Bienvenido a Vaiinilla", colors, Modifier.padding(top = 22.dp), inset = false)
            Text(
                if (placeName.isBlank()) "Armemos tu tienda" else "Armemos tu tienda, $placeName",
                fontSize = 32.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.8).sp,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 6.dp).semantics { heading() },
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
                    .padding(top = 26.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.accentLime)
                    .clickable(onClick = onAssistant)
                    .padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Platícalo con el asistente",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.accentInk,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "RECOMENDADO",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = colors.accentInk,
                        modifier =
                            Modifier
                                .clip(CircleShape)
                                .background(colors.accentInk.copy(alpha = 0.12f))
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                    )
                }
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                listOf("Tipo de negocio", "Cuándo pagan", "Cómo se entrega", "Horarios", "Cocina y barra").forEach {
                    StaticChip(it, colors)
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
                changes = proposal.changes,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssistantChat(
    viewModel: AdminStoreViewModel,
    state: StoreAssistantState,
    colors: OperationalColors,
    onClose: () -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val list = rememberLazyListState()
    val rows = state.messages.size + 2
    LaunchedEffect(state.messages.size, state.thinking, state.error, state.proposal) {
        list.animateScrollToItem((rows - 1).coerceAtLeast(0))
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AdminRoundButton(Icons.AutoMirrored.Outlined.ArrowBack, "Regresar", colors, onClose)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                AdminCapsLabel("Flujo de mi tienda", colors, inset = false)
                Text(
                    "Asistente",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
            }
            if (state.messages.size > 1) {
                AdminRoundButton(Icons.Rounded.Refresh, "Empezar de nuevo", colors, viewModel::restartAssistant)
            }
        }
        LazyColumn(
            state = list,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(state.messages, key = { index, _ -> index }) { _, message -> Bubble(message, colors) }
            item(key = "estado") {
                when {
                    state.thinking ->
                        Bubble(
                            AssistantMessage(fromOwner = false, text = "Escribiendo…"),
                            colors,
                            muted = true,
                        )
                    state.error != null ->
                        Column(Modifier.padding(start = 36.dp)) {
                            Text(
                                state.error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary,
                            )
                            Box(Modifier.padding(top = 8.dp)) {
                                AdminFilterChip(
                                    "Reintentar",
                                    selected = false,
                                    colors = colors,
                                    onClick = viewModel::retryAssistant,
                                )
                            }
                        }
                    state.options.isNotEmpty() ->
                        FlowRow(
                            Modifier.padding(start = 36.dp, top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            state.options.forEach { option ->
                                AdminFilterChip(option, selected = false, colors = colors, onClick = {
                                    viewModel.sendToAssistant(option)
                                })
                            }
                        }
                    else -> Spacer(Modifier.height(1.dp))
                }
            }
            item(key = "propuesta") {
                val proposal = state.proposal
                if (proposal != null && !state.thinking) {
                    AdminPrimaryButton(
                        title = "Revisar la propuesta (${proposal.changes.size})",
                        colors = colors,
                        icon = Icons.Rounded.Check,
                        fill = colors.accentLime,
                        contentColor = colors.accentInk,
                        modifier = Modifier.padding(top = 8.dp),
                        onClick = { viewModel.reviewProposal(true) },
                    )
                } else {
                    Spacer(Modifier.height(1.dp))
                }
            }
        }
        val send = {
            if (state.canSend && draft.isNotBlank()) {
                viewModel.sendToAssistant(draft)
                draft = ""
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
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = draft,
                onValueChange = { draft = it.take(800) },
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
                        .heightIn(min = 50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(colors.cardBackground)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(25.dp))
                        .semantics { contentDescription = "Escribe tu respuesta" },
                decorationBox = { field ->
                    Box(
                        Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
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
            val canSend = state.canSend && draft.isNotBlank()
            Box(
                Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(if (canSend) colors.accentLime else colors.cardBackground)
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
    }
}

@Composable
private fun Bubble(
    message: AssistantMessage,
    colors: OperationalColors,
    muted: Boolean = false,
) {
    val mine = message.fromOwner
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!mine) {
            Box(
                Modifier
                    .padding(end = 8.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(colors.accentLime),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = colors.accentInk, modifier = Modifier.size(16.dp))
            }
        }
        Text(
            message.text,
            fontSize = 14.5.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color =
                when {
                    mine -> colors.background
                    muted -> colors.textMuted
                    else -> colors.textPrimary
                },
            modifier =
                Modifier
                    .widthIn(max = 290.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (mine) 20.dp else 6.dp,
                            bottomEnd = if (mine) 6.dp else 20.dp,
                        ),
                    ).background(if (mine) colors.textPrimary else colors.cardBackground)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
        )
    }
}

@Composable
private fun AssistantReview(
    changes: List<AssistantChange>,
    state: StoreAssistantState,
    colors: OperationalColors,
    readOnly: Boolean,
    onBack: () -> Unit,
    onAccept: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AdminRoundButton(Icons.AutoMirrored.Outlined.ArrowBack, "Seguir platicando", colors, onBack)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                AdminCapsLabel("Asistente", colors, inset = false)
                Text(
                    "Revisa tu configuración",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }
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
            item {
                AdminPanel(
                    colors,
                    modifier = Modifier.padding(top = 14.dp),
                    padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    changes.forEachIndexed { index, change ->
                        if (index > 0) AdminRowDivider(colors)
                        ChangeRow(change, colors)
                    }
                }
            }
            item {
                Text(
                    "${changes.size} ${if (changes.size == 1) "cambio" else "cambios"} · Lo demás se queda como está",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            state.error?.let {
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
                title = "Aceptar y guardar",
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
private fun ChangeRow(
    change: AssistantChange,
    colors: OperationalColors,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp)) {
        Box(
            Modifier
                .padding(top = 1.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(colors.accentLime),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Check, null, tint = colors.accentInk, modifier = Modifier.size(13.dp))
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

@Composable
private fun StaticChip(
    label: String,
    colors: OperationalColors,
) {
    Text(
        label,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = colors.textSecondary,
        modifier =
            Modifier
                .clip(CircleShape)
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, CircleShape)
                .padding(horizontal = 13.dp, vertical = 8.dp),
    )
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
