package com.vaiinilla.app.ui.parentmode

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.domain.guardianship.ChildRestriction
import com.vaiinilla.app.domain.guardianship.GuardianshipRepository
import com.vaiinilla.app.domain.guardianship.MyGuardianControls
import com.vaiinilla.app.domain.guardianship.compactMoney
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Los controles que el tutor le puso al alumno (GET /tutelas/mis-controles), para que el menú, la ficha
 * y el carrito los digan antes de pagar. Sin tutela activa o fuera de una escuela es `null` y no se ve nada;
 * PRODUCT_BLOCKED_BY_GUARDIAN y DAILY_LIMIT_EXCEEDED del servidor quedan como respaldo.
 */
@HiltViewModel
class GuardianControlsViewModel
    @Inject
    constructor(
        private val guardianship: GuardianshipRepository,
    ) : ViewModel() {
        var controls: MyGuardianControls?
            get() = LocalChildControls.value
            private set(value) {
                LocalChildControls.value = value
            }
        private var job: Job? = null

        fun refresh(enabled: Boolean) {
            job?.cancel()
            if (!enabled) {
                controls = null
                return
            }
            job =
                viewModelScope.launch {
                    // Si falla (sin contexto, sin red) se queda lo último: el servidor sigue rechazando con 422.
                    withContext(Dispatchers.IO) { guardianship.myControls() }.onSuccess { controls = it }
                }
        }

        fun clear() {
            job?.cancel()
            controls = null
        }
    }

/**
 * Los controles del alumno que está pidiendo; `null` fuera de una escuela o sin tutor. Es un estado de la
 * app (hay un solo alumno con sesión), así el menú, la ficha y el carrito lo leen sin cambiar sus firmas.
 */
object LocalChildControls {
    internal var value by mutableStateOf<MyGuardianControls?>(null)

    val current: MyGuardianControls?
        @Composable get() = value
}

/** "Te quedan $40 de tu límite de hoy", o lo que falta si el carrito ya no cabe. */
fun dailyLimitLine(
    controls: MyGuardianControls?,
    total: BigDecimal,
): String? {
    val available = controls?.availableToday ?: return null
    return if (controls.fitsToday(total)) {
        "Te quedan ${compactMoney(available)} de tu límite de hoy"
    } else {
        "Tu límite de hoy no alcanza: te quedan ${compactMoney(available)}"
    }
}

/** Debajo del precio en el menú: por qué no se puede agregar. */
@Composable
fun ChildRestrictionTag(
    restriction: ChildRestriction,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            when (restriction.reason) {
                ChildRestriction.Reason.BLOCKED -> Icons.Outlined.Lock
                ChildRestriction.Reason.ALLERGEN -> Icons.Outlined.ErrorOutline
                ChildRestriction.Reason.ALLERGENS_UNKNOWN -> Icons.Outlined.HelpOutline
                ChildRestriction.Reason.SOLD_OUT -> Icons.Outlined.Block
            },
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        Text(
            restriction.label,
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
