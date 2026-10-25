package com.vaiinilla.app.core.permissions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Abre la ficha de la app en Ajustes: único camino cuando Android ya no vuelve a mostrar el diálogo de permiso. */
fun openAppSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Sin pantalla de ajustes que abrir no hay nada más que ofrecer.
    }
}
