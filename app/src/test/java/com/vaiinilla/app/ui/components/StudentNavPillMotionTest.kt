package com.vaiinilla.app.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StudentNavPillMotionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        StudentNavPillMotion.index = 0f
        StudentNavPillMotion.lastTab = StudentTab.MENU
    }

    @Test
    fun `pill motion settles on new tab after spring`() {
        var tab by mutableStateOf(StudentTab.MENU)
        composeTestRule.setContent {
            VaiinillaBottomNav(
                activeTab = tab,
                cartCount = 0,
                onTabSelected = { tab = it },
                enableDrag = false,
            )
        }
        composeTestRule.waitForIdle()
        assertEquals(0f, StudentNavPillMotion.index, 0.01f)

        tab = StudentTab.CART
        composeTestRule.waitForIdle()

        assertEquals(3f, StudentNavPillMotion.index, 0.01f)
    }

    @Test
    fun `remount seeds from previous tab then settles on target`() {
        StudentNavPillMotion.lastTab = StudentTab.MENU
        StudentNavPillMotion.index = 0f
        composeTestRule.setContent {
            VaiinillaBottomNav(
                activeTab = StudentTab.ORDERS,
                cartCount = 0,
                onTabSelected = {},
                enableDrag = false,
            )
        }
        composeTestRule.waitForIdle()

        assertEquals(1f, StudentNavPillMotion.index, 0.01f)
        assertEquals(StudentTab.ORDERS, StudentNavPillMotion.lastTab)
    }
}
