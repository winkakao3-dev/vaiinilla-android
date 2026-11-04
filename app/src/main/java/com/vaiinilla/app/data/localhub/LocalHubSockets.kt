package com.vaiinilla.app.data.localhub

import com.vaiinilla.app.domain.localhub.LocalHubRules
import com.vaiinilla.app.domain.localhub.LocalHubSigning
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * El servidor que corre dentro de la app de Caja. Socket TCP con una línea JSON por petición y otra de respuesta: no
 * es HTTP porque Android bloquea HTTP sin cifrar, y la seguridad la da la firma de [LocalHubSigning].
 */
class LocalHubServer(
    private val handler: LocalHubHandler,
    private val port: Int = LocalHubRules.PORT,
) {
    private var socket: ServerSocket? = null
    private var workers: ExecutorService? = null

    /** El puerto real (0 pide uno libre en las pruebas). */
    val boundPort: Int get() = socket?.localPort ?: -1

    @Synchronized
    fun start() {
        if (socket != null) return
        val server = ServerSocket().apply {
            reuseAddress = true
            bind(InetSocketAddress(port))
        }
        val pool = Executors.newFixedThreadPool(WORKERS)
        socket = server
        workers = pool
        Thread({ acceptLoop(server, pool) }, "vaiinilla-local-hub").apply {
            isDaemon = true
            start()
        }
    }

    @Synchronized
    fun stop() {
        runCatching { socket?.close() }
        workers?.shutdownNow()
        socket = null
        workers = null
    }

    private fun acceptLoop(
        server: ServerSocket,
        pool: ExecutorService,
    ) {
        while (!server.isClosed) {
            val client =
                try {
                    server.accept()
                } catch (_: SocketException) {
                    return
                }
            runCatching { pool.execute { serve(client) } }.onFailure { runCatching { client.close() } }
        }
    }

    private fun serve(client: Socket) {
        client.use { connection ->
            connection.soTimeout = READ_TIMEOUT_MS
            val reader = BufferedReader(InputStreamReader(connection.getInputStream(), Charsets.UTF_8))
            val line = readBoundedLine(reader) ?: return
            val writer = OutputStreamWriter(connection.getOutputStream(), Charsets.UTF_8)
            writer.write(handler.handle(line))
            writer.write("\n")
            writer.flush()
        }
    }

    private companion object {
        const val WORKERS = 4
        const val READ_TIMEOUT_MS = 5_000
    }
}

/** Lo que usan Mesero y Cocina para hablar con la Caja del local. */
class LocalHubClient(
    private val connectTimeoutMs: Int = 2_000,
    private val readTimeoutMs: Int = 5_000,
) {
    fun call(
        host: String,
        port: Int,
        key: String,
        operation: String,
        payload: String,
        nowMs: Long = System.currentTimeMillis(),
    ): Result<LocalHubJson.Response> =
        runCatching {
            val request =
                LocalHubJson.Request(
                    op = operation,
                    ts = nowMs,
                    firma = LocalHubSigning.sign(key, nowMs, operation, payload),
                    payload = payload,
                )
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
                socket.soTimeout = readTimeoutMs
                val writer = OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)
                writer.write(LocalHubJson.encodeRequest(request))
                writer.write("\n")
                writer.flush()
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                val line = readBoundedLine(reader) ?: error("El concentrador no respondió")
                LocalHubJson.decodeResponse(line)
            }
        }
}

/** Una línea, cortada si pasa del tamaño máximo (nadie manda comandas de megas). */
private fun readBoundedLine(reader: BufferedReader): String? {
    val builder = StringBuilder()
    while (true) {
        val next = reader.read()
        if (next == -1) return builder.takeIf { it.isNotEmpty() }?.toString()
        if (next == '\n'.code) return builder.toString()
        if (builder.length >= LocalHubRules.MAXIMUM_MESSAGE_BYTES) return null
        builder.append(next.toChar())
    }
}
