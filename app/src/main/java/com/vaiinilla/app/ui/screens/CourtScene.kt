package com.vaiinilla.app.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.vaiinilla.app.ui.order.courtLights
import com.vaiinilla.app.ui.order.courtMinutesLeft
import java.time.Instant
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// Una renta en el Vainiverso es la cancha vista desde arriba, en pixel art como el plano de mesas
// (`FloorPlanScene`), igual que `CourtScene.swift` en iOS. Las líneas se van pintando con el tiempo jugado, los
// reflectores bajan en los últimos minutos (`courtLights`) y se apagan al terminar. Mientras se juega hay peloteo, el
// marcador cuenta los minutos que quedan y, de noche, las palomillas rondan los reflectores.

/** La cancha rentada: su nombre y su horario. */
internal data class CourtRental(
    val name: String,
    val start: Instant,
    val end: Instant,
)

/** La cuadrícula de la cancha, sobre la misma de 96 × 96 del plano de mesas. */
internal object CourtLayout {
    val court = GridRect(30, 12, 36, 72)
    const val NET_Y = 47
    val lamps = listOf(GridCell(25, 7), GridCell(68, 7), GridCell(25, 86), GridCell(68, 86))
    val vainiCell = GridCell(3, 52)
    val board = GridRect(72, 30, 20, 20)

    /** El centro de la cancha, a donde mira la cámara. */
    fun center(stage: Size): Offset =
        FloorPlanLayout.point((court.x + court.width / 2).toFloat(), (NET_Y + 1).toFloat(), stage)

    /** Las líneas en el orden en que se pintan: el perímetro, las de saque y la central. */
    val linePixels: List<GridCell> by lazy {
        val x0 = court.x + 1
        val x1 = court.x + court.width - 2
        val y0 = court.y + 1
        val y1 = court.y + court.height - 2
        buildList {
            for (y in y1 downTo y0) add(GridCell(x0, y))
            for (x in x0 + 1..x1) add(GridCell(x, y0))
            for (y in y0 + 1..y1) add(GridCell(x1, y))
            for (x in x1 - 1 downTo x0 + 1) add(GridCell(x, y1))
            val serviceTop = NET_Y - 17
            val serviceBottom = NET_Y + 18
            for (x in x0 + 1 until x1) add(GridCell(x, serviceTop))
            for (x in x0 + 1 until x1) add(GridCell(x, serviceBottom))
            val mid = court.x + court.width / 2
            for (y in serviceTop + 1 until serviceBottom) if (y != NET_Y && y != NET_Y + 1) add(GridCell(mid, y))
        }
    }
}

private fun lineCount(fraction: Double): Int = floor(fraction.coerceIn(0.0, 1.0) * CourtLayout.linePixels.size).toInt()

/**
 * La cancha a tamaño de pantalla, a la hora [now]. Con [tick] = null queda quieta, sin jugadores ni Vaini: es la
 * cancha que muestrea el mosaico del QR. [lights] reemplaza la luz de los reflectores, para que baje con un fundido.
 */
internal fun DrawScope.drawCourt(
    stage: Size,
    palette: ScenePalette,
    court: CourtRental,
    now: Instant,
    tick: Int? = null,
    lights: Float? = null,
) {
    val art = PixelPalette.of(palette)
    val state = courtLights(court.start, court.end, now)
    val frame = FloorPlanLayout.frame(stage)
    drawRect(palette.sidewalk, size = stage)
    drawPixels(CourtArt.base(art, court.name), frame)
    CourtArt.lines(art, lineCount(state.lines))?.let { drawPixels(it, frame) }
    val light = lights ?: state.lights.toFloat()
    if (tick == null) {
        if (light > 0.001f) drawPixels(CourtArt.light(art), frame, light)
        return
    }
    val playing = !now.isBefore(court.start) && now.isBefore(court.end)
    val board = CourtLayout.board
    drawPixels(
        CourtArt.board(art, if (playing) courtMinutesLeft(court.end, now) else null),
        cells(board.x, board.y, board.width, board.height, stage),
    )
    if (playing) drawRally(tick, stage)
    // Los reflectores: la luz cálida encima de todo lo que está en la cancha.
    if (light > 0.001f) drawPixels(CourtArt.light(art), frame, light)
    if (art.night && light > 0.5f && tick > 0) drawMoths(tick, stage)
    val pose =
        when {
            !now.isBefore(court.end) -> if (tick % 2 == 0) PixelVainiPose.HAPPY else PixelVainiPose.WAVE
            tick % 14 == 13 -> PixelVainiPose.BLINK
            else -> PixelVainiPose.REST
        }
    val vaini = CourtLayout.vainiCell
    drawPixels(
        FloorPlanArt.standingVaini(pose),
        cells(vaini.x, vaini.y, FloorPlanArt.VAINI_WIDTH, FloorPlanArt.STANDING_VAINI_HEIGHT, stage),
    )
}

