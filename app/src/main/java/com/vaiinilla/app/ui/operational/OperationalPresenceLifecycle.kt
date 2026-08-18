package com.vaiinilla.app.ui.operational

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vaiinilla.app.domain.model.OperationalRole

@Composable
fun OperationalPresenceLifecycle(
    role: OperationalRole,
    viewModel: OperationalViewModel,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
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
