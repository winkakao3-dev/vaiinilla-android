package com.vaiinilla.app

import com.vaiinilla.app.domain.arcade.ArcadeGame
import com.vaiinilla.app.domain.arcade.ArcadeKind
import com.vaiinilla.app.domain.arcade.FlappyGame
import com.vaiinilla.app.domain.arcade.GalaxyGame
import com.vaiinilla.app.domain.arcade.GravityGame
import com.vaiinilla.app.domain.arcade.SkateGame
import com.vaiinilla.app.domain.arcade.StackGame
import com.vaiinilla.app.ui.arcade.ArcadeFrameInfo
import com.vaiinilla.app.ui.arcade.ArcadeRenderer
import com.vaiinilla.app.ui.arcade.ArcadeScreen
import com.vaiinilla.app.ui.arcade.PixelCanvas
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.Inflater

/**
 * Replays the input scripts recorded from the web prototype and compares the rendered frame with the prototype's own
 * frame. The scenes are ports, so they must look the same; a handful of pixels may differ by libm rounding.
 */
class ArcadeReferenceTest {
    private class Op(
        val step: Int,
        val name: String,
        val value: Double?,
    )

    private fun resource(path: String) =
        checkNotNull(javaClass.classLoader?.getResourceAsStream("arcade-reference/$path")) {
            path
        }

    private fun kind(name: String) =
        when (name) {
            "flappy" -> ArcadeKind.FLAPPY
            "skate" -> ArcadeKind.SKATE
            "apila" -> ArcadeKind.APILA
            "gravedad" -> ArcadeKind.GRAVEDAD
            else -> ArcadeKind.GALAXIA
        }

    private fun makeGame(
        name: String,
        seed: Int,
    ): ArcadeGame =
        when (name) {
            "flappy" -> FlappyGame(seed)
            "skate" -> SkateGame(seed)
            "apila" -> StackGame(seed)
            "gravedad" -> GravityGame(seed)
            else -> GalaxyGame(seed)
        }

    private fun screen(name: String?) =
        when (name) {
            "playing" -> ArcadeScreen.PLAYING
            "over" -> ArcadeScreen.OVER
            else -> ArcadeScreen.TITLE
        }

    /** Decodes the reference PNGs (8-bit RGBA, no filtering, as the prototype writes them) into RGBA bytes. */
    private fun decodePng(bytes: ByteArray): ByteArray {
        val buf = ByteBuffer.wrap(bytes)
        buf.position(8)
        val idat = ByteArrayOutputStream()
        var width = 0
        var height = 0
        while (buf.remaining() > 12) {
            val len = buf.int
            val type = ByteArray(4).also { buf.get(it) }.decodeToString()
            val data = ByteArray(len).also { buf.get(it) }
            buf.int // crc
            when (type) {
                "IHDR" -> {
                    val h = ByteBuffer.wrap(data)
                    width = h.int
                    height = h.int
                }
                "IDAT" -> idat.write(data)
            }
        }
        val inflater = Inflater().apply { setInput(idat.toByteArray()) }
        val raw = ByteArray((width * 4 + 1) * height)
        var off = 0
        while (off < raw.size && !inflater.finished()) off += inflater.inflate(raw, off, raw.size - off)
        val out = ByteArray(width * height * 4)
        for (y in 0 until height) {
            check(raw[y * (width * 4 + 1)].toInt() == 0) { "unexpected PNG filter" }
            System.arraycopy(raw, y * (width * 4 + 1) + 1, out, y * width * 4, width * 4)
        }
        return out
    }

    /** Fraction of pixels whose colour differs by more than a few levels. */
    private fun mismatch(
        canvas: PixelCanvas,
        id: String,
    ): Double {
        val ref = decodePng(resource("$id.png").readBytes())
        var bad = 0
        for (i in 0 until canvas.width * canvas.height) {
            val p = canvas.pixels[i]
            val dr = kotlin.math.abs(((p shr 16) and 255) - (ref[i * 4].toInt() and 255))
            val dg = kotlin.math.abs(((p shr 8) and 255) - (ref[i * 4 + 1].toInt() and 255))
            val db = kotlin.math.abs((p and 255) - (ref[i * 4 + 2].toInt() and 255))
            if (maxOf(dr, dg, db) > 3) bad += 1
        }
        return bad.toDouble() / (canvas.width * canvas.height)
    }

    @Test
    fun `every scenario matches the prototype frame`() {
        val only = System.getenv("ARCADE_ONLY")
        val scenarios = Json.parseToJsonElement(resource("scenarios.json").readBytes().decodeToString()).jsonArray
        val report = mutableListOf<String>()
        for (element in scenarios) {
            val d: JsonObject = element.jsonObject
            val id = d["id"]!!.jsonPrimitive.content
            if (only != null && !id.startsWith(only)) continue
            val game = d["game"]!!.jsonPrimitive.content
            val seed = d["seed"]!!.jsonPrimitive.int
            val steps = d["steps"]!!.jsonPrimitive.int
            val drawEvery = d["drawEvery"]?.jsonPrimitive?.int ?: 0
            val ui = d["ui"]!!.jsonObject
            val ops =
                (d["ops"] as JsonArray).map {
                    val a = it.jsonArray
                    Op(
                        a[0].jsonPrimitive.int,
                        a[1].jsonPrimitive.content,
                        if (a.size >
                            2
                        ) {
                            a[2].jsonPrimitive.double
                        } else {
                            null
                        },
                    )
                }
            val opsByStep = ops.groupBy { it.step }
            val instance = makeGame(game, seed)
            val renderer = ArcadeRenderer()
            val dt = 1.0 / 120.0
            for (step in 0 until steps) {
                for (op in opsByStep[step].orEmpty()) {
                    when (op.name) {
                        "tap" -> instance.tap()
                        "release" -> instance.release()
                        "aim" -> instance.aim(op.value ?: 240.0)
                    }
                }
                instance.step(dt)
                instance.drainEvents()
                // Skate's particles only age while drawing; the last two seconds are enough to reproduce them
                if (drawEvery > 0 && step % drawEvery == 1 && step >= steps - 240) {
                    val s =
                        if (screen(ui["screen"]?.jsonPrimitive?.content) ==
                            ArcadeScreen.TITLE
                        ) {
                            ArcadeScreen.TITLE
                        } else {
                            ArcadeScreen.PLAYING
                        }
                    renderer.render(kind(game), instance, null, ArcadeFrameInfo(s, 0.0, 0, false, 1.0 / 60.0))
                }
            }
            renderer.render(
                kind(game),
                instance,
                null,
                ArcadeFrameInfo(
                    screen(ui["screen"]?.jsonPrimitive?.content),
                    ui["screenTime"]?.jsonPrimitive?.double ?: 0.0,
                    ui["best"]?.jsonPrimitive?.int ?: 0,
                    ui["newBest"]?.jsonPrimitive?.boolean ?: false,
                    1.0 / 60.0,
                ),
            )
            val bad = mismatch(renderer.canvas, id)
            report += "%s: %.3f%%".format(id, bad * 100)
            assertTrue("$id differs from the prototype frame in ${bad * 100}% of its pixels", bad < 0.005)
        }
        println("ARCADE REFERENCE\n" + report.joinToString("\n"))
    }
}
