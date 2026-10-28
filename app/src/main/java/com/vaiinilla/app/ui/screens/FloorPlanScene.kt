package com.vaiinilla.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.vaiinilla.app.ui.order.FLOOR_TABLE_COUNT
import com.vaiinilla.app.ui.order.floorTableIndex
import kotlinx.coroutines.delay
import java.text.Normalizer
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// Comer aquí en el Vainiverso, como `FloorPlanScene.swift` en iOS: el lugar visto desde arriba, en pixel art. La
// barra con Vaini, las mesas y la tuya marcada con su nombre. El QR de la mesa se vuelve este plano al entrar
// (`StoreEntrance`) y, al seguir el pedido (`PickupJourneyScreen`), el plato sale a la barra cuando está listo y
// llega a tu mesa al entregarlo. El plano es una imagen de 96 × 96 pixeles que se escala sin suavizar, así que se
// ve nítido con cualquier zoom.

/** La cuadrícula del plano: 96 × 96 pixeles a todo lo ancho, arriba de la hoja de vidrio (~55 % del alto). */
internal object FloorPlanLayout {
    const val COLUMNS = 96
    const val ROWS = 96

    /** Centros de las mesas en la cuadrícula: tres columnas y dos filas. */
    private val tableColumns = listOf(18, 48, 78)
    private val tableRows = listOf(44, 73)
    val bar = GridRect(16, 22, 64, 7)

    /** Donde la cocina deja los platos, en el tapete de la barra. */
    val passCell = GridCell(51, 25)
    val vainiCell = GridCell(20, 5)
    val cupCell = GridCell(75, 25)
    val catCell = GridCell(58, 81)

    fun pixel(stage: Size): Float = stage.width / COLUMNS

    /** Un punto de la cuadrícula en la pantalla. */
    fun point(
        x: Float,
        y: Float,
        stage: Size,
    ): Offset {
        val p = pixel(stage)
        return Offset(x * p, (stage.height * 0.062f).roundToInt() + y * p)
    }

    fun frame(stage: Size): Rect {
        val o = point(0f, 0f, stage)
        val p = pixel(stage)
        return Rect(o, Size(COLUMNS * p, ROWS * p))
    }

    /** El salón con sus muros. */
    fun room(stage: Size): Rect = Rect(point(3f, 2f, stage), point(93f, 90f, stage))

    fun tableCell(index: Int): GridCell = GridCell(tableColumns[index % 3], tableRows[(index / 3) % 2])

    /** El centro de la mesa [index] (0 a `FLOOR_TABLE_COUNT` - 1). */
    fun table(
        index: Int,
        stage: Size,
    ): Offset {
        val cell = tableCell(index)
        return point(cell.x.toFloat(), cell.y.toFloat(), stage)
    }
}

internal data class GridCell(
    val x: Int,
    val y: Int,
)

internal data class GridRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

/** Tu mesa en el plano: cuál es y cómo se llama. */
internal data class FloorTable(
    val index: Int,
    val name: String,
) {
    companion object {
        /** La mesa de un espacio con mesas; `null` si es una cancha. */
        fun of(
            id: Int,
            name: String,
            type: String?,
        ): FloorTable? {
            if (type == "cancha") return null
            val label = name.trim()
            return FloorTable(floorTableIndex(label, id), label.ifEmpty { "Tu mesa" })
        }
    }
}

/** El plato de tu pedido: [served] lo pone en la barra (0–1) y [delivered] lo lleva a tu mesa (0–1). */
internal data class FloorPlate(
    val served: Float,
    val delivered: Float,
)

internal enum class PixelVainiPose { REST, BLINK, HAPPY, WAVE }

/**
 * La vida del lugar va a 4 cuadros por segundo, como un sprite: Vaini parpadea (y saluda con tu pedido listo), la
 * taza de la barra echa vapor y el gato de la puerta mueve la cola. Con movimiento reducido se queda en 0.
 */
@Composable
internal fun rememberPixelTick(moving: Boolean): Int {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val tick by produceState(0, moving, lifecycle) {
        if (!moving) {
            value = 0
            return@produceState
        }
        // Con la app en segundo plano nadie ve el sprite: se detiene hasta que vuelve (EQUIPO-21).
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(250)
                value++
            }
        }
    }
    return tick
}

/**
 * El plano a tamaño de pantalla, con Vaini en la barra y, si hay pedido, su plato. Con [tick] = null queda quieto y
 * sin la vida del lugar: es el plano que muestrea el mosaico del QR.
 */
