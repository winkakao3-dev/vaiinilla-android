package com.vaiinilla.app.ui.arcade

import com.vaiinilla.app.arcade.ArcadeGame
import com.vaiinilla.app.arcade.ArcadeKind
import com.vaiinilla.app.arcade.FlappyGame
import com.vaiinilla.app.arcade.GalaxyGame
import com.vaiinilla.app.arcade.GravityGame
import com.vaiinilla.app.arcade.SkateGame
import com.vaiinilla.app.arcade.StackGame

/** Big title on the pixel title screen. */
private val ArcadeKind.pixelTitle: String
    get() =
        when (this) {
            ArcadeKind.FLAPPY -> "VUELA"
            ArcadeKind.SKATE -> "VAINI SKATE"
            ArcadeKind.APILA -> "APILA"
            ArcadeKind.GRAVEDAD -> "GRAVEDAD"
            ArcadeKind.GALAXIA -> "GALAXIA"
        }

/** Blinking hint under the title. */
private val ArcadeKind.pixelHint: String
    get() =
        when (this) {
            ArcadeKind.FLAPPY -> "TOCA PARA VOLAR"
            ArcadeKind.SKATE -> "TOCA PARA EMPEZAR"
            ArcadeKind.APILA -> "TOCA PARA SOLTAR"
            ArcadeKind.GRAVEDAD -> "TOCA PARA VOLTEAR"
            ArcadeKind.GALAXIA -> "ARRASTRA PARA MOVER"
        }

/** Composes one frame of a mini game (scene, effects, HUD, title or game-over panel) into a pixel canvas. */
internal class ArcadeRenderer {
    val canvas = PixelCanvas()
    private val skateScene = SkateScene()

    fun render(
        kind: ArcadeKind,
        game: ArcadeGame,
        effects: ArcadeEffects?,
        original: ArcadeFrameInfo,
    ) {
        val info =
            if (original.holdHint) {
                // both times are in the visible phase of the blink
                ArcadeFrameInfo(
                    original.screen,
                    if (original.screen ==
                        ArcadeScreen.OVER
                    ) {
                        1.4
                    } else {
                        0.3
                    },
                    original.best,
                    original.newBest,
                    original.dt,
                    original.touch,
                    true,
                )
            } else {
                original
            }
        val cv = canvas
        var onLight = false
        var extras: (() -> Unit)? = null
        when (game) {
            is SkateGame -> {
                skateScene.draw(cv, game, info)
                effects?.draw(cv)
                return
            }
            is FlappyGame -> {
                FlappyScene.draw(cv, game)
                extras = { FlappyScene.drawHudExtras(cv, game) }
            }
            is StackGame -> {
                StackScene.draw(cv, game)
                extras = { StackScene.drawHudExtras(cv, game) }
            }
            is GravityGame -> {
                GravityScene.draw(cv, game)
                onLight = true
            }
            is GalaxyGame -> {
                GalaxyScene.draw(cv, game)
                extras = { GalaxyScene.drawHudExtras(cv, game) }
            }
        }
        effects?.draw(cv)
        if (info.screen == ArcadeScreen.PLAYING || info.screen == ArcadeScreen.OVER) {
            cv.hudScore(game.score, info.best, onLight)
            extras?.invoke()
        }
        if (info.screen == ArcadeScreen.TITLE) cv.drawTitle(info.screenTime, kind.pixelTitle, kind.pixelHint)
        if (info.screen == ArcadeScreen.OVER) cv.drawGameOver(info.screenTime, game.score, info.best, info.newBest)
    }
}
