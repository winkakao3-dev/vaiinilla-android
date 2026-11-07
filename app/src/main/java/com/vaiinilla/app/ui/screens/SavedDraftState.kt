package com.vaiinilla.app.ui.screens

import androidx.compose.runtime.saveable.Saver
import com.vaiinilla.app.domain.model.TipChoice

/**
 * Lo que el personal ya escribió en una hoja de cobro (propina, pedidos marcados) tiene que sobrevivir a que Android
 * recree la actividad (modo oscuro, tamaño de letra, memoria) y a un cambio de proceso: con `remember` se perdía y el
 * cajero volvía a teclear el monto con el cliente esperando.
 */
internal val TipChoiceSaver: Saver<TipChoice, Any> =
    Saver(
        save = { choice ->
            when (choice) {
                TipChoice.None -> "none"
                is TipChoice.Percent -> "percent:${choice.percent}"
                is TipChoice.Custom -> "custom:${choice.amount}"
            }
        },
        restore = { saved ->
            val text = saved as? String ?: return@Saver TipChoice.None
            when {
                text.startsWith("percent:") -> TipChoice.Percent(text.removePrefix("percent:").toIntOrNull() ?: 0)
                text.startsWith("custom:") -> TipChoice.Custom(text.removePrefix("custom:"))
                else -> TipChoice.None
            }
        },
    )

/** Un `listSaver` devuelve null con la lista vacía y entonces se restauraba "todo marcado": aquí vacía se guarda igual. */
internal val StringSetSaver: Saver<Set<String>, Any> =
    Saver(
        save = { ArrayList(it) },
        restore = { saved -> (saved as? List<*>).orEmpty().filterIsInstance<String>().toSet() },
    )
