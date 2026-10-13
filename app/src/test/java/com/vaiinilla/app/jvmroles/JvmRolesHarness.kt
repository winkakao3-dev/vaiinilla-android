package com.vaiinilla.app.jvmroles

import android.os.Looper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.vaiinilla.app.MainActivity
import com.vaiinilla.app.core.auth.VaiinillaJwtRefreshCoordinator
import com.vaiinilla.app.domain.model.GeoCoordinates
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLog
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * La app completa en la JVM, manejada por comandos (`vbuild jvm-roles` la lanza sin Gradle vivo y
 * `vaiinilla-roles/android/jvm-ui.py` le habla). Un solo proceso para los 4 roles: `role <rol>` cierra
 * la actividad, cambia la persona del sandbox y abre una nueva, que entra a su modo con el flujo real.
 * En una corrida normal de tests se salta.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(
    application = HiltTestApplication::class,
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class JvmRolesHarness {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Inject
    lateinit var identities: SandboxIdentities

    @Inject
    lateinit var refreshCoordinator: VaiinillaJwtRefreshCoordinator

    @Inject
    lateinit var location: FakeLocationProvider

    private var scenario: ActivityScenario<MainActivity>? = null

    @Test
    fun serve() {
        assumeTrue("Solo con -Djvmroles.serve=true (lo lanza vbuild jvm-roles)", System.getProperty("jvmroles.serve") == "true")
        hilt.inject()
        ShadowLog.stream = System.out
        // El reloj de Compose lo mueve el arnés: con avance automático, una animación infinita no deja terminar nada.
        compose.mainClock.autoAdvance = false
        val stateFile = File(System.getProperty("jvmroles.stateFile") ?: error("Falta -Djvmroles.stateFile"))
        CommandServer(stateFile).use { server ->
            println("jvm-roles listo en ${stateFile.path}")
            while (true) {
                val command = server.queue.poll(IDLE_STEP_MILLIS, TimeUnit.MILLISECONDS)
                if (command == null) {
                    // Sin comandos la app sigue viva: el polling y las animaciones avanzan a tiempo real.
                    pump(IDLE_STEP_MILLIS, realTime = false)
                    continue
                }
                println("> ${command.name} ${command.args.joinToString(" ")}")
                val reply =
                    runCatching { CommandServer.Reply(true, run(command)) }
                        .getOrElse { error -> CommandServer.Reply(false, error.message ?: error.toString()) }
                command.reply.complete(reply)
                if (command.name == "stop") break
            }
        }
        scenario?.close()
    }

    private fun run(command: CommandServer.Command): String {
        val args = command.args
        return when (command.name) {
            "role" -> switchRole(args)
            "cual" -> "${ActiveRole.current?.cliName ?: "nada"}: ${summary()}"
            "list" -> listNodes()
            "tap" -> tap(args.joinToString(" "))
            "text", "input" -> input(args.joinToString(" "))
            "wait" -> {
                pump(args.firstOrNull()?.toLong() ?: SETTLE_MILLIS, realTime = true)
                summary()
            }
            "advance" -> {
                pump(args.firstOrNull()?.toLong() ?: SETTLE_MILLIS, realTime = false)
                summary()
            }
            "shot" -> shot(args.firstOrNull() ?: error("Uso: shot <ruta.png>"))
            "location" -> {
                location.coordinates = GeoCoordinates(args[0].toDouble(), args[1].toDouble())
                "ubicación ${args[0]}, ${args[1]}"
            }
            "stop" -> "adiós"
            else -> error("No conozco '${command.name}'. Comandos: role, cual, list, tap, text, wait, advance, shot, location, stop.")
        }
    }

    private fun switchRole(args: List<String>): String {
        val role =
            HarnessRole.parse(args.firstOrNull().orEmpty())
                ?: error("Rol desconocido. Roles: ${HarnessRole.entries.joinToString { it.cliName }}.")
        scenario?.close()
        scenario = null
        // El coordinador refresca en segundo plano el JWT del rol anterior; se para antes de cambiar de persona.
        refreshCoordinator.clearSession()
        ActiveRole.current = role
        identities.signIn(role)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        if (role != HarnessRole.ALUMNO) {
            // Con modos de personal el arranque cae en la lista de modos: se entra como lo haría la persona.
            val enter = hasText("Entrar como ${role.operationalRole.label}", ignoreCase = true) and hasClickAction()
            if (!waitFor(ROLE_TIMEOUT_MILLIS) { exists(enter) }) {
                error("No apareció \"Entrar como ${role.operationalRole.label}\". En pantalla: ${summary()}")
            }
            compose.onAllNodes(enter)[0].performClick()
            if (!waitFor(ROLE_TIMEOUT_MILLIS) { !exists(enter) }) {
                error("Tocó \"Entrar como ${role.operationalRole.label}\" pero la app no cambió de pantalla: ${summary()}")
            }
        }
        pump(SETTLE_MILLIS, realTime = true)
        return "al frente ${role.cliName}: ${summary()}"
    }

    private fun tap(text: String): String {
        val matcher = (hasText(text, substring = true, ignoreCase = true) or hasContentDescription(text, substring = true, ignoreCase = true)) and hasClickAction()
        if (!exists(matcher)) error("No hay nada tocable con \"$text\". En pantalla: ${summary()}")
        val label = labelOf(compose.onAllNodes(matcher)[0].fetchSemanticsNode())
        compose.onAllNodes(matcher)[0].performClick()
        pump(SETTLE_MILLIS, realTime = true)
        return "tap '$label'"
    }

    private fun input(text: String): String {
        val focused = hasSetTextAction() and isFocused()
        val matcher = if (exists(focused)) focused else hasSetTextAction()
        if (!exists(matcher)) error("No hay campo de texto en pantalla.")
        compose.onAllNodes(matcher)[0].performTextInput(text)
        pump(SETTLE_MILLIS / 2, realTime = false)
        return "texto enviado"
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun shot(path: String): String {
        File(path).parentFile?.mkdirs()
        captureScreenRoboImage(path)
        return path
    }

    private fun listNodes(): String =
        visibleNodes()
            .mapNotNull { node ->
                val label = labelOf(node).takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val b = node.boundsInWindow
                val click = if (SemanticsActions.OnClick in node.config) " CLICK" else ""
                "'${label.take(100)}' [${b.left.toInt()},${b.top.toInt()}][${b.right.toInt()},${b.bottom.toInt()}]$click"
            }.joinToString("\n")
            .ifEmpty { "(sin textos en pantalla)" }

    private fun summary(): String =
        runCatching {
            visibleNodes().map(::labelOf).filter { it.isNotBlank() }.take(SUMMARY_TEXTS).joinToString(" · ")
        }.getOrDefault("(sin pantalla)")

    private fun visibleNodes(): List<SemanticsNode> {
        fun walk(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::walk)
        return compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired = false).flatMap(::walk)
    }

    private fun labelOf(node: SemanticsNode): String {
        val config = node.config
        return config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }
            ?: config.getOrNull(SemanticsProperties.EditableText)?.text
            ?: config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ")
            ?: ""
    }

    private fun exists(matcher: SemanticsMatcher): Boolean =
        compose.onAllNodes(matcher).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()

    private fun waitFor(
        timeoutMillis: Long,
        condition: () -> Boolean,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            pump(POLL_STEP_MILLIS, realTime = true)
            if (condition()) return true
        }
        return false
    }

    /** Mueve el reloj de Compose y el looper principal; con [realTime], también deja correr la red. */
    private fun pump(
        millis: Long,
        realTime: Boolean,
    ) {
        var left = millis
        while (left > 0) {
            val step = minOf(FRAME_STEP_MILLIS, left)
            if (realTime) Thread.sleep(step)
            if (scenario != null) compose.mainClock.advanceTimeBy(step)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(step))
            left -= step
        }
    }

    private companion object {
        const val IDLE_STEP_MILLIS = 100L
        const val FRAME_STEP_MILLIS = 50L
        const val POLL_STEP_MILLIS = 250L
        const val SETTLE_MILLIS = 1_500L
        const val ROLE_TIMEOUT_MILLIS = 30_000L
        const val SUMMARY_TEXTS = 6
    }
}
