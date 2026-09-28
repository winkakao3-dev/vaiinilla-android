package com.vaiinilla.app.domain.arcade

internal enum class ArcadeKind(
    val label: String,
    val hint: String,
) {
    FLAPPY("Vuela", "Toca para volar"),
    SKATE("Skate", "Toca para saltar"),
    APILA("Apila", "Toca para soltar"),
    GRAVEDAD("Gravedad", "Toca para cambiar la gravedad"),
    GALAXIA("Galaxia", "Arrastra para mover"),
}

internal enum class ArcadeStatus { IDLE, PLAY, DEAD }

internal interface ArcadeScoreStore {
    fun top(kind: ArcadeKind): List<Int>

    fun record(
        kind: ArcadeKind,
        score: Int,
    )
}

internal class MemoryArcadeScoreStore : ArcadeScoreStore {
    private val data = mutableMapOf<ArcadeKind, List<Int>>()

    override fun top(kind: ArcadeKind): List<Int> = data[kind].orEmpty()

    override fun record(
        kind: ArcadeKind,
        score: Int,
    ) {
        data[kind] = (top(kind) + score).sortedDescending().take(8)
    }
}

internal data class ArcadeBoardRow(
    val name: String,
    val points: Int,
    val me: Boolean = false,
)

internal val ARCADE_SEEDS: Map<ArcadeKind, List<Pair<String, Int>>> =
    mapOf(
        ArcadeKind.FLAPPY to listOf("LA COCINA" to 9, "DOÑA V" to 6, "CAJA" to 4),
        ArcadeKind.SKATE to listOf("LA COCINA" to 2400, "DOÑA V" to 1700, "MESERO" to 900),
        ArcadeKind.APILA to listOf("DOÑA V" to 14, "LA COCINA" to 11, "MESERO" to 8),
        ArcadeKind.GRAVEDAD to listOf("MESERO" to 15, "DOÑA V" to 12, "LA COCINA" to 9),
        ArcadeKind.GALAXIA to listOf("LA COCINA" to 1200, "DOÑA V" to 800, "MESERO" to 450),
    )
