package com.vaiinilla.app.ui.components

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeMode
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

private val AuthEase = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private val AuthPaper3Light = Color(0xFFFBF8EF)
private val AuthPlaceholder = Color(0xFF9B9D94)
private val AuthHeroLeafA = Color(0xFFE4EA49)
private val AuthHeroLeafB = Color(0xFFD9DF43)
private val FieldShape = RoundedCornerShape(16.dp)
private val SheetShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
private val HeroHeight = 230.dp
private val SheetOverlap = 28.dp

@Composable
fun AuthSheetHeader(
    kicker: String,
    title: String,
    intro: String,
    kickerIcon: ImageVector = Icons.Outlined.Lock,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.paper2)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                kickerIcon,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                kicker,
                color = colors.muted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            color = colors.ink,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            intro,
            color = colors.muted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
fun AuthHeroSheetScaffold(
    kicker: String,
    title: String,
    intro: String,
    loading: Boolean,
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    kickerIcon: ImageVector = Icons.Outlined.Lock,
    scrollSheet: Boolean = true,
    collapsibleHero: Boolean = !scrollSheet,
    headerInsideScroll: Boolean = !scrollSheet,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val isSystemDark = isSystemInDarkTheme()
    val isDark = LocalVaiinillaThemeMode.current.resolveEffectiveMode(isSystemDark) != VaiinillaThemeMode.Light
    val pane = if (isDark) colors.paper else AuthPaper3Light
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val maxHeroCollapsePx = with(density) { (HeroHeight - 12.dp).toPx() }
    var heroOffsetPx by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection =
        remember(collapsibleHero, maxHeroCollapsePx) {
            if (!collapsibleHero) {
                null
            } else {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        val delta = available.y
                        if (delta < 0) {
                            val prev = heroOffsetPx
                            val newOffset = (heroOffsetPx + delta).coerceIn(-maxHeroCollapsePx, 0f)
                            heroOffsetPx = newOffset
                            return Offset(0f, newOffset - prev)
                        }
                        return Offset.Zero
                    }

                    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                        val delta = available.y
                        if (delta > 0) {
                            val prev = heroOffsetPx
                            val newOffset = (heroOffsetPx + delta).coerceIn(-maxHeroCollapsePx, 0f)
                            heroOffsetPx = newOffset
                            return Offset(0f, newOffset - prev)
                        }
                        return Offset.Zero
                    }
                }
            }
        }

    val busyAlpha by animateFloatAsState(
        targetValue = if (loading) 0.48f else 1f,
        animationSpec = tween(700, easing = AuthEase),
        label = "authBusyAlpha",
    )
    val busyScale by animateFloatAsState(
        targetValue = if (loading) 0.99f else 1f,
        animationSpec = tween(700, easing = AuthEase),
        label = "authBusyScale",
    )

    val heroCollapseOffsetDp = with(density) { heroOffsetPx.toDp() }
    val currentSheetTop =
        (HeroHeight + statusTop - SheetOverlap + heroCollapseOffsetDp).coerceAtLeast(statusTop + 48.dp)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.accent)
                .then(if (nestedScrollConnection != null) Modifier.nestedScroll(nestedScrollConnection) else Modifier),
    ) {
        AuthBrandHero(
            showBack = showBack,
            onBack = onBack,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(HeroHeight + statusTop)
                    .padding(top = statusTop)
                    .graphicsLayer {
                        translationY = heroOffsetPx
                        alpha = if (collapsibleHero) (1f + heroOffsetPx / maxHeroCollapsePx).coerceIn(0.15f, 1f) else 1f
                    },
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = currentSheetTop)
                    .shadow(12.dp, SheetShape, ambientColor = Color(0x14171817), spotColor = Color(0x14171817))
                    .clip(SheetShape)
                    .background(pane)
                    .navigationBarsPadding()
                    .imePadding(),
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.ink) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .then(if (scrollSheet) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                            .padding(
                                start = 18.dp,
                                end = 18.dp,
                                top = if (headerInsideScroll) 14.dp else 24.dp,
                                bottom = 14.dp,
                            )
                            .graphicsLayer {
                                alpha = busyAlpha
                                scaleX = busyScale
                                scaleY = busyScale
                            },
                ) {
                    if (!headerInsideScroll) {
                        AuthSheetHeader(
                            kicker = kicker,
                            title = title,
                            intro = intro,
                            kickerIcon = kickerIcon,
                        )
                    }
                    content()
                }
            }
            if (loading) {
                AuthLoadingOverlay()
            }
        }
    }
}

