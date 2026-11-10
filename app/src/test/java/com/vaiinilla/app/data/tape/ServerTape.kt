package com.vaiinilla.app.data.tape

import com.vaiinilla.app.core.network.ApiResponseParser
import com.vaiinilla.app.core.network.VaiinillaApiClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File
import java.net.URLEncoder
import java.text.Normalizer

/**
 * Cinta de un recorrido contra el servidor de pruebas, una por método de prueba.
 *
 * Sin configurar nada repite la cinta guardada en `src/test/resources/tapes` y exige que la app pida lo
 * mismo y en el mismo orden. Con la variable VAIINILLA_TAPE_SIGNER (un comando que imprime la dirección del
 * servidor, sus tiendas de grabación y una sesión por cuenta) habla con el servidor real y vuelve a grabar.
 * La cinta nunca guarda sesiones: lo que parezca un token se guarda tachado.
 */
class ServerTape : TestRule {
    private var file = File("")
    private var live: LiveServer? = null
    private var recorded = TapeFile()
    private val exchanges = mutableListOf<Exchange>()
    private val values = mutableMapOf<String, String>()
    private val stores = mutableMapOf<String, TapeStore>()
    private var next = 0

    override fun apply(
        base: Statement,
        description: Description,
    ): Statement =
        object : Statement() {
            override fun evaluate() {
                val method =
                    Normalizer
                        .normalize(description.methodName, Normalizer.Form.NFD)
                        .replace(Regex("\\p{M}"), "")
                        .replace(Regex("[^A-Za-z0-9]+"), "-")
                        .trim('-')
                file = File("src/test/resources/tapes/${description.testClass.simpleName}/$method.json")
                live = LiveServer.fromEnvironment()
                if (live == null) {
                    check(
                        file.exists(),
                    ) { "Falta la cinta ${file.path}: hay que grabarla contra el servidor de pruebas." }
                    recorded = FORMAT.decodeFromString(file.readText())
                }
                try {
                    base.evaluate()
                } catch (failure: Throwable) {
                    // Una grabación que falla deja lo que alcanzó a hablar con el servidor, para leerlo.
                    if (live != null) write(File("build/tapes-fallidas/${file.parentFile.name}-${file.name}"))
                    throw failure
                }
                finish()
            }
        }

    /** Datos de una tienda de grabación: en vivo los da el servidor y al repetir salen de la cinta. */
    fun store(name: String): TapeStore =
        stores.getOrPut(name) { live?.stores?.getValue(name) ?: recorded.stores.getValue(name) }

    /** Un valor que el recorrido inventa (un sufijo, una fecha): se guarda para repetirlo igual. */
    fun value(
        key: String,
        fresh: () -> String,
    ): String = values.getOrPut(key) { if (live != null) fresh() else recorded.values.getValue(key) }

    /** El transporte de la app con la sesión de una cuenta de grabación, como `hotel.admin`. */
    fun client(session: String): VaiinillaApiClient = TapeClient(session, bearer = null)

    /** El transporte con una sesión que nació en el recorrido (la del hijo que abre su tutor, un invitado). */
    fun clientWithBearer(
        session: String,
        bearer: String,
    ): VaiinillaApiClient = TapeClient(session, bearer)

    private fun finish() {
        if (live == null) {
            assertEquals("La cinta ${file.name} trae intercambios que la app ya no pide", recorded.exchanges.size, next)
            return
        }
        write(file)
    }

    private fun write(target: File) {
        target.parentFile.mkdirs()
        target.writeText(FORMAT.encodeToString(TapeFile(stores.toSortedMap(), values.toSortedMap(), exchanges)) + "\n")
    }

    private fun exchange(request: Exchange): Result<String> {
        val server = live
        val answered =
            if (server != null) {
                server.send(request).also { exchanges += it.copy(response = it.response?.let(::withoutTokens)) }
            } else {
                val expected = recorded.exchanges.getOrNull(next) ?: fail("La app pidió de más: ${request.summary()}")
                next += 1
                assertEquals("Intercambio $next de ${file.name}", (expected as Exchange).asRequest(), request)
                expected
            }
        val raw = answered.response?.toString() ?: answered.responseText.orEmpty()
        if (answered.status in 200..299) return Result.success(raw)
        return Result.failure(ApiResponseParser().parseError(raw, answered.status))
    }

