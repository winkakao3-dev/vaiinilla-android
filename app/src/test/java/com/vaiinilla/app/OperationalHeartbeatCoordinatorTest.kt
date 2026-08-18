package com.vaiinilla.app

import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.repository.DeviceHeartbeatRepository
import com.vaiinilla.app.ui.operational.OperationalHeartbeatCoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OperationalHeartbeatCoordinatorTest {
    @Test
    fun `starts immediately, keeps cadence, and does not duplicate same role`() {
        val dispatcher = StandardTestDispatcher()
        val scope = TestScope(dispatcher)
        val repository = RecordingHeartbeatRepository()
        val coordinator =
            OperationalHeartbeatCoordinator(
                scope = scope,
                repository = repository,
                deviceId = { "android-stable" },
                intervalMs = 5_000L,
                ioDispatcher = dispatcher,
            )

        coordinator.start(OperationalRole.CASHIER)
        scope.runCurrent()
        assertEquals(listOf(OperationalRole.CASHIER), repository.roles)

        coordinator.start(OperationalRole.CASHIER)
        scope.advanceTimeBy(5_000L)
        scope.runCurrent()
        assertEquals(2, repository.roles.size)

        coordinator.pause()
        scope.advanceTimeBy(10_000L)
        scope.runCurrent()
        assertEquals(2, repository.roles.size)
    }

    @Test
    fun `role change cancels old loop and temporary failure does not kill cadence`() {
        val dispatcher = StandardTestDispatcher()
        val scope = TestScope(dispatcher)
        val repository = RecordingHeartbeatRepository(failuresRemaining = 1)
        val coordinator =
            OperationalHeartbeatCoordinator(
                scope = scope,
                repository = repository,
                deviceId = { "android-stable" },
                intervalMs = 5_000L,
                ioDispatcher = dispatcher,
            )

        coordinator.start(OperationalRole.KITCHEN)
        scope.runCurrent()
        assertEquals(1, repository.roles.size)
        assertTrue(repository.results.first() == false)

        scope.advanceTimeBy(5_000L)
        scope.runCurrent()
        assertEquals(2, repository.roles.size)
        assertTrue(repository.results[1])

        coordinator.start(OperationalRole.CASHIER)
        scope.runCurrent()
        assertEquals(OperationalRole.CASHIER, repository.roles.last())
        assertEquals(3, repository.roles.size)
    }

    private class RecordingHeartbeatRepository(
        private var failuresRemaining: Int = 0,
    ) : DeviceHeartbeatRepository {
        val roles = mutableListOf<OperationalRole>()
        val results = mutableListOf<Boolean>()

        override fun sendHeartbeat(
            deviceId: String,
            role: OperationalRole,
        ): Result<Unit> {
            roles += role
            val failed = failuresRemaining > 0
            if (failed) failuresRemaining--
            results += !failed
            return if (failed) Result.failure(IllegalStateException("temporary")) else Result.success(Unit)
        }
    }
}
