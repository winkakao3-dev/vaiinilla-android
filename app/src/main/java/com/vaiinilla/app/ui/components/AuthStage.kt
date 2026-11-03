package com.vaiinilla.app.ui.components

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.vaiinilla.app.ui.theme.Coral
import com.vaiinilla.app.ui.theme.DarkAccentInk
import com.vaiinilla.app.ui.theme.DarkInk
import com.vaiinilla.app.ui.theme.DarkInk2
import com.vaiinilla.app.ui.theme.DarkMuted
import com.vaiinilla.app.ui.theme.DarkPaper
import com.vaiinilla.app.ui.theme.DarkPaper2
import com.vaiinilla.app.ui.theme.Lime
import com.vaiinilla.app.ui.theme.Yolk
import kotlinx.coroutines.delay

/** Las pantallas de acceso con correo siguen el lienzo de Doop: siempre oscuras, sin importar el tema. */
internal object AuthStageColors {
    val Paper = DarkPaper
    val Paper2 = DarkPaper2
    val Paper3 = Color(0xFF33342F)
    val Ink = DarkInk
    val Ink2 = DarkInk2
    val Muted = DarkMuted
    val Faint = Color(0xFF7D7C74)
    val Placeholder = Color(0xFF6F6E66)
    val Line = Color(0x1FFFFFFF)
    val Accent = Lime
    val AccentInk = DarkAccentInk
    val Danger = Coral
    val Warning = Yolk
}

private val StageEase = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val FieldShape = RoundedCornerShape(20.dp)
private val PillShape = RoundedCornerShape(999.dp)

data class AuthStageProgress(
    val step: Int,
    val count: Int,
)

/**
 * Lienzo común del acceso: barra de arriba (volver y pasos), contenido con scroll y acciones fijas abajo que suben
 * con el teclado.
 */
@Composable
fun AuthStage(
    loading: Boolean,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backDescription: String = "Volver",
    progress: AuthStageProgress? = null,
    contentTopPadding: Dp = 12.dp,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    LightStatusBarIcons()
    val busyAlpha by animateFloatAsState(
        targetValue = if (loading) 0.55f else 1f,
        animationSpec = tween(400, easing = StageEase),
        label = "authStageBusy",
    )
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(AuthStageColors.Paper),
    ) {
        CompositionLocalProvider(LocalContentColor provides AuthStageColors.Ink) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .imePadding(),
            ) {
                if (onBack != null || progress != null) {
                    AuthStageTopBar(onBack = onBack, backDescription = backDescription, progress = progress)
                }
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 22.dp, end = 22.dp, top = contentTopPadding, bottom = 16.dp)
                            .graphicsLayer { alpha = busyAlpha },
                    content = content,
                )
                if (actions != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 16.dp),
                        content = actions,
                    )
                }
            }
        }
        if (loading) {
            CircularProgressIndicator(
                color = AuthStageColors.Accent,
                strokeWidth = 3.dp,
                modifier = Modifier.align(Alignment.Center).size(36.dp),
            )
        }
    }
}

@Composable
private fun LightStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val before = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            if (controller != null && before != null) controller.isAppearanceLightStatusBars = before
        }
    }
}

@Composable
private fun AuthStageTopBar(
    onBack: (() -> Unit)?,
    backDescription: String,
    progress: AuthStageProgress?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 22.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            AuthBackCircle(onClick = onBack, contentDescription = backDescription)
        }
        if (progress != null) {
            Spacer(Modifier.width(14.dp))
            AuthStepBar(progress = progress, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(14.dp))
            Text(
                "${progress.step + 1} de ${progress.count}",
                color = AuthStageColors.Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
fun AuthBackCircle(
    onClick: () -> Unit,
    contentDescription: String,
) {
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .clip(CircleShape)
                .background(AuthStageColors.Paper2)
                .semantics {
                    this.contentDescription = contentDescription
                    role = Role.Button
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = null,
            tint = AuthStageColors.Ink,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AuthStepBar(
    progress: AuthStageProgress,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(progress.count) { index ->
            val fill by animateFloatAsState(
                targetValue = if (index <= progress.step) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 260f),
                label = "authStepFill$index",
            )
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(PillShape)
                        .background(AuthStageColors.Paper3),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(fill)
                            .height(5.dp)
                            .clip(PillShape)
                            .background(AuthStageColors.Accent),
                )
            }
        }
    }
}

