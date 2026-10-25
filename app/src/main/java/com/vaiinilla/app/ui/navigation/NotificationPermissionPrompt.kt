package com.vaiinilla.app.ui.navigation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.vaiinilla.app.core.permissions.openAppSettings

/** El permiso de avisos quedó denegado: sin él no llegan los pedidos nuevos, y solo Ajustes lo puede cambiar. */
@Composable
internal fun NotificationPermissionPrompt(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Los avisos están apagados") },
        text = {
            Text("Sin ellos no te enteras de los pedidos nuevos hasta abrir la app. Puedes activarlos en Ajustes.")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    openAppSettings(context)
                    onDismiss()
                },
            ) { Text("Abrir ajustes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Ahora no") } },
    )
}
