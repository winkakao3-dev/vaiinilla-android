package com.vaiinilla.app.ui.parentmode

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.GuardianshipException
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Cartera › Soy tutor con y sin el código de la escuela, y la hoja que lo pide. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class ParentUnlockUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun status(unlocked: Boolean) =
        OperationalStatus(
            acceptingOrders = true,
            cashSessionOpen = true,
            cashierOnline = true,
            kitchenOnline = true,
            estimatedTimeMinutes = 5,
            consultedAt = "2026-10-22T16:53:34Z",
            isSchool = true,
            isParentUnlocked = unlocked,
        )

    /** Responde al código como el servidor: SANDBX pasa, cualquier otro es JOIN_CODE_INVALID. */
    private class UnlockGuardianship(
        children: List<Dependent> = emptyList(),
    ) : FakeGuardianship(children) {
        val sentCodes = mutableListOf<String>()

        override fun unlockParent(
            schoolCode: String,
            idempotencyKey: String,
        ): Result<Unit> {
            sentCodes += schoolCode
            return if (schoolCode == "SANDBX") {
                Result.success(Unit)
            } else {
                Result.failure(
                    GuardianshipException(GuardianshipException.JOIN_CODE_INVALID, "Ese no es el código.", 422),
                )
            }
        }
    }

    /** Pide el código de la escuela al agregar hasta que se lo dan. */
    private class LockedAddGuardianship : FakeGuardianship() {
        var unlocked = false
        val added = mutableListOf<String>()

        override fun unlockParent(
            schoolCode: String,
            idempotencyKey: String,
        ): Result<Unit> = Result.success(Unit).also { unlocked = true }

        override fun addDependent(
            name: String,
            studentId: String,
            group: String?,
            consentVersion: String,
            idempotencyKey: String,
        ): Result<Dependent> {
            if (!unlocked) {
                return Result.failure(
                    GuardianshipException(GuardianshipException.PARENT_CODE_REQUIRED, "Falta el código", 403),
                )
            }
            added += name
            return Result.success(ParentFixtures.tilin)
        }
    }

    private fun waitForText(text: String) =
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }

    private fun showOptions(
        guardianship: FakeGuardianship,
        unlocked: Boolean,
    ) {
        val children =
            ChildrenState(ParentFixtures.services(guardianship, catalog = FakeCatalog(status(unlocked))))
        runBlocking { children.load() }
        composeTestRule.setContent {
            ScreenshotTheme {
                SchoolOptionsPage(
                    children = children,
                    role = SchoolRole.PARENT,
                    hasTutors = false,
                    onAccountClaimed = {},
                    onBack = {},
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `sin el código de la escuela solo sale Tengo el código`() {
        showOptions(UnlockGuardianship(), unlocked = false)

        composeTestRule.onNodeWithTag("school-options-unlock").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tengo el código de la escuela").assertIsDisplayed()
        composeTestRule.onNodeWithTag("school-options-add").assertDoesNotExist()
    }

    @Test
    fun `ya habilitado sale Agregar a mi hijo como antes`() {
        showOptions(UnlockGuardianship(), unlocked = true)

        composeTestRule.onNodeWithText("Agregar a mi hijo").assertIsDisplayed()
        composeTestRule.onNodeWithTag("school-options-unlock").assertDoesNotExist()
    }

    @Test
    fun `quien ya cuida hijos puede agregar otro aunque no haya dado el código`() {
        showOptions(UnlockGuardianship(listOf(ParentFixtures.tilin)), unlocked = false)

        composeTestRule.onNodeWithText("Agregar otro hijo").assertIsDisplayed()
        composeTestRule.onNodeWithTag("school-options-unlock").assertDoesNotExist()
    }

    @Test
    fun `un código malo se queda en la hoja con un error claro y el bueno habilita agregar`() {
        val guardianship = UnlockGuardianship()
        showOptions(guardianship, unlocked = false)

        composeTestRule.onNodeWithTag("school-options-unlock").performClick()
        composeTestRule.onNodeWithText("El código de la escuela").assertIsDisplayed()
        // Lo tecleado pasa a mayúsculas y pierde lo ambiguo (0, O, 1, I).
        composeTestRule.onNodeWithTag("parent-unlock-field").performTextInput("zz0zzz")
        composeTestRule.onNodeWithTag("parent-unlock-submit").performClick()
        val invalid = "Ese no es el código de esta escuela. Revisa la circular o pídeselo a la escuela."
        waitForText(invalid)
        assertEquals(listOf("ZZZZZ"), guardianship.sentCodes)
        composeTestRule.onNodeWithText(invalid).assertIsDisplayed()

        guardianship.sentCodes.clear()
        composeTestRule.onNodeWithTag("parent-unlock-field").performTextClearance()
        composeTestRule.onNodeWithTag("parent-unlock-field").performTextInput("sandbx")
        composeTestRule.onNodeWithTag("parent-unlock-submit").performClick()
        waitForText("Agregar a mi hijo")
        assertEquals(listOf("SANDBX"), guardianship.sentCodes)
        composeTestRule.onNodeWithText("Agregar a mi hijo").assertIsDisplayed()
    }

    @Test
    fun `al agregar sin codigo de la escuela se pide y se reenvia lo escrito`() {
        val guardianship = LockedAddGuardianship()
        var unlockedCalls = 0
        composeTestRule.setContent {
            ScreenshotTheme {
                AddChildSheet(
                    guardianship,
                    onAdded = {},
                    onDismiss = {},
                    onParentUnlocked = { unlockedCalls++ },
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Nombre").performTextInput("Tilin Ramírez")
        composeTestRule.onNodeWithText("Matrícula").performTextInput("A100")
        composeTestRule.onNodeWithTag("add-child-consent").performClick()
        composeTestRule.onNodeWithTag("add-child-submit").performClick()
        waitForText("El código de la escuela")
        composeTestRule.onNodeWithText("El código de la escuela").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parent-unlock-field").performTextInput("SANDBX")
        composeTestRule.onNodeWithTag("parent-unlock-submit").performClick()
        composeTestRule.waitUntil(5_000) { guardianship.added.isNotEmpty() }

        assertEquals(1, unlockedCalls)
        assertEquals(listOf("Tilin Ramírez"), guardianship.added)
    }

    @Test
    fun `muchos intentos dicen cuánto esperar`() {
        val limited =
            GuardianshipException(
                GuardianshipException.RATE_LIMITED,
                "Demasiados intentos",
                429,
                ApiClientException("RATE_LIMITED", "Demasiados intentos", 429, retryAfterSeconds = 541),
            )
        assertEquals("Fueron muchos intentos. Espera 10 minutos y vuelve a probar.", parentUnlockMessage(limited))
        val withoutHeader = GuardianshipException(GuardianshipException.RATE_LIMITED, "Demasiados intentos", 429)
        assertEquals(
            "Fueron muchos intentos. Espera unos minutos y vuelve a probar.",
            parentUnlockMessage(withoutHeader),
        )
    }
}
