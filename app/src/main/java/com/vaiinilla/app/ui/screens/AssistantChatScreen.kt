package com.vaiinilla.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.assistant.AssistantLocalReplies
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.VaiinillaBottomNav
import com.vaiinilla.app.ui.components.VaiinillaMark
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.order.cartItemCount
import com.vaiinilla.app.ui.theme.AccentInk
import com.vaiinilla.app.ui.theme.Cream
import com.vaiinilla.app.ui.theme.CreamDeep
import com.vaiinilla.app.ui.theme.Ink
import com.vaiinilla.app.ui.theme.Lime
import com.vaiinilla.app.ui.theme.LimeSoft
import com.vaiinilla.app.ui.theme.MutedInk

private val chatSuggestions = listOf(
    "¿Qué es bueno sin gluten?",
    "Algo ligero y fresco",
    "¿Qué recomiendas?",
)

private data class ChatMessage(
    val text: String,
    val fromUser: Boolean,
)

@Composable
fun AssistantChatScreen(
    state: OrderFlowUiState,
    onClose: () -> Unit,
    onMenu: () -> Unit,
    onOrders: () -> Unit,
    onWallet: () -> Unit,
    onCart: () -> Unit,
) {
    val products = state.catalog?.products.orEmpty()
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    val reduceMotion = rememberReducedMotion()

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        messages.add(ChatMessage(trimmed, fromUser = true))
        messages.add(ChatMessage(AssistantLocalReplies.reply(trimmed, products), fromUser = false))
        input = ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Lime.copy(alpha = 0.16f), Color.Transparent),
                    radius = 900f,
                    center = androidx.compose.ui.geometry.Offset(0.5f, 0f),
                ),
            )
            .background(Cream),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = LimeSoft.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(13.dp),
                ) {
                    Text(
                        "✦",
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        color = AccentInk,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                    )
                }
                Text(
                    "Asistente Vaiinilla",
                    color = Ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 9.dp),
                )
                IconButton(onClick = { messages.clear() }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Limpiar chat", tint = Ink)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = Ink)
                }
            }

            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    VaiinillaMark(modifier = Modifier.size(width = 132.dp, height = 110.dp))
                    Text(
                        "¡Hola! Soy tu Asistente Vaiinilla. Pregúntame sobre el menú: dietas, recomendaciones, ingredientes y más.",
                        color = MutedInk,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        chatSuggestions.forEach { suggestion ->
                            ChatSuggestionChip(
                                label = suggestion,
                                onClick = { sendMessage(suggestion) },
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(messages, key = { "${it.fromUser}-${it.text.hashCode()}-${messages.indexOf(it)}" }) { message ->
                        ChatBubble(message = message, reduceMotion = reduceMotion)
                    }
                    item { Spacer(Modifier.size(88.dp)) }
                }
            }

            ChatComposer(
                value = input,
                onValueChange = { input = it },
                onSend = { sendMessage(input) },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 96.dp, top = 8.dp),
            )
        }

        VaiinillaBottomNav(
            activeTab = StudentTab.ASSISTANT,
            cartCount = state.cartItemCount,
            onMenu = onMenu,
            onAssistant = {},
            onOrders = onOrders,
            onWallet = onWallet,
            onCart = onCart,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun ChatSuggestionChip(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = Cream,
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Lime.copy(alpha = 0.55f)),
    ) {
        Text(
            label,
            color = Ink,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, reduceMotion: Boolean) {
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
    androidx.compose.runtime.LaunchedEffect(message) { visible = true }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                },
            color = if (message.fromUser) LimeSoft else CreamDeep,
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(
                message.text,
                color = Ink,
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
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CreamDeep,
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Ink.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 14.sp),
                decorationBox = { inner ->
                    Box {
                        if (value.isBlank()) {
                            Text("Pregúntame sobre el menú…", color = MutedInk, fontSize = 14.sp)
                        }
                        inner()
                    }
                },
            )
            IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Mic, contentDescription = "Micrófono", tint = Ink)
            }
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Lime),
            ) {
                Icon(Icons.Outlined.Send, contentDescription = "Enviar", tint = AccentInk)
            }
        }
    }
}
