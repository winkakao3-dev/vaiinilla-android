package com.vaiinilla.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.common.BitMatrix
import com.vaiinilla.app.core.qr.QrCodeEncoder
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.order.MOSAIC_FLIP_LENGTH
import com.vaiinilla.app.ui.order.PickupStep
import com.vaiinilla.app.ui.order.PurchaseCelebrationKind
import com.vaiinilla.app.ui.order.mosaicCellStart
import com.vaiinilla.app.ui.order.pickupCodeUseful
import com.vaiinilla.app.ui.order.pickupCodeHint
import com.vaiinilla.app.ui.order.pickupHint
import com.vaiinilla.app.ui.order.pickupStep
import com.vaiinilla.app.ui.order.pickupTitle
import com.vaiinilla.app.ui.order.shouldAutoRevealPickupCode
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeMode
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Colores de la escena y del vidrio. El tema no oscurece la escena de día: es la misma tienda a otra hora
 * (claro = día, oscuro = noche con las luces prendidas, AMOLED = medianoche con el letrero en neón).
 */
private class ScenePalette(
    val skyTop: Color,
    val skyBottom: Color,
    val facade: Color,
    val facadeShade: Color,
    val facadeLight: Color,
    val windowFrame: Color,
    val windowGlass: Color,
    val windowGlassLight: Color,
    val windowLit: Color,
    val windowLitDeep: Color,
    val litWindows: Set<Int>,
    val awning: Color,
    val awningDark: Color,
    val signInk: Color,
    val signText: Color,
    val signGlow: Color,
    val shopLight: Color,
    val shopLightDeep: Color,
    val shopShelf: Color,
    val lampGlow: Color,
    val spill: Color,
    val doorGreen: Color,
    val sidewalk: Color,
    val street: Color,
    val leaf: Color,
    val leafDark: Color,
    val pot: Color,
    val stage: Color,
    val ink: Color,
    val muted: Color,
    val glassTint: Color,
    val glassEdge: Color,
    val glassLine: Color,
    val glassCard: Color,
    val glassButton: Color,
    val sheetSolid: Color,
)

private val Day =
    ScenePalette(
        skyTop = Color(0xFFEAE3D0),
        skyBottom = Color(0xFFF4EFE2),
        facade = Color(0xFFE6D3AE),
        facadeShade = Color(0xFFD5BE92),
        facadeLight = Color(0xFFF3E9D0),
        windowFrame = Color(0xFFF8F2E2),
        windowGlass = Color(0xFF7D96A0),
        windowGlassLight = Color(0xFFA3B8BE),
        windowLit = Color(0xFFFFE6A8),
        windowLitDeep = Color(0xFFF2C46E),
        litWindows = emptySet(),
        awning = Color(0xFFB7D464),
        awningDark = Color(0xFF9DBB4C),
        signInk = Color(0xFF1F2716),
        signText = Color(0xFFF3EFE4),
        signGlow = Color.Transparent,
        shopLight = Color(0xFFFFF2D3),
        shopLightDeep = Color(0xFFF2D9A2),
        shopShelf = Color(0xFFD9B98A),
        lampGlow = Color(0x66FFE7A8),
        spill = Color.Transparent,
        doorGreen = Color(0xFF2E3B1E),
        sidewalk = Color(0xFFDCD3C1),
        street = Color(0xFFC9C1B0),
        leaf = Color(0xFF8DAF48),
        leafDark = Color(0xFF6E9131),
        pot = Color(0xFFB98A5E),
        stage = Color(0xFFF4F1E8),
        ink = Color(0xFF151513),
        muted = Color(0xFF6F6B62),
        glassTint = Color(0xBDFBF8F1),
        glassEdge = Color(0xA6FFFFFF),
        glassLine = Color(0x1F151513),
        glassCard = Color(0x99FFFFFF),
        glassButton = Color(0x8CFFFFFF),
        sheetSolid = Color(0xFFFBF8F0),
    )

private val Night =
    ScenePalette(
        skyTop = Color(0xFF141B2B),
        skyBottom = Color(0xFF27304A),
        facade = Color(0xFF4A4034),
        facadeShade = Color(0xFF3A3229),
        facadeLight = Color(0xFF5B4F40),
        windowFrame = Color(0xFF6A5D4C),
        windowGlass = Color(0xFF232C3D),
        windowGlassLight = Color(0xFF34405A),
        windowLit = Color(0xFFFFDC8F),
        windowLitDeep = Color(0xFFE9A94F),
        litWindows = setOf(0, 2),
        awning = Color(0xFF7C9A34),
        awningDark = Color(0xFF627C27),
        signInk = Color(0xFF10140C),
        signText = Color(0xFFFFE9B8),
        signGlow = Color(0x33FFD98A),
        shopLight = Color(0xFFFFE3A6),
        shopLightDeep = Color(0xFFF0BE68),
        shopShelf = Color(0xFFB08A58),
        lampGlow = Color(0x99FFD27A),
        spill = Color(0x59FFC86A),
        doorGreen = Color(0xFF1A2312),
        sidewalk = Color(0xFF3B3731),
        street = Color(0xFF2A2724),
        leaf = Color(0xFF5B7830),
        leafDark = Color(0xFF425A21),
        pot = Color(0xFF7A5A3C),
        stage = Color(0xFF151513),
        ink = Color(0xFFF3EFE4),
        muted = Color(0xFFB8B3AA),
        glassTint = Color(0xB31C1D1A),
        glassEdge = Color(0x2EFFFFFF),
        glassLine = Color(0x29F3EFE4),
        glassCard = Color(0x1AFFFFFF),
        glassButton = Color(0x1FFFFFFF),
        sheetSolid = Color(0xFF1A1B19),
    )

