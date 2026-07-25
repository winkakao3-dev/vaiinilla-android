package com.vaiinilla.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.ui.components.paymentMethodLabel
import com.vaiinilla.app.ui.components.sticker.StickerOrderData
import com.vaiinilla.app.ui.components.sticker.StickerSize
import com.vaiinilla.app.ui.components.sticker.StickerStyle
import com.vaiinilla.app.ui.components.sticker.StickerStyleContent
import com.vaiinilla.app.ui.components.sticker.demoStickerOrderData
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReceiptStickerScreen(
    order: OrderDetail?,
    onBack: () -> Unit,
    initialStyleIndex: Int = 0,
) {
    val colors = LocalVaiinillaColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reducedMotion = remember {
        runCatching {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
            ) == 0f
        }.getOrDefault(false)
    }
    val stickerOrder = remember(order) { order.toStickerOrderData() }
    val styles = StickerStyle.entries
    val pagerState = rememberPagerState(
        initialPage = initialStyleIndex.coerceIn(0, (styles.size - 1).coerceAtLeast(0)),
        pageCount = { styles.size },
    )
    var selectedSize by rememberSaveable { mutableStateOf(StickerSize.M) }
    var entered by remember { mutableStateOf(reducedMotion) }

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            entered = true
        }
    }

    val enterScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.96f,
        animationSpec = if (reducedMotion) tween(0) else tween(220),
        label = "sticker-enter-scale",
    )
    val enterAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = if (reducedMotion) tween(0) else tween(220),
        label = "sticker-enter-alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = colors.ink)
                }
                Text(
                    "Tu receipt sticker",
                    modifier = Modifier.weight(1f),
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                )
                IconButton(onClick = { /* share no-op */ }) {
                    Icon(Icons.Outlined.Share, contentDescription = "Compartir", tint = colors.ink)
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(styles) { style ->
                    StyleChip(
                        label = style.label,
                        selected = styles[pagerState.currentPage] == style,
                        onClick = {
                            val index = styles.indexOf(style)
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    index,
                                    animationSpec = if (reducedMotion) {
                                        tween(0)
                                    } else {
                                        spring(stiffness = Spring.StiffnessMediumLow)
                                    },
                                )
                            }
                        },
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                pageSpacing = 12.dp,
            ) { page ->
                val style = styles[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = enterScale
                            scaleY = enterScale
                            alpha = enterAlpha
                        },
                    contentAlignment = Alignment.TopCenter,
                ) {
                    StickerStyleContent(
                        style = style,
                        order = stickerOrder,
                        size = selectedSize,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (styles[pagerState.currentPage] == StickerStyle.Editorial) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(StickerSize.entries) { size ->
                        StyleChip(
                            label = size.label,
                            selected = selectedSize == size,
                            onClick = { selectedSize = size },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        onClick = onClick,
        color = if (selected) colors.accent else colors.paper2,
        shape = RoundedCornerShape(99.dp),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (selected) colors.accentInk else colors.ink,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
        )
    }
}

private fun OrderDetail?.toStickerOrderData(): StickerOrderData {
    if (this == null) return demoStickerOrderData()
    val productName = items.firstOrNull()?.productName ?: "Burrito norteño"
    val paymentLabel = paymentMethodLabel(summary.paymentMethod)
    return StickerOrderData(
        folio = summary.folio,
        total = summary.total,
        productName = productName,
        paymentLabel = paymentLabel,
        destinationLabel = summary.destination.label,
        date = summary.operationalDate,
    )
}
