package com.vaiinilla.app.jvmroles

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.ApiResponseParser
import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.di.ApiClientModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Lo que la red le hace a la app dentro del arnés: deja pasar todo (normal), tarda (lento), falla a veces
 * (fallas) o no responde nada (caido). Lleva la cuenta de peticiones en curso y una bitácora corta, que
 * el vigía usa para distinguir "esperando a la red" de "se quedó cargando sin nada pendiente".
 * Nunca inventa un 401: dispararía el refresco de sesión y sacaría al rol.
 */
@Singleton
class VigiaNetwork
    @Inject
    constructor(
        private val parser: ApiResponseParser,
    ) {
        enum class Mode { NORMAL, LENTO, FALLAS, CAIDO }

        data class Event(
            val id: Long,
            val method: String,
            val path: String,
            val startedAt: Long,
            val endedAt: Long?,
            val outcome: String?,
            val failed: Boolean,
            val sabotaged: Boolean,
        )

        @Volatile var mode: Mode = Mode.NORMAL
            private set

        @Volatile private var delayMillis = 0L

        @Volatile private var failPercent = 0

        @Volatile private var onlyPath: String? = null

        /**
         * Sin escrituras, nada de lo que toque el explorador cambia el sandbox de dev que comparten otros agentes:
         * cada POST/PUT/DELETE (salvo sesión y latidos) vuelve como error sin salir. La app debe avisarlo igual.
         */
        @Volatile var writesAllowed = true

        private var random = Random(DEFAULT_SEED)
        private val ids = AtomicLong()
        private val running = AtomicInteger()
        private val totals =
            mapOf(
                "peticiones" to AtomicInteger(),
                "saboteadas" to AtomicInteger(),
                "bloqueadas" to AtomicInteger(),
            )
        private val events = ArrayDeque<Event>()

        val inFlight: Int get() = running.get()

        fun configure(args: List<String>): String {
            val pathFlag = args.indexOf("--ruta")
            onlyPath = if (pathFlag >= 0) args.getOrNull(pathFlag + 1) else null
            val rest = if (pathFlag >= 0) args.take(pathFlag) else args
            val name = rest.firstOrNull()?.lowercase() ?: "normal"
            mode =
                Mode.entries.firstOrNull { it.name.lowercase() == name }
                    ?: error("Modos: normal, lento [ms], fallas [%] [semilla], caido.")
            delayMillis = if (mode == Mode.LENTO) rest.getOrNull(1)?.toLong() ?: DEFAULT_DELAY_MILLIS else 0L
            failPercent =
                when (mode) {
                    Mode.FALLAS -> rest.getOrNull(1)?.toInt() ?: DEFAULT_FAIL_PERCENT
                    Mode.CAIDO -> 100
                    else -> 0
                }
            synchronized(this) { random = Random(rest.getOrNull(2)?.toLong() ?: DEFAULT_SEED) }
            return describe()
        }

        fun describe(): String =
            when (mode) {
                Mode.NORMAL -> "red normal"
                Mode.LENTO -> "red lenta: +$delayMillis ms por petición"
                Mode.FALLAS -> "red con fallas: $failPercent % de las peticiones"
                Mode.CAIDO -> "red caída: ninguna petición responde"
            } + (onlyPath?.let { " (solo rutas con '$it')" } ?: "")

        /** Eventos terminados o en curso desde [sinceId] (exclusivo). */
        fun eventsAfter(sinceId: Long): List<Event> = synchronized(this) { events.filter { it.id > sinceId } }

        fun lastId(): Long = ids.get()

        /** Cuántas peticiones pasaron, cuántas se sabotearon y cuántas escrituras se frenaron desde que arrancó la JVM. */
        fun totals(): Map<String, Int> = totals.mapValues { it.value.get() }

        /** Las últimas [count] peticiones, una por línea, para el informe y el comando `trafico`. */
        fun recent(count: Int): List<String> =
            eventsAfter(lastId() - count).map { event ->
                val state = event.outcome ?: "en curso"
                val millis = event.endedAt?.let { " ${it - event.startedAt} ms" } ?: ""
                "${event.method} ${event.path} → $state$millis" + if (event.sabotaged) " (sabotaje)" else ""
            }

        fun reset() {
            configure(emptyList())
            synchronized(this) { events.clear() }
        }

        fun intercept(
            method: String,
            path: String,
            call: () -> Result<String>,
        ): Result<String> {
            val id = ids.incrementAndGet()
            val shownPath = sanitize(path)
            val startedAt = System.currentTimeMillis()
            record(Event(id, method, shownPath, startedAt, null, null, failed = false, sabotaged = false))
            running.incrementAndGet()
            totals.getValue("peticiones").incrementAndGet()
            var sabotaged = false
            val result =
                try {
                    val targeted = onlyPath?.let { path.contains(it) } ?: true
                    if (targeted && delayMillis > 0) {
                        sabotaged = true
                        Thread.sleep(delayMillis)
                    }
                    val blocked = !writesAllowed && method != "GET" && WRITE_ALLOWLIST.none { path.startsWith(it) }
                    val failure =
                        if (blocked) {
                            blockedWrite()
                        } else if (targeted) {
                            pickFailure()
                        } else {
                            null
                        }
                    if (blocked) totals.getValue("bloqueadas").incrementAndGet()
                    if (failure != null) {
                        sabotaged = true
                        Result.failure(failure)
                    } else {
                        call()
                    }
                } finally {
                    running.decrementAndGet()
                }
            if (sabotaged) totals.getValue("saboteadas").incrementAndGet()
            val outcome =
                result.fold(
                    onSuccess = { "ok" },
                    onFailure = { error ->
                        (error as? ApiClientException)?.let { "HTTP ${it.httpStatus} ${it.code}" }
                            ?: error.javaClass.simpleName
                    },
                )
            record(
                Event(
                    id,
                    method,
                    shownPath,
                    startedAt,
                    System.currentTimeMillis(),
                    outcome,
                    result.isFailure,
                    sabotaged,
                ),
            )
            return result
        }

        private fun blockedWrite(): Throwable =
            parser.parseError(
                """{"error":{"code":"VIGIA_SOLO_LECTURA","message":"El vigía no deja escribir en el sandbox."}}""",
                503,
            )

        private fun pickFailure(): Throwable? {
            if (failPercent <= 0) return null
            val (roll, kind) = synchronized(this) { random.nextInt(100) to random.nextInt(3) }
            if (mode != Mode.CAIDO && roll >= failPercent) return null
            return when (if (mode == Mode.CAIDO) 2 else kind) {
                0 ->
                    parser.parseError(
                        """{"error":{"code":"VIGIA_500","message":"Falla del servidor (vigía)."}}""",
                        500,
                    )
                1 -> SocketTimeoutException("vigía: tiempo agotado")
                else -> ConnectException("vigía: sin red")
            }
        }

        private fun record(event: Event) =
            synchronized(this) {
                val index = events.indexOfFirst { it.id == event.id }
                if (index >= 0) events[index] = event else events.addLast(event)
                while (events.size > MAX_EVENTS) events.removeFirst()
            }

        private fun sanitize(path: String): String =
            path.substringBefore('?').split('/').joinToString("/") { segment ->
                if (segment.length >= 24 ||
                    segment.count { it == '-' } >= 3 ||
                    segment.all(Char::isDigit) &&
                    segment.isNotEmpty()
                ) {
                    "{id}"
                } else {
                    segment
                }
            }

        private companion object {
            const val MAX_EVENTS = 300
            const val DEFAULT_DELAY_MILLIS = 4_000L
            const val DEFAULT_FAIL_PERCENT = 30
            const val DEFAULT_SEED = 7L
            val WRITE_ALLOWLIST = listOf("sesiones/", "latidos", "dispositivos")
        }
    }