internal fun DrawScope.drawFloorPlan(
    stage: Size,
    palette: ScenePalette,
    mine: FloorTable?,
    tick: Int? = null,
    cheering: Boolean = false,
    plate: FloorPlate? = null,
) {
    val art = PixelPalette.of(palette)
    drawRect(palette.sidewalk, size = stage)
    drawPixels(FloorPlanArt.plan(art, mine), FloorPlanLayout.frame(stage))
    val pose =
        when {
            tick == null -> PixelVainiPose.REST
            cheering -> if (tick % 2 == 0) PixelVainiPose.HAPPY else PixelVainiPose.WAVE
            tick % 14 == 13 -> PixelVainiPose.BLINK
            else -> PixelVainiPose.REST
        }
    drawPixels(
        FloorPlanArt.vaini(pose),
        cellRect(FloorPlanLayout.vainiCell, FloorPlanArt.VAINI_WIDTH, FloorPlanArt.VAINI_HEIGHT, stage),
    )
    if (tick == null) return
    drawPixels(
        FloorPlanArt.cat(art, tailUp = tick % 12 >= 9),
        cellRect(FloorPlanLayout.catCell, FloorPlanArt.CAT_WIDTH, FloorPlanArt.CAT_HEIGHT, stage),
    )
    drawPixelSteam(FloorPlanLayout.cupCell, tick, if (art.night) 0.32f else 0.55f, stage)
    if (plate != null && mine != null && plate.served > 0.001f) {
        drawPlate(plate, mine.index, art, stage)
        if (plate.delivered > 0.98f) {
            val cell = FloorPlanLayout.tableCell(mine.index)
            drawPixelSteam(GridCell(cell.x, cell.y - 4), tick + 2, if (art.night) 0.4f else 0.6f, stage)
        }
    }
}

private fun cellRect(
    cell: GridCell,
    width: Int,
    height: Int,
    stage: Size,
): Rect {
    val at = FloorPlanLayout.point(cell.x.toFloat(), cell.y.toFloat(), stage)
    val p = FloorPlanLayout.pixel(stage)
    return Rect(at, Size(width * p, height * p))
}

/** Una imagen de pixeles en [rect], sin suavizar. */
internal fun DrawScope.drawPixels(
    image: ImageBitmap,
    rect: Rect,
    alpha: Float = 1f,
) {
    drawImage(
        image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt()),
        dstSize = IntSize(rect.width.roundToInt(), rect.height.roundToInt()),
        alpha = alpha,
        filterQuality = FilterQuality.None,
    )
}

/** Dos bocanadas de vapor que suben de pixel en pixel desde [cell] y se desvanecen (un ciclo cada segundo). */
internal fun DrawScope.drawPixelSteam(
    cell: GridCell,
    tick: Int,
    alpha: Float,
    stage: Size,
) {
    val p = FloorPlanLayout.pixel(stage)
    for (puff in 0 until 2) {
        val phase = (tick + puff * 2) % 4
        val at =
            FloorPlanLayout.point(
                (cell.x + listOf(0, 1, 0, -1)[phase]).toFloat(),
                (cell.y - 1 - phase * 2).toFloat(),
                stage,
            )
        drawRect(
            Color.White.copy(alpha = alpha * (1f - phase * 0.22f)),
            topLeft = at,
            size = Size(p, p * (if (phase == 0) 1 else 2)),
        )
    }
}

/**
 * El plato: sale en la barra con un saltito y, al entregarlo, cruza el salón por encima de las mesas (su sombra se
 * queda en el piso) y se asienta en la tuya. Se mueve de pixel en pixel, como todo en el plano.
 */
private fun DrawScope.drawPlate(
    plate: FloorPlate,
    table: Int,
    art: PixelPalette,
    stage: Size,
) {
    val p = FloorPlanLayout.pixel(stage)
    val travel = SceneCurve.eased(plate.delivered, 0f, 1f)
    val from = FloorPlanLayout.passCell
    val to = FloorPlanLayout.tableCell(table)
    val gx = (from.x + (to.x - from.x) * travel).roundToInt().toFloat()
    val gy = (from.y + (to.y - from.y) * travel).roundToInt().toFloat()
    val lift =
        (6f * sin(PI.toFloat() * plate.delivered.coerceIn(0f, 1f)) + 3f * (1f - min(plate.served, 1f)))
            .roundToInt()
            .toFloat()
    val alpha = min(plate.served * 3f, 1f)
    val top = FloorPlanLayout.point(gx - 4, gy - 4 - lift, stage)
    val ground = FloorPlanLayout.point(gx - 4, gy + 2, stage)
    if (lift > 0f) drawPixels(FloorPlanArt.plateShadow(art), Rect(ground, Size(8 * p, 3 * p)), alpha)
    drawPixels(FloorPlanArt.plate(art), Rect(top, Size(8 * p, 8 * p)), alpha)
}

// El dibujo de pixeles.

