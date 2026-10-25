package com.vaiinilla.app.ui.wallet

import com.vaiinilla.app.domain.model.WalletClient
import com.vaiinilla.app.domain.model.WalletData
import com.vaiinilla.app.domain.model.WalletReloadReceipt
import com.vaiinilla.app.domain.model.WalletSnapshot
import com.vaiinilla.app.domain.repository.WalletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class WalletViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `clearing the session drops the balance and a late response does not bring it back`() {
        val gate = CountDownLatch(1)
        val repository = FakeWallet(gate = gate)
        val viewModel = WalletViewModel(repository)

        viewModel.refresh()
        viewModel.clearForSessionTermination()
        gate.countDown()
        Thread.sleep(200)

        assertNull(viewModel.state.value.data)
        assertEquals(false, viewModel.state.value.loading)
    }

    @Test
    fun `a failed refresh keeps the last balance and shows the error`() {
        val repository = FakeWallet()
        val viewModel = WalletViewModel(repository)
        viewModel.refresh()
        waitUntil { viewModel.state.value.data != null }
        val shown = viewModel.state.value.data

        repository.next = Result.failure(IllegalStateException("sin red"))
        viewModel.refresh()
        waitUntil { viewModel.state.value.error != null }

        assertSame(shown, viewModel.state.value.data)
    }

    @Test
    fun `balance from another venue is never shown`() {
        val state = WalletRemoteUiState(data = walletOf("est-a"))

        assertSame(state, state.visibleFor("est-a"))
        assertSame(state, state.visibleFor(null))
        assertNull(state.visibleFor("est-b").data)
    }

    @Test
    fun `asking for a different venue discards the cached balance and reloads`() {
        val repository = FakeWallet(next = Result.success(walletOf("est-a")))
        val viewModel = WalletViewModel(repository)
        viewModel.refreshIfStale("est-a")
        waitUntil { viewModel.state.value.data != null }

        repository.next = Result.success(walletOf("est-b"))
        viewModel.refreshIfStale("est-b")
        waitUntil { establishmentShown(viewModel) == "est-b" }

        assertEquals(2, repository.calls)
    }

    @Test
    fun `a fresh balance is not reloaded until it is invalidated`() {
        val repository = FakeWallet()
        val viewModel = WalletViewModel(repository)
        viewModel.refreshIfStale("est-a")
        waitUntil { viewModel.state.value.data != null }

        viewModel.refreshIfStale("est-a")
        assertEquals(1, repository.calls)

        viewModel.invalidate()
        viewModel.refreshIfStale("est-a")
        waitUntil { repository.calls == 2 }
    }

    private fun establishmentShown(viewModel: WalletViewModel): String? =
        viewModel.state.value.data
            ?.wallet
            ?.establishmentId

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 3_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(10)
        check(condition()) { "La condición no se cumplió a tiempo" }
    }

    private class FakeWallet(
        var next: Result<WalletData> = Result.success(walletOf("est-a")),
        private val gate: CountDownLatch? = null,
    ) : WalletRepository {
        @Volatile var calls = 0

        override fun getMyWallet(): Result<WalletData> {
            calls++
            gate?.await(2, TimeUnit.SECONDS)
            return next
        }

        override fun searchClients(query: String): Result<List<WalletClient>> = Result.success(emptyList())

        override fun lookupClient(userId: String): Result<WalletClient> =
            Result.failure(UnsupportedOperationException())

        override fun reloadCash(
            userId: String,
            amount: String,
            idempotencyKey: String,
        ): Result<WalletReloadReceipt> = Result.failure(UnsupportedOperationException())
    }

    private companion object {
        fun walletOf(establishmentId: String) =
            WalletData(
                wallet = WalletSnapshot("w", "u", establishmentId, "10.00", null),
                movements = emptyList(),
            )
    }
}