    private inner class TapeClient(
        private val session: String,
        private val bearer: String?,
    ) : VaiinillaApiClient {
        override val baseUrl: String get() = live?.url ?: "https://cinta.invalid/api/v1/"

        private fun call(
            verb: String,
            path: String,
            query: Map<String, String> = emptyMap(),
            body: String? = null,
            headers: Map<String, String> = emptyMap(),
            explicitBearer: String? = null,
            upload: Upload? = null,
        ): Result<String> =
            exchange(
                Exchange(
                    session = session,
                    verb = verb,
                    path = path.trimStart('/'),
                    query = query,
                    body = upload?.describe() ?: body?.let(Json::parseToJsonElement),
                    idempotent = headers.keys.any { it.equals("Idempotency-Key", ignoreCase = true) },
                ).also {
                    it.headers = headers
                    it.bearer = explicitBearer ?: bearer
                    it.upload = upload
                },
            )

        override fun get(
            path: String,
            query: Map<String, String>,
        ) = call("GET", path, query)

        override fun post(
            path: String,
            body: String,
            headers: Map<String, String>,
        ) = call("POST", path, body = body, headers = headers)

        override fun postWithoutBody(
            path: String,
            headers: Map<String, String>,
        ) = call("POST", path, headers = headers)

        override fun delete(
            path: String,
            body: String?,
            headers: Map<String, String>,
        ) = call("DELETE", path, body = body, headers = headers)

        override fun patch(
            path: String,
            body: String,
            headers: Map<String, String>,
        ) = call("PATCH", path, body = body, headers = headers)

        override fun put(
            path: String,
            body: String,
            headers: Map<String, String>,
        ) = call("PUT", path, body = body, headers = headers)

        override fun getWithBearer(
            bearer: String,
            path: String,
            query: Map<String, String>,
        ) = call("GET", path, query, explicitBearer = bearer)

        override fun postWithBearer(
            bearer: String,
            path: String,
            body: String?,
            headers: Map<String, String>,
        ) = call("POST", path, body = body, headers = headers, explicitBearer = bearer)

        override fun deleteWithBearer(
            bearer: String,
            path: String,
            body: String?,
            headers: Map<String, String>,
        ) = call("DELETE", path, body = body, headers = headers, explicitBearer = bearer)

        override fun putMultipart(
            path: String,
            fieldName: String,
            filename: String,
            mimeType: String,
            bytes: ByteArray,
            headers: Map<String, String>,
        ) = call("PUT", path, headers = headers, upload = Upload(fieldName, filename, mimeType, bytes))

        override fun postMultipart(
            path: String,
            fieldName: String,
            filename: String,
            mimeType: String,
            bytes: ByteArray,
            headers: Map<String, String>,
        ) = call("POST", path, headers = headers, upload = Upload(fieldName, filename, mimeType, bytes))
    }

    private companion object {
        val FORMAT =
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }
        val TOKEN = Regex("^eyJ[\\w-]+\\.[\\w-]+\\.[\\w-]+$")

        fun withoutTokens(element: JsonElement): JsonElement =
            when (element) {
                is JsonObject -> JsonObject(element.mapValues { withoutTokens(it.value) })
                is JsonArray -> JsonArray(element.map(::withoutTokens))
                is JsonPrimitive ->
                    if (element.isString &&
                        TOKEN.matches(element.content)
                    ) {
                        JsonPrimitive("TACHADO")
                    } else {
                        element
                    }
            }
    }
}

