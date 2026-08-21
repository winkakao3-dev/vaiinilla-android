package com.vaiinilla.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vaiinilla.app.data.guest.GuestSessionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StripeIdempotencyPersistenceTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        GuestSessionStore(context).clearAll()
    }

    @Test
    fun `create idempotency key survives store recreation after lost response`() {
        GuestSessionStore(context).savePendingCreateIdempotency(
            fingerprint = "cart-fingerprint-a",
            idempotencyKey = "2ca3f3bd-7d08-4d76-bb08-679d4f8e9d02",
        )

        val recreated = GuestSessionStore(context)

        assertEquals(
            "2ca3f3bd-7d08-4d76-bb08-679d4f8e9d02",
            recreated.readPendingCreateIdempotency("cart-fingerprint-a"),
        )
        assertNull(recreated.readPendingCreateIdempotency("different-cart"))
    }

    @Test
    fun `stripe retry idempotency key survives store recreation for same order`() {
        GuestSessionStore(context).savePendingStripeRetryIdempotency(
            orderId = "order-1",
            idempotencyKey = "7e0c8b2d-2d11-43f8-9a22-0fdc1b8a8a8a",
        )

        assertEquals(
            "7e0c8b2d-2d11-43f8-9a22-0fdc1b8a8a8a",
            GuestSessionStore(context).readPendingStripeRetryIdempotency("order-1"),
        )
    }
}
