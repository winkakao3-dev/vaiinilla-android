package com.vaiinilla.app.ui.parentmode

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
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
        dark {
            WalletFamilySection(
                children = children,
                onOpen = {},
                onAdd = {},
                onConsent = {},
                entry = WalletSchoolEntry.UNKNOWN,
                onRole = {},
                onLinkCode = {},
            )
        }
        settle()
        captureScreenRoboImage("lugar/e05_primera_vez.png")
    }

    @Test
    @Config(qualifiers = "w411dp-h1800dp-normal-long-notround-any-xxxhdpi")
    fun e07_ficha_del_hijo() {
        val state = ChildAccountState(sofia, ParentFixtures.services(FakeGuardianship(), FakeAccount(ParentFixtures.wallet, weekOrders())))
        dark { ChildAccountContent(state = state, onChildChanged = {}, onUnlinked = {}, onBack = {}) }
        settle()
        captureScreenRoboImage("lugar/e07_ficha.png")
    }

    @Test
    @Config(qualifiers = "w411dp-h1100dp-normal-long-notround-any-xxxhdpi")
    fun e11_seguridad_y_tutores() {
        val state = ChildAccountState(sofia, ParentFixtures.services(FakeGuardianship()))
        dark {
            ChildAccountContent(state = state, onChildChanged = {}, onUnlinked = {}, onBack = {}, startOnSecurity = true)
        }
        settle()
        captureScreenRoboImage("lugar/e11_seguridad.png")
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
        captureScreenRoboImage("lugar/e08_controles.png")
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
            )
        }
        settle()
        captureScreenRoboImage("lugar/e09_recargar.png")
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
                            schedule = OrderSchedule("2026-11-03", 2, null, null, emptyList()),
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
            )
        }
        settle()
        captureScreenRoboImage("lugar/e10_apartar.png")
    }
}