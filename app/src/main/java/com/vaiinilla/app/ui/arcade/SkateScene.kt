package com.vaiinilla.app.ui.arcade

import com.vaiinilla.app.arcade.ArcadeRandom
import com.vaiinilla.app.arcade.SkateEvent
import com.vaiinilla.app.arcade.SkateGame
import com.vaiinilla.app.arcade.SkateMissions
import com.vaiinilla.app.arcade.SkateObstacle
import com.vaiinilla.app.arcade.SkateObstacleKind
import com.vaiinilla.app.arcade.SkateRun
import com.vaiinilla.app.arcade.clamp01
import com.vaiinilla.app.arcade.jsRound
import com.vaiinilla.app.arcade.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Vaini Skate: a run through the city from golden sunset into night, with tricks, stars, challenges and lives.
 * The scene keeps its own sparks and popups, fed by the street events the rules raise.
 */
internal class SkateScene {
    private class Spark(
        var x: Double,
        var y: Double,
        val vx: Double,
        var vy: Double,
        var life: Double,
        val color: Int,
        val isStar: Boolean = false,
    )

    private class Popup(
        val text: String,
        var life: Double,
    )

    private val sparks = mutableListOf<Spark>()
    private val popups = mutableListOf<Popup>()
    private var rand = ArcadeRandom(7)
    private var lastStartCount = 0

    // sparks and popups

    private fun absorb(events: List<SkateEvent>) {
        for (e in events) {
            when (e) {
                is SkateEvent.Star ->
                    for (i in 0 until 8) {
                        val a = i / 8.0 * 2 * PI
                        sparks += Spark(e.x, e.y, cos(a) * 50, sin(a) * 50, 0.4, 0xFFE45C)
                    }
                is SkateEvent.Land ->
                    for (i in 0 until 6) {
                        sparks +=
                            Spark(
                                SkateRun.PLAYER_X - 12 + i * 5,
                                e.playerY,
                                (i - 2.5) * 12,
                                -20 - rand.next() * 20,
                                0.3,
                                0xFFE8D0,
                            )
                    }
                is SkateEvent.GrindSpark -> {
                    val vx = -60 - rand.next() * 60
                    val vy = -40 - rand.next() * 60
                    val color = if (rand.next() > 0.5) 0xFFE45C else 0xFFFFFF
                    sparks += Spark(SkateRun.PLAYER_X - 8, e.playerY + 2, vx, vy, 0.25, color)
                }
                is SkateEvent.Hit ->
                    for (i in 0 until 5) {
                        sparks +=
                            Spark(
                                SkateRun.PLAYER_X,
                                e.playerY - 40,
                                (i - 2) * 30.0,
                                -60.0,
                                0.6,
                                0xFFD36E,
                                isStar = true,
                            )
                    }
                is SkateEvent.Points -> popups += Popup(e.text, 1.1)
                is SkateEvent.Bail -> popups += Popup(e.text, 1.1)
                else -> Unit
            }
        }
    }

    private fun drawEffects(
        cv: PixelCanvas,
        dt: Double,
    ) {
        for (p in sparks) {
            p.life -= dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += 200 * dt
            if (p.isStar) star(cv, p.x, p.y, p.life * 4) else cv.px(p.x, p.y, p.color, min(1.0, p.life * 3))
        }
        sparks.removeAll { it.life <= 0 }
        for ((i, p) in popups.withIndex()) {
            p.life -= dt
            val y = 150 - i * 9 - (1.1 - p.life) * 12
            val w = PixelFont.width(p.text)
            cv.text(p.text, SkateRun.PLAYER_X - w / 2 + 1, y + 1, 0x2A1A30, min(1.0, p.life * 2))
            cv.text(p.text, SkateRun.PLAYER_X - w / 2, y, 0xFFF6C8, min(1.0, p.life * 2))
        }
        popups.removeAll { it.life <= 0 }
        while (popups.size > 4) popups.removeAt(0)
    }