/** Entrada escalonada: cada bloque sube 14 dp y aparece, como el `.ar` del lienzo. */
fun Modifier.authArrive(index: Int): Modifier =
    composed {
        val still = reducedMotion()
        val progress = remember { Animatable(if (still) 1f else 0f) }
        LaunchedEffect(Unit) {
            if (still) return@LaunchedEffect
            delay(index * 45L)
            progress.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 220f))
        }
        graphicsLayer {
            alpha = progress.value.coerceIn(0f, 1f)
            translationY = (1f - progress.value) * 14.dp.toPx()
        }
    }

@Composable
fun AuthHeading(
    cap: String,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
) {
    val align = if (centered) TextAlign.Center else TextAlign.Start
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            cap.uppercase(),
            color = AuthStageColors.Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.2.sp,
            textAlign = align,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            color = AuthStageColors.Ink,
            fontSize = 34.sp,
            lineHeight = 35.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.4).sp,
            textAlign = align,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                color = AuthStageColors.Muted,
                fontSize = 14.5.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = align,
            )
        }
    }
}

/** Campo con la etiqueta adentro de la caja. [shakeKey] distinto de 0 sacude la caja cada vez que cambia. */
@Composable
fun AuthBoxField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    password: Boolean = false,
    isError: Boolean = false,
    isValid: Boolean = false,
    enabled: Boolean = true,
    shakeKey: Int = 0,
    focusRequester: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val runImeAction = { onImeAction?.invoke() ?: focusManager.moveFocus(FocusDirection.Down) }
    val border by animateColorAsState(
        targetValue =
            when {
                isError -> AuthStageColors.Danger
                focused -> AuthStageColors.Ink
                else -> AuthStageColors.Line
            },
        animationSpec = tween(220),
        label = "authFieldBorder",
    )
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) {
        if (shakeKey == 0) return@LaunchedEffect
        shake.animateTo(
            0f,
            keyframes {
                durationMillis = 420
                -10f at 60
                9f at 130
                -7f at 200
                5f at 270
                -2f at 340
            },
        )
    }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .graphicsLayer { translationX = shake.value.dp.toPx() }
                .clip(FieldShape)
                .background(AuthStageColors.Paper2)
                .border(1.5.dp, border, FieldShape)
                .padding(start = 16.dp, end = if (password) 6.dp else 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isError) AuthStageColors.Danger else AuthStageColors.Muted,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = AuthStageColors.Muted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                        .onFocusChanged { focused = it.isFocused }
                        .semantics { contentDescription = label },
                singleLine = true,
                cursorBrush = SolidColor(AuthStageColors.Accent),
                textStyle =
                    TextStyle(
                        color = AuthStageColors.Ink,
                        fontSize = 16.5.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                visualTransformation =
                    if (password && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = imeAction,
                        capitalization = capitalization,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onDone = { runImeAction() },
                        onGo = { runImeAction() },
                        onNext = { runImeAction() },
                    ),
                decorationBox = { input ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                color = AuthStageColors.Placeholder,
                                fontSize = 16.5.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        input()
                    }
                },
            )
        }
        if (isValid && !password) {
            val pop by animateFloatAsState(1f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "authValidPop")
            Box(
                modifier =
                    Modifier
                        .size(26.dp)
                        .graphicsLayer {
                            scaleX = pop
                            scaleY = pop
                        }.clip(CircleShape)
                        .background(AuthStageColors.Accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = AuthStageColors.AccentInk, modifier = Modifier.size(16.dp))
            }
        }
        if (password) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .physicalPress(scale = PhysicalPressScale.Small) { revealed = !revealed }
                        .semantics {
                            contentDescription = if (revealed) "Ocultar contraseña" else "Mostrar contraseña"
                            role = Role.Button
                        },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = AuthStageColors.Ink2,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
fun AuthLimeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showArrow: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val background by animateColorAsState(
        if (enabled) AuthStageColors.Accent else AuthStageColors.Paper3,
        tween(220),
        label = "authLimeBg",
    )
    val content by animateColorAsState(
        if (enabled) AuthStageColors.AccentInk else AuthStageColors.Faint,
        tween(220),
        label = "authLimeInk",
    )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(58.dp)
                .physicalPress(enabled = enabled, onClick = onClick)
                .clip(PillShape)
                .background(background)
                .semantics { role = Role.Button },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = content, fontSize = 17.sp, fontWeight = FontWeight.Black)
        if (showArrow) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(19.dp).graphicsLayer { translationX = if (pressed) 4.dp.toPx() else 0f },
            )
        }
    }
}