@Composable
fun AuthAccessScaffold(
    kicker: String,
    title: String,
    intro: String,
    loading: Boolean,
    hintPrefix: String,
    hintAction: String,
    onHintAction: () -> Unit,
    privacyUrl: String,
    termsUrl: String,
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    AuthHeroSheetScaffold(
        kicker = kicker,
        title = title,
        intro = intro,
        loading = loading,
        modifier = modifier,
        showBack = showBack,
        onBack = onBack,
    ) {
        content()
        Spacer(Modifier.height(12.dp))
        Text(
            buildAnnotatedString {
                append(hintPrefix)
                append(" ")
                withStyle(SpanStyle(color = colors.ink, fontWeight = FontWeight.SemiBold)) {
                    append(hintAction)
                }
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onHintAction,
                    ),
            color = colors.muted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        AuthLegalFooter(privacyUrl = privacyUrl, termsUrl = termsUrl)
    }
}

@Composable
private fun AuthBrandHero(
    showBack: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val context = LocalContext.current
    val motionOn =
        remember {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        }
    val infinite = rememberInfiniteTransition(label = "authHeroFloat")
    val discY by infinite.animateFloat(
        initialValue = 0f,
        targetValue = if (motionOn) 12f else 0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(5000, easing = AuthEase),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "discY",
    )
    val tileY by infinite.animateFloat(
        initialValue = 0f,
        targetValue = if (motionOn) -12f else 0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(6000, easing = AuthEase),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "tileY",
    )
    Box(modifier = modifier.fillMaxWidth().background(colors.accent)) {
        VaiinillaMark(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 72.dp, y = 82.dp)
                    .size(width = 295.dp, height = 245.dp)
                    .graphicsLayer {
                        rotationZ = -4f
                        alpha = 0.065f
                    },
            cream = colors.ink,
            leafA = colors.ink,
            leafB = colors.ink,
            coral = colors.ink,
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 70.dp, end = 24.dp)
                    .offset(y = discY.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.coral),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 104.dp, end = 68.dp)
                    .offset(y = tileY.dp)
                    .size(54.dp)
                    .graphicsLayer { rotationZ = 8f }
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.yolk),
        )
        Column(modifier = Modifier.fillMaxSize().padding(start = 18.dp, end = 18.dp, bottom = 40.dp, top = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showBack) {
                    AuthPressIcon(
                        onClick = onBack,
                        contentDescription = "Volver",
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        tint = colors.accentInk,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                VaiinillaMark(
                    modifier = Modifier.size(38.dp),
                    cream = colors.ink,
                    leafA = AuthHeroLeafA,
                    leafB = AuthHeroLeafB,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Vaiinilla",
                    color = colors.accentInk,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "COMEDOR CONECTADO",
                color = colors.accentInk.copy(alpha = 0.66f),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Come mejor.\nEspera menos.",
                color = colors.accentInk,
                fontSize = 36.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.4).sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Tu menú, tus pedidos y tu saldo viven en un solo lugar. Entra antes de llegar a la barra.",
                color = colors.accentInk.copy(alpha = 0.72f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
fun AuthAccessField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    kind: AuthAccessFieldKind,
    modifier: Modifier = Modifier,
    trailingLabel: String? = null,
    onTrailingLabel: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    error: String? = null,
) {
    val colors = LocalVaiinillaColors.current
    var focused by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(false) }
    val isPassword = kind == AuthAccessFieldKind.Password
    val lift by animateDpAsState(
        targetValue = if (focused) (-1).dp else 0.dp,
        animationSpec = tween(200, easing = AuthEase),
        label = "fieldLift",
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = colors.ink, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
            if (trailingLabel != null && onTrailingLabel != null) {
                Text(
                    trailingLabel,
                    modifier =
                        Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onTrailingLabel,
                        ),
                    color = colors.muted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .offset(y = lift)
                    .clip(FieldShape)
                    .background(colors.paper)
                    .border(
                        width = if (focused) 2.dp else 1.dp,
                        color =
                            when {
                                error != null -> colors.coral
                                focused -> colors.accent.copy(alpha = 0.68f)
                                else -> colors.line
                            },
                        shape = FieldShape,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                kind.icon,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.padding(start = 16.dp).size(20.dp),
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = if (isPassword) 4.dp else 16.dp)
                        .onFocusChanged { focused = it.isFocused }
                        .semantics { contentDescription = label },
                singleLine = true,
                cursorBrush = SolidColor(colors.ink),
                textStyle = TextStyle(color = colors.ink, fontSize = 16.sp, lineHeight = 24.sp),
                visualTransformation =
                    if (isPassword && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = kind.keyboardType,
                        imeAction = imeAction,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onDone = { onImeAction() },
                        onGo = { onImeAction() },
                    ),
                decorationBox = { input ->
                    Box {
                        if (value.isBlank()) {
                            Text(placeholder, color = AuthPlaceholder, fontSize = 16.sp, lineHeight = 24.sp)
                        }
                        input()
                    }
                },
            )
            if (isPassword) {
                AuthRevealButton(revealed = revealed, onToggle = { revealed = !revealed })
            }
        }
        if (error != null) {
            Text(
                error,
                color = Color(0xFFB63834),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}

enum class AuthAccessFieldKind(
    val icon: ImageVector,
    val keyboardType: KeyboardType,
) {
    Email(Icons.Outlined.Email, KeyboardType.Email),
    Password(Icons.Outlined.Lock, KeyboardType.Password),
    Person(Icons.Outlined.Person, KeyboardType.Text),
    Id(Icons.Outlined.Badge, KeyboardType.Text),
}

@Composable
fun AuthInkSubmitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalVaiinillaColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.98f else 1f,
        animationSpec = tween(180, easing = AuthEase),
        label = "submitScale",
    )
    val arrowShift by animateDpAsState(
        targetValue = if (pressed && enabled) 4.dp else 0.dp,
        animationSpec = tween(180, easing = AuthEase),
        label = "submitArrow",
    )
    val pane =
        if (LocalVaiinillaThemeMode.current == VaiinillaThemeMode.Light) AuthPaper3Light else colors.paper
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(54.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = if (enabled) 1f else 0.76f
                }
                .shadow(14.dp, FieldShape, ambientColor = colors.ink.copy(alpha = 0.16f), spotColor = colors.ink.copy(alpha = 0.16f))
                .clip(FieldShape)
                .background(colors.ink)
                .clickable(
                    enabled = enabled,
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = pane, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(10.dp))
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = pane,
            modifier = Modifier.size(20.dp).offset(x = arrowShift),
        )
    }
}

