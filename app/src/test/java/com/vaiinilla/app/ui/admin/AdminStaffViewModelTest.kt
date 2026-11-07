package com.vaiinilla.app.ui.admin

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.domain.admin.InvitationState
import com.vaiinilla.app.domain.admin.InvitationsPage
import com.vaiinilla.app.domain.admin.StaffMembership
import com.vaiinilla.app.domain.repository.AdminStaffRepository
import com.vaiinilla.app.domain.repository.CatalogRepositoryException
import com.vaiinilla.app.ui.operational.StaffHarness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        StaffHarness.restoreMain()
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
        waitFor {
            viewModel.uiState.value.memberships
                .firstOrNull()
        }
        assertNull(viewModel.uiState.value.membershipsError)
        assertEquals(
            listOf("Ana"),
            viewModel.uiState.value.memberships
                .map { it.name },
        )
    }

    @Test
    fun `a server error on invite stays in the form`() {
        val serverError =
            CatalogRepositoryException(
                "INTERNAL_ERROR",
                "Error interno",
                ApiClientException("INTERNAL_ERROR", "Error interno", httpStatus = 500),
            )
        val message = inviteErrorMessage(serverError)
        assertTrue(message.startsWith("No se pudo enviar la invitación"))
        assertEquals(
            "Ya hay una invitación pendiente para ese correo y rol.",
            inviteErrorMessage(CatalogRepositoryException("INVITATION_PENDING_EXISTS", "x")),
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
