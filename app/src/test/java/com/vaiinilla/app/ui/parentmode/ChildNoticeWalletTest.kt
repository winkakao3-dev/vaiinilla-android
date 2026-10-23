package com.vaiinilla.app.ui.parentmode

import android.content.Intent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.vaiinilla.app.core.notifications.ChildDeepLink
import com.vaiinilla.app.core.notifications.ChildDeepLinkStore
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Tocar el aviso de un hijo abre su ficha en Cartera; si ya no es su tutor, se queda en Cartera. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class ChildNoticeWalletTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun clearStore() {
        ChildDeepLinkStore.clearPending()
        ChildDeepLinkStore.consumeChild()
    }

    private fun showWallet() {
        val guardianship = FakeGuardianship(listOf(ParentFixtures.ana, ParentFixtures.tilin))
        val viewModel =
            ChildrenViewModel(
                guardianship,
                { FakeAccount(ParentFixtures.wallet) },
                FakeCatalog(),
                ApplicationProvider.getApplicationContext(),
            )
        composeTestRule.setContent {
            ScreenshotTheme {
                ParentModeWalletHost(
                    enabled = true,
                    childrenViewModel = viewModel,
                    hasTutors = false,
                    onFullScreenChange = {},
                    onAccountClaimed = {},
                ) { family -> family?.invoke() }
            }
        }
    }

    @Test
    fun `el aviso guardado en el intent se lee una sola vez`() {
        val intent =
            Intent()
                .putExtra(ChildDeepLink.EXTRA_DEPENDENT_ID, "u-ana")
                .putExtra(ChildDeepLink.EXTRA_ESTABLISHMENT_ID, "est-1")
        ChildDeepLinkStore.capture(intent)
        assertEquals(ChildDeepLink("u-ana", "est-1"), ChildDeepLinkStore.pending.value)
        assertNull(ChildDeepLink.fromIntent(intent))
    }

    @Test
    fun `el aviso de un hijo abre su ficha`() {
        ChildDeepLinkStore.handToWallet(ChildDeepLink("u-ana", null))
        showWallet()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithTag("child-account").fetchSemanticsNodes().isNotEmpty()
        }
        assertNull(ChildDeepLinkStore.requestedChild.value)
    }

    @Test
    fun `si ya no es su tutor se queda en Cartera`() {
        ChildDeepLinkStore.handToWallet(ChildDeepLink("u-otro", null))
        showWallet()

        composeTestRule.waitUntil(10_000) { ChildDeepLinkStore.requestedChild.value == null }
        composeTestRule.onNodeWithText("Mis hijos").assertExists()
        composeTestRule.onAllNodesWithTag("child-account").fetchSemanticsNodes().let { assertEquals(0, it.size) }
    }
}
