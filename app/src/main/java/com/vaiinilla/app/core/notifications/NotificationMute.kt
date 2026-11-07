package com.vaiinilla.app.core.notifications

import android.content.Context

/**
 * «Apagar notificaciones» (Naev, 2026-11-05): cada quien apaga los avisos de este teléfono sin ir a los ajustes del
 * sistema. Hay dos interruptores porque una misma persona puede ser cliente y personal:
 * - [Scope.CLIENT]: sus pedidos, su renta, el mesero en camino y los avisos de sus hijos (Cuenta).
 * - [Scope.STAFF]: comandas, cobros, pedidos listos, llamadas de mesa y turnos, con su sonido dentro de la app
 *   (menú del avatar de Caja, Cocina, Mesero y Administración).
 * Los pushes siguen llegando y se anotan en «Avisos recibidos» como no mostrados; solo no suenan ni aparecen.
 */
object NotificationMute {
    enum class Scope(
        internal val key: String,
    ) {
        CLIENT("cliente"),
        STAFF("personal"),
    }

    private const val PREFS = "notificaciones_apagadas"

    fun isMuted(
        context: Context,
        scope: Scope,
    ): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(scope.key, false)

    fun setMuted(
        context: Context,
        scope: Scope,
        muted: Boolean,
    ) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(scope.key, muted)
            .apply()
    }
}
