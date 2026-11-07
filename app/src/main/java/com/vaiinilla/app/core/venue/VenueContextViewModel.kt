package com.vaiinilla.app.core.venue

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import com.vaiinilla.app.domain.model.VenueContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Entrega el [VenueContextStore] singleton a la raíz de Compose, que lo publica en [LocalVenueContext]. */
@HiltViewModel
class VenueContextViewModel
    @Inject
    constructor(
        val store: VenueContextStore,
    ) : ViewModel()

/** El negocio que se mira. Sin proveedor (previews y pruebas de pantalla) es el negocio estándar. */
val LocalVenueContext = staticCompositionLocalOf { VenueContext.STANDARD }
