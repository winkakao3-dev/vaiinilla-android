package com.vaiinilla.app.core.auth

import android.content.Context
import com.vaiinilla.app.core.notifications.DependentNamesStore
import com.vaiinilla.app.core.notifications.DeviceTokenRegistrar
import com.vaiinilla.app.core.security.PickupTokenStore
import com.vaiinilla.app.data.cache.RemoteResponseCache
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
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
        private val responseCache: RemoteResponseCache? = null,
        @ApplicationContext private val context: Context? = null,
    ) {
        // El borrado del token de dispositivo es una llamada de red y la espera del lock del coordinator puede
        // retener un refresh en vuelo: nada de esto puede correr en el hilo principal, venga de donde venga.
        internal var cleanupDispatcher: CoroutineDispatcher = Dispatchers.IO

        suspend fun clear() {
            withContext(cleanupDispatcher) {
                deviceTokenRegistrar?.clearRegistration()
                refreshCoordinator.clearSession()
                authRepository.signOut()
                guestSessionStore.clearAll()
                pickupTokenStore.clear()
                responseCache?.clear()
                context?.let(DependentNamesStore::clear)
            }
        }
    }
