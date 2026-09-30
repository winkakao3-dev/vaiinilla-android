package com.vaiinilla.app.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.AccountCollection
import com.vaiinilla.app.domain.repository.AccountOrder
import com.vaiinilla.app.domain.repository.BoardOrder
import com.vaiinilla.app.domain.repository.BoardTable
import com.vaiinilla.app.domain.repository.SpaceAccount
import com.vaiinilla.app.domain.repository.SpaceAvailability
import com.vaiinilla.app.domain.repository.SpaceAvailabilityState
import com.vaiinilla.app.domain.repository.SpaceSessionDetail
import com.vaiinilla.app.domain.repository.SpaceSessionInfo
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.ui.components.CourtsMapCard
import com.vaiinilla.app.ui.operational.CloseCashUi
import com.vaiinilla.app.ui.operational.WaiterUiState
import com.vaiinilla.app.ui.screens.CashCloseContent
import com.vaiinilla.app.ui.screens.CollectAccountContent
import com.vaiinilla.app.ui.screens.KitchenRejectContent
import com.vaiinilla.app.ui.screens.WaiterOperationalScreen
import com.vaiinilla.app.ui.screens.WaiterSpaceSection
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/** Capturas de lo nuevo de canchas y cuentas: para verlo de verdad, no solo compilarlo. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w411dp-h891dp-normal-long-notround-any-xxxhdpi",
    sdk = [33],
)
class SpacesScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun capture(
        name: String,
        mode: VaiinillaThemeMode = VaiinillaThemeMode.Light,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.setContent { ScreenshotTheme(mode = mode, content = content) }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(name)
    }

    private val now: Instant = Instant.now()

    private fun iso(minutes: Long): String = now.plusSeconds(minutes * 60).toString()

    private fun court(
        id: Int,
        state: SpaceAvailabilityState,
        settled: Boolean = true,
        startedMin: Long? = null,
        endsMin: Long? = null,
    ) = SpaceAvailability(
        space = TableSpace(id = id, name = "Cancha $id", type = "cancha"),
        state = state,
        settled = settled,
        endsAt = endsMin?.let(::iso),
        releasesAt = endsMin?.let { iso(it + 5) },
        remainingSeconds = endsMin?.let { (it * 60).toInt() },
        graceMinutes = 5,
        startedAt = startedMin?.let(::iso),
    )

    private val courts =
        listOf(
            court(1, SpaceAvailabilityState.OCUPADA, settled = false, startedMin = -40, endsMin = 20),
            court(2, SpaceAvailabilityState.LIBRE),
            court(3, SpaceAvailabilityState.EN_GRACIA, startedMin = -62, endsMin = -2),
            court(4, SpaceAvailabilityState.POR_COBRAR, settled = false, startedMin = -95, endsMin = -35),
        )

    private fun accountOrder(
        folio: Int,
        total: String,
        pending: Boolean,
        items: String,
    ) = AccountOrder(
        id = "o-$folio",
        folio = folio,
        state = "entregado",
        total = total,
        clientName = "Ana",
        itemsSummary = items,
        payAtEnd = true,
        pending = pending,
    )

    private fun detail(
        availability: SpaceAvailability,
        withSession: Boolean,
        pendingOrders: Boolean = true,
    ) = SpaceSessionDetail(
        availability = availability,
        session =
            if (withSession) {
                SpaceSessionInfo("s-1", iso(-40), availability.endsAt, 3)
            } else {
                null
            },
        account =
            if (withSession) {
                SpaceAccount(
                    orders =
                        listOf(
                            accountOrder(41, "110.00", false, "2× Agua, 1× Refresco"),
                            accountOrder(43, "110.00", pendingOrders, "1× Hamburguesa, 1× Agua"),
                        ),
                    total = "220.00",
                    pending = if (pendingOrders) "110.00" else "0.00",
                    paid = if (pendingOrders) "110.00" else "220.00",
                    settled = !pendingOrders,
                )
            } else {
                null
            },
    )

    @Composable
    private fun SheetHost(content: @Composable ColumnScope.() -> Unit) {
        val colors = rememberOperationalColors()
        Box(Modifier.fillMaxSize().background(colors.background)) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        }
    }

    @Composable
    private fun Section(
        detail: SpaceSessionDetail?,
        space: TableSpace,
        lastCollection: AccountCollection? = null,
        rentable: Boolean = false,
    ) {
        val colors = rememberOperationalColors()
        SheetHost {
            WaiterSpaceSection(
                space = space,
                detail = detail,
                loading = false,
                lastCollection = lastCollection,
                tick = 0,
                acting = false,
                readOnly = false,
                colors = colors,
                onOpenTurn = {},
                onExtend = {},
                onCollect = { _, _, _ -> },
                onRelease = {},
                onDismissCollection = {},
                rentable = rentable,
            )
        }
    }

    private val courtSpace = TableSpace(1, "Cancha 1", "cancha")

    @Test
    fun `spaces_waiter_board_courts`() {
        val tables =
            courts.map { court ->
                BoardTable(
                    space = court.space,
                    call = null,
                    orders =
                        if (court.space.id == 1) {
                            listOf(
                                BoardOrder("o-43", 43, OrderState.READY, 1, "Ana", "1× Hamburguesa", iso(-1)),
                            )
                        } else {
                            emptyList()
                        },
                    availability = court,
                )
            }
        capture("spaces_waiter_board_courts.png") {
            WaiterOperationalScreen(
                state = WaiterUiState(tables = tables, deliveryRequiresQr = false),
                onBack = {},
                placeName = "Club Pádel Norte",
            )
        }
    }

    @Test
    fun `spaces_section_court_busy`() {
        capture("spaces_section_court_busy.png") {
            Section(detail(courts[0], withSession = true), courtSpace)
        }
    }

    @Test
    fun `spaces_section_court_free`() {
        capture("spaces_section_court_free.png") {
            Section(detail(courts[1], withSession = false), TableSpace(2, "Cancha 2", "cancha"))
        }
    }

    @Test
    fun `spaces_section_court_grace`() {
        capture("spaces_section_court_grace.png") {
            Section(detail(courts[2], withSession = true, pendingOrders = false), TableSpace(3, "Cancha 3", "cancha"))
        }
    }

    @Test
    fun `spaces_section_court_unpaid`() {
        capture("spaces_section_court_unpaid.png") {
            Section(detail(courts[3], withSession = true), TableSpace(4, "Cancha 4", "cancha"))
        }
    }

    @Test
    fun `spaces_section_rent_free`() {
        capture("spaces_section_rent_free.png") {
            Section(detail(courts[1], withSession = false), TableSpace(2, "Cancha 2", "cancha"), rentable = true)
        }
    }

    @Test
    fun `spaces_section_rent_busy`() {
        capture("spaces_section_rent_busy.png") {
            Section(detail(courts[0], withSession = true), courtSpace, rentable = true)
        }
    }

    @Test
    fun `spaces_rental_collect`() {
        capture("spaces_rental_collect.png") {
            val colors = rememberOperationalColors()
            Box(Modifier.fillMaxSize().background(colors.background)) {
                CollectAccountContent(
                    total = "330.00",
                    payable = emptyList(),
                    confirming = false,
                    colors = colors,
                    title = "Rentar Cancha 2 · 1 h",
                    onConfirm = { _, _, _ -> },
                )
            }
        }
    }

    @Test
    fun `spaces_collected`() {
        capture("spaces_collected.png") {
            Section(
                detail = null,
                space = courtSpace,
                lastCollection = AccountCollection(2, "220.00", "300.00", "80.00"),
            )
        }
    }

    @Test
    fun `spaces_collect_sheet`() {
        capture("spaces_collect_sheet.png") {
            val colors = rememberOperationalColors()
            Box(Modifier.fillMaxSize().background(colors.background)) {
                CollectAccountContent(
                    total = "220.00",
                    payable = emptyList(),
                    confirming = false,
                    colors = colors,
                    onConfirm = { _, _, _ -> },
                )
            }
        }
    }

    @Test
    fun `spaces_collect_sheet_split`() {
        capture("spaces_collect_sheet_split.png") {
            val colors = rememberOperationalColors()
            Box(Modifier.fillMaxSize().background(colors.background)) {
                CollectAccountContent(
                    total = "220.00",
                    payable =
                        listOf(
                            accountOrder(41, "120.00", pending = true, items = "Hamburguesa y refresco"),
                            accountOrder(42, "60.00", pending = true, items = "Papas grandes"),
                            accountOrder(43, "40.00", pending = true, items = "Cerveza"),
                        ),
                    confirming = false,
                    colors = colors,
                    onConfirm = { _, _, _ -> },
                )
            }
        }
    }

    @Test
    fun `spaces_kitchen_reject_sheet`() {
        capture("spaces_kitchen_reject_sheet.png") {
            val colors = rememberOperationalColors()
            Box(Modifier.fillMaxSize().background(colors.background)) {
                KitchenRejectContent(folio = 312, colors = colors, onConfirm = {})
            }
        }
    }

    @Test
    fun `cash_close_sheet_count`() {
        capture("cash_close_sheet_count.png") {
            val colors = rememberOperationalColors()
            Box(Modifier.fillMaxSize().background(colors.background)) {
                CashCloseContent(CloseCashUi(), colors, onDismiss = {}, onConfirm = {}, onOpenAccounts = {})
            }
        }
    }

    @Test
    fun `cash_close_sheet_blocked`() {
        capture("cash_close_sheet_blocked.png") {
            val colors = rememberOperationalColors()
            Box(Modifier.fillMaxSize().background(colors.background)) {
                CashCloseContent(
                    CloseCashUi(blockedByAccounts = true),
                    colors,
                    onDismiss = {},
                    onConfirm = {},
                    onOpenAccounts = {},
                )
            }
        }
    }

    @Test
    fun `spaces_courts_map_light`() {
        capture("spaces_courts_map_light.png") {
            Box(Modifier.fillMaxSize().background(LocalVaiinillaColors.current.paper).padding(20.dp)) {
                CourtsMapCard(courts = courts)
            }
        }
    }

    @Test
    fun `spaces_courts_map_dark`() {
        capture("spaces_courts_map_dark.png", mode = VaiinillaThemeMode.Dark) {
            Box(Modifier.fillMaxSize().background(LocalVaiinillaColors.current.paper).padding(20.dp)) {
                CourtsMapCard(courts = courts)
            }
        }
    }
}
