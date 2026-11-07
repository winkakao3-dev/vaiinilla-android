package com.vaiinilla.app.ui.screens.admin.school

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableState
import com.vaiinilla.app.domain.school.MenuCalendar
import com.vaiinilla.app.domain.school.NoServiceDay
import com.vaiinilla.app.domain.school.RosterStudent
import com.vaiinilla.app.domain.school.SchoolAdminRepository
import com.vaiinilla.app.domain.school.SchoolSettings
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performScrollToNode
import com.vaiinilla.app.ui.admin.school.AdminSchoolUiState
import com.vaiinilla.app.ui.admin.school.AdminSchoolViewModel
import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import com.vaiinilla.app.ui.screens.admin.AdminDialogHost
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.lang.reflect.Proxy
import java.time.LocalDate
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

/**
 * Reglas de la escuela, solo mirar (F21): el código para unirse, el rango de recarga, cuántos días antes se aparta,
 * el padrón y el menú de cada día con su «Cerrar este día». Corre en la JVM con Robolectric y Compose; no cuenta
 * como recorrido en teléfono.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w390dp-h4600dp-normal-long-notround-any-xxhdpi",
    sdk = [33],
)
class RecorridosReglasEscuelaTest {
    @get:Rule
    val rule = createComposeRule()

    private inline fun <reified R : Any> hung(): R =
        Proxy.newProxyInstance(R::class.java.classLoader, arrayOf(R::class.java)) { _, method, args ->
            when {
                method.name == "toString" -> "hung"
                method.name == "hashCode" -> 0
                method.name == "equals" -> false
                args?.lastOrNull() is Continuation<*> -> COROUTINE_SUSPENDED
                else -> java.util.concurrent.CountDownLatch(1).await()
            }
        } as R

    @Suppress("UNCHECKED_CAST")
    private fun Any.put(state: AdminSchoolUiState) {
        val field = javaClass.getDeclaredField("_uiState")
        field.isAccessible = true
        (field.get(this) as MutableState<AdminSchoolUiState>).value = state
    }

    private fun show() {
        val today = LocalDate.of(2026, 11, 3)
        val products = ScreenshotFixtures.catalog().products
        val vm =
            AdminSchoolViewModel(hung<SchoolAdminRepository>()).apply {
                put(
                    AdminSchoolUiState(
                        settings = SchoolSettings("ABC123", true, true, "50.00", "2000.00", 5, "09:00", "08:00"),
                        menu =
                            MenuCalendar(
                                today,
                                mapOf(today to products.take(2).map { it.id }),
                                listOf(NoServiceDay(today.plusDays(3), "Junta de maestros")),
                            ),
                        roster = listOf(RosterStudent("A-001", "Ana López", "3°B")),
                    ),
                )
            }
        rule.setContent {
            ScreenshotTheme(mode = VaiinillaThemeMode.Dark) {
                val colors = rememberOperationalColors()
                AdminDialogHost(colors) {
                    Box(Modifier.fillMaxSize()) { AdminSchoolSection(vm, products, colors, false, {}) }
                }
            }
        }
        rule.mainClock.advanceTimeBy(1_500)
        rule.waitForIdle()
    }

    private fun seen(text: String) {
        val target = hasText(text, substring = true) or hasContentDescription(text, substring = true)
        val scrollers = rule.onAllNodes(hasScrollAction())
        for (i in scrollers.fetchSemanticsNodes().indices) {
            if (runCatching { scrollers[i].performScrollToNode(target) }.isSuccess) break
        }
        rule.waitForIdle()
        check(rule.onAllNodes(target).fetchSemanticsNodes().isNotEmpty()) { "No se ve: $text" }
    }

    @Test
    fun f21_laEscuelaMuestraElCodigoParaUnirseYLasReglasDeRecarga() {
        show()
        seen("Unirse a la escuela")
        seen("ABC123")
        seen("Recarga con tarjeta")
    }

    @Test
    fun f21_muestraComoSeApartaLaComidaYElPadron() {
        show()
        seen("Apartar comida")
        seen("Padrón de alumnos")
        seen("1 alumno")
    }

    @Test
    fun f21_cadaDiaTieneSuMenuYCerrarEsteDia() {
        show()
        seen("Cada día")
        seen("Cerrar este día")
    }
}
