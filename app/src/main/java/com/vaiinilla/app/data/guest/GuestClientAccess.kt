package com.vaiinilla.app.data.guest

import javax.inject.Inject
import javax.inject.Singleton

/**
 * La sesión de cliente del invitado (compra sin cuenta) para lo que el cliente hace en la mesa: unirse, llamar al
 * mesero. El backend la firma con rol cliente, igual que la de una cuenta, pero no vive en la sesión activa: quien
 * la usaba sin pasar por aquí fallaba con "Falta el contexto operativo. Inicia sesión…".
 */
@Singleton
class GuestClientAccess
    @Inject
    constructor(
        private val store: GuestSessionStore,
        private val policy: GuestCheckoutPolicy,
        val session: GuestReservationSession,
    ) {
        /** El negocio donde se compra sin cuenta; `null` con cuenta (entonces va la sesión activa). */
        fun guestSlug(): String? {
            val venue = store.readVenue() ?: return null
            return venue.establishment.slug.takeIf { policy.allows(venue) }
        }

        fun hasIdentity(): Boolean = session.hasIdentity()

        fun <T> withBearer(
            slug: String,
            request: (bearer: String) -> Result<T>,
        ): Result<T> = session.withBearer(slug, request)
    }
