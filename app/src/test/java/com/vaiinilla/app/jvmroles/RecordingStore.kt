package com.vaiinilla.app.jvmroles

import org.json.JSONObject
import java.io.File

/**
 * La tienda oculta del backend de pruebas donde corre el arnés cuando se lanza con `-Djvmroles.tienda`. Sus
 * personas son cuentas de mentira de esa tienda; `-Djvmroles.entradas` es el comando que le pide al servidor una
 * entrada de Firebase de un solo uso para cada una. Sin esas propiedades el arnés sigue en el sandbox de dev.
 */
class RecordingStore(
    val instance: String,
    val slug: String,
    val name: String,
    private val entriesCommand: String,
) {
    private var entries: JSONObject? = null
    private var fetchedAt = 0L

    /** Las entradas vencen en una hora: se piden de nuevo pasada media. Nunca se imprimen ni se guardan. */
    @Synchronized
    fun customToken(role: HarnessRole): String {
        val stale = System.currentTimeMillis() - fetchedAt > ENTRIES_FRESH_MILLIS
        val current = entries?.takeUnless { stale } ?: fetch().also { entries = it }
        val account = "$instance.${role.storeAccount}"
        return current
            .optString(
                account,
            ).ifEmpty { error("La tienda $instance no tiene la cuenta ${role.storeAccount}.") }
    }

    private fun fetch(): JSONObject {
        val process =
            ProcessBuilder("/bin/sh", "-c", entriesCommand)
                .redirectError(File("/dev/null"))
                .redirectInput(File("/dev/null"))
                .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        check(process.waitFor() == 0) { "El comando de -Djvmroles.entradas falló." }
        fetchedAt = System.currentTimeMillis()
        return JSONObject(output).getJSONObject("entradas")
    }

    companion object {
        private const val ENTRIES_FRESH_MILLIS = 30 * 60_000L

        val active: RecordingStore? by lazy {
            val instance = System.getProperty("jvmroles.tienda")?.takeIf { it.isNotBlank() } ?: return@lazy null
            RecordingStore(
                instance = instance,
                slug = System.getProperty("jvmroles.tienda.slug") ?: error("Falta -Djvmroles.tienda.slug"),
                name = System.getProperty("jvmroles.tienda.nombre") ?: error("Falta -Djvmroles.tienda.nombre"),
                entriesCommand = System.getProperty("jvmroles.entradas") ?: error("Falta -Djvmroles.entradas"),
            )
        }
    }
}