    /** Draws the whole street scene, its effects and, depending on the screen, the HUD, title or game-over panel. */
    fun draw(
        cv: PixelCanvas,
        game: SkateGame,
        info: ArcadeFrameInfo,
    ) {
        if (game.startCount != lastStartCount) {
            lastStartCount = game.startCount
            sparks.clear()
            popups.clear()
            rand = ArcadeRandom(7)
        }
        absorb(game.drainSkateEvents())
        val run = game.run
        sky(cv, run)
        farSkyline(cv, run)
        midBuildings(cv, run)
        street(cv, run)
        lamps(cv, run)
        for (o in run.obstacles) obstacle(cv, run, o)
        for (s in run.stars) star(cv, run.screenX(s.x), s.y, run.time)
        player(cv, run)
        drawEffects(cv, info.dt)
        game.toast?.let { cv.cornerToast(it.text, 2 - it.life, 0x7FFF9A, 44.0) }
        when (info.screen) {
            ArcadeScreen.PLAYING -> drawHud(cv, game)
            ArcadeScreen.TITLE -> drawTitle(cv, game, info.screenTime)
            ArcadeScreen.OVER -> game.result?.let { drawGameOver(cv, game, it, info.screenTime) }
        }
    }

    // scene layers

    private fun nightness(run: SkateRun): Double = clamp01(run.distance / 16000)

    private fun tileOffset(
        run: SkateRun,
        parallax: Double,
        p: Double,
    ): Double = ((run.distance * parallax) % p + p) % p

    private fun sky(
        cv: PixelCanvas,
        run: SkateRun,
    ) {
        val n = nightness(run)
        val c = IntArray(4) { mixColor(SUNSET[it], NIGHT[it], n) }
        cv.gradient(listOf(0 to c[0], 60 to c[1], 120 to c[2], 200 to c[3]))
        val sunY = lerp(108.0, 200.0, n)
        val sun = mixColor(0xFFD27A, 0xFF8A5A, n)
        var y = sunY - 42
        while (y < sunY + 42) {
            for (x in 333 until 417) {
                val inside = hypot(x - 375.0, y - sunY) < 42
                val band = y > sunY - 6 && (y - sunY + 6) % 6 < 2
                if (inside && !band) cv.px(x.toDouble(), y, sun)
            }
            y += 1
        }
        if (n > 0.4) {
            for (i in 0 until 70) {
                val fi = i.toDouble()
                val x = arcadeHash(fi, 11.0) * cv.width
                val yy = arcadeHash(fi, 12.0) * 120
                val tw = 0.5 + 0.5 * sin(run.time * 3 + fi)
                cv.px(x, yy, 0xFFFFFF, (n - 0.4) * 1.6 * tw)
            }
        }
    }

    private fun farSkyline(
        cv: PixelCanvas,
        run: SkateRun,
    ) {
        val p = 120.0
        val s = tileOffset(run, 0.15, p)
        val col = mixColor(0x6A3A78, 0x221A44, nightness(run))
        val heights = doubleArrayOf(52.0, 70.0, 44.0, 88.0, 60.0, 36.0)
        for (x in 0 until cv.width) {
            val u = (x + s) % p
            val h = heights[floor(u / 20).toInt()]
            var y = 176 - h
            while (y < 176) {
                cv.plot(x, y.toInt(), col, 1.0)
                y += 1
            }
            if (u % 20 == 10.0 && h > 60 && sin(run.time * 4) > 0) cv.px(x.toDouble(), 176 - h - 3, 0xFF5A6A)
        }
    }

    private fun midBuildings(
        cv: PixelCanvas,
        run: SkateRun,
    ) {
        val p = 240.0
        val s = tileOffset(run, 0.4, p)
        val n = nightness(run)
        val blocks =
            listOf(
                doubleArrayOf(0.0, 60.0, 86.0) to 0x8A4A6A,
                doubleArrayOf(62.0, 48.0, 110.0) to 0xA85A5A,
                doubleArrayOf(112.0, 70.0, 74.0) to 0x7A4A7A,
                doubleArrayOf(184.0, 54.0, 98.0) to 0x9A5060,
            )
        val unlit = mixColor(0x5A3050, 0x1A1430, n)
        for (rep in -1..2) {
            for ((b, col) in blocks) {
                val bx = b[0]
                val bw = b[1]
                val bh = b[2]
                val x0 = jsRound(bx + rep * p - s).toDouble()
                if (x0 > cv.width || x0 + bw < 0) continue
                cv.rect(x0, 196 - bh, bw, bh, mixColor(col, 0x2A2048, n * 0.8))
                var wy = 196 - bh + 8
                while (wy < 186) {
                    var wx = x0 + 5
                    while (wx < x0 + bw - 6) {
                        cv.rect(
                            wx,
                            wy,
                            5.0,
                            6.0,
                            if (arcadeHash(wx - x0 + bx, wy) >
                                0.55 - n * 0.25
                            ) {
                                0xFFD98A
                            } else {
                                unlit
                            },
                        )
                        wx += 9
                    }
                    wy += 10
                }
            }
        }
        for (rep in -1..2) {
            val x0 = jsRound(20 + rep * p - s).toDouble()
            cv.rect(x0, 170.0, 90.0, 26.0, 0x5A5A6A)
            cv.text("VAINI", x0 + 20, 176.0, 0x6BF0C8)
            cv.text("SKATE", x0 + 44, 184.0, 0xFF7AB8)
            cv.stroke(x0 + 8, 190.0, x0 + 18, 174.0, 1.2, 0xFFD36E)
            cv.disc(x0 + 76, 180.0, 5.0, 0x7AB8FF, 0.9)
        }
    }

