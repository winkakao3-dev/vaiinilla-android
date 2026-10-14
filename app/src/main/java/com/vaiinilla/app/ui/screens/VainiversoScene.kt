package com.vaiinilla.app.ui.screens

import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.theme.LocalVaiinillaThemeMode
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

// La tienda Vaiinilla del Vainiverso: la escena y sus colores por hora, compartidas por Recogida y Pedidos.

/**
 * Colores de la escena y del vidrio. El tema no oscurece la escena de día: es la misma tienda a otra hora
 * (claro = día, oscuro = noche con las luces prendidas, AMOLED = medianoche con el letrero en neón).
 */
internal class ScenePalette(
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
) {
    /** De noche la tienda tiene luces encendidas; igual que `isNight` en iOS. */
    val isNight: Boolean get() = litWindows.isNotEmpty()
}

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

internal val LocalScene = staticCompositionLocalOf { Day }

@Composable
internal fun scenePaletteForTheme(): ScenePalette =
    when (LocalVaiinillaThemeMode.current.resolveEffectiveMode(isSystemInDarkTheme())) {
        VaiinillaThemeMode.Dark -> Night
        VaiinillaThemeMode.Amoled -> Midnight
        else -> Day
    }

internal val SceneLime = Color(0xFFB7D464)
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

/** Movimiento de Vaini en la escena; la escena en sí es fija. */
internal data class SceneMotion(
    val blink: Float = 1f,
    val wave: Float = 0f,
    val happy: Boolean = false,
)

internal fun DrawScope.drawStorefront(
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
        drawRect(
            c.facadeShade,
            topLeft = Offset(left - hair * 3, top - hair * 3),
            size =
                Size(
                    ww + hair * 6,
                    wh + hair * 6,
                ),
        )
        drawRect(c.windowFrame, topLeft = Offset(left, top), size = Size(ww, wh))
        val inset = hair * 3.5f
        val glassTop = top + inset
        val glassSize = Size(ww - inset * 2, wh - inset * 2)
        if (lit) {
            val glowCenter = Offset(cx, top + wh / 2f)
            drawCircle(
                Brush.radialGradient(
                    listOf(c.lampGlow.copy(alpha = c.lampGlow.alpha * 0.45f), Color.Transparent),
                    glowCenter,
                    ww * 0.8f,
                ),
                radius = ww * 0.8f,
                center = glowCenter,
            )
        }
        drawRect(
            if (lit) {
                Brush.verticalGradient(
                    listOf(c.windowLit, c.windowLitDeep),
                    startY = glassTop,
                    endY =
                        glassTop + glassSize.height,
                )
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
        drawRect(
            c.facadeLight,
            topLeft = Offset(left - hair * 6, top + wh + hair * 2),
            size =
                Size(
                    ww + hair * 12,
                    hair * 6,
                ),
        )
    }

    // Cornisa, letrero y toldo.
    drawRect(c.facadeLight, topLeft = Offset(0f, h * (CORNICE_Y - 0.012f)), size = Size(w, h * 0.016f))
    drawLine(
        c.facadeShade,
        Offset(0f, h * (CORNICE_Y + 0.004f)),
        Offset(w, h * (CORNICE_Y + 0.004f)),
        strokeWidth =
            hair * 2,
    )
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
    drawRoundRect(
        c.signInk,
        topLeft = Offset(signLeft, signTop),
        size = Size(w * 0.86f, signHeight),
        cornerRadius =
            CornerRadius(
                hair * 3,
            ),
    )
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
        drawCircle(SceneLime, radius = hair * 3, center = Offset(w * dot, signTop + signHeight / 2f))
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
        drawRect(
            c.doorGreen,
            topLeft = Offset(left - hair * 3, shopTop - hair * 3),
            size =
                Size(
                    width + hair * 6,
                    glassBottom - shopTop + hair * 6,
                ),
        )
        drawRect(
            Brush.verticalGradient(listOf(c.shopLight, c.shopLightDeep), startY = shopTop, endY = glassBottom),
            topLeft = Offset(left, shopTop),
            size = Size(width, glassBottom - shopTop),
        )
        for (lamp in listOf(0.3f, 0.7f)) {
            val lx = left + width * lamp
            drawLine(c.signInk, Offset(lx, shopTop), Offset(lx, shopTop + h * 0.03f), strokeWidth = hair)
            drawArc(
                c.signInk,
                180f,
                180f,
                true,
                topLeft = Offset(lx - hair * 9, shopTop + h * 0.026f),
                size =
                    Size(
                        hair * 18,
                        hair * 12,
                    ),
            )
            drawCircle(c.lampGlow, radius = hair * 16, center = Offset(lx, shopTop + h * 0.04f))
        }
        val shelfY = shopTop + (glassBottom - shopTop) * 0.62f
        drawRect(c.shopShelf, topLeft = Offset(left, shelfY), size = Size(width, hair * 4))
        for (cup in 0..3) {
            val cx = left + width * (0.18f + cup * 0.21f)
            drawRoundRect(
                if (cup % 2 == 0) VainiCream else SceneLime,
                topLeft = Offset(cx - hair * 5, shelfY - hair * 12),
                size = Size(hair * 10, hair * 12),
                cornerRadius = CornerRadius(hair * 2),
            )
        }
        drawRect(
            c.doorGreen.copy(alpha = 0.18f),
            topLeft = Offset(left, glassBottom - hair * 2),
            size =
                Size(
                    width,
                    hair * 2,
                ),
        )
        drawRect(
            c.facadeShade,
            topLeft = Offset(left - hair * 3, glassBottom + hair * 3),
            size =
                Size(
                    width + hair * 6,
                    plinth - hair * 3,
                ),
        )
    }
    val doorLeft = w * 0.395f
    val doorWidth = w * 0.21f
    drawRect(
        c.doorGreen,
        topLeft = Offset(doorLeft - hair * 4, shopTop - hair * 4),
        size =
            Size(
                doorWidth + hair * 8,
                shopBottom - shopTop + hair * 4,
            ),
    )
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
        drawLine(
            c.street.copy(alpha = 0.5f),
            Offset(slab, shopBottom),
            Offset(slab, h * SIDEWALK_BOTTOM_Y),
            strokeWidth = hair,
        )
        slab += w * 0.125f
    }
    drawRect(c.street, topLeft = Offset(0f, h * SIDEWALK_BOTTOM_Y), size = Size(w, h * (1f - SIDEWALK_BOTTOM_Y)))
    if (c.spill.alpha > 0f) {
        drawRect(
            Brush.verticalGradient(
                listOf(c.spill, Color.Transparent),
                startY = shopBottom,
                endY =
                    h * (SIDEWALK_BOTTOM_Y + 0.06f),
            ),
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
        drawRoundRect(
            c.pot,
            topLeft = Offset(cx - w * 0.026f, potTop),
            size = Size(w * 0.052f, h * 0.026f),
            cornerRadius =
                CornerRadius(
                    hair * 3,
                ),
        )
    }
}

