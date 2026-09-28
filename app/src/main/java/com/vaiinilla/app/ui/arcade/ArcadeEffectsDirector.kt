package com.vaiinilla.app.ui.arcade

import com.vaiinilla.app.domain.arcade.ArcadeEvent
import com.vaiinilla.app.domain.arcade.ArcadeGame
import com.vaiinilla.app.domain.arcade.FlappyGame
import com.vaiinilla.app.domain.arcade.GalaxyEnemyKind
import com.vaiinilla.app.domain.arcade.GalaxyGame
import com.vaiinilla.app.domain.arcade.GravityGame
import com.vaiinilla.app.domain.arcade.SkateGame
import com.vaiinilla.app.domain.arcade.StackGame

/** How the player should feel a moment, for the haptic engine to play. */
internal enum class ArcadeHaptic { SOFT, HIT }

/**
 * Turns the events a game raises into sparks, popups and screen shake, and says which ones deserve a haptic.
 * Purely visual: the rules never read any of this.
 */
internal object ArcadeEffectsDirector {
    private val creamDust = intArrayOf(0xF2E6D0)

    fun absorb(
        events: List<ArcadeEvent>,
        game: ArcadeGame,
        fx: ArcadeEffects,
    ): List<ArcadeHaptic> {
        val haptics = mutableListOf<ArcadeHaptic>()
        for (event in events) {
            when (event) {
                ArcadeEvent.Flap -> (game as? FlappyGame)?.let { fx.dashes(108.0, it.y + 14, -1.0) }
                ArcadeEvent.Hit -> {
                    haptics += ArcadeHaptic.HIT
                    when (game) {
                        is SkateGame -> fx.shake = 0.4
                        is GalaxyGame -> fx.shake = 0.8
                        else -> {
                            fx.shake = 0.6
                            val (x, y) = hitOrigin(game)
                            fx.burst(
                                x,
                                y,
                                12,
                                intArrayOf(ArcadePalette.CREAM, ArcadePalette.LIME, ArcadePalette.GOLD),
                                speed = 90.0,
                                life = 0.6,
                                gravity = 220.0,
                                up = 40.0,
                            )
                        }
                    }
                }
                ArcadeEvent.Bail -> {
                    fx.shake = 0.4
                    haptics += ArcadeHaptic.SOFT
                }
                is ArcadeEvent.Place -> {
                    val cam = (game as? StackGame)?.cam ?: 0.0
                    fx.burst(event.x, event.y + 16 + cam, 6, creamDust, speed = 40.0, life = 0.3, gravity = 60.0)
                }
                is ArcadeEvent.Perfect -> {
                    val cam = (game as? StackGame)?.cam ?: 0.0
                    fx.burst(
                        event.x,
                        event.y + 8 + cam,
                        14,
                        intArrayOf(ArcadePalette.GOLD, ArcadePalette.WHITE, ArcadePalette.LIME_HI),
                        speed = 90.0,
                        life = 0.5,
                        gravity = 0.0,
                    )
                    fx.popup(
                        if (event.combo >
                            1
                        ) {
                            "PERFECTO x${event.combo}"
                        } else {
                            "¡PERFECTO!"
                        },
                        event.x,
                        event.y - 8 + cam,
                        ArcadePalette.GOLD,
                    )
                    haptics += ArcadeHaptic.SOFT
                }
                ArcadeEvent.Grow -> fx.popup("+ANCHO", 240.0, 130.0, ArcadePalette.LIME_HI)
                is ArcadeEvent.Flip ->
                    (game as? GravityGame)?.let {
                        fx.burst(
                            GravityGame.K.X,
                            it.y,
                            8,
                            intArrayOf(ArcadePalette.WHITE, ArcadePalette.LIME_HI),
                            speed = 70.0,
                            life = 0.3,
                            gravity = 0.0,
                        )
                    }
                is ArcadeEvent.Land -> {
                    val y = if (event.direction > 0) GravityGame.K.FLOOR else GravityGame.K.CEIL
                    fx.burst(
                        GravityGame.K.X,
                        y,
                        5,
                        intArrayOf(ArcadePalette.GOLD),
                        speed = 60.0,
                        life = 0.25,
                        gravity = if (event.direction > 0) 120.0 else -120.0,
                    )
                }
                is ArcadeEvent.Kill -> {
                    fx.burst(event.x, event.y, 10, killColors(event.kind), speed = 80.0, life = 0.45, gravity = 90.0)
                    fx.popup("+${event.points}", event.x, event.y - 6, ArcadePalette.GOLD)
                }
                ArcadeEvent.Hurt -> {
                    haptics += ArcadeHaptic.HIT
                    fx.shake = 0.7
                    fx.burst(
                        (game as? GalaxyGame)?.x ?: 240.0,
                        GalaxyGame.K.SHIP_Y,
                        14,
                        intArrayOf(ArcadePalette.CREAM, ArcadePalette.LIME, 0xFF9A3A),
                        speed = 90.0,
                        life = 0.6,
                        gravity = 60.0,
                    )
                }
                is ArcadeEvent.Power -> {
                    fx.popup(
                        "¡DISPARO x${event.n}!",
                        (game as? GalaxyGame)?.x ?: 240.0,
                        GalaxyGame.K.SHIP_Y - 44,
                        ArcadePalette.LIME_HI,
                    )
                    haptics += ArcadeHaptic.SOFT
                }
                ArcadeEvent.Life -> {
                    fx.popup("+VIDA", (game as? GalaxyGame)?.x ?: 240.0, GalaxyGame.K.SHIP_Y - 44, 0xFF8AA0)
                    haptics += ArcadeHaptic.SOFT
                }
                is ArcadeEvent.BossDown -> {
                    haptics += ArcadeHaptic.HIT
                    fx.shake = 0.8
                    fx.burst(
                        event.x,
                        event.y,
                        34,
                        intArrayOf(ArcadePalette.WHITE, ArcadePalette.GOLD, 0xFF8A5A, 0x3A3A4A),
                        speed = 150.0,
                        life = 0.9,
                        gravity = 60.0,
                    )
                    fx.popup("¡CAFETERA FUERA! +500", 240.0, 90.0, ArcadePalette.GOLD)
                }
                is ArcadeEvent.Score, is ArcadeEvent.Level, ArcadeEvent.Release, ArcadeEvent.Shoot,
                ArcadeEvent.HitEnemy, is ArcadeEvent.Wave, ArcadeEvent.BossFire,
                -> Unit
            }
        }
        return haptics
    }

    private fun hitOrigin(game: ArcadeGame): Pair<Double, Double> =
        when (game) {
            is FlappyGame -> FlappyGame.K.X to game.y
            is GravityGame -> 240.0 to game.y
            else -> 240.0 to 150.0
        }

    private fun killColors(kind: GalaxyEnemyKind): IntArray =
        when (kind) {
            GalaxyEnemyKind.DONA -> intArrayOf(0xFF9DB0, 0xFFFFFF, 0xD99A55)
            GalaxyEnemyKind.TAZA -> intArrayOf(0xD9714A, 0xFFFFFF, 0x5A3A26)
            GalaxyEnemyKind.CRUASAN -> intArrayOf(0xE8A040, 0xF8C860, 0xFFFFFF)
        }
}
