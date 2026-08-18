package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.repository.DeviceHeartbeatRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class OperationalHeartbeatCoordinator(
    private val scope: CoroutineScope,
    private val repository: DeviceHeartbeatRepository,
    private val deviceId: () -> String,
    private val intervalMs: Long = DEFAULT_INTERVAL_MS,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val onResult: (Boolean) -> Unit = {},
) {
    private var activeRole: OperationalRole? = null
    private var heartbeatJob: Job? = null

    fun start(role: OperationalRole) {
        if (role == OperationalRole.CLIENT) {
            stop()
            return
        }
        if (activeRole == role && heartbeatJob?.isActive == true) return
        activeRole = role
        pause()
        resume()
    }

    fun resume() {
        val role = activeRole ?: return
        if (role == OperationalRole.CLIENT || heartbeatJob?.isActive == true) return
        heartbeatJob =
            scope.launch {
                while (isActive) {
                    val result =
                        withContext(ioDispatcher) {
                            runCatching { repository.sendHeartbeat(deviceId(), role) }
                                .getOrNull()
                                ?.isSuccess == true
                        }
                    onResult(result)
                    delay(intervalMs)
                }
            }
    }

    fun pause() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun stop() {
        pause()
        activeRole = null
        onResult(false)
    }

    private companion object {
        const val DEFAULT_INTERVAL_MS = 5_000L
    }
}
