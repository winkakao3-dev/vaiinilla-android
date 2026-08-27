package com.vaiinilla.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.assistant.AssistantChatMessage
import com.vaiinilla.app.ui.components.EditorialConfirmSheet
import com.vaiinilla.app.ui.components.VaiinillaBottomNavClearance
import com.vaiinilla.app.ui.components.VaiinillaMark
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.launch

private val chatSuggestions =
    listOf(
        "¿Qué es bueno sin gluten?",
        "Algo ligero y fresco",
        "¿Qué recomiendas?",
    )

private val bottomNavClearance = VaiinillaBottomNavClearance

@Composable
fun AssistantChatScreen(
    state: OrderFlowUiState,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onClose: () -> Unit,
    onOrders: () -> Unit,
    onWallet: () -> Unit,
    onCart: () -> Unit,
    embeddedInBottomNav: Boolean = true,
) {
    val colors = LocalVaiinillaColors.current
    val messages = state.assistantChatMessages
    var input by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }
    val reduceMotion = rememberReducedMotion()
    val listState = rememberLazyListState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            if (reduceMotion) {
                listState.scrollToItem(messages.lastIndex)
            } else {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        onSendMessage(trimmed)
        input = ""
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(colors.accent.copy(alpha = 0.14f), Color.Transparent),
                            radius = 900f,
                            center = Offset(0.5f, 0f),
                        ),
                    ),
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding(),
        ) {
            AssistantChatHeader(
                onClear = {
                    if (messages.isEmpty()) {
                        onClearChat()
                    } else {
                        showClearConfirm = true
                    }
                },
                onClose = onClose,
            )

            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                contentPadding =
                    PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 4.dp,
                        bottom = 8.dp,
                    ),
                verticalArrangement =
                    if (messages.isEmpty()) {
                        Arrangement.Center
                    } else {
                        Arrangement.spacedBy(10.dp)
                    },
            ) {
                if (messages.isEmpty()) {
                    item(key = "welcome") {
                        AssistantWelcomeContent(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                            onSuggestionClick = ::sendMessage,
                        )
                    }
                }

                items(messages, key = { "${it.fromUser}-${it.text.hashCode()}" }) { message ->
                    ChatBubble(message = message, reduceMotion = reduceMotion)
                }
            }

            ChatComposer(
                value = input,
                onValueChange = { input = it },
                onSend = { sendMessage(input) },
                bringIntoViewRequester = bringIntoViewRequester,
                onFocus = {
                    scope.launch {
                        bringIntoViewRequester.bringIntoView()
                    }
                },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            )

            if (embeddedInBottomNav) {
                Spacer(Modifier.height(bottomNavClearance))
            }
        }

        if (showClearConfirm) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(colors.paper.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                EditorialConfirmSheet(
                    title = "¿Limpiar conversación?",
                    message = "Se borrará el historial del asistente en esta sesión.",
                    confirmLabel = "Limpiar",
                    dismissLabel = "Cancelar",
                    onConfirm = {
                        onClearChat()
                        showClearConfirm = false
                    },
                    onDismiss = { showClearConfirm = false },
                )
            }
        }
    }
}

@Preview(name = "Chat del asistente", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun AssistantChatScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        AssistantChatScreen(
            state = OrderFlowUiState(),
            onSendMessage = {},
            onClearChat = {},
            onClose = {},
            onOrders = {},
            onWallet = {},
            onCart = {},
            embeddedInBottomNav = false,
        )
    }
}

@Composable
private fun AssistantChatHeader(
    onClear: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val headerButtonShape = RoundedCornerShape(13.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Asistente Vaiinilla",
            color = colors.ink,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            textAlign = TextAlign.Start,
            modifier =
                Modifier
                    .weight(1f),
        )
        IconButton(
            onClick = onClear,
            modifier = Modifier.size(48.dp).background(colors.paper2, headerButtonShape),
        ) {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Limpiar chat", tint = colors.accentInk)
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(48.dp).background(colors.paper2, headerButtonShape),
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = colors.accentInk)
        }
    }
}

@Composable
private fun AssistantWelcomeContent(
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VaiinillaMark(
            modifier = Modifier.size(width = 132.dp, height = 110.dp),
            cream = colors.ink,
        )
        Text(
            "¡Hola! Soy tu Asistente Vaiinilla. Pregúntame sobre el menú: dietas, recomendaciones, ingredientes y más.",
            color = colors.muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 240.dp).padding(top = 12.dp),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 300.dp)
                    .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            chatSuggestions.forEach { suggestion ->
                ChatSuggestionChip(
                    label = suggestion,
                    onClick = { onSuggestionClick(suggestion) },
                )
            }
        }
    }
}

@Composable
private fun ChatSuggestionChip(
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        onClick = onClick,
        color = colors.paper,
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.accent.copy(alpha = 0.55f)),
    ) {
        Text(
            label,
            color = colors.ink,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun ChatBubble(
    message: AssistantChatMessage,
    reduceMotion: Boolean,
) {
    val colors = LocalVaiinillaColors.current
    var visible by remember(message) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible || reduceMotion) 1f else 0.94f,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 180),
        label = "chat-bubble-scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible || reduceMotion) 1f else 0f,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 180),
        label = "chat-bubble-alpha",
    )
    LaunchedEffect(message) { visible = true }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth(0.88f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    },
            color = if (message.fromUser) colors.accent2 else colors.paper2,
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(
                message.text,
                color = if (message.fromUser) colors.accentInk else colors.ink,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
private fun ChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    bringIntoViewRequester: BringIntoViewRequester,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val composerShape = RoundedCornerShape(22.dp)
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = composerShape,
                    clip = false,
                    ambientColor = colors.ink.copy(alpha = 0.08f),
                    spotColor = colors.ink.copy(alpha = 0.08f),
                ).bringIntoViewRequester(bringIntoViewRequester),
        color = colors.paper2,
        shape = composerShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.ink.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier =
                    Modifier
                        .weight(1f)
                        .onFocusChanged { state ->
                            if (state.isFocused) onFocus()
                        },
                singleLine = true,
                textStyle =
                    androidx.compose.ui.text
                        .TextStyle(color = colors.ink, fontSize = 14.sp),
                decorationBox = { inner ->
                    Box {
                        if (value.isBlank()) {
                            Text("Pregúntame sobre el menú…", color = colors.muted, fontSize = 14.sp)
                        }
                        inner()
                    }
                },
            )
            IconButton(
                onClick = onSend,
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(composerShape)
                        .background(colors.accent),
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Enviar",
                    tint = colors.accentInk,
                )
            }
        }
    }
}

@Composable
internal fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            ) == 0f
        }.getOrDefault(false)
    }
}