@Serializable
data class TapeStore(
    val slug: String,
    val id: String,
    val joinCode: String? = null,
    val users: Map<String, String> = emptyMap(),
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
internal data class TapeFile(
    val stores: Map<String, TapeStore> = emptyMap(),
    val values: Map<String, String> = emptyMap(),
    val exchanges: List<Exchange> = emptyList(),
)

internal class Upload(
    val field: String,
    val filename: String,
    val mimeType: String,
    val bytes: ByteArray,
) {
    fun describe(): JsonElement =
        buildJsonObject {
            put("multipart", field)
            put("filename", filename)
            put("mime", mimeType)
            put("bytes", bytes.size)
        }
}

@Serializable
internal data class Exchange(
    val session: String,
    val verb: String,
    val path: String,
    val query: Map<String, String> = emptyMap(),
    val body: JsonElement? = null,
    val idempotent: Boolean = false,
    val status: Int = 0,
    val response: JsonElement? = null,
    val responseText: String? = null,
) {
    @kotlinx.serialization.Transient
    var headers: Map<String, String> = emptyMap()

    @kotlinx.serialization.Transient
    var bearer: String? = null

    @kotlinx.serialization.Transient
    var upload: Upload? = null

    fun asRequest() = copy(status = 0, response = null, responseText = null)

    fun summary() = "$session $verb $path"
}

/** El servidor de pruebas de verdad, hablado con curl: el classpath de Android no trae PATCH ni java.net.http. */
internal class LiveServer(
    val url: String,
    val stores: Map<String, TapeStore>,
    private val sessions: Map<String, String>,
) {
    fun send(request: Exchange): Exchange {
        val bearer = request.bearer ?: sessions.getValue(request.session)
        val query =
            request.query.entries
                .joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }
                .let { if (it.isEmpty()) "" else "?$it" }
        val command = mutableListOf("curl", "-s", "-m", "60", "-X", request.verb, "-w", "\n%{http_code}", "-K", "-")
        request.headers.forEach { (key, value) -> command += listOf("-H", "$key: $value") }
        var temporary: File? = null
        val upload = request.upload
        if (upload != null) {
            temporary = File.createTempFile("cinta", ".bin").apply { writeBytes(upload.bytes) }
            command +=
                listOf("-F", "${upload.field}=@${temporary.path};type=${upload.mimeType};filename=${upload.filename}")
        } else if (request.body != null) {
            command += listOf("-H", "Content-Type: application/json", "--data-binary", request.body.toString())
        }
        command += url + request.path + query
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        // La sesión viaja por la entrada de curl para que no quede en la lista de procesos.
        process.outputStream.use { it.write("header = \"Authorization: Bearer $bearer\"\n".toByteArray()) }
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        temporary?.delete()
        val text = output.substringBeforeLast("\n")
        val json =
            runCatching { Json.parseToJsonElement(text) }.getOrNull()?.takeIf {
                it is JsonObject ||
                    it is JsonArray
            }
        return request.copy(
            status = output.substringAfterLast("\n").trim().toIntOrNull() ?: 0,
            response = json,
            responseText = if (json == null) text else null,
        )
    }

    companion object {
        private var cached: LiveServer? = null

        fun fromEnvironment(): LiveServer? {
            val signer = System.getenv("VAIINILLA_TAPE_SIGNER")?.takeIf { it.isNotBlank() } ?: return null
            cached?.let { return it }
            val process = ProcessBuilder("sh", "-c", signer).redirectError(File("/dev/null")).start()
            val output = process.inputStream.bufferedReader().readText()
            check(process.waitFor() == 0) { "El comando de VAIINILLA_TAPE_SIGNER falló." }
            val root = Json.parseToJsonElement(output).jsonObject
            val stores =
                root.getValue("tiendas").jsonObject.mapValues { (_, raw) ->
                    val store = raw.jsonObject
                    TapeStore(
                        slug = store.getValue("slug").jsonPrimitive.content,
                        id = store.getValue("establecimiento").jsonPrimitive.content,
                        joinCode = (store["codigo_union"] as? JsonPrimitive)?.takeIf { it.isString }?.content,
                        users = store.getValue("usuarios").jsonObject.mapValues { it.value.jsonPrimitive.content },
                        latitude =
                            (store["ubicacion"] as? JsonObject)
                                ?.get("lat")
                                ?.jsonPrimitive
                                ?.content
                                ?.toDouble(),
                        longitude =
                            (store["ubicacion"] as? JsonObject)
                                ?.get("lng")
                                ?.jsonPrimitive
                                ?.content
                                ?.toDouble(),
                    )
                }
            val sessions = root.getValue("sesiones").jsonObject.mapValues { it.value.jsonPrimitive.content }
            return LiveServer(root.getValue("url").jsonPrimitive.content, stores, sessions).also { cached = it }
        }
    }
}
