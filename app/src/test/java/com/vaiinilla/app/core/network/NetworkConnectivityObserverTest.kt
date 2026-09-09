package com.vaiinilla.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkInfo

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NetworkConnectivityObserverTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val shadowConnectivityManager = shadowOf(connectivityManager)
    private val observer = NetworkConnectivityObserver(context)

    @Test
    fun `reports online when there is a connected active network`() {
        val connectedInfo =
            ShadowNetworkInfo.newInstance(
                android.net.NetworkInfo.DetailedState.CONNECTED,
                ConnectivityManager.TYPE_WIFI,
                0,
                true,
                android.net.NetworkInfo.State.CONNECTED,
            )
        shadowConnectivityManager.setActiveNetworkInfo(connectedInfo)

        assertTrue(observer.isOnline())
    }

    @Test
    fun `reports offline when there is no active network`() {
        shadowConnectivityManager.setActiveNetworkInfo(null)

        assertFalse(observer.isOnline())
    }
}