/** Un color RGBA en 0–1 que se empaca en un pixel ARGB de Android. */
private data class PixelRgb(
    val r: Float,
    val g: Float,
    val b: Float,
    val a: Float = 1f,
) {
    constructor(color: Color) : this(color.red, color.green, color.blue, color.alpha)

    fun mix(
        other: PixelRgb,
        t: Float,
    ) = PixelRgb(r + (other.r - r) * t, g + (other.g - g) * t, b + (other.b - b) * t, a)

    val packed: Int get() =
        Color(
            r.coerceIn(0f, 1f),
            g.coerceIn(0f, 1f),
            b.coerceIn(0f, 1f),
            a.coerceIn(0f, 1f),
        ).toArgb()
}

private val PixelLime = Color(0xFFB7D464)
private val PixelCream = Color(0xFFF6EFDD)
private val PixelInk = Color(0xFF1A1414)
private val PixelPlateRim = Color(0xFFF7F3EA)
private val PixelPlateFood = Color(0xFFE38B5A)
private val PixelPlateLeaf = Color(0xFF8DAF48)

/** Los colores del pixel art, sacados de la paleta de la hora: el mismo lugar de día, de noche y a medianoche. */
internal class PixelPalette private constructor(
    val key: String,
    val night: Boolean,
    val midnight: Boolean,
    val outside: Int,
    val outsideDark: Int,
    val floor: Int,
    val floorDark: Int,
    val seam: Int,
    val floorLit: Int,
    val wall: Int,
    val wallLight: Int,
    val outline: Int,
    val wood: Int,
    val woodLight: Int,
    val woodDark: Int,
    val chair: Int,
    val chairDark: Int,
    val leaf: Int,
    val leafDark: Int,
    val leafLight: Int,
    val pot: Int,
    val glass: Int,
    val glassLight: Int,
    val metal: Int,
    val metalLight: Int,
    val lime: Int,
    val limeDark: Int,
    val cream: Int,
    val creamShade: Int,
    val ink: Int,
    val rim: Int,
    val rimShade: Int,
    val food: Int,
    val herb: Int,
    val tag: Int,
    val tagText: Int,
    val cat: Int,
    val catDark: Int,
    val shadow: Int,
) {
    companion object {
        private val cache = HashMap<String, PixelPalette>()

        fun of(c: ScenePalette): PixelPalette {
            // A medianoche solo queda una ventana prendida en la fachada.
            val key = if (c.isNight) (if (c.litWindows.size < 2) "midnight" else "night") else "day"
            return synchronized(cache) { cache.getOrPut(key) { make(c, key) } }
        }

        private fun make(
            c: ScenePalette,
            key: String,
        ): PixelPalette {
            val floor = PixelRgb(c.facadeLight)
            val shade = PixelRgb(c.facadeShade)
            val wall = PixelRgb(c.doorGreen)
            val wood = PixelRgb(c.shopShelf)
            val black = PixelRgb(0f, 0f, 0f)
            val white = PixelRgb(1f, 1f, 1f)
            val lime = PixelRgb(PixelLime)
            val cream = PixelRgb(PixelCream)
            val rim = PixelRgb(PixelPlateRim)
            val catDim = if (c.isNight) 0.35f else 0f
            return PixelPalette(
                key = key,
                night = c.isNight,
                midnight = key == "midnight",
                outside = PixelRgb(c.sidewalk).packed,
                outsideDark = PixelRgb(c.sidewalk).mix(black, 0.08f).packed,
                floor = floor.packed,
                floorDark = floor.mix(shade, 0.5f).packed,
                seam = shade.mix(black, 0.2f).packed,
                floorLit = floor.mix(PixelRgb(c.windowLit), if (c.isNight) 0.62f else 0.5f).packed,
                wall = wall.packed,
                wallLight = wall.mix(white, 0.18f).packed,
                outline = wall.mix(black, 0.5f).packed,
                wood = wood.packed,
                woodLight = wood.mix(PixelRgb(c.shopLight), 0.55f).packed,
                woodDark = wood.mix(black, 0.3f).packed,
                chair = wall.mix(white, 0.3f).packed,
                chairDark = wall.mix(white, 0.08f).packed,
                leaf = PixelRgb(c.leaf).packed,
                leafDark = PixelRgb(c.leafDark).packed,
                leafLight = PixelRgb(c.leaf).mix(white, 0.3f).packed,
                pot = PixelRgb(c.pot).packed,
                glass = PixelRgb(if (c.isNight) c.windowLitDeep else c.windowGlass).packed,
                glassLight = PixelRgb(if (c.isNight) c.windowLit else c.windowGlassLight).packed,
                metal = PixelRgb(c.signInk).mix(white, 0.15f).packed,
                metalLight = PixelRgb(c.signInk).mix(white, 0.45f).packed,
                lime = lime.packed,
                limeDark = lime.mix(black, 0.3f).packed,
                cream = cream.packed,
                creamShade = cream.mix(wood, 0.5f).packed,
                ink = PixelRgb(PixelInk).packed,
                rim = rim.packed,
                rimShade = rim.mix(shade, 0.55f).packed,
                food = PixelRgb(PixelPlateFood).packed,
                herb = PixelRgb(PixelPlateLeaf).packed,
                tag = PixelRgb(c.signInk).packed,
                tagText = PixelRgb(c.signText).packed,
                cat = PixelRgb(0.88f, 0.58f, 0.3f).mix(black, catDim).packed,
                catDark = PixelRgb(0.6f, 0.36f, 0.18f).mix(black, catDim).packed,
                shadow = PixelRgb(0f, 0f, 0f, 0.22f).packed,
            )
        }
    }
}

