package com.vaiinilla.app.arcade

import kotlin.math.floor
import kotlin.math.min

/** A challenge ("reto"): three at a time, scaled by the player's level. Progress reads the run's stats. */
internal data class SkateMission(
    val id: String,
    val target: Int,
    val label: String,
)

internal object SkateMissions {
    private class Template(
        val id: String,
        val base: Int,
        val grow: Int,
        val label: (Int) -> String,
        val value: (SkateRun) -> Int,
    )

    private val pool =
        listOf(
            Template("cones", 5, 3, { "SALTA $it CONOS" }, { it.stats.cones }),
            Template("kickflips", 2, 2, { "HAZ $it KICKFLIPS" }, { it.stats.kickflips }),
            Template("grinds", 2, 2, { "GRINDA $it VECES" }, { it.stats.grinds }),
            Template("stars", 15, 10, { "RECOGE $it ESTRELLAS" }, { it.stats.stars }),
            Template("distance", 300, 200, { "LLEGA A $it M" }, { it.meters }),
            Template("combo", 4, 2, { "COMBO DE $it" }, { it.bestCombo }),
            Template("clean", 150, 100, { "$it M SIN GOLPES" }, { floor(it.stats.cleanDistance / 20).toInt() }),
            Template("score", 2000, 1500, { "$it PUNTOS" }, { it.score }),
        )

    /** Three different challenges for a level, deterministic per level. */
    fun missionsFor(level: Int): List<SkateMission> {
        val picks = mutableListOf<Template>()
        var k = level * 7 + 3
        while (picks.size < 3) {
            k = ((k * 1103515245) + 12345) and 0x7FFFFFFF
            val m = pool[k % pool.size]
            if (picks.none { it.id == m.id }) picks += m
        }
        return picks.map {
            val target = it.base + it.grow * level
            SkateMission(it.id, target, it.label(target))
        }
    }

    /** Current value (capped at the target) and whether the challenge is done. */
    fun progress(
        mission: SkateMission,
        run: SkateRun,
    ): Pair<Int, Boolean> {
        val t = pool.firstOrNull { it.id == mission.id } ?: return 0 to false
        val v = t.value(run)
        return min(v, mission.target) to (v >= mission.target)
    }
}

/** Saved progress: best score, level and which of the current challenges are already done. */
internal data class SkateProgress(
    val best: Int = 0,
    val level: Int = 0,
    val done: List<String> = emptyList(),
)

internal interface SkateProgressStore {
    fun load(): SkateProgress

    fun save(progress: SkateProgress)
}

internal class MemorySkateProgressStore : SkateProgressStore {
    private var progress = SkateProgress()

    override fun load(): SkateProgress = progress

    override fun save(progress: SkateProgress) {
        this.progress = progress
    }
}