private val Midnight =
    ScenePalette(
        skyTop = Color.Black,
        skyBottom = Color.Black,
        facade = Color(0xFF15120F),
        facadeShade = Color(0xFF0C0A08),
        facadeLight = Color(0xFF1E1A15),
        windowFrame = Color(0xFF26211B),
        windowGlass = Color(0xFF07080B),
        windowGlassLight = Color(0xFF10131A),
        windowLit = Color(0xFFE8B866),
        windowLitDeep = Color(0xFFB9823A),
        litWindows = setOf(2),
        awning = Color(0xFF34421A),
        awningDark = Color(0xFF283414),
        signInk = Color.Black,
        signText = Color(0xFFD4F27A),
        signGlow = Color(0x66B7D464),
        shopLight = Color(0xFFF5CF84),
        shopLightDeep = Color(0xFFC98E3E),
        shopShelf = Color(0xFF7E6140),
        lampGlow = Color(0x80FFC861),
        spill = Color(0x40FFB84D),
        doorGreen = Color(0xFF0B1007),
        sidewalk = Color(0xFF0E0D0C),
        street = Color.Black,
        leaf = Color(0xFF2F3F18),
        leafDark = Color(0xFF212C10),
        pot = Color(0xFF3E2E1F),
        stage = Color.Black,
        ink = Color(0xFFF3EFE4),
        muted = Color(0xFFA19C93),
        glassTint = Color(0xC70A0A0A),
        glassEdge = Color(0x24FFFFFF),
        glassLine = Color(0x24F3EFE4),
        glassCard = Color(0x14FFFFFF),
        glassButton = Color(0x1AFFFFFF),
        sheetSolid = Color(0xFF0B0B0B),
    )

private val LocalScene = staticCompositionLocalOf { Day }

@Composable
private fun scenePaletteForTheme(): ScenePalette =
    when (LocalVaiinillaThemeMode.current.resolveEffectiveMode(isSystemInDarkTheme())) {
        VaiinillaThemeMode.Dark -> Night
        VaiinillaThemeMode.Amoled -> Midnight
        else -> Day
    }

private val Lime = Color(0xFFB7D464)
private val LimeInk = Color(0xFF172008)
private val QrDark = Color(0xFF151513)
private val QrLight = Color(0xFFFBF8F0)
private val VainiCream = Color(0xFFF6EFDD)
private val VainiLime = Color(0xFFA8D52C)
private val VainiInk = Color(0xFF1A1414)

// Proporciones de la fachada, en fracción del alto de la pantalla.
// La puerta y Vaini quedan arriba de donde llega la hoja de vidrio (~55 % del alto).
private const val WINDOWS_TOP_Y = 0.07f
private const val WINDOWS_HEIGHT = 0.094f
private const val CORNICE_Y = 0.19f
private const val SIGN_TOP_Y = 0.203f
private const val SIGN_BOTTOM_Y = 0.241f
private const val AWNING_BOTTOM_Y = 0.277f
private const val SHOP_BOTTOM_Y = 0.49f
private const val SIDEWALK_BOTTOM_Y = 0.535f

// La cámara vuela hacia la puerta de la tienda.
private const val FOCUS_Y = 0.395f
private const val ZOOM_MAX = 3.4f
private const val CAMERA_FROM = 0.04f
private const val CAMERA_TO = 0.5f
private const val HANDOFF = 0.4f
private const val SAMPLE_SCALE = 0.25f

private fun segment(
    p: Float,
    from: Float,
    to: Float,
): Float = ((p - from) / (to - from)).coerceIn(0f, 1f)

private fun eased(
    p: Float,
    from: Float,
    to: Float,
): Float = FastOutSlowInEasing.transform(segment(p, from, to))

private fun lerpF(
    a: Float,
    b: Float,
    t: Float,
): Float = a + (b - a) * t

/** Movimiento de Vaini en la escena; la escena en sí es fija. */
private data class SceneMotion(
    val blink: Float = 1f,
    val wave: Float = 0f,
    val happy: Boolean = false,
)

/** Cámara: `pantalla = anchor + (puntoEscena - focus) * zoom`. */
private data class Camera(
    val zoom: Float,
    val anchor: Offset,
    val focus: Offset,
) {
    fun toScreen(scene: Offset): Offset = anchor + (scene - focus) * zoom

    fun toScene(screen: Offset): Offset = focus + (screen - anchor) / zoom
}

private fun cameraAt(
    p: Float,
    width: Float,
    height: Float,
): Camera {
    val t = eased(p, CAMERA_FROM, CAMERA_TO)
    val focus = Offset(width * 0.5f, height * FOCUS_Y)
    val target = Offset(width * 0.5f, height * 0.42f)
    return Camera(
        zoom = lerpF(1f, ZOOM_MAX, t),
        anchor = focus + (target - focus) * t,
        focus = focus,
    )
}

/** Colores de la escena bajo cada bloque del mosaico, tal como se ve al pasar la cámara por [HANDOFF]. */
private class Mosaic(
    val matrix: BitMatrix,
    val sceneCenter: Offset,
    val sceneSide: Float,
    val sampled: Array<Color>,
)

