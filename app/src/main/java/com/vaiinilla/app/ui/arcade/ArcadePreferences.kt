package com.vaiinilla.app.ui.arcade

import android.content.Context
import com.vaiinilla.app.domain.arcade.ArcadeKind
import com.vaiinilla.app.domain.arcade.ArcadeScoreStore
import com.vaiinilla.app.domain.arcade.SkateProgress
import com.vaiinilla.app.domain.arcade.SkateProgressStore

/** Top scores per game, kept in the app's private preferences. */
internal class PrefsArcadeScoreStore(
    context: Context,
) : ArcadeScoreStore {
    private val prefs = context.getSharedPreferences("vaiinilla.arcade", Context.MODE_PRIVATE)

    override fun top(kind: ArcadeKind): List<Int> =
        prefs
            .getString(kind.name, "")
            .orEmpty()
            .split(",")
            .mapNotNull { it.toIntOrNull() }

    override fun record(
        kind: ArcadeKind,
        score: Int,
    ) {
        val list = (top(kind) + score).sortedDescending().take(8)
        prefs.edit().putString(kind.name, list.joinToString(",")).apply()
    }
}

/** Vaini Skate's best score, level and finished challenges, kept in the app's private preferences. */
internal class PrefsSkateProgressStore(
    context: Context,
) : SkateProgressStore {
    private val prefs = context.getSharedPreferences("vaiinilla.arcade", Context.MODE_PRIVATE)

    override fun load(): SkateProgress =
        SkateProgress(
            best = prefs.getInt("skate.best", 0),
            level = prefs.getInt("skate.level", 0),
            done =
                prefs
                    .getString("skate.done", "")
                    .orEmpty()
                    .split(",")
                    .filter { it.isNotBlank() },
        )

    override fun save(progress: SkateProgress) {
        prefs
            .edit()
            .putInt("skate.best", progress.best)
            .putInt("skate.level", progress.level)
            .putString("skate.done", progress.done.joinToString(","))
            .apply()
    }
}
