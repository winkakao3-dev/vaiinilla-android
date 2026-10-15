package com.vaiinilla.app.jvmroles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.core.network.ApiResponseParser
import com.vaiinilla.app.ui.components.SkeletonBlock
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class VigiaTest {
    @get:Rule
    val compose = createComposeRule()

    private val network = VigiaNetwork(ApiResponseParser())
    private lateinit var vigia: Vigia

    @Before
    fun setUp() {
        // Igual que el arnés: con avance automático, una animación infinita no deja que la pantalla quede quieta.
        compose.mainClock.autoAdvance = false
        vigia =
            Vigia(network, ::nodes) { node -> node.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") ?: "" }
        vigia.loadingLimitMillis = LIMIT
    }

    @Test
    fun `a spinner with nothing pending on the network is an endless load`() {
        compose.setContent { CircularProgressIndicator() }

        tick(LIMIT + 500)

        assertEquals(listOf("carga-eterna:error"), kinds())
    }

    @Test
    fun `a skeleton counts as loading too`() {
        compose.setContent { SkeletonBlock(Modifier.size(120.dp, 40.dp)) }

        tick(LIMIT + 500)

        assertEquals(listOf("carga-eterna:error"), kinds())
    }

    @Test
    fun `a spinner waiting on the network is fine until it takes far too long`() {
        compose.setContent { CircularProgressIndicator() }
        val release = CountDownLatch(1)
        val started = CountDownLatch(1)
        val request =
            thread {
                network.intercept("GET", "pedidos") {
                    started.countDown()
                    release.await()
                    Result.success("{}")
                }
            }
        started.await()

        tick(LIMIT * 2)
        assertEquals(emptyList<String>(), kinds())

        tick(LIMIT * 2)
        assertEquals(listOf("carga-larga:sospecha"), kinds())

        release.countDown()
        request.join()
    }

    @Test
    fun `a failed write that leaves the screen unchanged is a silent error`() {
        compose.setContent { Text("Carrito") }
        tick(500)
        network.configure(listOf("caido"))

        network.intercept("POST", "pedidos") { Result.success("{}") }
        tick(6_000)

        assertEquals(listOf("error-mudo:error"), kinds())
    }

    @Test
    fun `a failed write that shows a message is not reported`() {
        val message = mutableStateOf("")
        compose.setContent {
            Column {
                Text("Carrito")
                if (message.value.isNotEmpty()) Text(message.value)
            }
        }
        tick(500)
        network.configure(listOf("caido"))

        network.intercept("POST", "pedidos") { Result.success("{}") }
        // Esta pantalla no tiene animaciones: se deja que el reloj avance solo para que Compose la redibuje.
        compose.mainClock.autoAdvance = true
        message.value = "No pudimos enviar tu pedido"
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("No pudimos enviar tu pedido").assertExists()
        tick(6_000)

        assertEquals(emptyList<String>(), kinds())
    }

    @Test
    fun `a failed heartbeat in the background is not a silent error`() {
        compose.setContent { Text("Mesas") }
        tick(500)
        network.configure(listOf("caido"))

        network.intercept("POST", "latidos") { Result.success("") }
        tick(6_000)

        assertEquals(emptyList<String>(), kinds())
    }

    @Test
    fun `read-only mode keeps writes off the sandbox but lets reads, session and heartbeats through`() {
        network.writesAllowed = false
        val sent = mutableListOf<String>()

        val write =
            network.intercept("POST", "pedidos/9/transiciones") {
                Result.success("{}").also {
                    sent +=
                        "transiciones"
                }
            }
        network.intercept("GET", "catalogo") { Result.success("{}").also { sent += "catalogo" } }
        network.intercept("POST", "latidos") { Result.success("").also { sent += "latidos" } }
        network.intercept("POST", "sesiones/contexto") { Result.success("{}").also { sent += "contexto" } }

        assertEquals(listOf("catalogo", "latidos", "contexto"), sent)
        assertEquals(true, write.isFailure)
    }

    private fun tick(millis: Long) {
        var left = millis
        while (left > 0) {
            vigia.tick(STEP)
            left -= STEP
        }
    }

    private fun kinds(): List<String> {
        val found = vigia.drain().getJSONArray("hallazgos")
        return List(found.length()) { i ->
            found.getJSONObject(i).let {
                "${it.getString("tipo")}:${it.getString("gravedad")}"
            }
        }
    }

    private fun nodes(): List<SemanticsNode> {
        fun walk(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::walk)
        return compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired = false).flatMap(::walk)
    }

    private companion object {
        const val LIMIT = 4_000L
        const val STEP = 250L
    }
}