private fun buildMosaic(
    matrix: BitMatrix,
    width: Float,
    height: Float,
    density: Density,
    textMeasurer: TextMeasurer,
    palette: ScenePalette,
): Mosaic {
    val camera = cameraAt(HANDOFF, width, height)
    val screenSide = width * 1.18f
    val sceneCenter = camera.toScene(Offset(width * 0.5f, height * 0.42f))
    val sceneSide = screenSide / camera.zoom
    val bitmap = ImageBitmap((width * SAMPLE_SCALE).roundToInt().coerceAtLeast(1), (height * SAMPLE_SCALE).roundToInt().coerceAtLeast(1))
    CanvasDrawScope().draw(
        density,
        LayoutDirection.Ltr,
        androidx.compose.ui.graphics.Canvas(bitmap),
        Size(bitmap.width.toFloat(), bitmap.height.toFloat()),
    ) {
        withTransform({ scale(SAMPLE_SCALE, SAMPLE_SCALE, pivot = Offset.Zero) }) {
            drawStorefront(Size(width, height), textMeasurer, palette, SceneMotion())
        }
    }
    val pixels: PixelMap = bitmap.toPixelMap()
    val n = matrix.width
    val sampled =
        Array(n * n) { index ->
            val col = index % n
            val row = index / n
            val point =
                Offset(
                    sceneCenter.x + ((col + 0.5f) / n - 0.5f) * sceneSide,
                    sceneCenter.y + ((row + 0.5f) / n - 0.5f) * sceneSide,
                )
            val x = (point.x * SAMPLE_SCALE).roundToInt()
            val y = (point.y * SAMPLE_SCALE).roundToInt()
            if (x in 0 until pixels.width && y in 0 until pixels.height) pixels[x, y] else palette.skyBottom
        }
    return Mosaic(matrix, sceneCenter, sceneSide, sampled)
}

