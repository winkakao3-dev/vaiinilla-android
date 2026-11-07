package com.vaiinilla.app.ui.hotel

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

class ProbeViewModel : ViewModel()

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HotelViewModelsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `dentro de una ruta el ViewModel sale de la actividad, que es la que trae la fabrica de Hilt`() {
        var fromRoute: ProbeViewModel? = null
        var plainFromRoute: ProbeViewModel? = null
        rule.setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "a") {
                composable("a") {
                    fromRoute = hotelViewModel()
                    plainFromRoute = viewModel()
                }
            }
        }
        rule.waitForIdle()
        val owner = ViewModelProvider(rule.activity)[ProbeViewModel::class.java]
        assertSame(owner, fromRoute)
        // La entrada de la ruta es otro dueno: es el que no sabe construir un @HiltViewModel.
        assertNotSame(owner, plainFromRoute)
    }

    @Test
    fun `ninguna pantalla del hotel pide su ViewModel con viewModel() dentro de una ruta`() {
        val root = File("src/main/java/com/vaiinilla/app/ui/hotel")
        assertNotNull(root.listFiles())
        val offenders =
            root
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" && it.name != "HotelViewModels.kt" }
                .filter { file -> file.readLines().any { Regex("""=\s*viewModel\(\)""").containsMatchIn(it) } }
                .map { it.name }
                .toList()
        org.junit.Assert.assertEquals("Usa hotelViewModel(): $offenders", emptyList<String>(), offenders)
    }
}
