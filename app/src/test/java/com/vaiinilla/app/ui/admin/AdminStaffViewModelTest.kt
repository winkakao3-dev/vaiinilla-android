package com.vaiinilla.app.ui.admin

import com.vaiinilla.app.domain.admin.InvitationState
import com.vaiinilla.app.domain.admin.InvitationsPage
import com.vaiinilla.app.domain.admin.StaffMembership
import com.vaiinilla.app.domain.repository.AdminStaffRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

/** T07 en vivo: si los accesos no cargan, Personal lo dice en vez de enseñar que no hay nadie. */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminStaffViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a failed memberships load is shown and a retry clears it`() {
        var calls = 0
        val viewModel =
            AdminStaffViewModel(
                repository(
                    onMemberships = {
                        calls++
                        if (calls == 1) {
                            Result.failure(IllegalStateException())
                        } else {
                            Result.success(listOf(StaffMembership("m1", "Ana", "ana@correo.mx", "cajero", "")))
                        }
                    },
                ),
            )

        viewModel.onVisible("admin-1")
        val error = waitFor { viewModel.uiState.value.membershipsError }
        assertEquals("No se pudo cargar quién tiene acceso.", error)

        viewModel.refresh()
        waitFor { viewModel.uiState.value.memberships.firstOrNull() }
        assertNull(viewModel.uiState.value.membershipsError)
        assertEquals(
            listOf("Ana"),
            viewModel.uiState.value.memberships
                .map { it.name },
        )
    }

    private fun <T> waitFor(read: () -> T?): T? {
        repeat(100) {
            read()?.let { return it }
            Thread.sleep(20)
        }
        return read()
    }

    /** Solo la lista de invitaciones y la de accesos; el resto no se toca aquí. */
    private fun repository(onMemberships: () -> Result<List<StaffMembership>>): AdminStaffRepository =
        object : AdminStaffRepository by unsupported() {
            override fun invitations(
                state: InvitationState?,
                cursor: String?,
            ) = Result.success(InvitationsPage(emptyList(), null))

            override fun memberships() = onMemberships()
        }

    private fun unsupported(): AdminStaffRepository =
        Proxy.newProxyInstance(
            AdminStaffRepository::class.java.classLoader,
            arrayOf(AdminStaffRepository::class.java),
        ) { _, method, _ ->
            throw UnsupportedOperationException(method.name)
        } as AdminStaffRepository
}
