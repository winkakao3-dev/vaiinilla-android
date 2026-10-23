package com.vaiinilla.app.core.notifications

import android.Manifest
import android.app.NotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * F20: con la app cerrada el push de datos despierta solo al servicio, sin pantalla que haya creado el
 * canal antes; el aviso del tutor tiene que crear el canal y mostrarse por sí mismo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuardianPushBackgroundTest {
    @Test
    fun avisoDelTutorEnProcesoFrioCreaElCanalYSeMuestra() {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val manager = app.getSystemService(NotificationManager::class.java)

        GuardianPushNotifier.post(
            app,
            "saldo-bajo:hijo-1",
            GuardianPushNotifier.lowBalanceText("Tilin", "20.00"),
            ChildDeepLink(dependentUserId = "hijo-1", establishmentId = "esc-1"),
        )

        val channel = manager.getNotificationChannel("order_tracking")
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertEquals(1, shadowOf(manager).allNotifications.size)
    }
}
