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
 * Nunca inventa un 401: dispararía el refresco de sesión y sacaría al rol. Ve tanto lo que pasa por
 * [VigiaApiClient] como lo que sale directo por HttpURLConnection hacia la API de dev ([VigiaHttp]).
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
                    val targeted =
                        SABOTAGE_EXEMPT.none { path.startsWith(it) } && (onlyPath?.let { path.contains(it) } ?: true)
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
                        accounted(call)
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
            parser.parseError(errorEnvelope("VIGIA_SOLO_LECTURA", "El vigía no deja escribir en el sandbox."), 503)

        private fun pickFailure(): Throwable? {
            if (failPercent <= 0) return null
            val (roll, kind) = synchronized(this) { random.nextInt(100) to random.nextInt(3) }
            if (mode != Mode.CAIDO && roll >= failPercent) return null
            return when (if (mode == Mode.CAIDO) 2 else kind) {
                0 -> parser.parseError(errorEnvelope("VIGIA_500", "Falla del servidor (vigía)."), 500)
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

        companion object {
            /** Escrituras de fondo que la app necesita para seguir viva; ni se frenan ni se espera que se vean. */
            val WRITE_ALLOWLIST = listOf("sesiones/", "latidos", "devices")

            /** Nunca se sabotean: el explorador entra a cada rol con la red ya caída y el login debe funcionar. */
            val SABOTAGE_EXEMPT = listOf("sesiones/")

            private const val NO_META = """{"page":null,"total_pages":null,"total_items":null,"cursor":null}"""
            private val accountedThread = ThreadLocal.withInitial { false }

            /** El sobre de error del backend (utils/envelope.js). Sin `meta`, ApiResponseParser no lee el código. */
            fun errorEnvelope(
                code: String,
                message: String,
            ): String = """{"data":null,"meta":$NO_META,"error":{"code":"$code","message":"$message","details":[]}}"""

            /**
             * Lo que se conecte en este hilo dentro de [block] ya quedó contado en [intercept] o es tráfico del
             * propio arnés: VigiaHttp lo deja salir sin anotarlo ni sabotearlo.
             */
            fun <T> accounted(block: () -> T): T {
                val outer = accountedThread.get()
                accountedThread.set(true)
                try {
                    return block()
                } finally {
                    accountedThread.set(outer)
                }
            }

            fun isAccounted(): Boolean = accountedThread.get()

            private const val MAX_EVENTS = 300
            private const val DEFAULT_DELAY_MILLIS = 4_000L
            private const val DEFAULT_FAIL_PERCENT = 30
            private const val DEFAULT_SEED = 7L
        }
    }

/**
 * Pasa cada llamada de la app por [VigiaNetwork]. Las seis clases que inyectan HttpVaiinillaApiClient directo
 * (DeviceTokenRegistrar, RemoteAuthorizedAccessApi, RemoteAccessEmailApi, SesionesContextoExchange,
 * StaffPresenceCoordinator, RemoteStudentEnrollmentApi) no pasan por aquí: a esas las toma [VigiaHttp] en la
 * conexión, con la misma bitácora, el mismo sabotaje y el mismo solo lectura. Lo que pasa por aquí no se vuelve a
 * contar allá.
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

    override fun patch(
        path: String,
        body: String,
        headers: Map<String, String>,
    ) = network.intercept("PATCH", path) { delegate.patch(path, body, headers) }

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
