package com.vaiinilla.app.jvmroles

import com.vaiinilla.app.core.auth.VaiinillaJwtRefreshCoordinator
import com.vaiinilla.app.core.security.PickupTokenStore
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.location.DeviceLocationProvider
import com.vaiinilla.app.domain.model.GeoCoordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** Un token de contexto por rol, en memoria: ir a cocina y volver no le borra la sesión a la caja. */
class RoleSessionStore : SecureSessionStore {
    private val tokens = ConcurrentHashMap<HarnessRole, String>()

    override fun saveAccessToken(token: String) {
        ActiveRole.current?.let { tokens[it] = token }
    }

    override fun readAccessToken(): String? = ActiveRole.current?.let(tokens::get)

    override fun clear() {
        ActiveRole.current?.let(tokens::remove)
    }
}

/** Tokens de recogida por rol, en memoria: el real cifra con AndroidKeyStore, que Robolectric no tiene. */
class RolePickupTokenStore : PickupTokenStore {
    private val tokens = ConcurrentHashMap<HarnessRole, ConcurrentHashMap<String, String>>()

    private fun current(): ConcurrentHashMap<String, String>? =
        ActiveRole.current?.let {
            tokens.getOrPut(it) { ConcurrentHashMap() }
        }

    override fun save(
        orderId: String,
        pickupToken: String,
    ) {
        if (orderId.isBlank() || pickupToken.isBlank()) return
        current()?.put(orderId, pickupToken)
    }

    override fun read(orderId: String): String? = current()?.get(orderId)

    override fun clear() {
        current()?.clear()
    }
}

/**
 * La identidad de Firebase del rol al frente. Lo demás (accesos, /sesiones, el refresco del JWT)
 * corre con el código real de la app contra el backend de dev.
 */
@Singleton
class SandboxStudentAuthRepository
    @Inject
    constructor(
        private val identities: SandboxIdentities,
        private val sessionStore: SecureSessionStore,
        private val refreshCoordinator: VaiinillaJwtRefreshCoordinator,
    ) : StudentAuthRepository {
        override fun peekSession(): StudentAuthSession? = ActiveRole.current?.let(identities::session)

        override fun isReadyForCheckout(establishmentId: String?): Boolean =
            peekSession()?.emailVerified == true && !sessionStore.readAccessToken().isNullOrBlank()

        override suspend fun signUp(
            email: String,
            password: String,
            displayName: String,
        ): Result<StudentAuthSession> = unsupported()

        override suspend fun signIn(
            email: String,
            password: String,
        ): Result<StudentAuthSession> = unsupported()

        override suspend fun reloadSession(): Result<StudentAuthSession?> = Result.success(peekSession())

        override suspend fun getIdToken(forceRefresh: Boolean): Result<String> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val role = ActiveRole.current ?: throw IllegalStateException("No hay rol al frente.")
                    identities.idToken(role, forceRefresh)
                }
            }

        override suspend fun signOut() {
            ActiveRole.current?.let(identities::forget)
            refreshCoordinator.clearSession()
            sessionStore.clear()
        }

        private fun unsupported(): Result<StudentAuthSession> =
            Result.failure(UnsupportedOperationException("En el arnés se entra con `role <rol>`, no con correo."))
    }

/** Ubicación que pone el comando `location <lat> <lng>`; sin él, el teléfono no sabe dónde está. */
@Singleton
class FakeLocationProvider
    @Inject
    constructor() : DeviceLocationProvider {
        @Volatile
        var coordinates: GeoCoordinates? = null

        override suspend fun currentLocation(): GeoCoordinates? = coordinates
    }
