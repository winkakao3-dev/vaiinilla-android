package com.vaiinilla.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.io.readBytesLimited
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.domain.model.CatalogProductDraft
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.VaiinillaAssistantOrb
import com.vaiinilla.app.ui.components.VaiinillaMark
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.operational.OperationalUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeMode
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeModeChanger
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

/**
 * Preset photos available for quick product creation without camera.
 */
data class PresetProductPhoto(
    val id: String,
    val name: String,
    val imageUrl: String,
)

val PRESET_PRODUCT_PHOTOS =
    listOf(
        PresetProductPhoto("waffle", "Waffles", "waffle"),
        PresetProductPhoto("burrito_norteno", "Burrito", "burrito_norteno"),
        PresetProductPhoto("torta", "Torta", "torta"),
        PresetProductPhoto("jamaica", "Agua de Jamaica", "jamaica"),
        PresetProductPhoto("quesa", "Quesadilla", "quesa"),
        PresetProductPhoto("fruta", "Vaso de fruta", "fruta"),
        PresetProductPhoto("sincronizada_nortena", "Sincronizada", "sincronizada_nortena"),
    )

/**
 * Adaptive operational color tokens supporting Light, Dark, and pure-pitch AMOLED.
 */
data class OperationalColors(
    val background: Color,
    val surfacePaper: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val cardInner: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentLime: Color,
    val accentInk: Color,
    val pillBackground: Color,
    val pillBorder: Color,
    val buttonSecondary: Color,
    val buttonSecondaryInk: Color,
    val highlightBorder: Color,
    val isDark: Boolean,
)

