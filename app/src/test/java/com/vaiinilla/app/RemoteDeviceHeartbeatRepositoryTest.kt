package com.vaiinilla.app

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.operational.RemoteDeviceHeartbeatRepository
import com.vaiinilla.app.domain.model.OperationalRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteDeviceHeartbeatRepositoryTest {
    @Test
    fun `heartbeat uses repeatable endpoint without idempotency header`() {
        val client = RecordingApiClient()
        val repository = RemoteDeviceHeartbeatRepository(client)

        val result =
            repository.sendHeartbeat(
                deviceId = "android-cocina",
                role = OperationalRole.KITCHEN,
            )

        assertTrue(result.isSuccess)
        assertEquals("latidos", client.path)
        assertTrue(client.headers.isEmpty())
        assertEquals("{\"dispositivo\":\"android-cocina\",\"rol\":\"cocina\"}", client.body)
    }

    private class RecordingApiClient : VaiinillaApiClient {
        override val baseUrl: String = "https://example.invalid/api/v1/"
        var path: String? = null
        var body: String? = null
        var headers: Map<String, String> = emptyMap()

        override fun get(
            path: String,
            query: Map<String, String>,
        ): Result<String> = Result.failure(AssertionError("GET no esperado"))

        override fun post(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> {
            this.path = path
            this.body = body
            this.headers = headers
            return Result.success("")
        }
    }
}
