package com.vaiinilla.app.ui.navigation

import com.vaiinilla.app.ui.components.StudentTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudentShellTest {
    @Test
    fun `student nav hides for catalog detail but stays for primary cart`() {
        assertEquals(false, shouldShowStudentNav(Routes.CATALOG, catalogDetailOpen = true))
        assertEquals(true, shouldShowStudentNav(Routes.CATALOG, catalogDetailOpen = false))
        assertEquals(true, shouldShowStudentNav(Routes.CART, catalogDetailOpen = true))
        assertEquals(false, shouldShowStudentNav(Routes.ASSISTANT, catalogDetailOpen = false))
        assertEquals(false, shouldShowStudentNav(Routes.ASSISTANT_CHAT, catalogDetailOpen = false))
        assertEquals(false, shouldShowStudentNav(Routes.WALLET_METHODS, catalogDetailOpen = false))
    }

    @Test
    fun `studentTabForRoute maps student destinations`() {
        assertEquals(StudentTab.MENU, studentTabForRoute(Routes.CATALOG))
        assertEquals(StudentTab.ASSISTANT, studentTabForRoute(Routes.ASSISTANT))
        assertEquals(StudentTab.ASSISTANT, studentTabForRoute(Routes.ASSISTANT_CHAT))
        assertEquals(StudentTab.ORDERS, studentTabForRoute(Routes.STUDENT_TRACKING))
        assertEquals(StudentTab.WALLET, studentTabForRoute(Routes.WALLET))
        assertEquals(StudentTab.CART, studentTabForRoute(Routes.CART))
        assertNull(studentTabForRoute(Routes.SPLASH))
        assertNull(studentTabForRoute(Routes.WALLET_METHODS))
    }

    @Test
    fun `routeForStudentTab is inverse of primary tabs`() {
        StudentTab.entries.forEach { tab ->
            assertEquals(tab, studentTabForRoute(routeForStudentTab(tab)))
        }
    }

    @Test
    fun `studentTabOrder orders primary dock tabs left to right`() {
        assertEquals(0, studentTabOrder(StudentTab.MENU))
        assertEquals(1, studentTabOrder(StudentTab.ORDERS))
        assertEquals(2, studentTabOrder(StudentTab.WALLET))
        assertEquals(3, studentTabOrder(StudentTab.CART))
        assertEquals(-1, studentTabOrder(StudentTab.ASSISTANT))
        assertEquals(-1, studentTabOrder(null))
    }
}