    private fun street(
        cv: PixelCanvas,
        run: SkateRun,
    ) {
        val s = tileOffset(run, 1.0, 60.0)
        val n = nightness(run)
        val w = cv.width.toDouble()
        val h = cv.height.toDouble()
        cv.rect(0.0, 196.0, w, 30.0, mixColor(0x7A6A78, 0x3A3448, n))
        for (x in 0 until cv.width) if ((x + s) % 60 < 1) cv.rect(x.toDouble(), 196.0, 1.0, 30.0, 0x4A4058)
        cv.rect(0.0, 196.0, w, 2.0, 0xB09AA8)
        cv.rect(0.0, SkateRun.GROUND, w, 4.0, 0xC8B0B8)
        cv.rect(0.0, SkateRun.GROUND + 4, w, h - SkateRun.GROUND - 4, mixColor(0x3A3040, 0x1A1624, n))
        val s2 = tileOffset(run, 1.25, 80.0)
        for (x in 0 until cv.width) if ((x + s2) % 80 < 36) cv.px(x.toDouble(), 250.0, 0xF2E28A)
    }

    private fun lamps(
        cv: PixelCanvas,
        run: SkateRun,
    ) {
        val p = 160.0
        val s = tileOffset(run, 0.85, p)
        val n = nightness(run)
        for (rep in -1..3) {
            val x = jsRound(60 + rep * p - s).toDouble()
            if (x < -30 || x > cv.width + 30) continue
            cv.rect(x, 132.0, 3.0, 94.0, 0x2A2030)
            cv.stroke(x + 1, 134.0, x + 14, 130.0, 1.0, 0x2A2030)
            cv.rect(x + 10, 128.0, 10.0, 4.0, 0x2A2030)
            cv.rect(x + 11, 131.0, 8.0, 2.0, 0xFFF0B8)
            cv.glow(x + 15, 150.0, 40 + n * 20, 0xFFD98A, 0.2 + n * 0.25, 1.6)
        }
    }

    private fun obstacle(
        cv: PixelCanvas,
        run: SkateRun,
        o: SkateObstacle,
    ) {
        val k = o.kind
        val x = jsRound(run.screenX(o.x)).toDouble()
        val ground = SkateRun.GROUND
        val top = ground - k.height
        if (x < -60 || x > cv.width + 60) return
        cv.ellipse(x, ground + 1, k.width / 2 + 2, 2.0, 0x1A1624, 0.4)
        when (k) {
            SkateObstacleKind.CONE -> {
                cv.poly(doubleArrayOf(x - 6, ground, x + 6, ground, x + 2, top, x - 2, top), 0xFF7A2A)
                cv.rect(x - 4, ground - 10, 8.0, 2.0, 0xFFFFFF)
                cv.rect(x - 8, ground - 2, 16.0, 2.0, 0xE8602A)
            }
            SkateObstacleKind.BIN -> {
                cv.rect(x - 8, top + 4, 16.0, k.height - 4, 0x5A7A6A)
                cv.rect(x - 9, top, 18.0, 4.0, 0x7A9A8A)
                for (i in intArrayOf(-5, 0, 5)) cv.rect(x + i, top + 7, 1.0, k.height - 10, 0x4A6A5A)
            }
            SkateObstacleKind.BENCH -> {
                cv.rect(x - 22, top, 44.0, 3.0, 0xC98B4E)
                cv.rect(x - 22, top + 4, 44.0, 3.0, 0xB07A42)
                cv.rect(x - 19, top + 7, 3.0, k.height - 7, 0x3A3040)
                cv.rect(x + 16, top + 7, 3.0, k.height - 7, 0x3A3040)
            }
            SkateObstacleKind.RAIL -> {
                cv.rect(x - 48, top, 96.0, 3.0, 0xD8DCE4)
                cv.rect(x - 48, top, 96.0, 1.0, 0xFFFFFF)
                for (px0 in doubleArrayOf(x - 44, x, x + 42)) cv.rect(px0, top + 3, 3.0, k.height - 3, 0x8A90A0)
            }
        }
    }

