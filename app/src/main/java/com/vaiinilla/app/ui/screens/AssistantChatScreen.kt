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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Mic
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.assistant.AssistantChatMessage
import com.vaiinilla.app.ui.components.EditorialConfirmSheet
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.VaiinillaBottomNav
import com.vaiinilla.app.ui.components.VaiinillaBottomNavClearance
import com.vaiinilla.app.ui.components.VaiinillaMark
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.cartItemCount
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
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
    onMenu: () -> Unit,
    onOrders: () -> Unit,
    onWallet: () -> Unit,
    onCart: () -> Unit,
    embeddedInBottomNav: Boolean = true,
    showDemoTabs: Boolean = false,
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
            listState.animateScrollToItem(messages.lastIndex)
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
                            center = androidx.compose.ui.geometry.Offset(0.5f, 0f),
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
                embeddedInBottomNav = embeddedInBottomNav,
                hasMessages = messages.isNotEmpty(),
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
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

            Spacer(Modifier.height(bottomNavClearance))
        }

        VaiinillaBottomNav(
            showDemoTabs = showDemoTabs,
            activeTab = StudentTab.ASSISTANT,
            cartCount = state.cartItemCount,
            onMenu = onMenu,
            onAssistant = {},
            onOrders = onOrders,
            onWallet = onWallet,
            onCart = onCart,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
        )

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

@Composable
private fun AssistantChatHeader(
    embeddedInBottomNav: Boolean,
    hasMessages: Boolean,
    onClear: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!embeddedInBottomNav) {
            Spacer(Modifier.size(48.dp))
        } else {
            Spacer(Modifier.size(48.dp))
        }
        Text(
            "Asistente Vaiinilla",
            color = colors.ink,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
        )
        when {
            hasMessages ->
                IconButton(onClick = onClear) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Limpiar chat", tint = colors.ink)
                }
            embeddedInBottomNav ->
                Spacer(Modifier.size(48.dp))
            else ->
                Spacer(Modifier.size(48.dp))
        }
        if (!embeddedInBottomNav) {
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = colors.ink)
            }
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
        VaiinillaMark(modifier = Modifier.size(width = 96.dp, height = 76.dp))
        Text(
            "¡Hola! Soy tu Asistente Vaiinilla. Pregúntame sobre el menú: dietas, recomendaciones, ingredientes y más.",
            color = colors.muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
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
        modifier = Modifier.fillMaxWidth(),
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
                color = colors.ink,
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
    Surface(
        modifier = modifier.fillMaxWidth().bringIntoViewRequester(bringIntoViewRequester),
        color = colors.paper2,
        shape = RoundedCornerShape(22.dp),
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
            IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Mic, contentDescription = "Micrófono", tint = colors.ink)
            }
            IconButton(
                onClick = onSend,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
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
