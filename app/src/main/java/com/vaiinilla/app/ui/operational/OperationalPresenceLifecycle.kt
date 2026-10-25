package com.vaiinilla.app.ui.operational

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.vaiinilla.app.domain.model.OperationalRole

@Composable
fun OperationalPresenceLifecycle(
    role: OperationalRole,
    viewModel: OperationalViewModel,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    // Con la pantalla de Caja o Cocina a la vista, una comanda nueva avisa aunque el push no haya llegado.
    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var lastPlayedAt = 0L
            viewModel.staffAlerts.collect { alert ->
                val now = System.currentTimeMillis()
                // Varios pedidos en el mismo sondeo suenan una sola vez.
                if (now - lastPlayedAt >= ALERT_COOLDOWN_MS) {
                    lastPlayedAt = now
                    playStaffAlert(context, alert)
                }
            }
        }
    }
    DisposableEffect(lifecycleOwner, role, viewModel) {
        val activity = context as? Activity
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> viewModel.onOperationalForeground(role)
                    Lifecycle.Event.ON_STOP -> viewModel.onOperationalBackground(role)
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        viewModel.onOperationalForeground(role)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onOperationalBackground(role)
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

private const val ALERT_COOLDOWN_MS = 2_000L
