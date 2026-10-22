package com.vaiinilla.app.jvmroles

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.vaiinilla.app.BuildConfig
import com.vaiinilla.app.MainActivity
import com.vaiinilla.app.core.auth.VaiinillaJwtRefreshCoordinator
import com.vaiinilla.app.data.guest.GuestReservationSession
import com.vaiinilla.app.domain.model.GeoCoordinates
import com.vaiinilla.app.domain.model.OperationalRole
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.json.JSONArray
import org.json.JSONObject
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
 * Cada rol conserva su token de contexto, sus tokens de recogida y sus preferencias (RoleScopedApplication).
 * En una corrida normal de tests se salta.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(
    application = HarnessApplication_Application::class,
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

    @Inject
    lateinit var guestReservationSession: GuestReservationSession

    @Inject
    lateinit var network: VigiaNetwork

    private lateinit var vigia: Vigia

    private var scenario: ActivityScenario<MainActivity>? = null

    @Test
    fun serve() {
        assumeTrue(
            "Solo con -Djvmroles.serve=true (lo lanza vbuild jvm-roles)",
            System.getProperty("jvmroles.serve") == "true",
        )
        hilt.inject()
        ShadowLog.stream = System.out
        // Antes de la primera petición. Sin esto un 401 a un POST no llega a la app y lo que se salta VigiaApiClient
        // no queda en la bitácora.
        if (!VigiaHttp.install(network, BuildConfig.API_BASE_URL)) {
            println("vigía: otra URLStreamHandlerFactory ya estaba puesta; la red queda como la da el JDK")
        }
        vigia = Vigia(network, ::visibleNodes, ::labelOf)
        // En el teléfono una excepción sin atrapar en cualquier hilo cierra la app; aquí solo mataría ese hilo.
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            vigia.recordCrash(error)
            previousHandler?.uncaughtException(thread, error)
        }
        // El reloj de Compose lo mueve el arnés: con avance automático, una animación infinita no deja terminar nada.
        compose.mainClock.autoAdvance = false
        val stateFile = File(System.getProperty("jvmroles.stateFile") ?: error("Falta -Djvmroles.stateFile"))
        CommandServer(stateFile).use { server ->
            println("jvm-roles listo en ${stateFile.path}")
            val idleMinutes = System.getProperty("jvmroles.idleMinutes")?.toLongOrNull() ?: IDLE_LIMIT_MINUTES
            val idleLimitMillis = idleMinutes * 60_000
            var lastCommandAt = System.currentTimeMillis()
            while (true) {
                val command = server.queue.poll(IDLE_STEP_MILLIS, TimeUnit.MILLISECONDS)
                if (command == null) {
                    if (System.currentTimeMillis() - lastCommandAt > idleLimitMillis) {
                        println("jvm-roles se apaga: sin comandos en $idleMinutes min")
                        break
                    }
                    // En espera solo corre el looper (red y polling de los viewmodels). El reloj de Compose se queda
                    // quieto: si avanzara, una animación infinita dibujaría cuadros sin parar con nadie mirando.
                    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(IDLE_STEP_MILLIS))
                    continue
                }
                lastCommandAt = System.currentTimeMillis()
                println("> ${command.name} ${command.args.joinToString(" ")}")
                val reply =
                    runCatching { CommandServer.Reply(true, run(command)) }
                        .getOrElse { error -> CommandServer.Reply(false, error.message ?: error.toString()) }
                command.reply.complete(reply)
                if (command.name == "stop") break
            }
        }
        closeActivity()
    }

    /**
     * Cerrar la actividad no saca sus vistas de la ventana en Robolectric: el registro de Compose del test
     * las sigue viendo y cada una retiene su renderer nativo (~100 MB por cambio de rol). Quitar el
     * contenido primero desecha la composición y libera todo.
     */
    private fun closeActivity() {
        val current = scenario ?: return
        // Si la app ya se cerró sola (atrás en la raíz, "Salir"), onActivity falla: no queda nada que quitar.
        runCatching {
            current.onActivity { activity -> activity.findViewById<ViewGroup>(android.R.id.content).removeAllViews() }
        }
        current.close()
        scenario = null
        shadowOf(Looper.getMainLooper()).idle()
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
            "location" -> setLocation(args)
            "deeplink" -> deeplink(args.firstOrNull() ?: error("Uso: deeplink <https://vaiinilla.app/...>"))
            "red" -> network.configure(args)
            "escrituras" -> {
                network.writesAllowed = args.firstOrNull() == "si"
                if (network.writesAllowed) {
                    "escrituras al sandbox permitidas"
                } else {
                    "solo lectura: las escrituras vuelven como error"
                }
            }
            "vigia" -> vigia.drain().toString()
            "trafico" ->
                network.recent(args.firstOrNull()?.toInt() ?: TRAFFIC_LINES).joinToString("\n").ifEmpty {
                    "(sin peticiones)"
                }
            "limite" -> {
                vigia.loadingLimitMillis = args.firstOrNull()?.toLong() ?: error("Uso: limite <ms>")
                "carga eterna a partir de ${vigia.loadingLimitMillis} ms"
            }
            "nodos" -> tappableNodes().toString()
            "revisar" -> layoutAudit().toString()
            "tocar" -> tapNode(args.firstOrNull()?.toInt() ?: error("Uso: tocar <id de nodos>")).toString()
            "atras" -> back().toString()
            "bajar" -> scroll(down = true)
            "subir" -> scroll(down = false)
            "stop" -> "adiós"
            else ->
                error(
                    "No conozco '${command.name}'. Comandos: role, cual, list, tap, text, wait, advance, shot, " +
                        "location, deeplink, red, escrituras, vigia, trafico, limite, nodos, revisar, tocar, atras, bajar, subir, stop.",
                )
        }
    }

    /** Desliza la lista visible (la última con scroll: la de una hoja abierta antes que la de atrás). */
    private fun scroll(down: Boolean): String {
        val lists = compose.onAllNodes(hasScrollAction())
        val count = lists.fetchSemanticsNodes().size
        if (count == 0) error("No hay nada que deslizar. En pantalla: ${summary()}")
        lists[count - 1].performTouchInput { if (down) swipeUp() else swipeDown() }
        pump(SETTLE_MILLIS, realTime = true)
        return summary()
    }

    private fun switchRole(args: List<String>): String {
        val role =
            HarnessRole.parse(args.firstOrNull().orEmpty())
                ?: error("Rol desconocido. Roles: ${HarnessRole.entries.joinToString { it.cliName }}.")
        closeActivity()
        // Lo único que vive en memoria entre llamadas: el refresco del JWT del rol anterior y el bearer de invitado.
        refreshCoordinator.clearSession()
        guestReservationSession.invalidate()
        ActiveRole.current = role
        vigia.reset()
        identities.signIn(role)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        if (role.operationalRole != OperationalRole.CLIENT) {
            // Con modos de personal el arranque cae en la lista de modos: se entra como lo haría la persona.
            // "Entrar como Caja", o "Continuar como Caja" si el modo seguía activo. El botón existe
            // deshabilitado mientras carga la lista de modos; un toque ahí no hace nada.
            val enter =
                (
                    hasText("Entrar como ${role.operationalRole.label}", ignoreCase = true) or
                        hasText("Continuar como ${role.operationalRole.label}", ignoreCase = true)
                ) and hasClickAction()
            val deadline = System.currentTimeMillis() + ROLE_TIMEOUT_MILLIS
            var entered = false
            while (!entered && System.currentTimeMillis() < deadline) {
                if (waitFor(ROLE_STEP_MILLIS) { exists(enter and isEnabled()) }) {
                    compose.onAllNodes(enter and isEnabled())[0].performClick()
                    entered = waitFor(ROLE_STEP_MILLIS) { !exists(enter) }
                }
            }
            if (!entered) error("No se pudo entrar como ${role.operationalRole.label}. En pantalla: ${summary()}")
        }
        pump(SETTLE_MILLIS, realTime = true)
        return "al frente ${role.cliName}: ${summary()}"
    }

    /** Como escanear el QR o tocar un enlace con la app abierta: llega por onNewIntent, igual que en el teléfono. */
    private fun deeplink(url: String): String {
        val activityScenario = scenario ?: error("Primero entra a un rol.")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        activityScenario.onActivity { activity ->
            val launchIntent = activity.intent
            MainActivity::class.java
                .getDeclaredMethod("onNewIntent", Intent::class.java)
                .apply { isAccessible = true }
                .invoke(activity, intent)
            // onNewIntent hace setIntent(); ActivityScenario reconoce su actividad por el intent con que la
            // lanzó y, si cambia, ignora sus eventos: el siguiente close() esperaría para siempre.
            activity.intent = launchIntent
        }
        pump(SETTLE_MILLIS * 2, realTime = true)
        return summary()
    }

    private fun setLocation(args: List<String>): String {
        val (latitude, longitude) =
            args.map { it.toDouble() }.takeIf { it.size == 2 }
                ?: error("Uso: location <lat> <lng>")
        shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>())
            .grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        location.coordinates = GeoCoordinates(latitude, longitude)
        return "ubicación $latitude, $longitude (permiso concedido)"
    }

    private fun tap(text: String): String {
        val matcher =
            (
                hasText(text, substring = true, ignoreCase = true) or
                    hasContentDescription(text, substring = true, ignoreCase = true)
            ) and
                hasClickAction()
        if (!exists(matcher)) error("No hay nada tocable con \"$text\". En pantalla: ${summary()}")
        val label = labelOf(compose.onAllNodes(matcher)[0].fetchSemanticsNode())
        compose.onAllNodes(matcher)[0].clickEvenOffscreen()
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
                "'${label.take(
                    100,
                )}' [${b.left.toInt()},${b.top.toInt()}][${b.right.toInt()},${b.bottom.toInt()}]$click"
            }.joinToString("\n")
            .ifEmpty { "(sin textos en pantalla)" }

    private fun summary(): String =
        runCatching {
            visibleNodes()
                .map(::labelOf)
                .filter { it.isNotBlank() }
                .take(SUMMARY_TEXTS)
                .joinToString(" · ")
        }.getOrDefault("(sin pantalla)")

    /** Lo tocable y habilitado que se ve en la ventana, con un id estable mientras la pantalla no cambie. */
    private fun clickableNodes(): List<SemanticsNode> {
        val window = windowBounds()
        // Con una hoja o un diálogo abierto, lo de atrás sigue en el árbol pero el dedo no lo alcanza: solo cuenta
        // la ventana de arriba. Su velo (tocable, sin nombre y del tamaño de la pantalla) tampoco es un botón.
        val top = compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired = false).lastOrNull()
        return uncovered(top ?: return emptyList(), window).filter { node ->
            val bounds = node.boundsInWindow
            SemanticsActions.OnClick in node.config &&
                SemanticsProperties.Disabled !in node.config &&
                !isScrim(node, window) &&
                bounds.width > 0f &&
                bounds.height > 0f &&
                bounds.bottom > 0f &&
                bounds.top < window
        }
    }

    /**
     * Los nodos de [root] que el dedo alcanza. Una hoja dibujada en la misma ventana (la ficha del producto) pone un
     * velo tocable de pantalla completa: lo que va antes del velo en el árbol queda debajo de él.
     */
    private fun uncovered(
        root: SemanticsNode,
        window: Float,
    ): List<SemanticsNode> {
        fun walk(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::walk)
        val nodes = walk(root)
        val scrim = nodes.indexOfLast { isScrim(it, window) }
        return if (scrim < 0) nodes else nodes.drop(scrim)
    }

    // El velo de las hojas de Material se llama "Close sheet"; tocado al centro cae sobre la hoja y no la cierra.
    private fun isScrim(
        node: SemanticsNode,
        window: Float,
    ): Boolean =
        SemanticsActions.OnClick in node.config &&
            (labelOf(node).isBlank() || labelOf(node) in SCRIM_LABELS) &&
            node.boundsInWindow.height >= window * SCRIM_HEIGHT_RATIO

    private fun windowBounds(): Float =
        compose
            .onAllNodes(isRoot())
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .maxOfOrNull { it.boundsInWindow.bottom } ?: Float.MAX_VALUE

    /** Identifica la pantalla por lo que se puede tocar, sin números (horas, totales, contadores cambian solos). */
    private fun screenKey(): String =
        clickableNodes()
            .map { labelOf(it).replace(Regex("[0-9]+"), "#").trim() }
            .sorted()
            .joinToString("|")
            .hashCode()
            .toUInt()
            .toString(16)

    /** Todo lo que cambia a la vista: textos y estados de selección. Si un toque no lo mueve, el toque no hizo nada. */
    private fun screenState(): String =
        visibleNodes().joinToString("\n") { node ->
            val config = node.config
            val selected = config.getOrNull(SemanticsProperties.Selected)?.let { " sel=$it" } ?: ""
            val toggle = config.getOrNull(SemanticsProperties.ToggleableState)?.let { " tog=$it" } ?: ""
            val focus = config.getOrNull(SemanticsProperties.Focused)?.let { " foco=$it" } ?: ""
            val description = config.getOrNull(SemanticsProperties.ContentDescription)?.let { " desc=$it" } ?: ""
            // Un botón que se deshabilita o un indicador de progreso que aparece también es respuesta visible.
            val disabled = if (SemanticsProperties.Disabled in config) " off" else ""
            val progress =
                config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)?.let { " prog=${it.current}" } ?: ""
            labelOf(node) + selected + toggle + focus + description + disabled + progress
        }

    private fun tappableNodes(): JSONObject {
        val list = JSONArray()
        clickableNodes().forEach { node ->
            val bounds = node.boundsInWindow
            val config = node.config
            list.put(
                JSONObject()
                    .put("id", node.id)
                    .put("texto", labelOf(node).take(LABEL_LIMIT))
                    .put("x", bounds.left.toInt())
                    .put("y", bounds.top.toInt())
                    .put(
                        "alterna",
                        SemanticsProperties.ToggleableState in config || SemanticsProperties.Selected in config,
                    ).put(
                        "marcado",
                        config.getOrNull(SemanticsProperties.ToggleableState) == ToggleableState.On ||
                            config.getOrNull(SemanticsProperties.Selected) == true,
                    ).put("campo", SemanticsActions.SetText in config),
            )
        }
        return JSONObject()
            .put("pantalla", screenKey())
            .put("activa", scenario != null)
            .put("textos", JSONArray(summaryTexts()))
            .put("nodos", list)
    }

    private fun tapNode(id: Int): JSONObject {
        val node =
            clickableNodes().firstOrNull { it.id == id }
                ?: error("No hay nodo tocable con id $id; vuelve a pedir nodos.")
        val label = labelOf(node)
        val keyBefore = screenKey()
        val stateBefore = screenState()
        val firstEvent = network.lastId()
        val lookBefore = nodePixels(id)
        // Lo que la pantalla ya pedía sola (polling) no cuenta como respuesta al toque.
        val polled =
            network
                .eventsAfter(0)
                .filter { it.startedAt >= System.currentTimeMillis() - POLLING_WINDOW_MILLIS }
                .map { it.method to it.path }
                .toSet()
        startedOutside()
        val tappedAt = System.currentTimeMillis()
        compose.onAllNodes(SemanticsMatcher("id $id") { it.id == id })[0].clickEvenOffscreen()
        pump(SETTLE_MILLIS, realTime = true)
        // "Reintentar" pide justo lo que la pantalla ya pedía sola: si sale enseguida del toque, es respuesta al toque.
        val requests =
            network.eventsAfter(firstEvent).count {
                (it.method to it.path) !in polled || it.startedAt - tappedAt < TAP_REACTION_MILLIS
            }
        val gone = scenario?.state?.isAtLeast(Lifecycle.State.CREATED) != true
        if (gone) closeActivity()
        val outside = startedOutside()
        val effect =
            when {
                gone -> "salio"
                // Un enlace a la web o a mapas abre otra app: aquí no hay navegador, pero el toque sí hizo algo.
                outside -> "otra-app"
                screenKey() != keyBefore -> "pantalla-nueva"
                screenState() != stateBefore -> "cambio"
                requests > 0 -> "red"
                // Elegir un tema o un chip solo cambia colores: mismos textos, pero el botón se ve distinto.
                lookBefore != null && !lookBefore.contentEquals(nodePixels(id)) -> "cambio"
                else -> "nada"
            }
        return JSONObject()
            .put("texto", label.take(LABEL_LIMIT))
            .put("efecto", effect)
            .put("peticiones", requests)
            .put("pantalla", if (gone) "" else screenKey())
    }

    /** Si la app pidió abrir algo fuera de ella desde la última llamada (y vacía la lista). */
    private fun startedOutside(): Boolean {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val started = generateSequence { shadowOf(app).nextStartedActivity }.toList()
        return started.any { it.component?.packageName != app.packageName }
    }

    /**
     * Los pixeles del nodo [id] tal como se ven, o null si ya no está o no se pudo dibujar. Se dibuja la ventana de
     * arriba, de donde salen los tocables: captureToImage solo mira la de la actividad y no ve menús ni diálogos.
     */
    private fun nodePixels(id: Int): IntArray? =
        runCatching {
            val bounds = clickableNodes().first { it.id == id }.boundsInWindow
            val view = topWindowView()
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val left = bounds.left.toInt().coerceIn(0, view.width - 1)
            val top = bounds.top.toInt().coerceIn(0, view.height - 1)
            val width = bounds.width.toInt().coerceIn(1, view.width - left)
            val height = bounds.height.toInt().coerceIn(1, view.height - top)
            IntArray(width * height).also { bitmap.getPixels(it, 0, width, left, top, width, height) }
        }.getOrNull()

    private fun topWindowView(): View {
        val global = Class.forName("android.view.WindowManagerGlobal")
        val instance = global.getMethod("getInstance").invoke(null)
        val views = global.getDeclaredField("mViews").apply { isAccessible = true }.get(instance) as List<*>
        return views.filterIsInstance<View>().last { it.isShown && it.width > 0 && it.height > 0 }
    }

    /** El botón de atrás del teléfono. Si la app se cierra (atrás en la raíz), lo dice en vez de quedarse esperando. */
    private fun back(): JSONObject {
        val activityScenario = scenario ?: error("Primero entra a un rol.")
        var finishing = false
        activityScenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
            finishing = activity.isFinishing
        }
        if (finishing) {
            closeActivity()
            return JSONObject().put("efecto", "salio")
        }
        pump(SETTLE_MILLIS, realTime = true)
        return JSONObject().put("efecto", "atras").put("pantalla", screenKey())
    }

    /** Texto cortado, cosas fuera de la pantalla y tocables encimados en la ventana de arriba. */
    private fun layoutAudit(): JSONObject {
        val first = layoutIssues()
        // Una pantalla que todavía entra (un texto que se despliega) se ve mal medio segundo: solo cuenta lo que sigue.
        pump(SETTLE_MILLIS / 2, realTime = false)
        val issues = layoutIssues().filter { it in first }
        val out = JSONArray()
        issues.distinct().forEach { issue ->
            out.put(
                JSONObject()
                    .put("tipo", issue.kind)
                    .put("gravedad", issue.severity)
                    .put("detalle", issue.detail)
                    .put("pantalla", JSONArray(summaryTexts()))
                    .put("red", JSONArray()),
            )
        }
        return JSONObject().put("hallazgos", out)
    }

    private fun layoutIssues(): List<VigiaLayout.Issue> {
        val roots =
            compose
                .onAllNodes(
                    isRoot(),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes(atLeastOneRootRequired = false)
        val top = roots.lastOrNull() ?: return emptyList()

        fun walk(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::walk)
        return VigiaLayout.audit(walk(top), clickableNodes(), top.boundsInWindow, ::labelOf) + hiddenButReachable()
    }

    /** Botones que una hoja dibujada en la misma ventana tapa, pero que TalkBack todavía recorre y anuncia. */
    private fun hiddenButReachable(): List<VigiaLayout.Issue> {
        val window = windowBounds()
        val top =
            compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired = false).lastOrNull()
                ?: return emptyList()

        fun walk(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::walk)
        val reachable = uncovered(top, window).map { it.id }.toSet()
        val hidden =
            walk(top)
                .filter { it.id !in reachable && SemanticsActions.OnClick in it.config }
                .map(::labelOf)
                .filter { it.isNotBlank() }
        if (hidden.isEmpty()) return emptyList()
        val names = hidden.take(HIDDEN_NAMES).joinToString(", ") { "\"${it.take(LABEL_LIMIT / 2)}\"" }
        return listOf(
            VigiaLayout.Issue(
                "detras-accesible",
                "sospecha",
                "Con la hoja abierta, TalkBack todavía llega a ${hidden.size} botón(es) que quedaron debajo: $names.",
            ),
        )
    }

    private fun summaryTexts(): List<String> =
        runCatching {
            visibleNodes().map(::labelOf).filter { it.isNotBlank() }.take(SUMMARY_TEXTS * 2)
        }.getOrDefault(emptyList())

    private fun visibleNodes(): List<SemanticsNode> {
        val window = windowBounds()
        return compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired = false).flatMap {
            uncovered(it, window)
        }
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
            try {
                if (scenario != null) compose.mainClock.advanceTimeBy(step)
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(step))
            } catch (error: Throwable) {
                vigia.recordCrash(error)
                vigia.tick(step)
                throw error
            }
            if (scenario != null) vigia.tick(step)
            left -= step
        }
    }

    private companion object {
        const val IDLE_STEP_MILLIS = 100L
        const val IDLE_LIMIT_MINUTES = 15L
        const val FRAME_STEP_MILLIS = 50L
        const val POLL_STEP_MILLIS = 250L
        const val SETTLE_MILLIS = 1_500L
        const val ROLE_TIMEOUT_MILLIS = 30_000L
        const val ROLE_STEP_MILLIS = 5_000L
        const val SUMMARY_TEXTS = 6
        const val LABEL_LIMIT = 80
        const val SCRIM_HEIGHT_RATIO = 0.6f
        const val TRAFFIC_LINES = 20
        const val POLLING_WINDOW_MILLIS = 10_000L
        const val TAP_REACTION_MILLIS = 400L
        const val HIDDEN_NAMES = 3
        val SCRIM_LABELS = setOf("Close sheet", "Cerrar hoja")
    }
}

/**
 * performClick toca el centro del nodo: si quedó fuera de la pantalla (el final de una hoja larga), el toque cae
 * en otra cosa, como el fondo que cierra la hoja. Fuera de la pantalla se usa la acción de clic del nodo.
 */
private fun SemanticsNodeInteraction.clickEvenOffscreen() {
    if (runCatching { assertIsDisplayed() }.isSuccess) {
        performClick()
    } else {
        performSemanticsAction(SemanticsActions.OnClick)
    }
}