private fun DrawScope.drawStorefront(
    stage: Size,
    textMeasurer: TextMeasurer,
    c: ScenePalette,
    motion: SceneMotion,
) {
    val w = stage.width
    val h = stage.height
    val hair = 1.dp.toPx()
    drawRect(Brush.verticalGradient(listOf(c.skyTop, c.skyBottom), endY = h * 0.6f), size = stage)

    // Fachada del piso de arriba, con hiladas finas.
    drawRect(c.facade, size = Size(w, h * SHOP_BOTTOM_Y))
    var course = h * 0.014f
    while (course < h * CORNICE_Y) {
        drawLine(c.facadeShade.copy(alpha = 0.32f), Offset(0f, course), Offset(w, course), strokeWidth = hair)
        course += h * 0.0185f
    }
    for (x in listOf(0.035f, 0.965f)) {
        drawRect(c.facadeLight, topLeft = Offset(w * x - w * 0.018f, 0f), size = Size(w * 0.036f, h * CORNICE_Y))
    }

    // Tres ventanas; de noche algunas están prendidas.
    val top = h * WINDOWS_TOP_Y
    val wh = h * WINDOWS_HEIGHT
    for (col in 0..2) {
        val cx = w * (0.2f + col * 0.3f)
        val ww = w * 0.17f
        val left = cx - ww / 2f
        val lit = col in c.litWindows
        drawRect(c.facadeShade, topLeft = Offset(left - hair * 3, top - hair * 3), size = Size(ww + hair * 6, wh + hair * 6))
        drawRect(c.windowFrame, topLeft = Offset(left, top), size = Size(ww, wh))
        val inset = hair * 3.5f
        val glassTop = top + inset
        val glassSize = Size(ww - inset * 2, wh - inset * 2)
        if (lit) {
            val glowCenter = Offset(cx, top + wh / 2f)
            drawCircle(
                Brush.radialGradient(listOf(c.lampGlow.copy(alpha = c.lampGlow.alpha * 0.45f), Color.Transparent), glowCenter, ww * 0.8f),
                radius = ww * 0.8f,
                center = glowCenter,
            )
        }
        drawRect(
            if (lit) {
                Brush.verticalGradient(listOf(c.windowLit, c.windowLitDeep), startY = glassTop, endY = glassTop + glassSize.height)
            } else {
                Brush.linearGradient(
                    listOf(c.windowGlass, c.windowGlassLight, c.windowGlass),
                    start = Offset(left, glassTop),
                    end = Offset(left + ww, glassTop + wh),
                )
            },
            topLeft = Offset(left + inset, glassTop),
            size = glassSize,
        )
        drawLine(c.windowFrame, Offset(cx, glassTop), Offset(cx, glassTop + glassSize.height), strokeWidth = hair * 3)
        val mid = glassTop + glassSize.height * 0.42f
        drawLine(c.windowFrame, Offset(left + inset, mid), Offset(left + ww - inset, mid), strokeWidth = hair * 3)
        drawRect(c.facadeLight, topLeft = Offset(left - hair * 6, top + wh + hair * 2), size = Size(ww + hair * 12, hair * 6))
    }

    // Cornisa, letrero y toldo.
    drawRect(c.facadeLight, topLeft = Offset(0f, h * (CORNICE_Y - 0.012f)), size = Size(w, h * 0.016f))
    drawLine(c.facadeShade, Offset(0f, h * (CORNICE_Y + 0.004f)), Offset(w, h * (CORNICE_Y + 0.004f)), strokeWidth = hair * 2)
    val signLeft = w * 0.07f
    val signTop = h * SIGN_TOP_Y
    val signHeight = h * (SIGN_BOTTOM_Y - SIGN_TOP_Y)
    if (c.signGlow.alpha > 0f) {
        drawRoundRect(
            c.signGlow,
            topLeft = Offset(signLeft - hair * 10, signTop - hair * 8),
            size = Size(w * 0.86f + hair * 20, signHeight + hair * 16),
            cornerRadius = CornerRadius(hair * 14),
        )
    }
    drawRoundRect(c.signInk, topLeft = Offset(signLeft, signTop), size = Size(w * 0.86f, signHeight), cornerRadius = CornerRadius(hair * 3))
    val signStyle =
        TextStyle(
            color = c.signText,
            fontSize = (signHeight * 0.46f / density / fontScale).sp,
            fontFamily = VaiinillaSerif,
            fontWeight = FontWeight.Bold,
            letterSpacing = (signHeight * 0.08f / density / fontScale).sp,
        )
    val sign = textMeasurer.measure("VAIINILLA", signStyle)
    val signAt = Offset((w - sign.size.width) / 2f, signTop + (signHeight - sign.size.height) / 2f)
    val neon = if (c.signGlow.alpha > 0f) Shadow(c.signText.copy(alpha = 0.8f), blurRadius = hair * 9) else null
    drawText(sign, topLeft = signAt, shadow = neon)
    for (dot in listOf(0.2f, 0.8f)) {
        drawCircle(Lime, radius = hair * 3, center = Offset(w * dot, signTop + signHeight / 2f))
    }
    val awningTop = h * SIGN_BOTTOM_Y
    val awningBottom = h * AWNING_BOTTOM_Y
    val awningLeft = w * 0.05f
    val awningWidth = w * 0.9f
    val stripes = 11
    val stripe = awningWidth / stripes
    for (i in 0 until stripes) {
        drawRect(
            if (i % 2 == 0) c.awning else c.awningDark,
            topLeft = Offset(awningLeft + i * stripe, awningTop),
            size = Size(stripe + 0.5f, awningBottom - awningTop),
        )
        drawCircle(
            if (i % 2 == 0) c.awning else c.awningDark,
            radius = stripe / 2f,
            center = Offset(awningLeft + i * stripe + stripe / 2f, awningBottom),
        )
    }
    drawRect(Color(0x1A000000), topLeft = Offset(awningLeft, awningTop), size = Size(awningWidth, hair * 2))

    // Planta baja: dos vitrinas con luz cálida y la puerta abierta.
    val shopTop = awningBottom + h * 0.016f
    val shopBottom = h * SHOP_BOTTOM_Y
    val plinth = h * 0.03f
    for ((from, to) in listOf(0.06f to 0.37f, 0.63f to 0.94f)) {
        val left = w * from
        val width = w * (to - from)
        val glassBottom = shopBottom - plinth
        drawRect(c.doorGreen, topLeft = Offset(left - hair * 3, shopTop - hair * 3), size = Size(width + hair * 6, glassBottom - shopTop + hair * 6))
        drawRect(
            Brush.verticalGradient(listOf(c.shopLight, c.shopLightDeep), startY = shopTop, endY = glassBottom),
            topLeft = Offset(left, shopTop),
            size = Size(width, glassBottom - shopTop),
        )
        for (lamp in listOf(0.3f, 0.7f)) {
            val lx = left + width * lamp
            drawLine(c.signInk, Offset(lx, shopTop), Offset(lx, shopTop + h * 0.03f), strokeWidth = hair)
            drawArc(c.signInk, 180f, 180f, true, topLeft = Offset(lx - hair * 9, shopTop + h * 0.026f), size = Size(hair * 18, hair * 12))
            drawCircle(c.lampGlow, radius = hair * 16, center = Offset(lx, shopTop + h * 0.04f))
        }
        val shelfY = shopTop + (glassBottom - shopTop) * 0.62f
        drawRect(c.shopShelf, topLeft = Offset(left, shelfY), size = Size(width, hair * 4))
        for (cup in 0..3) {
            val cx = left + width * (0.18f + cup * 0.21f)
            drawRoundRect(
                if (cup % 2 == 0) VainiCream else Lime,
                topLeft = Offset(cx - hair * 5, shelfY - hair * 12),
                size = Size(hair * 10, hair * 12),
                cornerRadius = CornerRadius(hair * 2),
            )
        }
        drawRect(c.doorGreen.copy(alpha = 0.18f), topLeft = Offset(left, glassBottom - hair * 2), size = Size(width, hair * 2))
        drawRect(c.facadeShade, topLeft = Offset(left - hair * 3, glassBottom + hair * 3), size = Size(width + hair * 6, plinth - hair * 3))
    }
    val doorLeft = w * 0.395f
    val doorWidth = w * 0.21f
    drawRect(c.doorGreen, topLeft = Offset(doorLeft - hair * 4, shopTop - hair * 4), size = Size(doorWidth + hair * 8, shopBottom - shopTop + hair * 4))
    drawRect(
        Brush.verticalGradient(listOf(c.shopLightDeep, c.shopLight), startY = shopTop, endY = shopBottom),
        topLeft = Offset(doorLeft, shopTop),
        size = Size(doorWidth, shopBottom - shopTop),
    )
    val vainiBox = doorWidth * 1.08f
    withTransform({ translate(doorLeft + (doorWidth - vainiBox) / 2f, shopBottom - vainiBox * 0.8f) }) {
        drawVainiStanding(
            boxPx = vainiBox,
            paperColor = VainiCream,
            accentColor = VainiLime,
            inkColor = VainiInk,
            blink = motion.blink,
            waveDegrees = motion.wave,
            happy = motion.happy,
        )
    }

    // Banqueta, macetas y calle. De noche la luz de la tienda cae sobre la banqueta.
    drawRect(c.sidewalk, topLeft = Offset(0f, shopBottom), size = Size(w, h * (SIDEWALK_BOTTOM_Y - SHOP_BOTTOM_Y)))
    var slab = 0f
    while (slab < w) {
        drawLine(c.street.copy(alpha = 0.5f), Offset(slab, shopBottom), Offset(slab, h * SIDEWALK_BOTTOM_Y), strokeWidth = hair)
        slab += w * 0.125f
    }
    drawRect(c.street, topLeft = Offset(0f, h * SIDEWALK_BOTTOM_Y), size = Size(w, h * (1f - SIDEWALK_BOTTOM_Y)))
    if (c.spill.alpha > 0f) {
        drawRect(
            Brush.verticalGradient(listOf(c.spill, Color.Transparent), startY = shopBottom, endY = h * (SIDEWALK_BOTTOM_Y + 0.06f)),
            topLeft = Offset(w * 0.04f, shopBottom),
            size = Size(w * 0.92f, h * (SIDEWALK_BOTTOM_Y + 0.06f - SHOP_BOTTOM_Y)),
        )
    }
    drawRect(Color(0x14000000), topLeft = Offset(0f, h * SIDEWALK_BOTTOM_Y), size = Size(w, hair * 3))
    for (x in listOf(0.03f, 0.97f)) {
        val cx = w * x
        val potTop = shopBottom - h * 0.012f
        drawCircle(c.leafDark, radius = w * 0.04f, center = Offset(cx, potTop - w * 0.032f))
        drawCircle(c.leaf, radius = w * 0.034f, center = Offset(cx - w * 0.01f, potTop - w * 0.046f))
        drawRoundRect(c.pot, topLeft = Offset(cx - w * 0.026f, potTop), size = Size(w * 0.052f, h * 0.026f), cornerRadius = CornerRadius(hair * 3))
    }
}

