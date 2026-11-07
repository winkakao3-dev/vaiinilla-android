package com.vaiinilla.app.core.venue

import com.vaiinilla.app.domain.model.VenueContext
import com.vaiinilla.app.domain.model.VenueKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * El tipo del negocio abierto, visible para pantallas (vía `LocalVenueContext`) y ViewModels (por constructor).
 * Lo escriben la raíz de navegación y los repositorios que leen el estado del negocio.
 */
@Singleton
class VenueContextStore
    @Inject
    constructor() {
        private val _context = MutableStateFlow(VenueContext.STANDARD)
        val context: StateFlow<VenueContext> = _context.asStateFlow()

        val current: VenueContext get() = _context.value

        fun update(type: String?) {
            val kind = VenueKind.fromWire(type)
            _context.update { if (it.kind == kind) it else it.copy(kind = kind) }
        }

        /** Anota lo que el servidor dijo de un espacio: `false` es una mesa del hotel; `true` o `null`, no lo es. */
        fun markSpace(
            id: Int,
            isRoom: Boolean?,
        ) {
            val isTable = isRoom == false
            _context.update { current ->
                if (isTable == (id in current.hotelTables)) {
                    current
                } else {
                    current.copy(hotelTables = if (isTable) current.hotelTables + id else current.hotelTables - id)
                }
            }
        }
    }
