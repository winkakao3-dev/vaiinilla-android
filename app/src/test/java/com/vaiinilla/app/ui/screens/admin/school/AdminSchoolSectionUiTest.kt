package com.vaiinilla.app.ui.screens.admin.school

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.domain.repository.CatalogRepositoryException
import com.vaiinilla.app.domain.school.GuardianRequest
import com.vaiinilla.app.domain.school.MenuCalendar
import com.vaiinilla.app.domain.school.RefundMethod
import com.vaiinilla.app.domain.school.RosterStudent
import com.vaiinilla.app.domain.school.SchoolSettings
import com.vaiinilla.app.domain.school.SchoolStudentRequest
import com.vaiinilla.app.domain.school.StudentRequestKind
import com.vaiinilla.app.ui.admin.school.AdminSchoolUiState
import com.vaiinilla.app.ui.admin.school.CASH_SESSION_REQUIRED_TEXT
import com.vaiinilla.app.ui.admin.school.resolvedText
import com.vaiinilla.app.ui.admin.school.schoolErrorMessage
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AdminSchoolSectionUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val settings =
        SchoolSettings(
            joinCode = "SANDBX",
            joinRequiresCode = false,
            guardiansNeedApproval = true,
            reloadMinimum = "50",
            reloadMaximum = "2000",
            daysAhead = 5,
            orderCutoff = "09:00",
            cancelCutoff = "08:00",
        )

    private val request =
        GuardianRequest(
            id = "t1",
            guardianName = "Rosa Ramírez",
            guardianEmail = "rosa@x.mx",
            studentName = "Tilin Ramírez",
            studentId = "A100",
            group = "3B",
            studentHasAccount = false,
            inRoster = false,
            otherGuardians = 0,
        )

    private val cashRefund =
        SchoolStudentRequest(
            id = "s1",
            kind = StudentRequestKind.REFUND,
            refundMethod = RefundMethod.CASH,
            note = "Se cambia de escuela",
            balanceAtRequest = "150.00",
            currentBalance = "120.00",
            guardianName = "Rosa Ramírez",
            guardianEmail = null,
            studentName = "Tilin Ramírez",
            studentId = "A100",
            group = "3B",
        )

    private fun state(
        requests: List<GuardianRequest> = emptyList(),
        studentRequests: List<SchoolStudentRequest> = emptyList(),
    ) = AdminSchoolUiState(
        settings = settings,
        menu = MenuCalendar(LocalDate.of(2026, 10, 21), emptyMap(), emptyList()),
        roster = listOf(RosterStudent("A200", "Otro", "1A")),
        requests = requests,
        guardianships = null,
        studentRequests = studentRequests,
    )

    @Test
    fun `guardian request shows the student, roster facts and approves`() {
        val reviewed = mutableListOf<Pair<String, Boolean>>()
        composeTestRule.setContent {
            AdminSchoolContent(
                state = state(requests = listOf(request)),
                products = emptyList(),
                colors = rememberOperationalColors(),
                readOnly = false,
                actions = AdminSchoolActions(onReview = { r, approve -> reviewed += r.id to approve }),
                onBack = {},
            )
        }
        composeTestRule.onNodeWithText("Tilin Ramírez").assertExists()
        composeTestRule.onNodeWithText("A100 · 3B").assertExists()
        composeTestRule.onNodeWithText("Lo pide Rosa Ramírez · rosa@x.mx").assertExists()
        composeTestRule.onNodeWithText("No está en el padrón · Sin otro tutor · Sin cuenta propia").assertExists()

        composeTestRule.onNodeWithText("Aprobar").performClick()
        assertEquals(listOf("t1" to true), reviewed)

        composeTestRule.onNodeWithText("Rechazar").performClick()
        composeTestRule.onNodeWithText("¿Rechazar a Rosa Ramírez?").assertExists()
        composeTestRule
            .onNodeWithText(
                "No verá ni cuidará a Tilin Ramírez. " +
                    "Si nadie más lo cuida, su matrícula queda libre para su verdadero tutor.",
            ).assertExists()
        composeTestRule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Rechazar"))
        composeTestRule.onAllNodes(hasText("Rechazar")).onFirst().performClick()
        assertEquals(listOf("t1" to true, "t1" to false), reviewed)
    }

    @Test
    fun `cash refund confirmation says it leaves the open cash register`() {
        val resolved = mutableListOf<String>()
        composeTestRule.setContent {
            AdminSchoolContent(
                state = state(studentRequests = listOf(cashRefund)),
                products = emptyList(),
                colors = rememberOperationalColors(),
                readOnly = false,
                actions = AdminSchoolActions(onResolve = { resolved += it.id }),
                onBack = {},
            )
        }
        composeTestRule
            .onAllNodes(
                hasScrollAction(),
            ).onFirst()
            .performScrollToNode(hasText("Devolver $120 en efectivo"))
        composeTestRule.onNodeWithText("Devolver $120 en efectivo").assertExists()
        composeTestRule.onNodeWithText("Al pedirla tenía $150; se entrega lo de hoy.").assertExists()
        composeTestRule.onNodeWithText("“Se cambia de escuela”").assertExists()

        composeTestRule.onNodeWithText("Ya se lo entregué").performClick()
        composeTestRule.onNodeWithText("¿Ya le entregaste $120 a Rosa Ramírez?").assertExists()
        composeTestRule
            .onNodeWithText(
                "Sale de la caja abierta y queda en su corte. " +
                    "Su saldo queda en cero; lo que gaste después ya no entra en esta solicitud.",
            ).assertExists()
        composeTestRule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Sí, ya se lo entregué"))
        composeTestRule.onNodeWithText("Sí, ya se lo entregué").performClick()
        assertEquals(listOf("s1"), resolved)
    }

    @Test
    fun `refund without balance cannot be resolved and rejecting asks for a reason`() {
        val rejected = mutableListOf<String>()
        composeTestRule.setContent {
            AdminSchoolContent(
                state = state(studentRequests = listOf(cashRefund.copy(currentBalance = "0.00"))),
                products = emptyList(),
                colors = rememberOperationalColors(),
                readOnly = false,
                actions = AdminSchoolActions(onReject = { _, reason, _ -> rejected += reason }),
                onBack = {},
            )
        }
        composeTestRule
            .onAllNodes(
                hasScrollAction(),
            ).onFirst()
            .performScrollToNode(hasText("Ya no le queda saldo: recházala."))
        composeTestRule.onNodeWithText("Ya se lo entregué").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Rechazar").performClick()
        composeTestRule.onNodeWithText("¿Por qué no procede?").assertExists()
        composeTestRule.onNodeWithText("Rosa Ramírez lo verá en la ficha de Tilin Ramírez.").assertExists()
    }

    @Test
    fun `cash refund success and closed register texts talk about the cash register`() {
        assertEquals("Listo: salieron $120 de la caja para Rosa Ramírez", resolvedText(cashRefund))
        assertEquals(
            "Listo: entrega $120 a Rosa Ramírez",
            resolvedText(cashRefund.copy(refundMethod = RefundMethod.TRANSFER)),
        )
        val server =
            CatalogRepositoryException(
                "CASH_SESSION_REQUIRED",
                "Abre la caja para devolver efectivo: el dinero sale del cajón.",
                ApiClientException(
                    "CASH_SESSION_REQUIRED",
                    "Abre la caja para devolver efectivo: el dinero sale del cajón.",
                    422,
                ),
            )
        assertEquals(
            "Abre la caja para devolver efectivo: el dinero sale del cajón.",
            schoolErrorMessage(server, "No se guardó."),
        )
        val bare = CatalogRepositoryException("CASH_SESSION_REQUIRED", "CASH_SESSION_REQUIRED")
        assertEquals(CASH_SESSION_REQUIRED_TEXT, schoolErrorMessage(bare, "No se guardó."))
    }
}
