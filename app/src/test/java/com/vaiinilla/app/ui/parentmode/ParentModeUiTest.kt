package com.vaiinilla.app.ui.parentmode

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Cartera › Mis hijos y la ficha de un hijo, con repositorios de mentira. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class ParentModeUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Mis hijos muestra el saldo, al pendiente sin saldo y el aviso que falta`() {
        val children =
            ChildrenState(ParentFixtures.services(FakeGuardianship(listOf(ParentFixtures.ana, ParentFixtures.tilin))))
        runBlocking { children.load() }
        val opened = mutableListOf<String>()
        composeTestRule.setContent {
            ScreenshotTheme {
                WalletFamilySection(
                    children = children,
                    onOpen = { opened += it.userId },
                    onAdd = {},
                    onConsent = {},
                    entry = WalletSchoolEntry.PARENT,
                    onRole = {},
                    onLinkCode = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Mis hijos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Falta tu permiso para los datos de Ana").assertIsDisplayed()
        // Su saldo ($40) ya bajó del aviso que puso el tutor ($50).
        composeTestRule.onNodeWithText("Saldo bajo").assertIsDisplayed()
        composeTestRule.onNodeWithText("\$40").assertIsDisplayed()
        // El pendiente no tiene saldo: ni "$0" ni ficha que abrir.
        composeTestRule.onNodeWithText("Esperando a que la escuela te apruebe").assertIsDisplayed()
        composeTestRule.onNodeWithText("\$0").assertDoesNotExist()
        composeTestRule.onNodeWithText("Agregar otro hijo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cómo funciona para tutores").assertIsDisplayed()
        composeTestRule.onNodeWithText("¿Eres tutor o alumno?").assertDoesNotExist()

        composeTestRule.onNodeWithTag("child-u-ana").performClick()
        assertEquals(listOf("u-ana"), opened)
    }

    @Test
    fun `aceptar el aviso reemplaza la lista y la tarjeta se va`() {
        val children = ChildrenState(ParentFixtures.services(FakeGuardianship(listOf(ParentFixtures.ana))))
        runBlocking {
            children.load()
            children.acceptPrivacyNotice("v1", "llave")
        }
        assertEquals(emptyList<Any>(), children.childrenNeedingConsent)
    }

    @Test
    fun `la ficha del principal trae quién lo cuida, En Caja y Si deja la escuela`() {
        val state = ChildAccountState(ParentFixtures.ana, ParentFixtures.services(FakeGuardianship()))
        composeTestRule.setContent {
            ScreenshotTheme { ChildAccountContent(state = state, onChildChanged = {}, onUnlinked = {}, onBack = {}) }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Ana López").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recargar con tarjeta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hasta \$60 al día").assertIsDisplayed()
        val list =
            composeTestRule.onNodeWithTag("child-account-list")
        list.performScrollToNode(hasText("Quién lo cuida"))
        composeTestRule.onNodeWithText("Marta López (tú)").assertIsDisplayed()
        list.performScrollToNode(hasText("Invitar a otro tutor"))
        list.performScrollToNode(hasText("Recarga con tarjeta"))
        list.performScrollToNode(hasText("Bloquear su credencial"))
        list.performScrollToNode(hasText("Pedir que le devuelvan su saldo"))
    }

    @Test
    fun `quien no es principal ve quién pone los controles y no ve En Caja`() {
        val luisView =
            ParentFixtures.ana.copy(
                isPrincipal = false,
                guardians =
                    listOf(
                        ParentFixtures.ana.guardians[0].copy(isYou = false),
                        ParentFixtures.ana.guardians[1].copy(isYou = true),
                    ),
            )
        val state = ChildAccountState(luisView, ParentFixtures.services(FakeGuardianship()))
        composeTestRule.setContent {
            ScreenshotTheme { ChildAccountContent(state = state, onChildChanged = {}, onUnlinked = {}, onBack = {}) }
        }
        composeTestRule.waitForIdle()

        val list =
            composeTestRule.onNodeWithTag("child-account-list")
        list.performScrollToNode(
            hasText("Marta pone el límite, los bloqueos y las alergias. Tú ves, recargas y apartas."),
        )
        composeTestRule.onNodeWithText("En Caja").assertDoesNotExist()
        composeTestRule.onNodeWithText("Si deja la escuela").assertDoesNotExist()
        composeTestRule.onNodeWithText("Invitar a otro tutor").assertDoesNotExist()
    }
}
