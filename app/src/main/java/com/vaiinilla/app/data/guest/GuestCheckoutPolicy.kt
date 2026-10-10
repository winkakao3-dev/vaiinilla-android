package com.vaiinilla.app.data.guest

import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.model.GuestVenueContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GuestCheckoutPolicy
    @Inject
    constructor(
        private val studentAuth: StudentAuthRepository,
    ) {
        /**
         * Compra sin cuenta: el negocio no pide identificador y no hay sesión de cuenta. Con sesión
         * (verificada o no) sigue el flujo de cuenta de siempre.
         */
        fun allows(venue: GuestVenueContext?): Boolean =
            venue != null && !venue.establishment.clientIdRequired && studentAuth.peekSession() == null
    }