/**
 * Pasa cada llamada de la app por [VigiaNetwork].
 * Las clases que inyectan HttpVaiinillaApiClient directo (sesión, presencia) no pasan por aquí.
 */
class VigiaApiClient(
    private val delegate: VaiinillaApiClient,
    private val network: VigiaNetwork,
) : VaiinillaApiClient {
    override val baseUrl: String get() = delegate.baseUrl

    override fun readCachedGet(
        path: String,
        query: Map<String, String>,
    ): String? = delegate.readCachedGet(path, query)

    override fun get(
        path: String,
        query: Map<String, String>,
    ) = network.intercept("GET", path) { delegate.get(path, query) }

    override fun post(
        path: String,
        body: String,
        headers: Map<String, String>,
    ) = network.intercept("POST", path) { delegate.post(path, body, headers) }

    override fun postWithoutBody(
        path: String,
        headers: Map<String, String>,
    ) = network.intercept("POST", path) { delegate.postWithoutBody(path, headers) }

    override fun delete(
        path: String,
        body: String?,
        headers: Map<String, String>,
    ) = network.intercept("DELETE", path) { delegate.delete(path, body, headers) }

    override fun getWithBearer(
        bearer: String,
        path: String,
        query: Map<String, String>,
    ) = network.intercept("GET", path) { delegate.getWithBearer(bearer, path, query) }

    override fun postWithBearer(
        bearer: String,
        path: String,
        body: String?,
        headers: Map<String, String>,
    ) = network.intercept("POST", path) { delegate.postWithBearer(bearer, path, body, headers) }

    override fun deleteWithBearer(
        bearer: String,
        path: String,
        body: String?,
        headers: Map<String, String>,
    ) = network.intercept("DELETE", path) { delegate.deleteWithBearer(bearer, path, body, headers) }

    override fun deleteWithBearerExpecting200(
        bearer: String,
        path: String,
        body: String?,
        headers: Map<String, String>,
    ) = network.intercept("DELETE", path) { delegate.deleteWithBearerExpecting200(bearer, path, body, headers) }

    override fun put(
        path: String,
        body: String,
        headers: Map<String, String>,
    ) = network.intercept("PUT", path) { delegate.put(path, body, headers) }

    override fun putMultipart(
        path: String,
        fieldName: String,
        filename: String,
        mimeType: String,
        bytes: ByteArray,
        headers: Map<String, String>,
    ) = network.intercept("PUT", path) { delegate.putMultipart(path, fieldName, filename, mimeType, bytes, headers) }

    override fun postMultipart(
        path: String,
        fieldName: String,
        filename: String,
        mimeType: String,
        bytes: ByteArray,
        headers: Map<String, String>,
    ) = network.intercept("POST", path) { delegate.postMultipart(path, fieldName, filename, mimeType, bytes, headers) }

    override fun getPublic(
        path: String,
        query: Map<String, String>,
    ) = network.intercept("GET", path) { delegate.getPublic(path, query) }

    override fun postPublic(
        path: String,
        body: String,
        headers: Map<String, String>,
    ) = network.intercept("POST", path) { delegate.postPublic(path, body, headers) }
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [ApiClientModule::class])
object VigiaApiClientModule {
    @Provides
    @Singleton
    fun provideApiClient(
        client: HttpVaiinillaApiClient,
        network: VigiaNetwork,
    ): VaiinillaApiClient = VigiaApiClient(client, network)
}
