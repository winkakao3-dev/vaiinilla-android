package com.vaiinilla.app.jvmroles

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

/**
 * Recibe comandos en 127.0.0.1 (una línea JSON por conexión) y los deja en [queue]: las APIs de
 * Compose solo corren en el hilo del test, que es quien los ejecuta y contesta.
 * El puerto y un token aleatorio quedan en [stateFile], legible solo por el dueño, porque el
 * arnés tiene sesiones reales de dev.
 */
class CommandServer(
    private val stateFile: File,
) : AutoCloseable {
    class Command(
        val name: String,
        val args: List<String>,
    ) {
        val reply = CompletableFuture<Reply>()
    }

    data class Reply(
        val ok: Boolean,
        val out: String,
    )

    val queue = LinkedBlockingQueue<Command>()
    private val token = UUID.randomUUID().toString()
    private val server = ServerSocket(0, 8, InetAddress.getLoopbackAddress())

    init {
        stateFile.parentFile?.mkdirs()
        stateFile.delete()
        Files.createFile(
            stateFile.toPath(),
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")),
        )
        stateFile.writeText(
            JSONObject()
                .put("port", server.localPort)
                .put("token", token)
                .toString(),
        )
        thread(isDaemon = true, name = "jvm-roles-server") { acceptLoop() }
    }

    private fun acceptLoop() {
        while (!server.isClosed) {
            val client = runCatching { server.accept() }.getOrNull() ?: break
            thread(isDaemon = true, name = "jvm-roles-client") { handle(client) }
        }
    }

    private fun handle(client: Socket) =
        client.use {
            val reply =
                runCatching {
                    val request = JSONObject(it.getInputStream().bufferedReader().readLine())
                    if (request.optString("token") != token) {
                        Reply(false, "Token inválido.")
                    } else {
                        val args = request.optJSONArray("args") ?: JSONArray()
                        val command = Command(request.getString("cmd"), List(args.length()) { i -> args.getString(i) })
                        queue.put(command)
                        command.reply.get()
                    }
                }.getOrElse { error -> Reply(false, error.message ?: error.toString()) }
            val writer = it.getOutputStream().bufferedWriter()
            writer.write(JSONObject().put("ok", reply.ok).put("out", reply.out).toString())
            writer.newLine()
            writer.flush()
        }

    override fun close() {
        server.close()
        stateFile.delete()
    }
}