private fun cells(
    x: Int,
    y: Int,
    w: Int,
    h: Int,
    stage: Size,
): Rect {
    val at = FloorPlanLayout.point(x.toFloat(), y.toFloat(), stage)
    val p = FloorPlanLayout.pixel(stage)
    return Rect(at, Size(w * p, h * p))
}

private val ShirtLavender = Color(0.73f, 0.65f, 0.91f)
private val ShirtOrange = Color(0.95f, 0.71f, 0.35f)
private val Hair = Color(0.23f, 0.16f, 0.13f)
private val Ball = Color(0.91f, 0.94f, 0.38f)
private val Moth = Color(1f, 0.96f, 0.85f, 0.85f)

/**
 * Dos jugadores (vistos desde arriba: la playera y el pelo) y la pelota que va y viene sobre la red, con su sombra
 * en el piso. Un peloteo dura 4 s y cada uno cae en otro lado de la cancha.
 */
private fun DrawScope.drawRally(
    tick: Int,
    stage: Size,
) {
    val p = FloorPlanLayout.pixel(stage)
    val rally = tick / 16
    val phase = tick % 16
    val bottomX = listOf(38, 52, 44, 58)[rally % 4]
    val topX = listOf(56, 40, 50, 36)[rally % 4]
    val up = phase < 8
    val u = (phase % 8) / 8f
    val from = if (up) GridCell(bottomX, 76) else GridCell(topX, 19)
    val to = if (up) GridCell(topX, 19) else GridCell(bottomX, 76)
    val bx = (from.x + (to.x - from.x) * u).roundToInt().toFloat()
    val by = (from.y + (to.y - from.y) * u).roundToInt().toFloat()
    val lift = (5f * sin(PI.toFloat() * u)).roundToInt().toFloat()

    fun player(
        x: Int,
        y: Int,
        shirt: Color,
    ) {
        val at = FloorPlanLayout.point((x - 2).toFloat(), (y - 2).toFloat(), stage)
        drawRect(shirt, topLeft = at + Offset(0f, p), size = Size(4 * p, 2 * p))
        drawRect(shirt, topLeft = at + Offset(p, 0f), size = Size(2 * p, 4 * p))
        drawRect(Hair, topLeft = at + Offset(p, p), size = Size(2 * p, 2 * p))
    }

    fun pixel(
        x: Float,
        y: Float,
        color: Color,
    ) {
        drawRect(color, topLeft = FloorPlanLayout.point(x, y, stage), size = Size(p, p))
    }
    player(bottomX, 77, ShirtLavender)
    player(topX, 16, ShirtOrange)
    pixel(bx, by + 1, Color.Black.copy(alpha = 0.28f))
    pixel(bx, by - lift, Ball)
}

/** De noche, dos palomillas rondan cada reflector prendido. */
private fun DrawScope.drawMoths(
    tick: Int,
    stage: Size,
) {
    val p = FloorPlanLayout.pixel(stage)
    val orbit = listOf(-2 to -1, 0 to -3, 3 to -2, 4 to 0, 2 to 3, -1 to 2)
    for (index in 0 until CourtLayout.lamps.size * 2) {
        val lamp = CourtLayout.lamps[index / 2]
        val (dx, dy) = orbit[(tick + index * 3) % orbit.size]
        val at = FloorPlanLayout.point((lamp.x + 1 + dx).toFloat(), (lamp.y + dy).toFloat(), stage)
        drawRect(Moth, topLeft = at, size = Size(p, p))
    }
}