/**
 * Pantalla de recogida: la tienda con el estado del pedido y, cuando toca, el QR que sale de la fachada.
 * Si hay [celebration], abre con la tarjeta de "¡Pedido recibido!" sobre la tienda desenfocada; la tarjeta
 * cae y se vuelve la hoja de estado, sin cambiar de pantalla.
 */
@Composable
internal fun PickupJourneyScreen(
    order: OrderDetail,
    qrPayload: String?,
    onBack: () -> Unit,
    onViewTracking: () -> Unit,
    celebration: PurchaseCelebrationKind? = null,
    onCelebrationFinished: () -> Unit = {},
    deliveryRequiresQr: Boolean = true,
    showTrackingButton: Boolean = true,
) {
    val palette = scenePaletteForTheme()
    CompositionLocalProvider(LocalScene provides palette) {
        PickupJourney(
            order = order,
            qrPayload = qrPayload,
            onBack = onBack,
            onViewTracking = onViewTracking.takeIf { showTrackingButton },
            celebration = celebration,
            onCelebrationFinished = onCelebrationFinished,
            deliveryRequiresQr = deliveryRequiresQr,
        )
    }
}

/** Color de fondo del Vainiverso a la hora del tema, para lo que rodea a la escena. */
@Composable
internal fun vainiversoStageColor(): Color = scenePaletteForTheme().stage

