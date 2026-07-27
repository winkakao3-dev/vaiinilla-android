package com.vaiinilla.app.domain.repository

import com.vaiinilla.app.domain.model.OperationalRole

interface DeviceHeartbeatRepository {
    fun sendHeartbeat(
        deviceId: String,
        role: OperationalRole,
        idempotencyKey: String,
    ): Result<Unit>
}
