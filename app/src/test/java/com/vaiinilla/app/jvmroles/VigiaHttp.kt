package com.vaiinilla.app.jvmroles

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.ApiResponseParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.Proxy
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.security.cert.Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

/**
 * Las conexiones http/https de la JVM, con [install] (URL.setURLStreamHandlerFactory). Hace dos cosas:
 * 1. En Android HttpURLConnection es OkHttp y un 401 a un POST vuelve como 401. El del JDK, en modo streaming
 *    (setFixedLengthStreamingMode), no puede reintentar la autenticación y getInputStream lanza HttpRetryException:
 *    la app nunca veía el 401, ni refrescaba el JWT ni leía EMAIL_NOT_VERIFIED. Aquí el modo streaming se ignora y
 *    el JDK guarda el cuerpo en memoria antes de mandarlo.
 * 2. Las peticiones a la API de dev que no vienen de VigiaApiClient (clases que inyectan HttpVaiinillaApiClient
 *    directo) pasan por [VigiaNetwork.intercept]: quedan en la bitácora, se sabotean y respetan el solo lectura.
 *    Lo que ya viene de VigiaApiClient, o del propio arnés ([VigiaNetwork.accounted]), sale tal cual.
 */
class VigiaHttp(
    private val network: VigiaNetwork,
    apiBaseUrl: String,
) {
    private val apiBase = apiBaseUrl.trimEnd('/') + "/"
    private val parser = ApiResponseParser()

    /** Un manejador de [protocol] atado a esta instancia, sin instalar nada: las pruebas arman URLs con él. */
    fun handler(protocol: String): URLStreamHandler = Handler(protocol) { this }

    private fun open(
        url: URL,
        proxy: Proxy?,
    ): URLConnection {
        val spec = url.toExternalForm()
        val builtin = URL(builtins.getValue(url.protocol), spec)
        val connection =
            (if (proxy == null) builtin.openConnection() else builtin.openConnection(proxy)) as HttpURLConnection
        if (VigiaNetwork.isAccounted() || !spec.startsWith(apiBase)) return DelegatingConnection(url, connection)
        val path = spec.removePrefix(apiBase).substringBefore('?')
        return WatchedConnection(url, connection, network, parser, path)
    }

    private class Handler(
        private val protocol: String,
        private val owner: () -> VigiaHttp,
    ) : URLStreamHandler() {
        override fun openConnection(u: URL): URLConnection = owner().open(u, null)

        override fun openConnection(
            u: URL,
            p: Proxy,
        ): URLConnection = owner().open(u, p)

        override fun getDefaultPort(): Int = if (protocol == "https") 443 else 80
    }

    companion object {
        // Los manejadores del JDK no son accesibles en el JDK 17 (sun.net.www no se exporta). Una URL creada con una
        // de estas como contexto hereda su manejador, así que se toman antes de instalar la fábrica.
        private val builtins = mapOf("http" to URL("http://localhost/"), "https" to URL("https://localhost/"))

        @Volatile
        private var current: VigiaHttp? = null
        private var installed = false

        /**
         * La fábrica se puede poner una sola vez por JVM; si otro (u otro sandbox de Robolectric) ya la puso,
         * devuelve false y las conexiones siguen como el JDK las da. Llamarla otra vez solo cambia la red y la base.
         */
        @Synchronized
        fun install(
            network: VigiaNetwork,
            apiBaseUrl: String,
        ): Boolean {
            current = VigiaHttp(network, apiBaseUrl)
            if (!installed) {
                installed =
                    try {
                        URL.setURLStreamHandlerFactory { protocol ->
                            if (protocol in builtins) Handler(protocol) { checkNotNull(current) } else null
                        }
                        true
                    } catch (alreadySet: Error) {
                        false
                    }
            }
            return installed
        }
    }
}

/**
 * Delega todo en la conexión del JDK salvo el modo streaming. Extiende HttpsURLConnection para que un cast a HTTPS
 * siga funcionando; con http los métodos de TLS no aplican.
 */
