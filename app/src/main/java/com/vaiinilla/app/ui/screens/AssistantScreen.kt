package com.vaiinilla.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.assistant.AssistantLocalReplies
import com.vaiinilla.app.ui.assistant.AssistantRecommendation
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.VaiinillaBottomNav
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.order.cartItemCount
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.theme.AccentInk
import com.vaiinilla.app.ui.theme.Cream
import com.vaiinilla.app.ui.theme.CreamDeep
import com.vaiinilla.app.ui.theme.Ink
import com.vaiinilla.app.ui.theme.Lime
import com.vaiinilla.app.ui.theme.MutedInk

private val assistantChips = listOf(
    "Rápido y llenador",
    "Menos de $60",
    "Algo ligero",
    "Combo con bebida",
)

@Composable
fun AssistantScreen(
    state: OrderFlowUiState,
    onOpenChat: () -> Unit,
    onOpenProduct: (Int) -> Unit,
    onMenu: () -> Unit,
    onOrders: () -> Unit,
    onWallet: () -> Unit,
    onCart: () -> Unit,
) {
    val products = state.catalog?.products.orEmpty()
    var selectedChip by remember { mutableStateOf(assistantChips.first()) }
    val recommendations = remember(selectedChip, products) {
        AssistantLocalReplies.filterByChip(selectedChip, products)
    }
    val reduceMotion = rememberReducedMotion()
    var animateHero by remember { mutableStateOf(reduceMotion) }
    LaunchedEffect(Unit) { animateHero = true }
    val heroAlpha by animateFloatAsState(
        targetValue = if (animateHero) 1f else 0f,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 280),
        label = "assistant-hero-alpha",
    )
    val heroOffset by animateFloatAsState(
        targetValue = if (animateHero) 0f else 12f,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 280),
        label = "assistant-hero-offset",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Asistente Vaiinilla",
                        color = Ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onOpenChat,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CreamDeep),
                    ) {
                        Icon(Icons.Outlined.Chat, contentDescription = "Abrir chat", tint = Ink)
                    }
                    Spacer(Modifier.size(8.dp))
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CreamDeep),
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones", tint = Ink)
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = heroAlpha
                            translationY = if (reduceMotion) 0f else heroOffset
                        },
                    color = Ink,
                    shape = RoundedCornerShape(32.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Pide sin pensarlo tanto",
                            color = MutedInk.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        AssistantMascot(modifier = Modifier.padding(vertical = 16.dp))
                        Text(
                            "¿Qué necesitas hoy?",
                            color = Color(0xFFF6F1E5),
                            fontWeight = FontWeight.Black,
                            fontSize = 30.sp,
                            lineHeight = 32.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        TextButton(onClick = onOpenChat, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Chatear", color = Lime, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(assistantChips) { chip ->
                        AssistantChip(
                            label = chip,
                            selected = chip == selectedChip,
                            onClick = { selectedChip = chip },
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text("Te recomendamos", color = Ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("Según tu elección", color = MutedInk, fontSize = 12.sp)
                }
            }

            items(recommendations, key = AssistantRecommendation::name) { item ->
                RecommendationRow(
                    item = item,
                    onClick = { item.productId?.let(onOpenProduct) },
                )
            }
        }

        VaiinillaBottomNav(
            activeTab = StudentTab.ASSISTANT,
            cartCount = state.cartItemCount,
            onMenu = onMenu,
            onAssistant = {},
            onOrders = onOrders,
            onWallet = onWallet,
            onCart = onCart,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun AssistantMascot(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 92.dp, height = 82.dp)) {
        val triangle = Path().apply {
            moveTo(size.width / 2f, size.height * 0.024f)
            lineTo(size.width * 0.978f, size.height * 0.915f)
            lineTo(size.width * 0.022f, size.height * 0.915f)
            close()
        }
        drawPath(triangle, Lime)
        drawCircle(AccentInk, radius = size.width * 0.043f, center = Offset(size.width * 0.38f, size.height * 0.585f))
        drawCircle(AccentInk, radius = size.width * 0.043f, center = Offset(size.width * 0.62f, size.height * 0.585f))
        drawPath(
            path = Path().apply {
                moveTo(size.width * 0.359f, size.height * 0.732f)
                quadraticBezierTo(size.width / 2f, size.height * 0.878f, size.width * 0.641f, size.height * 0.732f)
            },
            color = AccentInk,
            style = Stroke(width = size.width * 0.033f),
        )
    }
}

@Composable
private fun AssistantChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) Ink else CreamDeep)
            .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = if (selected) Cream else Ink,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun RecommendationRow(item: AssistantRecommendation, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .physicalPress(onClick = onClick),
        color = CreamDeep,
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.padding(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductImage(
                imageUrl = item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(17.dp)),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(item.name, color = Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(item.meta, color = MutedInk, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Text(moneyLabel(item.price), color = Ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
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
