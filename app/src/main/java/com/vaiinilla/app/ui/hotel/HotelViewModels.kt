package com.vaiinilla.app.ui.hotel

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * El ViewModel de una pantalla del hotel, tomado de la actividad.
 *
 * Dentro de una ruta de navegación, `viewModel()` usa la entrada de la ruta, que no trae la fábrica de Hilt (el
 * proyecto no usa hilt-navigation-compose) y no puede construir un `@HiltViewModel`: la app se cierra al abrir la
 * pantalla. La actividad sí la trae, igual que los ViewModels que AppNavHost crea arriba y pasa hacia abajo.
 */
@Composable
internal inline fun <reified VM : ViewModel> hotelViewModel(): VM {
    val owner = hotelViewModelOwner()
    return if (owner != null) viewModel(viewModelStoreOwner = owner) else viewModel()
}

@PublishedApi
@Composable
internal fun hotelViewModelOwner(): ViewModelStoreOwner? = LocalContext.current.findComponentActivity()

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
