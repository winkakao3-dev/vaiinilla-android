package com.vaiinilla.app.jvmroles

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.robolectric.shadows.ShadowToast

/**
 * Mira la pantalla mientras el arnés deja correr la app y anota lo que un probador vería mal:
 *
 * - carga-eterna: un indicador de carga (o una barra quieta) que sigue en pantalla sin nada pendiente en la red.
 * - carga-larga: sigue cargando con la red pendiente más allá de lo razonable; la pantalla no ofrece salida.
 * - error-mudo: una escritura (POST/PUT/DELETE) falló y la pantalla quedó igual: ni texto nuevo ni toast.
 * - cierre: una excepción sin atrapar, que en el teléfono cierra la app.
 *
 * Solo ve lo que el arnés le muestra: el reloj de Compose avanza dentro de los comandos, así que muestrea
 * desde [JvmRolesHarness.pump] y mide en tiempo bombeado, no en tiempo de pared.
 */
class Vigia(
    private val network: VigiaNetwork,
    private val nodes: () -> List<SemanticsNode>,
    private val labelOf: (SemanticsNode) -> String,
) {
    data class Finding(
        val kind: String,
        val severity: String,
        val detail: String,
        val screen: List<String>,
        val network: List<String>,
    )

    /** [lastEventId]: la última petición que ya existía al tomar la muestra; ordena sin depender del reloj. */
    private class Sample(
        val lastEventId: Long,
        val texts: Set<String>,
    )

    private class PendingFailure(
        val event: VigiaNetwork.Event,
        val before: Set<String>,
        val toastsBefore: Int,
        val checkAt: Long,
    )

    var loadingLimitMillis = DEFAULT_LOADING_LIMIT_MILLIS
    private var pumped = 0L
    private var nextSampleAt = 0L
    private var loadingSince: Long? = null
    private var loadingSignature: String? = null
    private var loadingReported = false
    private var lastEventId = 0L
    private val samples = ArrayDeque<Sample>()
    private val pending = mutableListOf<PendingFailure>()
    private val findings = mutableListOf<Finding>()
    private val crashes = ArrayDeque<Throwable>()

    fun reset() {
        loadingSince = null
        loadingSignature = null
        loadingReported = false
        lastEventId = network.lastId()
        samples.clear()
        pending.clear()
    }

    fun recordCrash(error: Throwable) = synchronized(crashes) { crashes.addLast(error) }

    /** Lo llama el arnés por cada paso de reloj bombeado. */
    fun tick(stepMillis: Long) {
        pumped += stepMillis
        if (pumped < nextSampleAt) return
        nextSampleAt = pumped + SAMPLE_EVERY_MILLIS
        val visible = runCatching(nodes).getOrDefault(emptyList())
        val texts = visible.map(labelOf).filter { it.isNotBlank() }.toSet()
        samples.addLast(Sample(network.lastId(), texts))
        while (samples.size > MAX_SAMPLES) samples.removeFirst()
        drainCrashes(texts)
        watchLoading(visible, texts)
        watchFailures(texts)
    }

    fun drain(): JSONObject {
        val out = JSONArray()
        findings.forEach { finding ->
            out.put(
                JSONObject()
                    .put("tipo", finding.kind)
                    .put("gravedad", finding.severity)
                    .put("detalle", finding.detail)
                    .put("pantalla", JSONArray(finding.screen))
                    .put("red", JSONArray(finding.network)),
            )
        }
        findings.clear()
        return JSONObject()
            .put("hallazgos", out)
            .put("red", network.describe())
            .put("enCurso", network.inFlight)
            .put("cargando", loadingSince != null)
    }

    private fun drainCrashes(texts: Set<String>) {
        val errors = synchronized(crashes) { crashes.toList().also { crashes.clear() } }
        errors.forEach { error ->
            val where = error.stackTrace.firstOrNull { it.className.startsWith("com.vaiinilla") }
            findings +=
                Finding(
                    kind = "cierre",
                    severity = "error",
                    detail =
                        "${error.javaClass.simpleName}: ${error.message?.take(200)}" + (where?.let { " en $it" } ?: ""),
                    screen = texts.take(SCREEN_TEXTS),
                    network = recentNetwork(),
                )
        }
    }

    private fun watchLoading(
        visible: List<SemanticsNode>,
        texts: Set<String>,
    ) {
        val loaders = visible.mapNotNull { it.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo) }
        if (loaders.isEmpty()) {
            loadingSince = null
            loadingSignature = null
            loadingReported = false
            return
        }
        val determinate = loaders.filter { it != ProgressBarRangeInfo.Indeterminate }
        // Una barra que avanza reinicia la cuenta: lo que importa es que se quede quieta.
        val signature = determinate.joinToString { "%.3f".format(it.current) } + "|" + (loaders.size - determinate.size)
        if (signature != loadingSignature) {
            loadingSignature = signature
            loadingSince = pumped
            loadingReported = false
            return
        }
        val elapsed = pumped - (loadingSince ?: pumped)
        if (loadingReported) return
        val waitingOnNetwork = network.inFlight > 0
        val onlyDeterminate = determinate.size == loaders.size
        when {
            !waitingOnNetwork && elapsed >= loadingLimitMillis -> {
                loadingReported = true
                findings +=
                    Finding(
                        kind = "carga-eterna",
                        severity = if (onlyDeterminate) "sospecha" else "error",
                        detail =
                            if (onlyDeterminate) {
                                stuckBarDetail(
                                    elapsed,
                                    determinate,
                                )
                            } else {
                                endlessLoaderDetail(elapsed)
                            },
                        screen = texts.take(SCREEN_TEXTS),
                        network = recentNetwork(),
                    )
            }
            waitingOnNetwork && elapsed >= loadingLimitMillis * LONG_LOADING_FACTOR -> {
                loadingReported = true
                findings +=
                    Finding(
                        kind = "carga-larga",
                        severity = "sospecha",
                        detail =
                            "Lleva ${elapsed / 1000} s cargando con ${network.inFlight} petición(es) en curso " +
                                "y sin ofrecer salida ni reintento.",
                        screen = texts.take(SCREEN_TEXTS),
                        network = recentNetwork(),
                    )
            }
        }
    }

    private fun stuckBarDetail(
        elapsed: Long,
        bars: List<ProgressBarRangeInfo>,
    ): String {
        val values = bars.joinToString { "%.0f %%".format(it.current * 100) }
        return "Una barra de progreso lleva ${elapsed / 1000} s sin avanzar ($values) " +
            "y no hay nada pendiente en la red."
    }

    private fun endlessLoaderDetail(elapsed: Long): String =
        "Indicador de carga en pantalla ${elapsed / 1000} s sin ninguna petición pendiente: " +
            "la pantalla ya no va a salir sola de ahí."

    private fun watchFailures(texts: Set<String>) {
        val fresh = network.eventsAfter(lastEventId)
        val finished = fresh.takeWhile { it.endedAt != null }
        finished.lastOrNull()?.let { lastEventId = it.id }
        finished
            .filter { it.failed && it.method != "GET" }
            .forEach { event ->
                val before = samples.lastOrNull { it.lastEventId < event.id }?.texts ?: samples.first().texts
                pending += PendingFailure(event, before, ShadowToast.shownToastCount(), pumped + ERROR_FEEDBACK_MILLIS)
            }
        val due = pending.filter { pumped >= it.checkAt }
        pending.removeAll(due)
        due.forEach { failure ->
            val toastShown = ShadowToast.shownToastCount() > failure.toastsBefore
            val newTexts = texts - failure.before
            if (!toastShown && newTexts.isEmpty()) {
                findings +=
                    Finding(
                        kind = "error-mudo",
                        severity = "error",
                        detail =
                            "${failure.event.method} ${failure.event.path} falló (${failure.event.outcome}) " +
                                "y la pantalla no dijo nada en ${ERROR_FEEDBACK_MILLIS / 1000} s.",
                        screen = texts.take(SCREEN_TEXTS),
                        network = recentNetwork(),
                    )
            }
        }
    }

    private fun recentNetwork(): List<String> = network.recent(RECENT_EVENTS)

    private companion object {
        const val SAMPLE_EVERY_MILLIS = 250L
        const val MAX_SAMPLES = 400
        const val DEFAULT_LOADING_LIMIT_MILLIS = 12_000L
        const val LONG_LOADING_FACTOR = 3
        const val ERROR_FEEDBACK_MILLIS = 5_000L
        const val SCREEN_TEXTS = 12
        const val RECENT_EVENTS = 8
    }
}