@Composable
fun AuthGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(52.dp)
                .physicalPress(enabled = enabled, onClick = onClick)
                .clip(PillShape)
                .border(1.5.dp, AuthStageColors.Line, PillShape)
                .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (enabled) AuthStageColors.Ink else AuthStageColors.Faint,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
fun AuthTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AuthStageColors.Accent,
    enabled: Boolean = true,
) {
    Text(
        text,
        modifier =
            modifier
                .minTouchTarget()
                .physicalPress(enabled = enabled, scale = PhysicalPressScale.Small, onClick = onClick)
                .semantics { role = Role.Button },
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
    )
}

enum class AuthBannerTone { Error, Warning, Notice }

@Composable
fun AuthBanner(
    message: String,
    tone: AuthBannerTone,
    modifier: Modifier = Modifier,
) {
    val color =
        when (tone) {
            AuthBannerTone.Error -> AuthStageColors.Danger
            AuthBannerTone.Warning -> AuthStageColors.Warning
            AuthBannerTone.Notice -> AuthStageColors.Accent
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(color.copy(alpha = 0.13f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            if (tone == AuthBannerTone.Notice) Icons.Outlined.Check else Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(message, color = color, fontSize = 13.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
    }
}

/** Mosaico redondeado con un ícono grande y, si hace falta, un punto lima con un número. */
@Composable
fun AuthIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    iconSize: Dp = 34.dp,
    badge: String? = null,
) {
    val still = reducedMotion()
    val appear = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!still) appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 260f))
    }
    Box(
        modifier =
            modifier
                .size(size)
                .graphicsLayer {
                    val s = 0.9f + 0.1f * appear.value
                    scaleX = s
                    scaleY = s
                    alpha = appear.value.coerceIn(0f, 1f)
                    translationY = (1f - appear.value) * 20.dp.toPx()
                }.clip(RoundedCornerShape(size * 0.3f))
                .background(AuthStageColors.Paper2),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = AuthStageColors.Accent, modifier = Modifier.size(iconSize))
        if (badge != null) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = size * 0.14f, end = size * 0.14f)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AuthStageColors.Accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge, color = AuthStageColors.AccentInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun AuthChip(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(PillShape)
                .background(AuthStageColors.Paper2)
                .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = AuthStageColors.Accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = AuthStageColors.Ink, fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun AuthNumberedSteps(
    steps: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(AuthStageColors.Paper2),
    ) {
        steps.forEachIndexed { index, step ->
            if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(AuthStageColors.Line))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape).background(AuthStageColors.Paper3),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", color = AuthStageColors.Ink, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Text(step, color = AuthStageColors.Ink2, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AuthCheckCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    linkLabel: String,
    url: String,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val border by animateColorAsState(
        if (checked) AuthStageColors.Accent else AuthStageColors.Line,
        tween(220),
        label = "authCheckBorder",
    )
    val boxFill by animateColorAsState(
        if (checked) AuthStageColors.Accent else Color.Transparent,
        tween(180),
        label = "authCheckFill",
    )
    val tick by animateFloatAsState(
        if (checked) 1f else 0f,
        spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "authCheckTick",
    )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(FieldShape)
                .background(AuthStageColors.Paper2)
                .border(1.5.dp, border, FieldShape)
                .physicalPress { onCheckedChange(!checked) }
                .semantics {
                    role = Role.Checkbox
                    stateDescription = if (checked) "Aceptado" else "Sin aceptar"
                }.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(boxFill)
                    .border(2.dp, if (checked) AuthStageColors.Accent else AuthStageColors.Muted, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = AuthStageColors.AccentInk,
                modifier =
                    Modifier.size(16.dp).graphicsLayer {
                        scaleX = tick
                        scaleY = tick
                    },
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            color = AuthStageColors.Ink,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
        )
        if (url.isNotBlank()) {
            AuthTextLink(
                text = linkLabel,
                onClick = { uriHandler.openUri(url) },
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
fun AuthLegalLinks(
    privacyUrl: String,
    termsUrl: String,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("© 2026 Vaiinilla", color = AuthStageColors.Faint, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        if (privacyUrl.isNotBlank()) {
            AuthTextLink(
                "Privacidad",
                onClick = { uriHandler.openUri(privacyUrl) },
                color = AuthStageColors.Muted,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
        if (termsUrl.isNotBlank()) {
            AuthTextLink("Términos", onClick = { uriHandler.openUri(termsUrl) }, color = AuthStageColors.Muted)
        }
    }
}