private open class DelegatingConnection(
    url: URL,
    protected val delegate: HttpURLConnection,
) : HttpsURLConnection(url) {
    override fun setFixedLengthStreamingMode(contentLength: Int) = Unit

    override fun setFixedLengthStreamingMode(contentLength: Long) = Unit

    override fun setChunkedStreamingMode(chunklen: Int) = Unit

    override fun connect() = delegate.connect()

    override fun disconnect() = delegate.disconnect()

    override fun usingProxy(): Boolean = delegate.usingProxy()

    override fun getRequestMethod(): String = delegate.getRequestMethod()

    override fun setRequestMethod(method: String?) {
        delegate.setRequestMethod(method)
    }

    override fun getDoOutput(): Boolean = delegate.getDoOutput()

    override fun setDoOutput(dooutput: Boolean) {
        delegate.setDoOutput(dooutput)
    }

    override fun getDoInput(): Boolean = delegate.getDoInput()

    override fun setDoInput(doinput: Boolean) {
        delegate.setDoInput(doinput)
    }

    override fun getUseCaches(): Boolean = delegate.getUseCaches()

    override fun setUseCaches(usecaches: Boolean) {
        delegate.setUseCaches(usecaches)
    }

    override fun getConnectTimeout(): Int = delegate.getConnectTimeout()

    override fun setConnectTimeout(timeout: Int) {
        delegate.setConnectTimeout(timeout)
    }

    override fun getReadTimeout(): Int = delegate.getReadTimeout()

    override fun setReadTimeout(timeout: Int) {
        delegate.setReadTimeout(timeout)
    }

    override fun getInstanceFollowRedirects(): Boolean = delegate.getInstanceFollowRedirects()

    override fun setInstanceFollowRedirects(followRedirects: Boolean) {
        delegate.setInstanceFollowRedirects(followRedirects)
    }

    override fun getIfModifiedSince(): Long = delegate.getIfModifiedSince()

    override fun setIfModifiedSince(ifmodifiedsince: Long) {
        delegate.setIfModifiedSince(ifmodifiedsince)
    }

    override fun setRequestProperty(
        key: String?,
        value: String?,
    ) = delegate.setRequestProperty(key, value)

    override fun addRequestProperty(
        key: String?,
        value: String?,
    ) = delegate.addRequestProperty(key, value)

    override fun getRequestProperty(key: String?): String? = delegate.getRequestProperty(key)

    override fun getRequestProperties(): MutableMap<String, MutableList<String>> = delegate.getRequestProperties()

    override fun getOutputStream(): OutputStream = delegate.getOutputStream()

    override fun getInputStream(): InputStream = delegate.getInputStream()

    override fun getErrorStream(): InputStream? = delegate.getErrorStream()

    override fun getResponseCode(): Int = delegate.getResponseCode()

    override fun getResponseMessage(): String? = delegate.getResponseMessage()

    override fun getHeaderField(name: String?): String? = delegate.getHeaderField(name)

    override fun getHeaderField(n: Int): String? = delegate.getHeaderField(n)

    override fun getHeaderFieldKey(n: Int): String? = delegate.getHeaderFieldKey(n)

    override fun getHeaderFields(): MutableMap<String, MutableList<String>> = delegate.getHeaderFields()

    override fun getCipherSuite(): String = tls().getCipherSuite()

    override fun getLocalCertificates(): Array<Certificate>? = tls().getLocalCertificates()

    override fun getServerCertificates(): Array<Certificate> = tls().getServerCertificates()

    override fun getHostnameVerifier(): HostnameVerifier =
        (delegate as? HttpsURLConnection)?.getHostnameVerifier() ?: super.getHostnameVerifier()

    override fun setHostnameVerifier(v: HostnameVerifier?) {
        (delegate as? HttpsURLConnection)?.setHostnameVerifier(v)
    }

    override fun getSSLSocketFactory(): SSLSocketFactory =
        (delegate as? HttpsURLConnection)?.getSSLSocketFactory() ?: super.getSSLSocketFactory()

    override fun setSSLSocketFactory(sf: SSLSocketFactory?) {
        (delegate as? HttpsURLConnection)?.setSSLSocketFactory(sf)
    }

    private fun tls(): HttpsURLConnection =
        delegate as? HttpsURLConnection ?: throw IllegalStateException("Conexión sin TLS: ${getURL()}")
}

