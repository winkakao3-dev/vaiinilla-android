package com.vaiinilla.app.data.admin

import android.content.Context
import androidx.core.content.edit
import com.vaiinilla.app.core.security.JwtContextScope
import com.vaiinilla.app.core.security.SecureSessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Los productos archivados desde este teléfono. `GET catalogo` no trae los archivados (`archivado_en IS NULL`)
 * y no hay otra ruta que los liste, así que sin esto «Archivados» desaparecía al reiniciar la app (T03).
 * Guarda el cuerpo que devolvió `archivar`, por negocio, para poder pintarlo y desarchivarlo después.
 */
interface ArchivedProductsStore {
    /** Cuerpos crudos de `catalogo/productos/{id}/archivar` del negocio actual. */
    fun readAll(): List<String>

    fun put(
        productId: Int,
        rawProduct: String,
    )

    fun remove(productId: Int)
}

@Singleton
class PreferencesArchivedProductsStore
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val sessionStore: SecureSessionStore,
    ) : ArchivedProductsStore {
        private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

        override fun readAll(): List<String> {
            val prefix = keyPrefix() ?: return emptyList()
            return preferences.all
                .filterKeys { it.startsWith(prefix) }
                .values
                .filterIsInstance<String>()
        }

        override fun put(
            productId: Int,
            rawProduct: String,
        ) {
            val prefix = keyPrefix() ?: return
            preferences.edit { putString("$prefix$productId", rawProduct) }
        }

        override fun remove(productId: Int) {
            val prefix = keyPrefix() ?: return
            preferences.edit { remove("$prefix$productId") }
        }

        /** Por negocio, no por sesión: el archivo sigue ahí tras cerrar sesión o renovar el token. */
        private fun keyPrefix(): String? {
            val token = sessionStore.readAccessToken()?.takeIf { it.isNotBlank() } ?: return null
            val establishmentId = JwtContextScope.establishmentId(token) ?: return null
            return "$establishmentId$KEY_SEPARATOR"
        }

        private companion object {
            const val PREFERENCES_NAME = "vaiinilla_archived_products"
            const val KEY_SEPARATOR = "::"
        }
    }