    private fun star(
        cv: PixelCanvas,
        x: Double,
        y: Double,
        t: Double,
    ) {
        val r = 4 + sin(t * 6) * 0.6
        val pts = DoubleArray(20)
        for (i in 0 until 10) {
            val a = -PI / 2 + i * PI / 5 + t * 1.5
            val rr = if (i % 2 != 0) r * 0.45 else r
            pts[i * 2] = x + cos(a) * rr
            pts[i * 2 + 1] = y + sin(a) * rr
        }
        cv.poly(pts, 0xFFE45C)
        cv.px(x - 1, y - 1, 0xFFFFFF)
    }

    private fun player(
        cv: PixelCanvas,
        run: SkateRun,
    ) {
        val p = run.player
        if (!run.over && p.hurt > 0 && floor(p.hurt * 12).toInt() % 2 == 0) return // blink while invulnerable
        val y = jsRound(p.y) - 6.0
        val flipU = if (p.flipping) p.flip / SkateRun.FLIP_TIME else 0.0
        val frame = if (p.flipping) floor(flipU * 8).toInt() % 4 else 0
        val deckW = doubleArrayOf(30.0, 16.0, 4.0, 16.0)[frame]
        val tilt =
            if (p.stumble >
                0
            ) {
                0.35
            } else if (!p.grounded && p.grinding == null) {
                clamp01(-p.vy / 400) * 0.25
            } else {
                0.0
            }
        val px = SkateRun.PLAYER_X
        cv.stroke(
            px - deckW / 2,
            y + 4 + tilt * 8,
            px + deckW / 2,
            y + 4 - tilt * 8,
            if (frame ==
                2
            ) {
                2.0
            } else {
                1.4
            },
            if (frame % 2 != 0) 0xFF5AB4 else 0x2A2A3A,
        )
        if (frame == 0) for (wx in doubleArrayOf(px - 10, px + 10)) cv.disc(wx, y + 7, 2.0, 0xF2F2F2)
        val air = !p.grounded && p.grinding == null
        val pose = VainiPose()
        pose.armL = if (air) 2.2 else 1.2
        pose.armR = if (air) 2.0 else 1.0 + (if (p.grinding != null) 0.8 else 0.0)
        pose.blink = p.stumble > 0
        pose.blush = if (air || p.grinding != null) 1.0 else 0.2
        pose.lookX = 1.0
        pose.bob = if (p.grounded) jsRound(sin(run.time * 10) * 0.6).toDouble() else 0.0
        VainiSprite.draw(cv, px, y, pose)
        if (p.grinding ==
            null
        ) {
            cv.ellipse(px, SkateRun.GROUND + 1, max(6.0, 16 - (SkateRun.GROUND - p.y) * 0.15), 2.0, 0x1A1624, 0.45)
        }
    }

    // HUD and screens

    private fun heart(
        cv: PixelCanvas,
        x: Double,
        y: Double,
        full: Boolean,
    ) {
        val c = if (full) 0xFF5A7A else 0x5A4A60
        cv.rect(x, y + 1, 7.0, 3.0, c)
        cv.rect(x + 1, y, 2.0, 1.0, c)
        cv.rect(x + 4, y, 2.0, 1.0, c)
        cv.rect(x + 1, y + 4, 5.0, 1.0, c)
        cv.rect(x + 2, y + 5, 3.0, 1.0, c)
        cv.px(x + 3, y + 6, c)
    }

    private fun boxPanel(
        cv: PixelCanvas,
        x: Double,
        y: Double,
        w: Double,
        h: Double,
    ) {
        cv.rect(x, y, w, h, 0x1A1224, 0.72)
        cv.rect(x, y, w, 1.0, 0xFFFFFF, 0.25)
    }

    private fun missionsBox(
        cv: PixelCanvas,
        game: SkateGame,
        x: Double,
        y: Double,
    ) {
        boxPanel(cv, x, y, 150.0, 8.0 + game.missions.size * 9)
        for ((i, m) in game.missions.withIndex()) {
            val (value, doneNow) = SkateMissions.progress(m, game.run)
            val done = doneNow || game.progress.done.contains(m.id)
            val col = if (done) 0x7FFF9A else 0xE8E2F0
            val ry = y + 4 + i * 9
            cv.text(if (done) "+" else "-", x + 4, ry, col)
            cv.text(m.label, x + 10, ry, col)
            if (!done) {
                val progress = "$value/${m.target}"
                cv.text(progress, x + 146 - PixelFont.width(progress), ry, 0xFFD36E)
            }
        }
    }

