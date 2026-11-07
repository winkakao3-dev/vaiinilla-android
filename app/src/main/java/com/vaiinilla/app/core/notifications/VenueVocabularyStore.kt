package com.vaiinilla.app.core.notifications

import android.content.Context
import com.vaiinilla.app.core.venue.VenueContextStore

/**
 * El tipo del último negocio visto, para que un aviso que llega con la app cerrada use el vocabulario del negocio
 * («huésped» en un hotel) y no el de uno por omisión. [VenueContextStore] vive solo en memoria y el proceso nace sin
 * él cuando llega el push.
 */
object VenueVocabularyStore {
    private const val PREFS = "venue_vocabulary"
    private const val KEY_TYPE = "tipo"
    private const val KEY_SCHOOL = "escuela"

    /** Se llama al crear la aplicación: recupera lo guardado y guarda cada cambio de negocio. */
    fun install(
        context: Context,
        venueContext: VenueContextStore,
    ) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        venueContext.restore(prefs.getString(KEY_TYPE, null), prefs.getBoolean(KEY_SCHOOL, false))
        venueContext.onChange = { type, school ->
            prefs
                .edit()
                .putString(KEY_TYPE, type)
                .putBoolean(KEY_SCHOOL, school)
                .apply()
        }
    }
}
