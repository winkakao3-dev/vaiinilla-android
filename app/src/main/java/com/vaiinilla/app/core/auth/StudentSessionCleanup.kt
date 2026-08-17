package com.vaiinilla.app.core.auth

import com.vaiinilla.app.core.security.PickupTokenStore
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Central cleanup shared by ordinary logout and confirmed account deletion. */
@Singleton
class StudentSessionCleanup
    @Inject
    constructor(
        private val authRepository: StudentAuthRepository,
        private val guestSessionStore: GuestSessionStore,
        private val pickupTokenStore: PickupTokenStore,
        private val refreshCoordinator: VaiinillaJwtRefreshCoordinator,
    ) {
        suspend fun clear() {
            refreshCoordinator.clearSession()
            authRepository.signOut()
            guestSessionStore.clearAll()
            pickupTokenStore.clear()
        }
    }
