package com.vaiinilla.app.arcade

/** Vaini Skate as one of the arcade games: wraps the street rules with the challenges, levels and saved progress. */
internal class SkateGame(
    private val seed: Int = 4,
    private val store: SkateProgressStore = MemorySkateProgressStore(),
) : ArcadeGame {
    class Toast(
        val text: String,
        var life: Double,
    )

    class Result(
        val newBest: Boolean,
        val levelUp: Boolean,
        val missions: List<SkateMission>,
        val snapshot: SkateProgress,
    )

    var run = SkateRun(seed)
        private set

    /** Counts the times a player has started a run; scenes reset their effects when it changes. */
    var startCount = 0
        private set
    var progress = store.load()
        private set
    var missions = SkateMissions.missionsFor(progress.level)
        private set
    override var started = false
        private set
    override var over = false
        private set
    var deadT = 0.0
        private set
    override var score = 0.0
        private set
    var toast: Toast? = null
        private set
    var result: Result? = null
        private set
    var t = 0.0
        private set
    private val events = mutableListOf<ArcadeEvent>()
    private val skateEvents = mutableListOf<SkateEvent>()

    override val time: Double get() = t
    override val deadTime: Double get() = deadT

    private fun newRun() {
        run = SkateRun(seed)
    }

    override fun tap() {
        if (over) return
        if (!started) {
            started = true
            startCount += 1
            skateEvents.clear() // whatever the title demo raised belongs to the old scene
            newRun()
        }
        run.pressJump()
    }

    override fun release() = run.releaseJump()

    override fun fall(on: Boolean) = run.setFastFall(on)

    override fun drainEvents(): List<ArcadeEvent> = events.toList().also { events.clear() }

    /** Street events since the last call, for the scene's own effects (sparks, popups). */
    fun drainSkateEvents(): List<SkateEvent> = skateEvents.toList().also { skateEvents.clear() }

    override fun step(dt: Double) {
        t += dt
        toast?.let {
            it.life -= dt
            if (it.life <= 0) toast = null
        }
        if (!started) { // title: the demo run plays itself
            val r = run
            val ahead = r.obstacles.firstOrNull { it.x - r.distance > 120 && it.x - r.distance < 175 }
            if (ahead != null && r.player.grounded) r.pressJump()
            if (!r.player.grounded && r.player.vy > 0) r.releaseJump()
            r.step(dt)
            skateEvents += r.drainEvents()
            if (r.over) newRun()
            return
        }
        if (over) {
            deadT += dt
            return
        }
        run.step(dt)
        score = run.score.toDouble()
        val street = run.drainEvents()
        skateEvents += street
        for (e in street) {
            when (e) {
                is SkateEvent.Hit -> events += ArcadeEvent.Hit
                is SkateEvent.Bail -> events += ArcadeEvent.Bail
                else -> Unit
            }
        }
        for (m in missions) {
            if (progress.done.contains(m.id)) continue
            if (SkateMissions.progress(m, run).second) {
                progress = progress.copy(done = progress.done + m.id)
                store.save(progress)
                toast = Toast("¡RETO: ${m.label}!", 2.0)
            }
        }
        if (run.over) finish()
    }

    private fun finish() {
        over = true
        val newBest = run.score > progress.best
        if (newBest) progress = progress.copy(best = run.score)
        val levelUp = missions.all { progress.done.contains(it.id) }
        val shown = missions
        if (levelUp) {
            progress = progress.copy(level = progress.level + 1, done = emptyList())
            missions = SkateMissions.missionsFor(progress.level)
        }
        store.save(progress)
        val snapshot = if (levelUp) progress.copy(done = shown.map { it.id }) else progress
        result = Result(newBest, levelUp, shown, snapshot)
    }
}
