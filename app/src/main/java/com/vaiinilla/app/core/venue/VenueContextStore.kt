package com.vaiinilla.app.core.venue

import com.vaiinilla.app.domain.model.VenueContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * El tipo del negocio abierto, visible para pantallas (vía `LocalVenueContext`) y ViewModels (por constructor).
 * Lo escribe un solo sitio: la raíz de navegación, con el tipo del negocio que se mira.
 */
@Singleton
class VenueContextStore
    @Inject
    constructor() {
        private val _context = MutableStateFlow(VenueContext.STANDARD)
        val context: StateFlow<VenueContext> = _context.asStateFlow()

        val current: VenueContext get() = _context.value

        fun update(type: String?) {
            val next = VenueContext.of(type)
            if (next != _context.value) _context.value = next
        }
    }