/**
 * Una petición a la API de dev que no pasó por VigiaApiClient. El cuerpo se queda en memoria y la petición sale
 * entera con la primera lectura de la respuesta, dentro de [VigiaNetwork.intercept]: si el vigía la sabotea o la
 * frena (solo lectura), no toca la red. Un error del servidor inventado llega como 500/503 con el sobre del backend.
 */
private class WatchedConnection(
    url: URL,
    delegate: HttpURLConnection,
    private val network: VigiaNetwork,
    private val parser: ApiResponseParser,
    private val path: String,
) : DelegatingConnection(url, delegate) {
    private class Response(
        val status: Int,
        val body: ByteArray,
        val real: Boolean,
    )

    private var requestBody: ByteArrayOutputStream? = null
    private var response: Response? = null
    private var failure: IOException? = null

    // La conexión real se abre al pedir la respuesta, después de que el vigía decide.
    override fun connect() = Unit

    override fun getOutputStream(): OutputStream {
        if (!getDoOutput()) throw ProtocolException("cannot write to a URLConnection if doOutput=false")
        return requestBody ?: ByteArrayOutputStream().also { requestBody = it }
    }

    override fun getResponseCode(): Int = exchanged().status

    override fun getResponseMessage(): String? = if (exchanged().real) delegate.getResponseMessage() else null

    override fun getInputStream(): InputStream {
        val status = exchanged().status
        if (status == HttpURLConnection.HTTP_NOT_FOUND || status == HttpURLConnection.HTTP_GONE) {
            throw FileNotFoundException(getURL().toString())
        }
        if (status >= HttpURLConnection.HTTP_BAD_REQUEST) {
            throw IOException("Server returned HTTP response code: $status for URL: ${getURL()}")
        }
        return ByteArrayInputStream(exchanged().body)
    }

    override fun getErrorStream(): InputStream? =
        response
            ?.takeIf { it.status >= HttpURLConnection.HTTP_BAD_REQUEST && it.body.isNotEmpty() }
            ?.let { ByteArrayInputStream(it.body) }

    // Como el JDK: leer cabeceras no lanza; si la petición falló, no hay cabeceras.
    override fun getHeaderField(name: String?): String? {
        val current = exchangedOrNull() ?: return null
        if (current.real) return delegate.getHeaderField(name)
        return if (name.equals("Content-Type", ignoreCase = true)) JSON else null
    }

    override fun getHeaderField(n: Int): String? =
        exchangedOrNull()?.takeIf { it.real }?.let { delegate.getHeaderField(n) }

    override fun getHeaderFieldKey(n: Int): String? =
        exchangedOrNull()?.takeIf { it.real }?.let { delegate.getHeaderFieldKey(n) }

    override fun getHeaderFields(): MutableMap<String, MutableList<String>> {
        val current = exchangedOrNull() ?: return mutableMapOf()
        if (current.real) return delegate.getHeaderFields()
        return mutableMapOf("Content-Type" to mutableListOf(JSON))
    }

    private fun exchangedOrNull(): Response? = runCatching { exchanged() }.getOrNull()

    private fun exchanged(): Response {
        response?.let { return it }
        failure?.let { throw it }
        val result = network.intercept(getRequestMethod(), path, ::send)
        // send() corrió: es la respuesta del servidor, aunque sea un error.
        response?.let { return it }
        val error = result.exceptionOrNull()
        if (error is ApiClientException) {
            val body = VigiaNetwork.errorEnvelope(error.code, error.message.orEmpty())
            return Response(error.httpStatus, body.toByteArray(Charsets.UTF_8), real = false).also { response = it }
        }
        throw (error as? IOException ?: IOException(error?.message, error)).also { failure = it }
    }

    private fun send(): Result<String> =
        try {
            requestBody?.let { body -> delegate.getOutputStream().use { it.write(body.toByteArray()) } }
            val status = delegate.getResponseCode()
            val failed = status >= HttpURLConnection.HTTP_BAD_REQUEST
            val stream = if (failed) delegate.getErrorStream() else delegate.getInputStream()
            val body = stream?.use { it.readBytes() } ?: ByteArray(0)
            response = Response(status, body, real = true)
            if (failed) {
                Result.failure(parser.parseError(String(body, Charsets.UTF_8), status))
            } else {
                Result.success("")
            }
        } catch (error: IOException) {
            Result.failure(error)
        }

    private companion object {
        const val JSON = "application/json; charset=utf-8"
    }
}
