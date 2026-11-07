package com.vaiinilla.app.ui.parentmode

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.vaiinilla.app.domain.guardianship.DependentAccountRepository
import com.vaiinilla.app.domain.guardianship.MenuCalendar
import com.vaiinilla.app.domain.guardianship.OrderSchedule
import com.vaiinilla.app.domain.guardianship.ScheduledOrder
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.ui.screenshot.ScreenshotTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal
import java.time.LocalDate

/** Padres e hijos en oscuro junto al lienzo de Doop (E05 y E07 a E11). Solo captura: el comportamiento lo prueban ParentModeUiTest y compañía. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class CarteraDoopScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun dark(content: @Composable () -> Unit) =
        composeTestRule.setContent { ScreenshotTheme(mode = VaiinillaThemeMode.Dark, content = content) }

    /** Cuenta los nodos con texto sin esperar a que la pantalla quede quieta: las animaciones infinitas nunca la dejan. */
    private fun textNodeCount(): Int {
        val auto = composeTestRule.mainClock.autoAdvance
        composeTestRule.mainClock.autoAdvance = false
        try {
            return composeTestRule.onAllNodes(hasText("", substring = true)).fetchSemanticsNodes().size
        } finally {
            composeTestRule.mainClock.autoAdvance = auto
        }
    }

    /** La captura solo compara con una imagen si Roborazzi corre en modo verify; esta comprobación falla siempre que la pantalla sale vacía. */
    private fun capture(name: String) {
        val withText = textNodeCount()
        assertTrue("La pantalla $name salió sin texto", withText >= MIN_TEXT_NODES)
        captureScreenRoboImage("lugar/$name.png")
    }

    private fun settle(ms: Long = 1_500) {
        composeTestRule.mainClock.advanceTimeBy(ms)
        composeTestRule.waitForIdle()
    }

    private fun product(
        id: Int,
        name: String,
        price: String,
    ) = Product(
        id = id,
        categoryId = 1,
        preparationStation = PreparationStation.KITCHEN,
        name = name,
        description = "",
        ingredients = "",
        allergens = "",
        estimatedTimeMinutes = 10,
        counterPrice = price,
        digitalPrice = price,
        available = true,
        imageUrl = "",
        optionGroups = emptyList(),
    )

    @Test
    fun e05_primera_vez() {
        val children = ChildrenState(ParentFixtures.services(FakeGuardianship()))
        runBlocking { children.load() }
        children.markParentUnlocked()
        dark {
            SchoolOptionsPage(
                children = children,
                role = SchoolRole.PARENT,
                hasTutors = false,
                onAccountClaimed = {},
                onBack = {},
            )
        }
        settle()
        capture("e05_primera_vez")
    }

    @Test
    @Config(qualifiers = "w411dp-h1800dp-normal-long-notround-any-xxxhdpi")
    fun e07_ficha_del_hijo() {
        val state =
            ChildAccountState(
                sofia,
                ParentFixtures.services(FakeGuardianship(), FakeAccount(ParentFixtures.wallet, weekOrders())),
            )
        dark { ChildAccountContent(state = state, onChildChanged = {}, onUnlinked = {}, onBack = {}) }
        settle()
        capture("e07_ficha")
    }

    @Test
    @Config(qualifiers = "w411dp-h1100dp-normal-long-notround-any-xxxhdpi")
    fun e11_seguridad_y_tutores() {
        val state = ChildAccountState(sofia, ParentFixtures.services(FakeGuardianship()))
        dark {
            ChildAccountContent(
                state = state,
                onChildChanged = {},
                onUnlinked = {},
                onBack = {},
                startOnSecurity = true,
            )
        }
        settle()
        capture("e11_seguridad")
    }

    private val sofia =
        ParentFixtures.ana.copy(
            controls =
                ParentFixtures.ana.controls!!.copy(
                    spentToday = BigDecimal("42.00"),
                    availableToday = BigDecimal("18.00"),
                ),
        )

    private fun weekOrders(): List<ScheduledOrder> {
        val week = schoolWeek(LocalDate.now())
        return listOf(0, 1, 3).mapIndexed { i, day ->
            ScheduledOrder(
                id = "o$i",
                folio = 100 + i,
                statusWire = "cobrado",
                version = 1,
                total = BigDecimal("55.00"),
                deliveryDate = week[day].toString(),
                deliverySlot = "10:30-11:00",
                summary = "Molletes con fruta",
            )
        }
    }

    @Test
    fun e08_controles() {
        dark {
            ChildControlsSheet(
                child = ParentFixtures.ana,
                services = ParentFixtures.services(FakeGuardianship()),
                onSaved = {},
                onUnlinked = {},
                onDismiss = {},
            )
        }
        settle()
        capture("e08_controles")
    }

    @Test
    fun e09_recargar_hijo() {
        dark {
            ChildReloadSheet(
                child = ParentFixtures.ana,
                balance = BigDecimal("40.00"),
                account = FakeAccount(ParentFixtures.wallet),
                range = null,
                unsettledReloadId = null,
                onUnsettledChange = {},
                onCredited = {},
                onDismiss = {},
                initialAmount = 200,
            )
        }
        settle()
        capture("e09_recargar")
    }

    @Test
    fun e10_apartar_semana() {
        val catalog =
            Catalog(
                categories = listOf(Category(1, "Comida", order = 1)),
                products =
                    listOf(
                        product(10, "Sándwich de pavo y fruta", "55.00"),
                        product(11, "Arroz con pollo", "62.00"),
                        product(12, "Pan de nuez", "30.00"),
                    ),
                cursor = null,
            )
        val catalogRepository =
            object : CatalogRepository by FakeCatalog() {
                override fun getCatalog() = Result.success(catalog)
            }
        val account =
            object : DependentAccountRepository by FakeAccount(ParentFixtures.wallet) {
                override fun menuCalendar() =
                    Result.success(
                        MenuCalendar(
                            schedule =
                                OrderSchedule(
                                    "2026-11-03",
                                    4,
                                    null,
                                    null,
                                    listOf("09:30-10:00", "10:30-11:00", "12:00-12:30", "13:30-14:00"),
                                ),
                            days = emptyList(),
                        ),
                    )
            }
        dark {
            WeekPlannerSheet(
                child = ParentFixtures.ana,
                account = account,
                catalog = catalogRepository,
                balance = BigDecimal("42.00"),
                onReserved = {},
                onDismiss = {},
                onReload = {},
            )
        }
        settle()
        composeTestRule.onNodeWithTag("week-product-10").performClick()
        settle()
        capture("e10_apartar")
    }

    private companion object {
        const val MIN_TEXT_NODES = 3
    }
}