@Composable
fun rememberOperationalColors(): OperationalColors {
    val themeMode = LocalVaiinillaThemeMode.current
    val isSystemDark = isSystemInDarkTheme()
    val effectiveMode = themeMode.resolveEffectiveMode(isSystemDark)

    return when (effectiveMode) {
        VaiinillaThemeMode.Amoled ->
            OperationalColors(
                background = Color(0xFF000000),
                surfacePaper = Color(0xFF070806),
                cardBackground = Color(0xFF0D0E0C),
                cardBorder = Color(0xFF242620),
                cardInner = Color(0xFF141512),
                textPrimary = Color(0xFFFAF7F0),
                textSecondary = Color(0xFFA5A79E),
                textMuted = Color(0xFF75786E),
                accentLime = Color(0xFFB7DE63),
                accentInk = Color(0xFF000000),
                pillBackground = Color(0xFF181A16),
                pillBorder = Color(0xFF2C2F27),
                buttonSecondary = Color(0xFF1E201B),
                buttonSecondaryInk = Color(0xFFFAF7F0),
                highlightBorder = Color(0xFFB7DE63),
                isDark = true,
            )
        VaiinillaThemeMode.Dark ->
            OperationalColors(
                background = Color(0xFF1D1E1C),
                surfacePaper = Color(0xFF242622),
                cardBackground = Color(0xFF282A25),
                cardBorder = Color(0xFF3C3E37),
                cardInner = Color(0xFF32342E),
                textPrimary = Color(0xFFF7F3E7),
                textSecondary = Color(0xFFB2B4AA),
                textMuted = Color(0xFF86887E),
                accentLime = Color(0xFFB7DE63),
                accentInk = Color(0xFF171816),
                pillBackground = Color(0xFF2E312A),
                pillBorder = Color(0xFF45483E),
                buttonSecondary = Color(0xFF363931),
                buttonSecondaryInk = Color(0xFFF7F3E7),
                highlightBorder = Color(0xFFB7DE63),
                isDark = true,
            )
        else ->
            OperationalColors(
                background = Color(0xFFF7F3E7),
                surfacePaper = Color(0xFFEFEBDD),
                cardBackground = Color(0xB8FFFEF9),
                cardBorder = Color(0x2E171816),
                cardInner = Color(0xFFEEE8D8),
                textPrimary = Color(0xFF171816),
                textSecondary = Color(0xFF73766D),
                textMuted = Color(0xFF8E9087),
                accentLime = Color(0xFFB7DE63),
                accentInk = Color(0xFF171816),
                pillBackground = Color(0xFFE8E3D2),
                pillBorder = Color(0x1F171816),
                buttonSecondary = Color(0xFFE4DFCE),
                buttonSecondaryInk = Color(0xFF171816),
                highlightBorder = Color(0xFF96C83F),
                isDark = false,
            )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierOperationalScreen(
    state: OperationalUiState,
    onBack: () -> Unit,
    onOpenCashSession: () -> Unit,
    onCollect: (orderId: String, amount: String, version: Int) -> Unit,
    onDeliver: (orderId: String, version: Int) -> Unit,
    onScanDeliver: (orderId: String, version: Int) -> Unit = { _, _ -> },
    onSearchWalletClients: (String) -> Unit = {},
    onOpenWalletUserQr: () -> Unit = {},
    onReloadWallet: (userId: String, amount: String) -> Unit = { _, _ -> },
    onChangeMode: (() -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
    onToggleProductAvailable: (productId: Int, available: Boolean) -> Unit = { _, _ -> },
    onCreateCashierProduct: (CatalogProductDraft, ByteArray?, String?, String?) -> Unit = { _, _, _, _ -> },
    onUploadCashierProductImage: (Int, ByteArray, String, String) -> Unit = { _, _, _, _ -> },
) {
    val colors = rememberOperationalColors()
    val themeMode = LocalVaiinillaThemeMode.current
    val themeChanger = LocalVaiinillaThemeModeChanger.current
    val haptics = rememberVaiinillaHaptics()
    var addProductSheetOpen by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var highlightedTarget by remember { mutableStateOf<String?>(null) }
    val highlightAnim = rememberInfiniteTransition(label = "cashier_highlight")
    val highlightPulseWidth by highlightAnim.animateFloat(
        initialValue = 2f,
        targetValue = 3.5f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "cashier_highlight_width",
    )
    val highlightPulseAlpha by highlightAnim.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "cashier_highlight_alpha",
    )

    val recentOrder = state.orders.firstOrNull()
    val products = state.catalog?.products.orEmpty()
    val activeCount = products.count { it.available }
    val pausedCount = products.size - activeCount

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background),
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Topline
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        VaiinillaMark(modifier = Modifier.size(34.dp))
                        Column {
                            Text(
                                "Vaiinilla",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = colors.textPrimary,
                            )
                            Text(
                                "Cuenta de caja",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Quick Theme Switcher Button
                        IconButton(
                            onClick = {
                                haptics.selection()
                                val next =
                                    when (themeMode) {
                                        VaiinillaThemeMode.Light -> VaiinillaThemeMode.Dark
                                        VaiinillaThemeMode.Dark -> VaiinillaThemeMode.Amoled
                                        VaiinillaThemeMode.Amoled -> VaiinillaThemeMode.Light
                                        VaiinillaThemeMode.System -> VaiinillaThemeMode.Dark
                                    }
                                themeChanger?.invoke(next)
                            },
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.cardBackground)
                                    .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp)),
                        ) {
                            Icon(
                                imageVector =
                                    when (themeMode) {
                                        VaiinillaThemeMode.Light -> Icons.Outlined.LightMode
                                        VaiinillaThemeMode.Dark -> Icons.Outlined.DarkMode
                                        VaiinillaThemeMode.Amoled -> Icons.Outlined.Diamond
                                        VaiinillaThemeMode.System -> Icons.Outlined.AutoMode
                                    },
                                contentDescription = "Cambiar tema",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        // Account / Role chip
                        Box(
                            modifier =
                                Modifier
                                    .size(width = 48.dp, height = 40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.textPrimary)
                                    .clickable {
                                        haptics.impact()
                                        onChangeMode?.invoke() ?: onBack()
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "DR",
                                color = colors.background,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }

            // Title & Subtitle
            item {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        "TURNO DE HOY",
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.6.sp,
                    )
                    Text(
                        "Caja en control.",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        letterSpacing = (-1.2).sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "Entrega pedidos y mantén el menú disponible para todos.",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            // Session Alert if Closed
            if (state.cashSessionOpen == false) {
                item {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFE45244), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x1AE45244),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Sesión de caja cerrada",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = colors.textPrimary,
                                )
                                Text(
                                    "Ábrela para cobrar y recibir nuevos pedidos.",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                )
                            }
                            Button(
                                onClick = {
                                    haptics.impact()
                                    onOpenCashSession()
                                },
                                enabled = !state.acting && restrictedMode != RestrictedMode.READ_ONLY,
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = colors.accentLime,
                                        contentColor = colors.accentInk,
                                    ),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Abrir caja", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Pedido reciente section header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "Pedido reciente",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colors.textPrimary,
                    )
                    val recentStatus =
                        if (recentOrder != null) {
                            if (recentOrder.summary.state == OrderState.DELIVERED) {
                                "Completado"
                            } else {
                                "Por entregar"
                            }
                        } else {
                            "Al día"
                        }
                    Text(
                        text = recentStatus,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                    )
                }
            }

            // Hero Recent Order Card or Empty State
            item {
                if (recentOrder != null) {
                    val isDelivered = recentOrder.summary.state == OrderState.DELIVERED
                    val isOrderReady = recentOrder.summary.state == OrderState.READY
                    val highlightScan = highlightedTarget == "scan_button"

                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .shadow(12.dp, RoundedCornerShape(26.dp), spotColor = Color(0x1A171816))
                                .border(
                                    width = if (highlightScan) highlightPulseWidth.dp else 1.dp,
                                    color =
                                        if (highlightScan) {
                                            colors.accentLime.copy(
                                                alpha = highlightPulseAlpha,
                                            )
                                        } else {
                                            colors.cardBorder
                                        },
                                    shape = RoundedCornerShape(26.dp),
                                ),
                        shape = RoundedCornerShape(26.dp),
                        color = colors.cardBackground,
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column {
                                    Text(
                                        "Pedido más reciente",
                                        color = colors.textSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "#${recentOrder.summary.folio}",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = colors.textPrimary,
                                    )
                                }
                                // Status Pill
                                val pillBg = if (isDelivered) colors.cardInner else colors.accentLime
                                val pillColor = if (isDelivered) colors.textSecondary else colors.accentInk
                                Row(
                                    modifier =
                                        Modifier
                                            .clip(CircleShape)
                                            .background(pillBg)
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(pillColor),
                                    )
                                    Text(
                                        if (isDelivered) {
                                            "ENTREGADO"
                                        } else if (isOrderReady) {
                                            "LISTO"
                                        } else {
                                            "EN ESPERA"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pillColor,
                                    )
                                }
                            }

                            // Order Items List
                            Column(
                                modifier = Modifier.padding(vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                recentOrder.items.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colors.cardInner),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                "${item.quantity}",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                color = colors.textPrimary,
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                item.productName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = colors.textPrimary,
                                            )
                                            if (item.options.isNotEmpty()) {
                                                Text(
                                                    item.options.joinToString(" · ") { it.name },
                                                    color = colors.textSecondary,
                                                    fontSize = 12.sp,
                                                )
                                            }
                                        }
                                        Text(
                                            "$${item.unitDigitalPrice}",
                                            color = colors.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }

                            // Meta Summary
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Total comanda",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary,
                                )
                                Text(
                                    "$${recentOrder.summary.total}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colors.textPrimary,
                                )
                            }

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Button(
                                    onClick = {
                                        haptics.impact()
                                        onScanDeliver(recentOrder.summary.id, recentOrder.summary.version)
                                    },
                                    enabled = !isDelivered && restrictedMode != RestrictedMode.READ_ONLY,
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = colors.textPrimary,
                                            contentColor = colors.background,
                                            disabledContainerColor = colors.cardInner,
                                            disabledContentColor = colors.textMuted,
                                        ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.QrCodeScanner,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Escanear QR", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = {
                                        haptics.success()
                                        onDeliver(recentOrder.summary.id, recentOrder.summary.version)
                                        toastMessage = "Entregando comanda #${recentOrder.summary.folio}…"
                                    },
                                    enabled = !isDelivered && restrictedMode != RestrictedMode.READ_ONLY,
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = colors.buttonSecondary,
                                            contentColor = colors.buttonSecondaryInk,
                                            disabledContainerColor = colors.cardInner,
                                            disabledContentColor = colors.textMuted,
                                        ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                ) {
                                    Text("Entregar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.cardBorder, RoundedCornerShape(26.dp)),
                        shape = RoundedCornerShape(26.dp),
                        color = colors.cardBackground,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "No hay pedidos pendientes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = "Los pedidos que entren por ventanilla o app aparecerán aquí.",
                                fontSize = 13.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            // Productos Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Productos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = colors.textPrimary,
                        )
                        Text(
                            "$activeCount activos · $pausedCount pausados",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    // Glass FAB for Add Product
                    val highlightAdd = highlightedTarget == "add_product"
                    Box(
                        modifier =
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (highlightAdd) colors.accentLime else colors.cardBackground)
                                .border(
                                    width = if (highlightAdd) highlightPulseWidth.dp else 1.dp,
                                    color =
                                        if (highlightAdd) {
                                            colors.accentLime.copy(
                                                alpha = highlightPulseAlpha,
                                            )
                                        } else {
                                            colors.cardBorder
                                        },
                                    shape = CircleShape,
                                ).shadow(8.dp, CircleShape, spotColor = Color(0x1F171816))
                                .clickable {
                                    haptics.impact()
                                    addProductSheetOpen = true
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "Agregar producto",
                            tint = if (highlightAdd) colors.accentInk else colors.textPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            // Products List
            if (products.isNotEmpty()) {
                items(products, key = { it.id }) { product ->
                    ProductRowCard(
                        product = product,
                        highlightSwitch = highlightedTarget == "product_switch",
                        colors = colors,
                        onToggle = { isAvailable ->
                            haptics.selection()
                            onToggleProductAvailable(product.id, isAvailable)
                        },
                    )
                }
            } else {
                item {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        color = colors.cardBackground,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                "Catálogo vacío",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = "Usa el botón '+' para registrar el primer producto de la cafetería.",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        // Floating Toast Notification
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.textPrimary,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier.size(20.dp).clip(CircleShape).background(colors.accentLime),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = colors.accentInk,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    Text(
                        toastMessage.orEmpty(),
                        color = colors.background,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Floating Assistant Orb
        VaiinillaAssistantOrb(
            role = OperationalRole.CASHIER,
            onHighlightTarget = { highlightedTarget = it },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 28.dp),
        )

        // Sheet: Add Product
        if (addProductSheetOpen) {
            val context = LocalContext.current
            AddProductSheet(
                onDismiss = { addProductSheetOpen = false },
                onAdd = { name, price, imageUri, station ->
                    val draft =
                        CatalogProductDraft(
                            categoryId =
                                state.catalog
                                    ?.categories
                                    ?.firstOrNull()
                                    ?.id ?: 1,
                            preparationStation = station,
                            name = name,
                            description = "",
                            ingredients = "",
                            allergens = "",
                            estimatedTimeMinutes = 5,
                            counterPrice = price.toString(),
                            available = true,
                        )
                    var imageBytes: ByteArray? = null
                    var imageFilename: String? = null
                    var imageMime: String? = null
                    val isFileUri =
                        imageUri != null &&
                            (imageUri.startsWith("content://") || imageUri.startsWith("file://"))
                    if (isFileUri) {
                        runCatching {
                            val uri = Uri.parse(imageUri)
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                imageBytes = stream.readBytesLimited(5 * 1024 * 1024)
                                imageFilename = "product_${System.currentTimeMillis()}.jpg"
                                imageMime = "image/jpeg"
                            }
                        }
                    }
                    onCreateCashierProduct(draft, imageBytes, imageFilename, imageMime)
                    addProductSheetOpen = false
                    toastMessage = "$name registrado para el menú"
                },
            )
        }
    }
}

@Composable
private fun ProductRowCard(
    product: Product,
    highlightSwitch: Boolean,
    colors: OperationalColors,
    onToggle: (Boolean) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, colors.cardBorder, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = colors.cardBackground,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                // Product Thumbnail
                Box(
                    modifier =
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.cardInner),
                    contentAlignment = Alignment.Center,
                ) {
                    ProductImage(
                        imageUrl = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                val stationLabel =
                    if (product.preparationStation == PreparationStation.KITCHEN) {
                        "Cocina"
                    } else {
                        "Barra"
                    }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "$${product.digitalPrice} · $stationLabel",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Switch Availability
            Box(
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .border(
                            width = if (highlightSwitch) 2.5.dp else 0.dp,
                            color = if (highlightSwitch) colors.accentLime else Color.Transparent,
                            shape = CircleShape,
                        ).padding(4.dp),
            ) {
                Switch(
                    checked = product.available,
                    onCheckedChange = onToggle,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = colors.background,
                            checkedTrackColor = colors.accentLime,
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.cardInner,
                            uncheckedBorderColor = colors.cardBorder,
                        ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenOperationalScreen(
    state: OperationalUiState,
    onBack: () -> Unit,
    onStart: (orderId: String, version: Int) -> Unit,
    onReady: (orderId: String, version: Int) -> Unit,
    onChangeMode: (() -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
) {
    val colors = rememberOperationalColors()
    val themeMode = LocalVaiinillaThemeMode.current
    val themeChanger = LocalVaiinillaThemeModeChanger.current
    val haptics = rememberVaiinillaHaptics()
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var highlightedTarget by remember { mutableStateOf<String?>(null) }
    val highlightAnim = rememberInfiniteTransition(label = "kitchen_highlight")
    val highlightPulseWidth by highlightAnim.animateFloat(
        initialValue = 2f,
        targetValue = 3.5f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "kitchen_highlight_width",
    )
    val highlightPulseAlpha by highlightAnim.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "kitchen_highlight_alpha",
    )

    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    val activeOrder =
        state.orders.firstOrNull { it.summary.id == selectedOrderId }
            ?: state.orders.firstOrNull()
    val upcomingOrders = state.orders.filterNot { it.summary.id == activeOrder?.summary?.id }
    val orderState = activeOrder?.summary?.state ?: OrderState.PAID
    val isPreparing = orderState == OrderState.PREPARING
    val isReady = orderState == OrderState.READY || orderState == OrderState.DELIVERED

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background),
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Topline
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        VaiinillaMark(modifier = Modifier.size(34.dp))
                        Column {
                            Text(
                                "Vaiinilla",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = colors.textPrimary,
                            )
                            Text(
                                "Cuenta de cocina",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Quick Theme Switcher Button
                        IconButton(
                            onClick = {
                                haptics.selection()
                                val next =
                                    when (themeMode) {
                                        VaiinillaThemeMode.Light -> VaiinillaThemeMode.Dark
                                        VaiinillaThemeMode.Dark -> VaiinillaThemeMode.Amoled
                                        VaiinillaThemeMode.Amoled -> VaiinillaThemeMode.Light
                                        VaiinillaThemeMode.System -> VaiinillaThemeMode.Dark
                                    }
                                themeChanger?.invoke(next)
                            },
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.cardBackground)
                                    .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp)),
                        ) {
                            Icon(
                                imageVector =
                                    when (themeMode) {
                                        VaiinillaThemeMode.Light -> Icons.Outlined.LightMode
                                        VaiinillaThemeMode.Dark -> Icons.Outlined.DarkMode
                                        VaiinillaThemeMode.Amoled -> Icons.Outlined.Diamond
                                        VaiinillaThemeMode.System -> Icons.Outlined.AutoMode
                                    },
                                contentDescription = "Cambiar tema",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        // Account / Role chip
                        Box(
                            modifier =
                                Modifier
                                    .size(width = 48.dp, height = 40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.textPrimary)
                                    .clickable {
                                        haptics.impact()
                                        onChangeMode?.invoke() ?: onBack()
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "CK",
                                color = colors.background,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }

            // Title & Subtitle
            item {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        "COMANDAS EN VIVO",
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.6.sp,
                    )
                    Text(
                        "Una comanda a la vez.",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        letterSpacing = (-1.2).sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "Cambia el estado cuando el trabajo real cambie. El equipo verá la actualización.",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            // En preparación Section Label
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "En preparación",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colors.textPrimary,
                    )
                    val prepStatus =
                        if (activeOrder != null) {
                            if (isReady) {
                                "Lista para recoger"
                            } else if (isPreparing) {
                                "Preparación iniciada"
                            } else {
                                "Comanda #${activeOrder.summary.folio}"
                            }
                        } else {
                            "Al día"
                        }
                    Text(
                        text = prepStatus,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                    )
                }
            }

            // Kitchen Hero Ticket Card or Empty State
            item {
                if (activeOrder != null) {
                    val cardBorder = if (isReady) colors.accentLime else colors.cardBorder

                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .shadow(12.dp, RoundedCornerShape(26.dp), spotColor = Color(0x1A171816))
                                .border(1.5.dp, cardBorder, RoundedCornerShape(26.dp)),
                        shape = RoundedCornerShape(26.dp),
                        color = colors.cardBackground,
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column {
                                    Text(
                                        "#${activeOrder.summary.folio}",
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-1.dp).value.sp,
                                        color = colors.textPrimary,
                                    )
                                    val destName =
                                        if (activeOrder.summary.destination.name == "TAKE_AWAY") {
                                            "llevar"
                                        } else {
                                            "mesa/barra"
                                        }
                                    Text(
                                        text = "Comanda para $destName",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }

                                // Badge
                                val (badgeBg, badgeColor) =
                                    when {
                                        isReady -> colors.accentLime to colors.accentInk
                                        isPreparing ->
                                            (if (colors.isDark) Color(0xFF384B29) else Color(0xFF334429)) to
                                                (if (colors.isDark) Color(0xFFD8F28A) else Color.White)
                                        else -> colors.textPrimary to colors.background
                                    }
                                Box(
                                    modifier =
                                        Modifier
                                            .clip(CircleShape)
                                            .background(badgeBg)
                                            .padding(horizontal = 14.dp, vertical = 7.dp),
                                ) {
                                    Text(
                                        if (isReady) {
                                            "Lista"
                                        } else if (isPreparing) {
                                            "Preparando"
                                        } else {
                                            "Nueva"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor,
                                    )
                                }
                            }

                            // Items list
                            Column(
                                modifier = Modifier.padding(vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                activeOrder.items.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colors.cardInner),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                "${item.quantity}",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                color = colors.textPrimary,
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                item.productName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = colors.textPrimary,
                                            )
                                            if (item.options.isNotEmpty()) {
                                                Text(
                                                    item.options.joinToString(" · ") { it.name },
                                                    color = colors.textSecondary,
                                                    fontSize = 12.sp,
                                                )
                                            }
                                        }
                                        Text(
                                            "$${item.unitDigitalPrice}",
                                            color = colors.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }

                            // Dual Action Buttons: Preparando / Ya se preparó
                            val highlightPrep = highlightedTarget == "prep_button"
                            val highlightReady = highlightedTarget == "ready_button"

                            val prepContainer =
                                if (isPreparing) colors.textPrimary else colors.buttonSecondary
                            val prepContent =
                                if (isPreparing) colors.background else colors.buttonSecondaryInk
                            val prepDisabledBg =
                                if (isPreparing) colors.textPrimary else colors.cardBorder
                            val prepDisabledContent =
                                if (isPreparing) colors.background else colors.textMuted

                            val readyContainer =
                                if (isReady) colors.cardInner else colors.accentLime
                            val readyContent =
                                if (isReady) colors.textMuted else colors.accentInk

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                // Button: Preparando
                                Button(
                                    onClick = {
                                        haptics.impact()
                                        onStart(activeOrder.summary.id, activeOrder.summary.version)
                                        toastMessage = "Comanda #${activeOrder.summary.folio} en preparación"
                                    },
                                    enabled = !isPreparing && !isReady && restrictedMode != RestrictedMode.READ_ONLY,
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = prepContainer,
                                            contentColor = prepContent,
                                            disabledContainerColor = prepDisabledBg,
                                            disabledContentColor = prepDisabledContent,
                                        ),
                                    shape = RoundedCornerShape(16.dp),
                                    border =
                                        if (highlightPrep) {
                                            BorderStroke(
                                                highlightPulseWidth.dp,
                                                colors.accentLime.copy(alpha = highlightPulseAlpha),
                                            )
                                        } else {
                                            null
                                        },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                ) {
                                    Text(
                                        text = if (isPreparing) "Preparando..." else "Preparando",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                    )
                                }

                                // Button: Ya se preparó
                                Button(
                                    onClick = {
                                        haptics.success()
                                        onReady(activeOrder.summary.id, activeOrder.summary.version)
                                        toastMessage = "Comanda #${activeOrder.summary.folio} lista"
                                    },
                                    enabled = !isReady && restrictedMode != RestrictedMode.READ_ONLY,
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = readyContainer,
                                            contentColor = readyContent,
                                            disabledContainerColor = colors.cardInner,
                                            disabledContentColor = colors.textMuted,
                                        ),
                                    shape = RoundedCornerShape(16.dp),
                                    border =
                                        if (highlightReady) {
                                            BorderStroke(
                                                highlightPulseWidth.dp,
                                                colors.accentLime.copy(alpha = highlightPulseAlpha),
                                            )
                                        } else {
                                            null
                                        },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                ) {
                                    Text(
                                        text = if (isReady) "¡Listo!" else "Ya se preparó",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.cardBorder, RoundedCornerShape(26.dp)),
                        shape = RoundedCornerShape(26.dp),
                        color = colors.cardBackground,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "Cocina al día",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = "No hay comandas pendientes en este momento.",
                                fontSize = 13.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            // Siguientes Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "Siguientes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colors.textPrimary,
                    )
                    Text(
                        "${upcomingOrders.size} comandas",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                    )
                }
            }

            // Upcoming Orders List
            items(upcomingOrders, key = { it.summary.id }) { order ->
                val orderDest =
                    if (order.summary.destination.name == "TAKE_AWAY") {
                        "Para llevar"
                    } else {
                        "Comer aquí"
                    }
                val orderSummaryTitle =
                    order.items
                        .joinToString(" · ") { "${it.quantity}x ${it.productName}" }
                        .ifEmpty { "Productos del menú" }
                QueueTicketRow(
                    folio = "#${order.summary.folio}",
                    title = orderSummaryTitle,
                    subtitle = orderDest,
                    time = "En espera",
                    colors = colors,
                    onClick = {
                        haptics.selection()
                        selectedOrderId = order.summary.id
                        toastMessage = "Comanda #${order.summary.folio} seleccionada"
                    },
                )
            }
        }

        // Floating Toast Notification
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.textPrimary,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier.size(20.dp).clip(CircleShape).background(colors.accentLime),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = colors.accentInk,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    Text(
                        toastMessage.orEmpty(),
                        color = colors.background,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Floating Assistant Orb
        VaiinillaAssistantOrb(
            role = OperationalRole.KITCHEN,
            onHighlightTarget = { highlightedTarget = it },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 28.dp),
        )
    }
}