@Composable
fun AuthLegalCheckRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    linkLabel: String,
    url: String,
) {
    val colors = LocalVaiinillaColors.current
    val uriHandler = LocalUriHandler.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(FieldShape)
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (checked) colors.ink else colors.paper)
                    .border(1.dp, if (checked) colors.ink else colors.line, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Text("✓", color = AuthPaper3Light, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = colors.ink, fontSize = 14.sp, lineHeight = 20.sp)
            if (url.isNotBlank()) {
                Text(
                    linkLabel,
                    modifier =
                        Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { uriHandler.openUri(url) },
                    color = colors.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun AuthRevealButton(
    revealed: Boolean,
    onToggle: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(180, easing = AuthEase),
        label = "revealScale",
    )
    Box(
        modifier =
            Modifier
                .padding(end = 8.dp)
                .size(40.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onToggle,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            contentDescription = if (revealed) "Ocultar contraseña" else "Mostrar contraseña",
            tint = colors.muted,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AuthPressIcon(
    onClick: () -> Unit,
    contentDescription: String,
    icon: ImageVector,
    tint: Color,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(180, easing = AuthEase),
        label = "backScale",
    )
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}

@Composable
private fun AuthLegalFooter(
    privacyUrl: String,
    termsUrl: String,
) {
    val colors = LocalVaiinillaColors.current
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("© 2026 Vaiinilla", color = colors.muted, fontSize = 11.sp, lineHeight = 16.sp)
        Text(
            "Privacidad",
            modifier =
                if (privacyUrl.isNotBlank()) {
                    Modifier.clickable { uriHandler.openUri(privacyUrl) }
                } else {
                    Modifier
                },
            color = colors.muted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
        Text(
            "Términos",
            modifier =
                if (termsUrl.isNotBlank()) {
                    Modifier.clickable { uriHandler.openUri(termsUrl) }
                } else {
                    Modifier
                },
            color = colors.muted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun AuthLoadingOverlay() {
    val colors = LocalVaiinillaColors.current
    val pane =
        if (LocalVaiinillaThemeMode.current == VaiinillaThemeMode.Light) AuthPaper3Light else colors.paper
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(pane.copy(alpha = 0.92f))
                .padding(horizontal = 36.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(colors.paper2))
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(99.dp)).background(colors.paper2))
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(0.7f).height(12.dp).clip(RoundedCornerShape(99.dp)).background(colors.paper2))
        Spacer(Modifier.height(22.dp))
        Box(Modifier.fillMaxWidth().height(56.dp).clip(FieldShape).background(colors.paper2))
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(56.dp).clip(FieldShape).background(colors.paper2))
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(54.dp).clip(FieldShape).background(colors.ink.copy(alpha = 0.18f)))
    }
}
