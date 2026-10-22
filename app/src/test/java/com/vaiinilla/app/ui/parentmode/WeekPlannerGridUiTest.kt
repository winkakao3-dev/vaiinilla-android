package com.vaiinilla.app.ui.parentmode

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaiinilla.app.domain.guardianship.DependentAccountRepository
import com.vaiinilla.app.domain.guardianship.MenuCalendar
import com.vaiinilla.app.domain.guardianship.MyGuardianControls
import com.vaiinilla.app.domain.guardianship.OrderSchedule
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal

/** Apartar la semana: cuadrícula de dos columnas con foto; lo bloqueado dice por qué y no se agrega. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class WeekPlannerGridUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun product(
        id: Int,
        name: String,
    ) = Product(
        id = id,
        categoryId = 1,
        preparationStation = PreparationStation.KITCHEN,
        name = name,
        description = "",
        ingredients = "",
        allergens = "",
        estimatedTimeMinutes = 10,
        counterPrice = "30.00",
        digitalPrice = "30.00",
        available = true,
        imageUrl = "",
        optionGroups = emptyList(),
    )

    private val catalog =
        Catalog(
            categories = listOf(Category(1, "Comida", order = 1)),
            products = listOf(product(10, "Sándwich"), product(11, "Galletas de nuez"), product(12, "Jugo")),
            cursor = null,
        )

    private val account =
        object : DependentAccountRepository by FakeAccount(ParentFixtures.wallet) {
            // Miércoles, sin hora de corte: hoy se puede apartar.
            override fun menuCalendar() =
                Result.success(
                    MenuCalendar(
                        schedule = OrderSchedule("2026-10-21", 2, null, null, emptyList()),
                        days = emptyList(),
                    ),
                )

            override fun controls(): Result<MyGuardianControls?> =
                Result.success(
                    MyGuardianControls(
                        tutorNames = listOf("Marta"),
                        dailyLimit = null,
                        availableToday = null,
                        // El bloqueo del tutor no lo frena a él; la alergia sí.
                        blockedProductIds = setOf(12),
                        allergenProductIds = setOf(11),
                        allergens = emptyList(),
                    ),
                )
        }

    private val catalogRepository =
        object : CatalogRepository by FakeCatalog() {
            override fun getCatalog() = Result.success(catalog)
        }

    private fun showPlanner() {
        composeTestRule.setContent {
            ScreenshotTheme {
                WeekPlannerSheet(
                    child = ParentFixtures.ana,
                    account = account,
                    catalog = catalogRepository,
                    balance = BigDecimal("200.00"),
                    onReserved = {},
                    onDismiss = {},
                )
            }
        }
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(hasTestTag("week-grid")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun `los productos van en dos columnas`() {
        showPlanner()

        val first = composeTestRule.onNodeWithTag("week-product-10").getBoundsInRoot()
        val second = composeTestRule.onNodeWithTag("week-product-11").getBoundsInRoot()
        val third = composeTestRule.onNodeWithTag("week-product-12").getBoundsInRoot()
        assertEquals(first.top, second.top)
        assertTrue(second.left > first.right)
        assertEquals(first.left, third.left)
        assertNotEquals(first.top, third.top)
    }

    @Test
    fun `lo que tiene su alérgeno dice por qué bajo la foto y no tiene Agregar`() {
        showPlanner()

        composeTestRule.onNodeWithText("Tiene algo a lo que es alérgico").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Agregar Galletas de nuez").assertDoesNotExist()
        // Lo que el tutor le bloqueó al hijo sí se le puede apartar.
        composeTestRule.onNodeWithContentDescription("Agregar Jugo", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `lo elegido se marca alrededor de la foto`() {
        showPlanner()

        composeTestRule.onNodeWithTag("week-picked-10", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Agregar Sándwich").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("week-picked-10", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithContentDescription("Uno más", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithContentDescription("Agregar Sándwich").assertDoesNotExist()
    }
}