@Composable
private fun PickupJourney(
    order: OrderDetail,
    qrPayload: String?,
    onBack: () -> Unit,
    onViewTracking: (() -> Unit)?,
    celebration: PurchaseCelebrationKind?,
    onCelebrationFinished: () -> Unit,
    deliveryRequiresQr: Boolean,
) {
    val palette = LocalScene.current
    val reduceMotion = reducedMotion()
    val haptics = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()
    val matrix = remember(qrPayload) { qrPayload?.let { runCatching { QrCodeEncoder.encode(it) }.getOrNull() } }
    val hasCode = matrix != null && pickupCodeUseful(order, deliveryRequiresQr)
    val state = order.summary.state
    val step = pickupStep(state)
    val ready = step == PickupStep.READY

    // 1 = tarjeta de celebración al frente; 0 = ya es la hoja de estado.
    val intro = remember(order.summary.id) { Animatable(if (celebration != null) 1f else 0f) }
    val badge = remember(order.summary.id) { Animatable(if (celebration != null && !reduceMotion) 0f else 1f) }
    val check = remember(order.summary.id) { Animatable(if (celebration != null && !reduceMotion) 0f else 1f) }
    val latestOnCelebrationFinished by rememberUpdatedState(onCelebrationFinished)
    LaunchedEffect(order.summary.id, celebration) {
        if (celebration == null) return@LaunchedEffect
        intro.snapTo(1f)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (!reduceMotion) {
            badge.snapTo(0f)
            check.snapTo(0f)
            launch { badge.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 380f)) }
            launch {
                delay(140)
                check.animateTo(1f, spring(dampingRatio = 1f, stiffness = 90f))
            }
        }
        delay(if (reduceMotion) 900 else 1700)
        if (reduceMotion) intro.snapTo(0f) else intro.animateTo(0f, spring(dampingRatio = 0.92f, stiffness = 55f))
        latestOnCelebrationFinished()
    }
    val celebrating = celebration != null

    var revealed by rememberSaveable(order.summary.id) { mutableStateOf(false) }
    val reveal = remember(order.summary.id) { Animatable(if (revealed) 1f else 0f) }
    var autoRevealedState by rememberSaveable(order.summary.id) { mutableStateOf<String?>(null) }
    val autoReveal = shouldAutoRevealPickupCode(order, hasCode)
    LaunchedEffect(autoReveal, state, celebrating) {
        if (celebrating || !autoReveal || autoRevealedState == state.wireValue) return@LaunchedEffect
        // Deja ver a Vaini saludar antes de volar hacia el código.
        delay(if (reduceMotion) 300 else 1500)
        autoRevealedState = state.wireValue
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        revealed = true
    }
    LaunchedEffect(revealed) {
        val target = if (revealed) 1f else 0f
        if (reduceMotion) reveal.snapTo(target) else reveal.animateTo(target, spring(dampingRatio = 1f, stiffness = 14f))
    }
    LaunchedEffect(hasCode) { if (!hasCode) revealed = false }
    BackHandler(enabled = revealed) { revealed = false }
    ScreenBrightnessBoost(active = revealed)

    val idle = rememberInfiniteTransition(label = "pickup_vaini")
    val blink by idle.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                keyframes {
                    durationMillis = 3800
                    1f at 3200
                    0.06f at 3280
                    1f at 3390
                },
                RepeatMode.Restart,
            ),
        label = "blink",
    )
    val wave by idle.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                keyframes {
                    durationMillis = 2200
                    0f at 0
                    -112f at 380
                    -128f at 620
                    -98f at 860
                    -128f at 1100
                    -112f at 1320
                    0f at 1750
                },
                RepeatMode.Restart,
            ),
        label = "wave",
    )
    val cheering = ready || intro.value > 0.02f
    val motion =
        SceneMotion(
            blink = if (reduceMotion) 1f else blink,
            wave = if (cheering && !reduceMotion) wave else 0f,
            happy = cheering,
        )

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(palette.stage),
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val stage = Size(widthPx, heightPx)
        val mosaic =
            remember(matrix, widthPx, heightPx, palette) {
                matrix?.let { buildMosaic(it, widthPx, heightPx, density, textMeasurer, palette) }
            }
        val p = reveal.value
        val camera = cameraAt(p, widthPx, heightPx)
        val sceneAlpha = 1f - segment(p, 0.46f, 0.62f)
        val speedBlur = if (p in 0.03f..0.5f) min(10f, abs(reveal.velocity) * 9f) else 0f
        val introBlur = if (reduceMotion) 0f else intro.value * 18f
        val sceneBlur = maxOf(speedBlur, introBlur)

        if (sceneAlpha > 0.001f) {
            Canvas(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0f)
                            scaleX = camera.zoom
                            scaleY = camera.zoom
                            translationX = camera.anchor.x - camera.focus.x * camera.zoom
                            translationY = camera.anchor.y - camera.focus.y * camera.zoom
                            alpha = sceneAlpha
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && sceneBlur > 0.5f) {
                                renderEffect = BlurEffect(sceneBlur, sceneBlur, TileMode.Clamp)
                            }
                        },
            ) {
                drawStorefront(stage, textMeasurer, palette, motion)
            }
        }

        if (mosaic != null && hasCode && p > 0.3f) {
            Canvas(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = "Código QR del pedido ${order.summary.folio}" },
            ) {
                drawMosaic(mosaic, p, stage)
            }
        }

        StatusSheet(
            order = order,
            step = step,
            hasCode = hasCode,
            hidden = maxOf(eased(p, 0f, 0.2f), intro.value),
            backdrop = { drawStorefront(stage, textMeasurer, palette, motion) },
            onShowCode = { revealed = true },
            onViewTracking = onViewTracking,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (intro.value > 0.001f && celebration != null) {
            CelebrationCard(
                order = order,
                kind = celebration,
                shown = intro.value,
                badge = badge.value,
                check = check.value,
                dropPx = heightPx * 0.32f,
                backdrop = { drawStorefront(stage, textMeasurer, palette, motion) },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        val pickupIn = eased(p, 0.8f, 1f)
        if (pickupIn > 0.001f) {
            PickupCodeSheet(
                order = order,
                shown = pickupIn,
                onViewTracking = onViewTracking,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            GlassCircleButton(
                closing = segment(p, 0.4f, 0.7f),
                onClick = { if (revealed) revealed = false else onBack() },
            )
        }
    }
}

/**
 * La confirmación de compra, en vidrio sobre la tienda. Al irse ([shown] de 1 a 0) cae hacia la hoja de
 * estado mientras la hoja sube, así se lee como la misma tarjeta que se acomoda abajo.
 */
@Composable
private fun CelebrationCard(
    order: OrderDetail,
    kind: PurchaseCelebrationKind,
    shown: Float,
    badge: Float,
    check: Float,
    dropPx: Float,
    backdrop: DrawScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalScene.current
    val paid = kind == PurchaseCelebrationKind.PAYMENT_CONFIRMED
    val title = if (paid) "¡Compra confirmada!" else "¡Pedido recibido!"
    val message = if (paid) "Tu pago fue autorizado y tu pedido ya está en marcha." else "Paga en Caja para continuar con tu pedido."
    val leaving = 1f - shown
    GlassPanel(
        backdrop = backdrop,
        shape = RoundedCornerShape(32.dp),
        modifier =
            modifier
                .padding(horizontal = 20.dp)
                .graphicsLayer {
                    translationY = dropPx * leaving * leaving
                    val scale = 1f - 0.08f * leaving
                    scaleX = scale
                    scaleY = scale
                    alpha = (shown * 1.6f).coerceIn(0f, 1f)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && leaving > 0.05f) {
                        val blur = leaving * 10.dp.toPx()
                        renderEffect = BlurEffect(blur, blur, TileMode.Decal)
                    }
                },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(116.dp)
                        .graphicsLayer {
                            scaleX = badge
                            scaleY = badge
                        }.clip(CircleShape)
                        .background(c.glassCard)
                        .border(1.dp, c.glassEdge, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    Modifier
                        .size(84.dp)
                        .semantics { contentDescription = title },
                ) {
                    drawCircle(Lime)
                    drawCheck(check)
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                title,
                color = c.ink,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(message, color = c.muted, fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(c.glassCard)
                        .border(1.dp, c.glassEdge, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Pedido #${order.summary.folio}",
                    color = c.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                )
                Text(moneyLabel(order.summary.total), color = c.ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

/** Palomita que se dibuja de izquierda a derecha según [progress]. */
private fun DrawScope.drawCheck(progress: Float) {
    if (progress <= 0f) return
    val w = size.width
    val path =
        Path().apply {
            moveTo(w * 0.29f, w * 0.52f)
            lineTo(w * 0.44f, w * 0.66f)
            lineTo(w * 0.72f, w * 0.37f)
        }
    val measure = PathMeasure().apply { setPath(path, false) }
    val partial = Path()
    measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), partial, true)
    drawPath(partial, LimeInk, style = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawMosaic(
    mosaic: Mosaic,
    p: Float,
    stage: Size,
) {
    val n = mosaic.matrix.width
    val camera = cameraAt(p, stage.width, stage.height)
    val cameraCenter = camera.toScreen(mosaic.sceneCenter)
    val cameraSide = mosaic.sceneSide * camera.zoom
    val finalSide = min(stage.width * 0.74f, stage.height * 0.4f)
    val finalCenter = Offset(stage.width * 0.5f, stage.height * 0.38f)
    val settle = eased(p, 0.5f, 0.88f)
    val side = lerpF(cameraSide, finalSide, settle)
    val center = cameraCenter + (finalCenter - cameraCenter) * settle
    val rect = Rect(center.x - side / 2f, center.y - side / 2f, center.x + side / 2f, center.y + side / 2f)
    val cell = side / n
    val gridAlpha = eased(p, 0.3f, 0.44f)

    val cardAlpha = eased(p, 0.6f, 0.86f)
    if (cardAlpha > 0f) {
        val pad = cell * 1.6f
        val radius = CornerRadius(24.dp.toPx())
        drawRoundRect(
            Color.Black.copy(alpha = 0.06f * cardAlpha),
            topLeft = Offset(rect.left - pad, rect.top - pad + 10.dp.toPx()),
            size = Size(side + pad * 2, side + pad * 2),
            cornerRadius = radius,
        )
        drawRoundRect(
            QrLight.copy(alpha = cardAlpha),
            topLeft = Offset(rect.left - pad, rect.top - pad),
            size = Size(side + pad * 2, side + pad * 2),
            cornerRadius = radius,
        )
    }
    // Las juntas entre bloques se ven mientras es mosaico y se cierran antes del final para que escanee.
    val gap = cell * 0.08f * (1f - segment(p, 0.78f, 0.9f))
    for (row in 0 until n) {
        for (col in 0 until n) {
            val start = mosaicCellStart(col, row, n)
            val flip = eased(p, start, start + MOSAIC_FLIP_LENGTH)
            val target = if (mosaic.matrix[col, row]) QrDark else QrLight
            val color = lerp(mosaic.sampled[row * n + col], target, flip)
            val bump = 1f - 0.22f * sin(PI.toFloat() * flip)
            val size = (cell - gap) * bump
            val cx = rect.left + (col + 0.5f) * cell
            val cy = rect.top + (row + 0.5f) * cell
            drawRect(
                color.copy(alpha = color.alpha * gridAlpha),
                topLeft = Offset(cx - size / 2f, cy - size / 2f),
                size = Size(size + 0.5f, size + 0.5f),
            )
        }
    }
}

@Composable
private fun GlassCircleButton(
    closing: Float,
    onClick: () -> Unit,
) {
    val c = LocalScene.current
    Box(
        modifier =
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(c.glassTint)
                .border(1.dp, c.glassEdge, CircleShape)
                .physicalPress(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = if (closing < 0.5f) "Regresar" else null,
            tint = c.ink,
            modifier =
                Modifier.graphicsLayer {
                    alpha = 1f - closing
                    rotationZ = closing * 90f
                    scaleX = 1f - closing * 0.4f
                    scaleY = 1f - closing * 0.4f
                },
        )
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = if (closing >= 0.5f) "Cerrar código" else null,
            tint = c.ink,
            modifier =
                Modifier.graphicsLayer {
                    alpha = closing
                    rotationZ = (1f - closing) * -90f
                    scaleX = 0.6f + closing * 0.4f
                    scaleY = 0.6f + closing * 0.4f
                },
        )
    }
}

/** Vidrio esmerilado: la misma escena, desenfocada, detrás de un velo claro. Sin blur (Android < 12) queda un velo opaco. */
@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    backdrop: (DrawScope.() -> Unit)?,
    shape: RoundedCornerShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    content: @Composable () -> Unit,
) {
    val c = LocalScene.current
    var left by remember { mutableFloatStateOf(0f) }
    var top by remember { mutableFloatStateOf(0f) }
    val frosted = backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .onGloballyPositioned {
                    val at = it.positionInRoot()
                    left = at.x
                    top = at.y
                }
                .clip(shape),
    ) {
        if (frosted && backdrop != null) {
            Canvas(
                modifier =
                    Modifier
                        .matchParentSize()
                        .blur(26.dp),
            ) {
                withTransform({ translate(-left, -top) }) { backdrop() }
            }
        }
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(if (frosted) c.glassTint else c.sheetSolid)
                    .border(1.dp, c.glassEdge, shape),
        )
        content()
    }
}

@Composable
private fun StatusSheet(
    order: OrderDetail,
    step: PickupStep?,
    hasCode: Boolean,
    hidden: Float,
    backdrop: DrawScope.() -> Unit,
    onShowCode: () -> Unit,
    onViewTracking: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val c = LocalScene.current
    var height by remember { mutableFloatStateOf(0f) }
    val entrance = remember { Animatable(0f) }
    val reduceMotion = reducedMotion()
    LaunchedEffect(Unit) {
        if (reduceMotion) entrance.snapTo(1f) else entrance.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = 180f))
    }
    val progress = remember { Animatable((step?.ordinal ?: -1).toFloat() + 1f) }
    LaunchedEffect(step) {
        val target = (step?.ordinal ?: -1).toFloat() + 1f
        if (reduceMotion) progress.snapTo(target) else progress.animateTo(target, spring(dampingRatio = 0.9f, stiffness = 120f))
    }
    GlassPanel(
        backdrop = backdrop,
        modifier =
            modifier
                .onSizeChanged { height = it.height.toFloat() }
                .graphicsLayer {
                    val away = (1f - entrance.value) * 0.35f + hidden
                    translationY = height * away.coerceIn(0f, 1.1f)
                    alpha = (1f - hidden * 1.4f).coerceIn(0f, 1f)
                },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .background(c.glassLine, CircleShape),
            )
            Spacer(Modifier.height(12.dp))
            Text("Pedido #${order.summary.folio}", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            AnimatedContent(
                targetState = pickupTitle(order),
                transitionSpec = {
                    (fadeIn(spring(stiffness = 180f)) + scaleIn(spring(stiffness = 180f), initialScale = 0.92f) + slideInVertically { it / 4 }) togetherWith
                        (fadeOut(spring(stiffness = 300f)) + scaleOut(targetScale = 1.04f) + slideOutVertically { -it / 4 })
                },
                label = "pickup_title",
            ) { title ->
                Text(
                    title,
                    color = c.ink,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.2).sp,
                )
            }
            AnimatedContent(
                targetState = pickupHint(order),
                transitionSpec = { fadeIn(spring(stiffness = 180f)) togetherWith fadeOut(spring(stiffness = 300f)) },
                label = "pickup_hint",
            ) { hint ->
                Text(hint, color = c.muted, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (step != null) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    PickupStep.entries.forEach { item ->
                        val fill = (progress.value - item.ordinal).coerceIn(0f, 1f)
                        Column(Modifier.weight(1f)) {
                            Canvas(
                                Modifier
                                    .fillMaxWidth()
                                    .height(5.dp),
                            ) {
                                val r = CornerRadius(size.height / 2f)
                                drawRoundRect(c.glassLine, cornerRadius = r)
                                if (fill > 0f) drawRoundRect(c.ink, size = Size(size.width * fill, size.height), cornerRadius = r)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                item.label,
                                color = lerp(c.muted, c.ink, fill),
                                fontSize = 11.sp,
                                fontWeight = if (item == step) FontWeight.Black else FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            OrderSummaryCard(order)
            Spacer(Modifier.height(14.dp))
            if (hasCode || onViewTracking != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (hasCode) {
                        PillButton("Mostrar código", primary = true, onClick = onShowCode, modifier = Modifier.weight(1f))
                    }
                    if (onViewTracking != null) {
                        PillButton("Seguir pedido", primary = !hasCode, onClick = onViewTracking, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderSummaryCard(order: OrderDetail) {
    val c = LocalScene.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(c.glassCard)
                .border(1.dp, c.glassEdge, shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        val visible = order.items.take(2)
        visible.forEach { item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${item.quantity} × ${item.productName}",
                    color = c.ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(moneyLabel(item.subtotal), color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
        val hidden = order.items.size - visible.size
        if (hidden > 0) Text("+$hidden más", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier
                .padding(vertical = 6.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(c.glassLine),
        )
        Text(
            when (order.summary.destination) {
                OrderDestination.IN_SPACE -> "A ${order.summary.space?.name ?: "tu espacio"}"
                OrderDestination.TAKE_AWAY -> "Para llevar · recoge en barra"
            } + " · " + moneyLabel(order.summary.total),
            color = c.muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PickupCodeSheet(
    order: OrderDetail,
    shown: Float,
    onViewTracking: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val c = LocalScene.current
    var height by remember { mutableFloatStateOf(0f) }
    GlassPanel(
        backdrop = null,
        modifier =
            modifier
                .onSizeChanged { height = it.height.toFloat() }
                .graphicsLayer {
                    translationY = height * (1f - shown)
                    alpha = shown
                },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "#${order.summary.folio}",
                    color = c.ink,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.6).sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    order.summary.state.label,
                    color = LimeInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(Lime)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
            Text("Código de recogida", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(pickupCodeHint(order), color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(
                order.items.joinToString(" · ") { it.productName },
                color = c.muted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LightMode, contentDescription = null, tint = c.muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Subimos el brillo mientras está abierto.", color = c.muted, fontSize = 12.sp)
            }
            if (onViewTracking != null) {
                Spacer(Modifier.height(14.dp))
                PillButton("Seguir pedido", primary = true, onClick = onViewTracking, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PillButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalScene.current
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier =
            modifier
                .height(50.dp)
                .clip(shape)
                .background(if (primary) Lime else c.glassButton)
                .border(1.dp, if (primary) Color.Transparent else c.glassLine, shape)
                .physicalPress(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (primary) LimeInk else c.ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.3).sp,
        )
    }
}

/** Sube el brillo al máximo mientras el código está a la vista y lo regresa al cerrar. */
@Composable
private fun ScreenBrightnessBoost(active: Boolean) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(active, activity) {
        if (!active) return@DisposableEffect onDispose {}
        val window = activity.window
        val previous = window.attributes.screenBrightness
        window.attributes = window.attributes.apply { screenBrightness = 1f }
        onDispose { window.attributes = window.attributes.apply { screenBrightness = previous } }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