/** Color de fondo del Vainiverso a la hora del tema, para lo que rodea a la escena. */
@Composable
internal fun vainiversoStageColor(): Color = scenePaletteForTheme().stage

/**
 * La tienda de fondo para pantallas con lista: arriba de [sheetTop] se ve nítida y debajo queda borrosa
 * detrás de un vidrio. La escena no se mueve con el scroll, solo sube la hoja, así el desenfoque no se
 * recalcula en cada cuadro. Va dentro de un `LocalScene` ya provisto.
 */
@Composable
internal fun VainiversoStoreBackdrop(
    sheetTop: () -> Float,
    cheering: Boolean,
    modifier: Modifier = Modifier,
    shiftFraction: Float = 0.12f,
) {
    val palette = LocalScene.current
    val textMeasurer = rememberTextMeasurer()
    val reduceMotion = reducedMotion()
    val idle = rememberInfiniteTransition(label = "store_vaini")
    val blink =
        idle.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    keyframes {
                        durationMillis = 4200
                        1f at 3600
                        0.06f at 3680
                        1f at 3790
                    },
                    RepeatMode.Restart,
                ),
            label = "blink",
        )
    val wave =
        idle.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec =
                infiniteRepeatable(
                    keyframes {
                        durationMillis = 2600
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
    VainiversoBackdrop(sheetTop = sheetTop, modifier = modifier) {
        val motion =
            SceneMotion(
                blink = if (reduceMotion) 1f else blink.value,
                wave = if (cheering && !reduceMotion) wave.value else 0f,
                happy = cheering,
            )
        withTransform({ translate(0f, -size.height * shiftFraction) }) {
            drawStorefront(size, textMeasurer, palette, motion)
        }
    }
}

/**
 * Cualquier escena del Vainiverso detrás de una lista: [drawScene] se pinta nítido arriba de [sheetTop]
 * y borroso debajo, con el velo de vidrio encima. Va dentro de un `LocalScene` ya provisto.
 */
@Composable
internal fun VainiversoBackdrop(
    sheetTop: () -> Float,
    modifier: Modifier = Modifier,
    drawScene: DrawScope.() -> Unit,
) {
    val palette = LocalScene.current
    val glassShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(palette.stage),
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(24.dp) else Modifier),
        ) { drawScene() }
        Canvas(Modifier.fillMaxSize()) {
            clipRect(bottom = sheetTop().coerceIn(0f, size.height)) { drawScene() }
        }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = sheetTop().coerceAtLeast(0f) }
                .clip(glassShape)
                .background(palette.glassTint)
                .border(1.dp, palette.glassEdge, glassShape),
        )
    }
}