/** Una hoja de pixeles: se pinta con rectángulos y sprites y se vuelve una imagen. */
internal class PlanPixels(
    val width: Int,
    val height: Int,
) {
    private val pixels = IntArray(width * height)

    operator fun get(
        x: Int,
        y: Int,
    ): Int = if (x in 0 until width && y in 0 until height) pixels[y * width + x] else 0

    operator fun set(
        x: Int,
        y: Int,
        color: Int,
    ) {
        if (x in 0 until width && y in 0 until height) pixels[y * width + x] = color
    }

    fun rect(
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        color: Int,
    ) {
        for (yy in y until y + h) for (xx in x until x + w) this[xx, yy] = color
    }

    /** Dibuja [rows] con un color por letra; el punto es transparente. */
    fun sprite(
        rows: List<String>,
        x: Int,
        y: Int,
        colors: Map<Char, Int>,
    ) {
        rows.forEachIndexed { dy, row ->
            row.forEachIndexed { dx, char -> colors[char]?.let { this[x + dx, y + dy] = it } }
        }
    }

    fun image(): ImageBitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** Los dibujos ya pintados: se pintan una vez por hora (y por mesa) y se guardan. */
private object PixelCache {
    private val images = HashMap<String, ImageBitmap>()

    fun image(
        key: String,
        width: Int,
        height: Int,
        paint: PlanPixels.() -> Unit,
    ): ImageBitmap =
        synchronized(images) {
            images.getOrPut(key) { PlanPixels(width, height).apply(paint).image() }
        }
}

internal object FloorPlanArt {
    const val VAINI_WIDTH = 22
    const val VAINI_HEIGHT = 17
    const val STANDING_VAINI_HEIGHT = 23
    const val CAT_WIDTH = 10
    const val CAT_HEIGHT = 6

    fun plan(
        c: PixelPalette,
        mine: FloorTable?,
    ): ImageBitmap =
        PixelCache.image(
            "plan-${c.key}-${mine?.index ?: -1}-${mine?.name.orEmpty()}",
            FloorPlanLayout.COLUMNS,
            FloorPlanLayout.ROWS,
        ) { paintPlan(this, c, mine) }

    /**
     * Vaini de frente, como el sprite de la skill pixel-film a 3/5: la hoja crema con cuatro picos suaves arriba, la
     * esquina doblada en lima, dos rayitas lima y los ojos altos y negros abajo. Siempre crema, también de noche.
     * Detrás de la barra solo se le ve hasta los brazos.
     */
    fun vaini(pose: PixelVainiPose): ImageBitmap =
        PixelCache.image("vaini-$pose", VAINI_WIDTH, VAINI_HEIGHT) { paintVaini(this, pose, legs = false) }

    /** Vaini de cuerpo entero, parado sobre sus dos patitas (afuera de la barra). */
    fun standingVaini(pose: PixelVainiPose): ImageBitmap =
        PixelCache.image(
            "vaini-standing-$pose",
            VAINI_WIDTH,
            STANDING_VAINI_HEIGHT,
        ) { paintVaini(this, pose, legs = true) }

    private fun paintVaini(
        canvas: PlanPixels,
        pose: PixelVainiPose,
        legs: Boolean,
    ) {
        val cream = 0xFFF5EEDE.toInt()
        val creamHi = 0xFFFFFAF0.toInt()
        val creamShade = 0xFFE2D6BB.toInt()
        val outline = 0xFFB8A784.toInt()
        val lime = 0xFFA6D62A.toInt()
        val limeHi = 0xFFC6EC52.toInt()
        val limeShade = 0xFF6E9E1C.toInt()
        val eye = 0xFF141414.toInt()
        val blush = 0xFFFF9D8A.toInt()
        val bw = 18
        val bh = 17
        val ox = 2
        val oy = 1

        fun top(col: Int) = if (col >= 12) col - 12 else listOf(1, 0, 1)[col % 3]

        fun body(
            col: Int,
            row: Int,
        ): Boolean {
            if (col !in 0 until bw || row !in 0 until bh || row < top(col)) return false
            return !(row == bh - 1 && (col == 0 || col == bw - 1))
        }
        if (legs) {
            for (legCol in listOf(4, 11)) {
                canvas.rect(ox + legCol, oy + bh - 1, 4, 5, outline)
                canvas.rect(ox + legCol + 1, oy + bh - 1, 2, 4, cream)
                canvas[ox + legCol + 2, oy + bh] = creamShade
            }
        }
        // Los brazos salen de los costados, abajo y hacia afuera; al saludar el derecho sube.
        val left = listOf(-1 to 9, -1 to 10, -2 to 11)
        val right =
            if (pose == PixelVainiPose.WAVE) {
                listOf(bw to 8, bw + 1 to 7, bw + 1 to 6)
            } else {
                listOf(bw to 9, bw to 10, bw + 1 to 11)
            }
        val arms = (left + right).toSet()

        fun filled(
            col: Int,
            row: Int,
        ) = body(col, row) || (col to row) in arms
        for (row in -1..bh) {
            for (col in -3..bw + 2) {
                if (filled(col, row)) continue
                if (filled(col - 1, row) || filled(col + 1, row) || filled(col, row - 1) || filled(col, row + 1)) {
                    canvas[ox + col, oy + row] = outline
                }
            }
        }
        for ((col, row) in left + right) canvas[ox + col, oy + row] = cream
        for (row in 0 until bh) {
            for (col in 0 until bw) {
                if (!body(col, row)) continue
                canvas[ox + col, oy + row] =
                    when {
                        col >= bw - 2 || row >= bh - 2 -> creamShade
                        row <= top(col) + 1 && col < 12 -> creamHi
                        else -> cream
                    }
            }
        }
        // La esquina doblada hacia adelante y su sombra.
        for (row in 0..5) {
            for (col in 12 until bw) {
                if (row >= col - 12) canvas[ox + col, oy + row] = if (row == col - 12) limeHi else lime
            }
        }
        for (row in 0..5) canvas[ox + 12, oy + row] = limeShade
        for (col in 14 until bw) if (6 + (col - 14) / 2 < bh) canvas[ox + col, oy + 6] = limeShade
        // Las dos rayitas.
        for ((col0, length, row) in listOf(Triple(2, 8, 4), Triple(2, 5, 7))) {
            for (col in col0 until col0 + length) {
                canvas[ox + col, oy + row] = lime
                canvas[ox + col, oy + row + 1] = limeShade
            }
        }
        for (eyeCol in listOf(4, 11)) {
            val x = ox + eyeCol
            val y = oy + 10
            when (pose) {
                PixelVainiPose.REST -> {
                    canvas.rect(x, y, 2, 4, eye)
                    canvas[x, y + 1] = 0xFFFFFFFF.toInt()
                }
                PixelVainiPose.BLINK -> canvas.rect(x, y + 2, 2, 1, eye)
                PixelVainiPose.HAPPY, PixelVainiPose.WAVE -> {
                    canvas.sprite(listOf(".##.", "#..#"), x - 1, y + 1, mapOf('#' to eye))
                    canvas.rect(if (eyeCol == 4) ox + 1 else ox + 14, y + 3, 2, 1, blush)
                }
            }
        }
    }

    /** El gato del lugar, enroscado junto a la puerta, dormido; la cola se levanta de vez en cuando. */
    fun cat(
        c: PixelPalette,
        tailUp: Boolean,
    ): ImageBitmap =
        PixelCache.image("cat-${c.key}-$tailUp", CAT_WIDTH, CAT_HEIGHT) {
            sprite(
                listOf(
                    ".d.d......",
                    "dccd......",
                    "dccdddddd.",
                    "dcccccccd.",
                    "dcccccccd.",
                    ".dddddddd.",
                ),
                0,
                0,
                mapOf('d' to c.catDark, 'c' to c.cat),
            )
            this[1, 2] = c.catDark
            if (tailUp) {
                sprite(listOf("...t", "..t.", ".t..", "t..."), 6, 1, mapOf('t' to c.catDark))
            } else {
                sprite(listOf("tt", ".t"), 8, 4, mapOf('t' to c.catDark))
            }
        }

    fun plate(c: PixelPalette): ImageBitmap =
        PixelCache.image("plate-${c.key}", 8, 8) {
            sprite(
                listOf(
                    "..oooo..",
                    ".orrrro.",
                    "orrffrro",
                    "orfhffro",
                    "orffhfso",
                    "orrffsso",
                    ".osssso.",
                    "..oooo..",
                ),
                0,
                0,
                mapOf('o' to c.outline, 'r' to c.rim, 's' to c.rimShade, 'f' to c.food, 'h' to c.herb),
            )
        }

    fun plateShadow(c: PixelPalette): ImageBitmap =
        PixelCache.image("plate-shadow-${c.key}", 8, 3) {
            sprite(listOf(".ssssss.", "ssssssss", ".ssssss."), 0, 0, mapOf('s' to c.shadow))
        }

    // El salón.

    private fun paintPlan(
        canvas: PlanPixels,
        c: PixelPalette,
        mine: FloorTable?,
    ) {
        val w = FloorPlanLayout.COLUMNS
        val h = FloorPlanLayout.ROWS

        // Afuera: la banqueta con baldosas de 8 pixeles.
        canvas.rect(0, 0, w, h, c.outside)
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (y % 8 == 7 || (x + (y / 8) % 2 * 4) % 8 == 7) canvas[x, y] = c.outsideDark
            }
        }

        // El piso de duela: tablones de 4 pixeles con juntas desfasadas y algún nudo.
        val floor = GridRect(6, 5, 84, 82)
        for (y in floor.y until floor.y + floor.height) {
            val row = (y - floor.y) / 4
            for (x in floor.x until floor.x + floor.width) {
                var color = if ((y - floor.y) % 4 == 3) c.floorDark else c.floor
                if ((x - floor.x + row * 13) % 23 == 0) color = c.floorDark
                if ((x * 7 + row * 29) % 61 == 0 && (y - floor.y) % 4 == 1) color = c.floorDark
                canvas[x, y] = color
            }
        }
        // Sombra de los muros sobre el piso.
        canvas.rect(floor.x, floor.y, floor.width, 1, c.seam)
        canvas.rect(floor.x, floor.y, 1, floor.height, c.seam)

        // De noche cada mesa tiene su lámpara; a medianoche solo la tuya.
        if (c.night) {
            for (index in 0 until FLOOR_TABLE_COUNT) {
                if (c.midnight && index != mine?.index) continue
                val cell = FloorPlanLayout.tableCell(index)
                lightPool(canvas, cell.x, cell.y, c)
            }
        }

        // Muros con ventanas a los lados y la puerta abajo, con su tapete.
        wall(canvas, 3, 2, 90, 3, c)
        wall(canvas, 3, 2, 3, 88, c)
        wall(canvas, 90, 2, 3, 88, c)
        wall(canvas, 3, 87, 37, 3, c)
        wall(canvas, 56, 87, 37, 3, c)
        for (y in listOf(36, 64)) {
            window(canvas, 3, y, c)
            window(canvas, 90, y, c)
        }
        canvas.rect(42, 90, 12, 3, c.woodDark)
        for (x in 43 until 53 step 2) canvas[x, 91] = c.wood

        // La barra con el tapete donde la cocina deja los platos y la cafetera.
        val bar = FloorPlanLayout.bar
        canvas.rect(bar.x, bar.y, bar.width, bar.height, c.outline)
        canvas.rect(bar.x + 1, bar.y + 1, bar.width - 2, bar.height - 2, c.wood)
        canvas.rect(bar.x + 1, bar.y + 1, bar.width - 2, 1, c.woodLight)
        canvas.rect(bar.x + 1, bar.y + bar.height - 2, bar.width - 2, 1, c.woodDark)
        canvas.rect(bar.x + 1, bar.y + bar.height, bar.width - 1, 1, c.seam)
        canvas.sprite(
            listOf(
                "dddddddddd",
                "dlllllllld",
                "dlllllllld",
                "dddddddddd",
            ),
            46,
            23,
            mapOf('d' to c.woodDark, 'l' to c.woodLight),
        )
        canvas.sprite(
            listOf(
                ".oooooo.",
                "oMMMMMmo",
                "oMmoommo",
                "omoooomo",
                "ommoommo",
                ".oooooo.",
            ),
            64,
            22,
            mapOf('o' to c.outline, 'm' to c.metal, 'M' to c.metalLight),
        )
        canvas.sprite(
            listOf(".ooo.", "occco", "ocbco", "occco", ".ooo."),
            FloorPlanLayout.cupCell.x - 2,
            FloorPlanLayout.cupCell.y - 2,
            mapOf('o' to c.outline, 'c' to c.cream, 'b' to c.woodDark),
        )

        // Las mesas, cada una con cuatro sillas y un florerito.
        for (index in 0 until FLOOR_TABLE_COUNT) {
            val cell = FloorPlanLayout.tableCell(index)
            for ((dx, dy) in listOf(-2 to -10, -2 to 6, -10 to -2, 6 to -2)) {
                canvas.sprite(
                    listOf(".oo.", "occo", "occo", ".oo."),
                    cell.x + dx,
                    cell.y + dy,
                    mapOf('o' to c.chairDark, 'c' to c.chair),
                )
            }
            table(canvas, cell.x, cell.y, c)
        }

        // Macetas a los lados de la barra (la esquina de arriba a la izquierda la tapa el botón de regresar).
        plant(canvas, 7, 15, c)
        plant(canvas, 81, 15, c)

        // Tu mesa: esquinas lima alrededor y su nombre en una placa, como en un juego.
        if (mine != null) {
            val cell = FloorPlanLayout.tableCell(mine.index)
            val corners =
                listOf(
                    Triple(-11, -11, listOf("LLL", "L..", "L..")),
                    Triple(9, -11, listOf("LLL", "..L", "..L")),
                    Triple(-11, 9, listOf("L..", "L..", "LLL")),
                    Triple(9, 9, listOf("..L", "..L", "LLL")),
                )
            for ((dx, dy, rows) in corners) canvas.sprite(rows, cell.x + dx, cell.y + dy, mapOf('L' to c.lime))
            val topRow = (mine.index / 3) % 2 == 0
            nameTag(canvas, mine.name, cell.x, if (topRow) cell.y + 12 else cell.y - 19, pointsUp = topRow, c)
        }
    }

    private fun wall(
        canvas: PlanPixels,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        c: PixelPalette,
    ) {
        canvas.rect(x, y, w, h, c.wall)
        canvas.rect(x, y, w, 1, c.wallLight)
        canvas.rect(x, y + h - 1, w, 1, c.outline)
    }

    private fun window(
        canvas: PlanPixels,
        x: Int,
        y: Int,
        c: PixelPalette,
    ) {
        canvas.rect(x, y, 3, 10, c.outline)
        canvas.rect(x + 1, y + 1, 1, 8, c.glass)
        canvas[x + 1, y + 2] = c.glassLight
        canvas[x + 1, y + 3] = c.glassLight
    }

    /** Un círculo de luz con el borde tramado, como en los juegos de 16 bits. */
    private fun lightPool(
        canvas: PlanPixels,
        cx: Int,
        cy: Int,
        c: PixelPalette,
    ) {
        for (y in cy - 14..cy + 14) {
            for (x in cx - 14..cx + 14) {
                val d = sqrt(((x - cx) * (x - cx) + (y - cy) * (y - cy)).toFloat())
                if (!(d < 11f || (d < 14f && (x + y) % 2 == 0))) continue
                if (canvas[x, y] == c.floor || canvas[x, y] == c.floorDark) canvas[x, y] = c.floorLit
            }
        }
    }

    /** Una mesa redonda de 12 pixeles: luz arriba a la izquierda, borde oscuro y sombra abajo a la derecha. */
    private fun table(
        canvas: PlanPixels,
        cx: Int,
        cy: Int,
        c: PixelPalette,
    ) {
        val x0 = cx - 6
        val y0 = cy - 6
        for (y in 0 until 13) {
            for (x in 0 until 13) {
                if (inside(x - 1, y - 1) && !inside(x, y)) canvas[x0 + x, y0 + y] = c.seam
            }
        }
        for (y in 0 until 12) {
            for (x in 0 until 12) {
                if (!inside(x, y)) continue
                val edge = !inside(x - 1, y) || !inside(x + 1, y) || !inside(x, y - 1) || !inside(x, y + 1)
                canvas[x0 + x, y0 + y] =
                    when {
                        edge -> c.woodDark
                        x + y < 8 -> c.woodLight
                        x + y > 15 -> c.woodDark
                        else -> c.wood
                    }
            }
        }
        canvas.sprite(listOf(".l.", "lLl", ".l."), cx, cy - 4, mapOf('l' to c.leaf, 'L' to c.leafLight))
    }

    private fun inside(
        x: Int,
        y: Int,
    ): Boolean {
        val dx = x - 5.5f
        val dy = y - 5.5f
        return dx * dx + dy * dy <= 33f
    }

    private fun plant(
        canvas: PlanPixels,
        x: Int,
        y: Int,
        c: PixelPalette,
    ) {
        canvas.sprite(
            listOf(
                "..dddd..",
                ".dllLld.",
                "dlLlldld",
                "dlldllld",
                "dllldLld",
                "dlLlllld",
                ".dllldd.",
                "..dddd..",
            ),
            x,
            y,
            mapOf('d' to c.leafDark, 'l' to c.leaf, 'L' to c.leafLight),
        )
    }

    /** El nombre en una placa oscura con su piquito hacia la mesa, en letra de pixeles de 3 × 5. */
    private fun nameTag(
        canvas: PlanPixels,
        name: String,
        cx: Int,
        y: Int,
        pointsUp: Boolean,
        c: PixelPalette,
    ) {
        val text = pixelText(name)
        if (text.isEmpty()) return
        val width = text.length * 4 + 3
        val x = min(max(cx - width / 2, 7), FloorPlanLayout.COLUMNS - 7 - width)
        canvas.rect(x, y, width, 7, c.tag)
        if (pointsUp) canvas.rect(cx - 1, y - 1, 3, 1, c.tag) else canvas.rect(cx - 1, y + 7, 3, 1, c.tag)
        write(text, x + 2, y + 1, c.tagText, canvas)
    }

    /** [name] en mayúsculas, sin acentos y solo con las letras que tiene la letra de pixeles (hasta 18). */
    fun pixelText(name: String): String =
        Normalizer
            .normalize(name, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .uppercase()
            .filter { it in pixelFont }
            .let { text ->
                if (text.length <= 18) return@let text
                val cut = text.take(19).lastIndexOf(' ')
                if (cut > 0) text.take(cut) else text.take(18)
            }

    /** Escribe [text] en letra de pixeles de 3 × 5 (×[scale]), una letra cada 4 pixeles. */
    fun write(
        text: String,
        x: Int,
        y: Int,
        color: Int,
        canvas: PlanPixels,
        scale: Int = 1,
    ) {
        text.forEachIndexed { i, letter ->
            val glyph = pixelFont[letter] ?: return@forEachIndexed
            glyph.forEachIndexed { row, line ->
                line.forEachIndexed { col, dot ->
                    if (dot == '#') canvas.rect(x + (i * 4 + col) * scale, y + row * scale, scale, scale, color)
                }
            }
        }
    }

    private val pixelFont: Map<Char, List<String>> =
        mapOf(
            'A' to listOf(".#.", "#.#", "###", "#.#", "#.#"),
            'B' to listOf("##.", "#.#", "##.", "#.#", "##."),
            'C' to listOf(".##", "#..", "#..", "#..", ".##"),
            'D' to listOf("##.", "#.#", "#.#", "#.#", "##."),
            'E' to listOf("###", "#..", "##.", "#..", "###"),
            'F' to listOf("###", "#..", "##.", "#..", "#.."),
            'G' to listOf(".##", "#..", "#.#", "#.#", ".##"),
            'H' to listOf("#.#", "#.#", "###", "#.#", "#.#"),
            'I' to listOf("###", ".#.", ".#.", ".#.", "###"),
            'J' to listOf("..#", "..#", "..#", "#.#", ".#."),
            'K' to listOf("#.#", "#.#", "##.", "#.#", "#.#"),
            'L' to listOf("#..", "#..", "#..", "#..", "###"),
            'M' to listOf("#.#", "###", "#.#", "#.#", "#.#"),
            'N' to listOf("##.", "#.#", "#.#", "#.#", "#.#"),
            'O' to listOf(".#.", "#.#", "#.#", "#.#", ".#."),
            'P' to listOf("##.", "#.#", "##.", "#..", "#.."),
            'Q' to listOf(".#.", "#.#", "#.#", "##.", ".##"),
            'R' to listOf("##.", "#.#", "##.", "#.#", "#.#"),
            'S' to listOf(".##", "#..", ".#.", "..#", "##."),
            'T' to listOf("###", ".#.", ".#.", ".#.", ".#."),
            'U' to listOf("#.#", "#.#", "#.#", "#.#", "###"),
            'V' to listOf("#.#", "#.#", "#.#", "#.#", ".#."),
            'W' to listOf("#.#", "#.#", "###", "###", "#.#"),
            'X' to listOf("#.#", "#.#", ".#.", "#.#", "#.#"),
            'Y' to listOf("#.#", "#.#", ".#.", ".#.", ".#."),
            'Z' to listOf("###", "..#", ".#.", "#..", "###"),
            '0' to listOf("###", "#.#", "#.#", "#.#", "###"),
            '1' to listOf(".#.", "##.", ".#.", ".#.", "###"),
            '2' to listOf("##.", "..#", ".#.", "#..", "###"),
            '3' to listOf("##.", "..#", ".#.", "..#", "##."),
            '4' to listOf("#.#", "#.#", "###", "..#", "..#"),
            '5' to listOf("###", "#..", "##.", "..#", "##."),
            '6' to listOf(".##", "#..", "###", "#.#", "###"),
            '7' to listOf("###", "..#", ".#.", ".#.", ".#."),
            '8' to listOf("###", "#.#", "###", "#.#", "###"),
            '9' to listOf("###", "#.#", "###", "..#", "##."),
            ' ' to listOf("...", "...", "...", "...", "..."),
            '-' to listOf("...", "...", "###", "...", "..."),
            '#' to listOf(".#.", "###", ".#.", "###", ".#."),
        )
}