@Composable
private fun QueueTicketRow(
    folio: String,
    title: String,
    subtitle: String,
    time: String,
    colors: OperationalColors,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.textPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    folio,
                    color = colors.background,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }

            Text(
                time,
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// Modal Sheet: Add Product with Image Picker + Presets
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProductSheet(
    onDismiss: () -> Unit,
    onAdd: (name: String, price: Int, imageUri: String?, station: PreparationStation) -> Unit,
) {
    val colors = rememberOperationalColors()
    val haptics = rememberVaiinillaHaptics()
    var name by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var selectedImageUrl by remember { mutableStateOf<String?>("waffle") }
    var selectedStation by remember { mutableStateOf(PreparationStation.CASHIER) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val photoPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            if (uri != null) {
                selectedImageUrl = uri.toString()
            }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 36.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Nuevo producto",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = colors.textPrimary,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = colors.textPrimary)
                }
            }

            // Photo Preview & Picker
            Text(
                "Foto del producto",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Selected image preview
                Box(
                    modifier =
                        Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.cardBackground)
                            .border(1.5.dp, colors.cardBorder, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    val previewUrl = selectedImageUrl
                    if (previewUrl != null) {
                        ProductImage(
                            imageUrl = previewUrl,
                            contentDescription = name,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = colors.textMuted)
                    }
                }

                // Pick from gallery button
                Button(
                    onClick = { photoPicker.launch("image/*") },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colors.cardBackground,
                            contentColor = colors.textPrimary,
                        ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.cardBorder),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Elegir de galería", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Quick Preset Photos
            Text(
                "O elige un preset rápido:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PRESET_PRODUCT_PHOTOS) { preset ->
                    val isSelected = selectedImageUrl == preset.id || selectedImageUrl == preset.imageUrl
                    val chipBg =
                        if (isSelected) {
                            colors.accentLime.copy(alpha = 0.25f)
                        } else {
                            colors.cardBackground
                        }
                    val chipBorderColor = if (isSelected) colors.accentLime else colors.cardBorder
                    val chipBorderWidth = if (isSelected) 2.dp else 1.dp
                    Surface(
                        modifier =
                            Modifier.clickable {
                                haptics.selection()
                                selectedImageUrl = preset.imageUrl
                                if (name.isEmpty()) {
                                    name = preset.name
                                }
                                val isKitchenStation =
                                    preset.name in
                                        listOf(
                                            "Waffles",
                                            "Burrito",
                                            "Torta",
                                            "Quesadilla",
                                            "Sincronizada",
                                        )
                                selectedStation =
                                    if (isKitchenStation) {
                                        PreparationStation.KITCHEN
                                    } else {
                                        PreparationStation.CASHIER
                                    }
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = chipBg,
                        border = BorderStroke(width = chipBorderWidth, color = chipBorderColor),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                            ) {
                                ProductImage(
                                    imageUrl = preset.imageUrl,
                                    contentDescription = preset.name,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Text(
                                preset.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = colors.textPrimary,
                            )
                        }
                    }
                }
            }

            // Station Selector (Caja / Barra vs Cocina caliente)
            Text(
                "Estación de preparación",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val isCashier = selectedStation == PreparationStation.CASHIER
                val isKitchen = selectedStation == PreparationStation.KITCHEN

                Surface(
                    modifier =
                        Modifier.weight(1f).clickable {
                            haptics.selection()
                            selectedStation = PreparationStation.CASHIER
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCashier) colors.accentLime else colors.cardBackground,
                    border = BorderStroke(1.dp, if (isCashier) colors.accentLime else colors.cardBorder),
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "☕ Barra / Bebidas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isCashier) colors.accentInk else colors.textPrimary,
                        )
                    }
                }

                Surface(
                    modifier =
                        Modifier.weight(1f).clickable {
                            haptics.selection()
                            selectedStation = PreparationStation.KITCHEN
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isKitchen) colors.accentLime else colors.cardBackground,
                    border = BorderStroke(1.dp, if (isKitchen) colors.accentLime else colors.cardBorder),
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "🍳 Cocina caliente",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isKitchen) colors.accentInk else colors.textPrimary,
                        )
                    }
                }
            }

            // Name input
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Nombre", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle =
                        TextStyle(
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    cursorBrush = SolidColor(colors.textPrimary),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.cardBackground)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (name.isEmpty()) {
                                Text(
                                    "Ej. Matcha frío con avena",
                                    color = colors.textMuted,
                                    fontSize = 15.sp,
                                )
                            }
                            inner()
                        }
                    },
                )
            }

            // Price input
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Precio ($)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                )
                BasicTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it.filter { char -> char.isDigit() } },
                    singleLine = true,
                    textStyle =
                        TextStyle(
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    cursorBrush = SolidColor(colors.textPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.cardBackground)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (priceStr.isEmpty()) {
                                Text("Ej. 65", color = colors.textMuted, fontSize = 15.sp)
                            }
                            inner()
                        }
                    },
                )
            }

            Spacer(Modifier.height(4.dp))

            // Save button
            val isValid = name.isNotBlank() && priceStr.isNotBlank() && (priceStr.toIntOrNull() ?: 0) > 0
            Button(
                onClick = {
                    haptics.impact()
                    val price = priceStr.toIntOrNull() ?: 50
                    onAdd(name.trim(), price, selectedImageUrl, selectedStation)
                },
                enabled = isValid,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.accentLime,
                        contentColor = colors.accentInk,
                        disabledContainerColor = colors.cardBorder,
                        disabledContentColor = colors.textMuted,
                    ),
                shape = RoundedCornerShape(18.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Text(
                    "Guardar producto",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.2.sp,
                )
            }
        }
    }
}

@Composable
fun WaiterOperationalScreen(
    state: OperationalUiState,
    onBack: () -> Unit,
    onDeliver: (orderId: String, version: Int) -> Unit,
    onScanDeliver: (orderId: String, version: Int) -> Unit = { _, _ -> },
    onChangeMode: (() -> Unit)? = null,
    restrictedMode: RestrictedMode? = null,
) {
    val colors = rememberOperationalColors()
    Box(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Modo Mesero", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.textPrimary)
            Text("Entregas de pedidos en espacio", fontSize = 14.sp, color = colors.textSecondary)
        }
    }
}
