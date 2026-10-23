package com.vaiinilla.app.core.security

/**
 * Avisa cuando se guarda el JWT de otro contexto (otra cuenta, escuela o modo), no en cada refresh. El
 * token FCM se registra a nombre de quien guardó el contexto: atarlo a la pantalla que fija el rol dejaba
 * el teléfono a nombre de la cuenta anterior cuando el rol ya estaba puesto o el contexto llegaba después
 * (K03, M05, M06).
 */
class ContextAwareSessionStore(
    private val delegate: SecureSessionStore,
    private val onContextChanged: () -> Unit,
) : SecureSessionStore {
    @Volatile
    private var lastContextKey: String? = null

    override fun saveAccessToken(token: String) {
        delegate.saveAccessToken(token)
        val key = JwtContextScope.storageKey(token)
        if (key == lastContextKey) return
        lastContextKey = key
        onContextChanged()
    }

    override fun readAccessToken(): String? = delegate.readAccessToken()

    override fun clear() {
        lastContextKey = null
        delegate.clear()
    }
}