    private fun drawHud(
        cv: PixelCanvas,
        game: SkateGame,
    ) {
        val run = game.run
        boxPanel(cv, 4.0, 4.0, 96.0, 22.0)
        cv.text("${run.score}", 8.0, 8.0, 0xFFFFFF, 1.0, 2.0)
        cv.text("${run.meters} M", 70.0, 8.0, 0xFFD36E)
        for (i in 0 until 3) heart(cv, 70.0 + i * 9, 16.0, i < run.lives)
        if (run.combo >= 2) cv.text("COMBO ${run.combo}", 8.0, 30.0, 0xFF7AB8)
        missionsBox(cv, game, cv.width - 154.0, 4.0)
    }

    private fun drawTitle(
        cv: PixelCanvas,
        game: SkateGame,
        t: Double,
    ) {
        val w = cv.width.toDouble()
        boxPanel(cv, 90.0, 40.0, 300.0, 150.0)
        val title = "VAINI SKATE"
        cv.text(title, w / 2 - PixelFont.width(title, 4.0) / 2 + 2, 52.0 + 2, 0x2A1A30, 1.0, 4.0)
        cv.text(title, w / 2 - PixelFont.width(title, 4.0) / 2, 52.0, 0xFFF6C8, 1.0, 4.0)
        cv.text("NIVEL ${game.progress.level + 1}   RÉCORD ${game.progress.best}", 150.0, 82.0, 0xFFD36E)
        cv.text("RETOS DE ESTE NIVEL", 172.0, 98.0, 0xFF7AB8)
        for ((i, m) in game.missions.withIndex()) {
            val done = game.progress.done.contains(m.id)
            cv.text("${if (done) "+" else "-"} ${m.label}", 150.0, 110.0 + i * 9, if (done) 0x7FFF9A else 0xE8E2F0)
        }
        val help = listOf("SALTAR: TOCA. MANTÉN PARA SUBIR MÁS", "OTRA VEZ EN EL AIRE: KICKFLIP. BAJAR: CAER")
        for ((i, h) in help.withIndex()) cv.text(h, w / 2 - PixelFont.width(h) / 2, 146.0 + i * 10, 0xFFFFFF, 0.8)
        val start = "TOCA PARA EMPEZAR"
        if (sin(t * 5) > -0.3) cv.text(start, w / 2 - PixelFont.width(start, 2.0) / 2, 170.0, 0x7FFF9A, 1.0, 2.0)
    }

    private fun drawGameOver(
        cv: PixelCanvas,
        game: SkateGame,
        result: SkateGame.Result,
        t: Double,
    ) {
        val w = cv.width.toDouble()
        val run = game.run
        boxPanel(cv, 110.0, 44.0, 260.0, 140.0)
        val title = "FIN DE LA RONDA"
        cv.text(title, w / 2 - PixelFont.width(title, 3.0) / 2, 54.0, 0xFF7AB8, 1.0, 3.0)
        cv.text("PUNTOS ${run.score}", 150.0, 80.0, 0xFFFFFF, 1.0, 2.0)
        cv.text("${run.meters} M   COMBO ${run.bestCombo}   ${run.stats.stars} ESTRELLAS", 150.0, 96.0, 0xFFD36E)
        if (result.newBest) cv.text("¡NUEVO RÉCORD!", 150.0, 106.0, 0x7FFF9A)
        if (result.levelUp) {
            cv.text(
                "¡RETOS COMPLETOS! SUBES A NIVEL ${result.snapshot.level + 1}",
                130.0,
                116.0,
                0x7FFF9A,
            )
        }
        for ((i, m) in result.missions.withIndex()) {
            val d = result.snapshot.done.contains(m.id)
            cv.text("${if (d) "+" else "-"} ${m.label}", 150.0, 130.0 + i * 9, if (d) 0x7FFF9A else 0xE8E2F0)
        }
        val again = "TOCA PARA REINTENTAR"
        if (sin(t * 5) > -0.3 &&
            t > 0.8
        ) {
            cv.text(again, w / 2 - PixelFont.width(again, 2.0) / 2, 166.0, 0xFFFFFF, 1.0, 2.0)
        }
    }

    companion object {
        // Sky goes from golden sunset to night as the run gets longer.
        private val SUNSET = intArrayOf(0x3A2A6A, 0xC2507A, 0xF28A4A, 0xFFC86A)
        private val NIGHT = intArrayOf(0x0A0C24, 0x1C1F4A, 0x3A2A6A, 0x5A3A78)
    }
}