// El dibujo.

private object CourtArt {
    private val lineLock = Any()
    private var lastLines: Pair<String, ImageBitmap?>? = null
    private val images = HashMap<String, ImageBitmap>()

    private fun cached(
        key: String,
        width: Int,
        height: Int,
        paint: PlanPixels.() -> Unit,
    ): ImageBitmap = synchronized(images) { images.getOrPut(key) { PlanPixels(width, height).apply(paint).image() } }

    private fun rgb(
        r: Float,
        g: Float,
        b: Float,
        a: Float = 1f,
    ) = Color(r, g, b, a)

    private fun Color.dim(t: Float) = Color(red * (1 - t), green * (1 - t), blue * (1 - t), alpha)

    private class Surface(
        val court: Int,
        val courtDark: Int,
        val grass: Int,
        val grassDark: Int,
    )

    private fun surface(c: PixelPalette): Surface {
        val court = rgb(0.18f, 0.5f, 0.85f).dim(if (c.night) 0.5f else 0f)
        val grass = rgb(0.49f, 0.69f, 0.29f).dim(if (c.night) 0.62f else 0f)
        return Surface(court.packed(), court.dim(0.12f).packed(), grass.packed(), grass.dim(0.12f).packed())
    }

    private fun Color.packed(): Int = android.graphics.Color.argb(alpha, red, green, blue)

    /** Todo lo que no cambia: el pasto, la cancha con su cerca y su red, los postes, la banca y el letrero. */
    fun base(
        c: PixelPalette,
        name: String,
    ): ImageBitmap =
        cached("court-${c.key}-$name", FloorPlanLayout.COLUMNS, FloorPlanLayout.ROWS) {
            val s = surface(c)
            val w = FloorPlanLayout.COLUMNS
            val h = FloorPlanLayout.ROWS
            for (y in 0 until h) {
                for (x in 0 until w) {
                    this[x, y] =
                        if ((x * 3 + y * 7) % 11 == 0 || (x + y * 5) % 17 == 0) s.grassDark else s.grass
                }
            }
            val court = CourtLayout.court
            // La banqueta de concreto y la cerca con sus postes.
            rect(court.x - 3, court.y - 3, court.width + 6, court.height + 6, c.outside)
            rect(court.x - 1, court.y - 1, court.width + 2, court.height + 2, c.outline)
            for (y in court.y - 1 until court.y + court.height + 1 step 6) {
                this[court.x - 1, y] = c.wallLight
                this[court.x + court.width, y] = c.wallLight
            }
            for (y in court.y until court.y + court.height) {
                for (x in court.x until court.x + court.width) {
                    this[x, y] = if ((x + y) % 2 == 0 && (y - court.y) % 9 == 4) s.courtDark else s.court
                }
            }
            // La red: la cinta blanca y la malla tramada, con sus postes.
            val net = CourtLayout.NET_Y
            rect(court.x - 2, net, court.width + 4, 1, c.tagText)
            for (x in court.x - 1..court.x + court.width) this[x, net + 1] = if (x % 2 == 0) c.outline else s.courtDark
            rect(court.x - 2, net - 1, 2, 3, c.outline)
            rect(court.x + court.width, net - 1, 2, 3, c.outline)
            // Los cuatro reflectores, apagados.
            for (lamp in CourtLayout.lamps) {
                rect(lamp.x, lamp.y, 4, 3, c.outline)
                rect(lamp.x + 1, lamp.y + 1, 2, 1, c.metal)
            }
            // La banca con una maleta encima.
            rect(13, 24, 8, 12, c.outline)
            rect(14, 25, 6, 10, c.wood)
            rect(14, 25, 6, 1, c.woodLight)
            sprite(
                listOf(".oo.", "oMMo", "oMMo", "oooo"),
                15,
                28,
                mapOf('o' to c.outline, 'M' to ShirtLavender.packed()),
            )
            nameSign(this, name, c)
        }

