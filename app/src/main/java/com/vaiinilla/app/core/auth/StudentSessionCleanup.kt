package com.vaiinilla.app.core.auth

import com.vaiinilla.app.core.notifications.DeviceTokenRegistrar
import com.vaiinilla.app.core.security.PickupTokenStore
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        private val deviceTokenRegistrar: DeviceTokenRegistrar? = null,
    ) {
        // La espera del lock del coordinator puede retener un refresh de red en
        // vuelo: nunca debe ejecutarse en el hilo principal.
        internal var cleanupDispatcher: CoroutineDispatcher = Dispatchers.IO

        suspend fun clear() {
            deviceTokenRegistrar?.clearRegistration()
            withContext(cleanupDispatcher) { refreshCoordinator.clearSession() }
            authRepository.signOut()
            guestSessionStore.clearAll()
            pickupTokenStore.clear()
        }
    }
