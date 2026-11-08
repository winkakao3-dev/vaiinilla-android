package com.vaiinilla.app.core.venue

import com.vaiinilla.app.domain.model.VenueContext
import com.vaiinilla.app.domain.model.VenueKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

        /**
         * Lo llama quien guarda el último negocio visto, para que un aviso que llega con la app cerrada hable como el
         * negocio y no como «cliente» a secas.
         */
        var onChange: ((type: String?, school: Boolean) -> Unit)? = null

        /** `school` en null deja el valor que había: no todos los estados lo traen. */
        @Synchronized
        fun update(
            type: String?,
            school: Boolean? = null,
        ) {
            val before = _context.value
            val after = before.copy(kind = VenueKind.fromWire(type), isSchool = school ?: before.isSchool)
            if (after == before) return
            _context.value = after
            onChange?.invoke(after.kind.wire, after.isSchool)
        }

        /** Vuelve a poner el último negocio guardado sin avisar a [onChange]. */
        @Synchronized
        fun restore(
            type: String?,
            school: Boolean,
        ) {
            _context.value = _context.value.copy(kind = VenueKind.fromWire(type), isSchool = school)
        }

        /** Anota lo que el servidor dijo de un espacio: `false` es una mesa del hotel; `true` o `null`, no lo es. */
        @Synchronized
        fun markSpace(
            id: Int,
            isRoom: Boolean?,
        ) {
            val before = _context.value
            val isTable = isRoom == false
            if (isTable == (id in before.hotelTables)) return
            _context.value =
                before.copy(hotelTables = if (isTable) before.hotelTables + id else before.hotelTables - id)
        }
    }