    /** Las líneas pintadas hasta [count]; solo se guarda la última, que cambia con el tiempo jugado. */
    fun lines(
        c: PixelPalette,
        count: Int,
    ): ImageBitmap? {
        if (count <= 0) return null
        val key = "${c.key}-$count"
        synchronized(lineLock) {
            lastLines?.let { (lastKey, image) -> if (lastKey == key) return image }
            val canvas = PlanPixels(FloorPlanLayout.COLUMNS, FloorPlanLayout.ROWS)
            val paint = rgb(0.96f, 0.98f, 1f).dim(if (c.night) 0.35f else 0f).packed()
            val painted = CourtLayout.linePixels.take(count)
            for (cell in painted) canvas[cell.x, cell.y] = paint
            // La punta de la línea, donde va el rodillo, un poco más brillante.
            painted.lastOrNull()?.let { canvas[it.x, it.y] = c.lime }
            val image = canvas.image()
            lastLines = key to image
            return image
        }
    }

    /** La luz de los reflectores: un velo cálido sobre la cancha con el borde tramado y las caras encendidas. */
    fun light(c: PixelPalette): ImageBitmap =
        cached("court-light-${c.key}", FloorPlanLayout.COLUMNS, FloorPlanLayout.ROWS) {
            val court = CourtLayout.court
            val warm = rgb(1f, 0.9f, 0.6f, if (c.night) 0.26f else 0.12f).packed()
            val cx = court.x + court.width / 2.0
            val cy = court.y + court.height / 2.0
            for (y in 0 until FloorPlanLayout.ROWS) {
                for (x in 0 until FloorPlanLayout.COLUMNS) {
                    // Una elipse que cubre la cancha; afuera de la cerca la luz se deshace en tramado.
                    val dx = (x - cx) / (court.width / 2.0 + 9)
                    val dy = (y - cy) / (court.height / 2.0 + 7)
                    val d = dx * dx + dy * dy
                    if (d < 0.8 ||
                        (d < 1 && (x + y) % 2 == 0) ||
                        (d < 1.15 && x % 2 == 0 && y % 2 == 0)
                    ) {
                        this[x, y] = warm
                    }
                }
            }
            for (lamp in CourtLayout.lamps) {
                rect(lamp.x + 1, lamp.y + 1, 2, 1, rgb(1f, 0.98f, 0.85f).packed())
                for ((dx, dy) in listOf(0 to -1, 3 to -1, -1 to 1, 4 to 1, 0 to 3, 3 to 3)) {
                    this[lamp.x + dx, lamp.y + dy] = rgb(1f, 0.94f, 0.7f, 0.55f).packed()
                }
            }
        }

    /** El marcador con los minutos que quedan; sin juego, dos rayitas. */
    fun board(
        c: PixelPalette,
        minutes: Int?,
    ): ImageBitmap {
        val board = CourtLayout.board
        val label = minutes?.let { "%02d".format(min(it, 99)) } ?: "--"
        return cached("court-board-${c.key}-$label", board.width, board.height) {
            rect(0, 0, board.width, board.height, c.outline)
            rect(1, 1, board.width - 2, board.height - 2, c.tag)
            FloorPlanArt.write("MIN", 6, 3, c.tagText, this)
            val amber = rgb(1f, 0.8f, 0.35f).packed()
            label.forEachIndexed {
                i,
                digit,
                ->
                FloorPlanArt.write(digit.toString(), 3 + i * 8, 9, amber, this, scale = 2)
            }
        }
    }

    private fun nameSign(
        canvas: PlanPixels,
        name: String,
        c: PixelPalette,
    ) {
        val text = FloorPlanArt.pixelText(name)
        if (text.isEmpty()) return
        val width = text.length * 4 + 3
        val x = (FloorPlanLayout.COLUMNS - width) / 2
        canvas.rect(x, 1, width, 7, c.tag)
        FloorPlanArt.write(text, x + 2, 2, c.tagText, canvas)
    }
}
