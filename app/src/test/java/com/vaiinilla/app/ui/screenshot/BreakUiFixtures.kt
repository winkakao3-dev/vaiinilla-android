package com.vaiinilla.app.ui.screenshot

import com.vaiinilla.app.TestFixtureSource
import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.guardianship.ChildGuardian
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.DependentControls
import com.vaiinilla.app.domain.guardianship.MyGuardianControls
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.WalletData
import com.vaiinilla.app.domain.model.WalletMovement
import com.vaiinilla.app.domain.model.WalletSnapshot
import com.vaiinilla.app.domain.repository.BoardOrder
import com.vaiinilla.app.domain.repository.BoardTable
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.TableCallTaker
import com.vaiinilla.app.domain.repository.TableSpace
import com.vaiinilla.app.ui.operational.WaiterUiState
import com.vaiinilla.app.ui.order.OrderFlowUiState
import com.vaiinilla.app.ui.wallet.WalletRemoteUiState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.math.BigDecimal
import java.time.Instant

/** Los juegos de datos de break-ui: el de siempre, el peor caso creíble, vacío, uno solo y muchos. */
internal enum class BreakUiData(
    val slug: String,
) {
    DEMO("demo"),
    PEOR("peor"),
    VACIO("vacio"),
    UNO("uno"),
    MUCHOS("muchos"),
}

/**
 * El peor caso entra por el mismo lugar que el demo: el JSON del contrato pasa por los parsers de la app
 * (`fixtures/peor/`), y lo que solo existe como estado de pantalla se arma aquí. Los textos respetan los
 * límites del backend (nombre de producto 120, de espacio y opción 80, notas de cocina 500).
 */
internal object BreakUiFixtures {
    private val source = TestFixtureSource()
    private val parser = ContractResponseParser()
    private val orderJson = OrderContractJson()
    private val json = Json { prettyPrint = false }

    // Una hora fija: "hace 23 min" sale igual en cada corrida.
    private val now: Instant = Instant.now()

    private fun ago(seconds: Long) = now.minusSeconds(seconds).toString()

    private val peorCatalogRaw: String by lazy { source.read("fixtures/peor/catalog.json") }
    private val peorOrderRaw: String by lazy { source.read("fixtures/peor/order.json") }

    private fun JsonElement.obj() = jsonObject

    private fun JsonObject.with(vararg pairs: Pair<String, JsonElement>) = JsonObject(this + pairs)

    private fun catalogFrom(transform: (JsonObject) -> JsonObject): Catalog {
        val root = json.parseToJsonElement(peorCatalogRaw).obj()
        return parser.parseCatalog(json.encodeToString(JsonObject.serializer(), transform(root)))
    }

    fun catalog(data: BreakUiData): Catalog =
        when (data) {
            BreakUiData.DEMO -> ScreenshotFixtures.catalog()
            BreakUiData.PEOR -> parser.parseCatalog(peorCatalogRaw)
            BreakUiData.VACIO -> Catalog(emptyList(), emptyList(), null)
            BreakUiData.UNO ->
                catalogFrom { root ->
                    val data = root.getValue("data").obj()
                    val pan = data.getValue("productos").jsonArray.first { it.obj()["nombre"] == JsonPrimitive("Pan") }
                    val category =
                        data.getValue("categorias").jsonArray.first {
                            it.obj()["id"] ==
                                pan.obj()["categoria_id"]
                        }
                    root.with(
                        "data" to
                            data.with(
                                "categorias" to JsonArray(listOf(category)),
                                "productos" to JsonArray(listOf(pan)),
                            ),
                    )
                }
            // Un catálogo sin paginar de 1,000 productos: los del peor caso repetidos con su propio id.
            BreakUiData.MUCHOS ->
                catalogFrom { root ->
                    val data = root.getValue("data").obj()
                    val base = data.getValue("productos").jsonArray
                    val many =
                        (0 until 1_000).map { index ->
                            val product = base[index % base.size].obj()
                            product.with("id" to JsonPrimitive(100_000 + index))
                        }
                    root.with("data" to data.with("productos" to JsonArray(many)))
                }
        }

    fun catalogState(data: BreakUiData): OrderFlowUiState =
        OrderFlowUiState(
            loading = false,
            catalog = catalog(data),
            operationalStatus = ScreenshotFixtures.operationalStatus(),
        )

    private fun orderFrom(transform: (JsonObject) -> JsonObject): OrderDetail {
        val root = json.parseToJsonElement(peorOrderRaw).obj()
        val data = root.getValue("data").obj()
        return orderJson.parseOrderDetail(
            json.encodeToString(
                JsonObject.serializer(),
                root.with(
                    "data" to transform(data),
                ),
            ),
        )
    }

    /** El pedido de una persona: el peor caso trae 6 artículos, 12 piezas de uno, folio de 6 cifras y notas de 474 letras. */
    fun order(
        data: BreakUiData,
        state: OrderState = OrderState.PENDING_PAYMENT,
    ): OrderDetail {
        val order =
            when (data) {
                BreakUiData.DEMO -> ScreenshotFixtures.sampleOrder(state = state)
                BreakUiData.PEOR, BreakUiData.VACIO -> orderJson.parseOrderDetail(peorOrderRaw)
                // Un artículo, una pieza, sin notas ni opciones: "1 artículo".
                BreakUiData.UNO ->
                    orderFrom { order ->
                        val first = order.getValue("items").jsonArray[1].obj()
                        order.with(
                            "items" to JsonArray(listOf(first)),
                            "subtotal" to JsonPrimitive("12.00"),
                            "total" to JsonPrimitive("12.00"),
                            "ahorro_combinado" to JsonPrimitive("0.00"),
                            "cashback_otorgado" to JsonPrimitive("0.00"),
                            "notas_cocina" to JsonPrimitive(""),
                            "folio" to JsonPrimitive(1),
                        )
                    }
                // Un pedido de evento: 36 líneas, el peor caso repetido.
                BreakUiData.MUCHOS ->
                    orderFrom { order ->
                        val items = order.getValue("items").jsonArray
                        val many =
                            (0 until 36).map { index ->
                                items[index % items.size].obj().with(
                                    "id" to JsonPrimitive(20_000 + index),
                                )
                            }
                        order.with("items" to JsonArray(many))
                    }
            }
        return order.copy(summary = order.summary.copy(state = state))
    }

    private val orderStates =
        listOf(
            OrderState.PENDING_PAYMENT,
            OrderState.PAID,
            OrderState.PREPARING,
            OrderState.READY,
            OrderState.DELIVERED,
        )

    /** La lista de pedidos de una persona o de un turno. */
    fun orders(
        data: BreakUiData,
        muchos: Int = 300,
    ): List<OrderDetail> =
        when (data) {
            BreakUiData.VACIO -> emptyList()
            BreakUiData.UNO -> listOf(order(BreakUiData.UNO))
            BreakUiData.DEMO ->
                listOf(OrderState.PENDING_PAYMENT, OrderState.PREPARING).mapIndexed { index, state ->
                    val order = order(BreakUiData.DEMO, state)
                    order.copy(summary = order.summary.copy(id = "demo-$index", folio = order.summary.folio + index))
                }
            BreakUiData.PEOR ->
                listOf(
                    order(BreakUiData.PEOR, OrderState.PENDING_PAYMENT),
                    order(BreakUiData.UNO, OrderState.PREPARING),
                    order(BreakUiData.PEOR, OrderState.READY),
                    order(BreakUiData.MUCHOS, OrderState.PAID),
                ).mapIndexed { index, order ->
                    order.copy(summary = order.summary.copy(id = "peor-$index", folio = 104_382 + index * 7_919))
                }
            BreakUiData.MUCHOS ->
                (0 until muchos).map { index ->
                    val order =
                        order(
                            if (index % 3 ==
                                0
                            ) {
                                BreakUiData.UNO
                            } else {
                                BreakUiData.PEOR
                            },
                            orderStates[index % orderStates.size],
                        )
                    order.copy(summary = order.summary.copy(id = "muchos-$index", folio = 100_000 + index))
                }
        }

    fun cartState(data: BreakUiData): OrderFlowUiState {
        val catalog = catalog(if (data == BreakUiData.DEMO) BreakUiData.DEMO else BreakUiData.PEOR)
        if (data == BreakUiData.DEMO) return ScreenshotFixtures.cartState()
        val products = catalog.products.associateBy { it.id }
        val chilaquiles = products.getValue(1101)
        val lines =
            when (data) {
                BreakUiData.VACIO -> emptyList()
                BreakUiData.UNO ->
                    listOf(
                        CartLine(products.getValue(1401), quantity = 1, selectedOptionIds = emptySet()),
                    )
                BreakUiData.PEOR ->
                    listOf(
                        CartLine(
                            chilaquiles,
                            quantity = 12,
                            selectedOptionIds = setOf(3103, 3110, 3111, 3112, 3113, 3115),
                            // CART_LINE_NOTE_MAX es 90.
                            notes = "Sin cebolla y la salsa aparte, por favor: es para un niño alérgico al cacahuate.",
                        ),
                        CartLine(products.getValue(1001), quantity = 1, selectedOptionIds = emptySet()),
                        CartLine(products.getValue(1801), quantity = 3, selectedOptionIds = emptySet()),
                        CartLine(products.getValue(1301), quantity = 1, selectedOptionIds = emptySet()),
                        CartLine(products.getValue(1102), quantity = 1, selectedOptionIds = emptySet()),
                        CartLine(products.getValue(1003), quantity = 30, selectedOptionIds = emptySet()),
                    )
                else ->
                    catalog.products.filter { it.available }.flatMap { product ->
                        (1..5).map { copy ->
                            val defaults =
                                product.optionGroups.filter { it.minimumSelections > 0 }.map {
                                    it.options[
                                        copy %
                                            it.options.size,
                                    ].id
                                }
                            CartLine(
                                product,
                                quantity = copy,
                                selectedOptionIds = defaults.toSet(),
                                notes = "Para el salón $copy",
                            )
                        }
                    }
            }
        return OrderFlowUiState(
            loading = false,
            catalog = catalog,
            operationalStatus = ScreenshotFixtures.operationalStatus(),
            cartLines = lines,
            checkoutPayment = PaymentMethod.CASH,
            checkoutDestination = OrderDestination.TAKE_AWAY,
            selectedSpaceId = 77,
            kitchenNotes =
                if (data ==
                    BreakUiData.PEOR
                ) {
                    "Lo recoge la maestra Rocío a las 12:30 en la puerta de la dirección."
                } else {
                    ""
                },
        )
    }

    private fun establishment(
        index: Int,
        name: String,
        label: String = "Matrícula",
        required: Boolean = true,
        photo: String? = null,
    ) = PublicEstablishment(
        id = "e0000000-0000-4000-8000-%012d".format(index),
        name = name,
        slug =
            name
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .take(80),
        clientIdLabel = label,
        clientIdRequired = required,
        photoUrl = photo,
    )

    private val peorEstablishments =
        listOf(
            establishment(
                1,
                "Cafetería y Tienda Escolar del Instituto Tecnológico y de Estudios Superiores de Occidente, Campus Guadalajara",
                label = "Número de empleado o matrícula institucional (sin guiones)",
            ),
            establishment(2, "Lú", required = false),
            establishment(
                3,
                "🍩 Donas La Esperanza",
                required = false,
                photo = "https://storage.vaiinilla.app/negocios/no-existe.jpg",
            ),
            establishment(4, "CAFETERIA_NORTE_EDIFICIO_B_PLANTA_BAJA", label = "Número de empleado", required = false),
            establishment(5, "Comedor Ñandutí"),
            establishment(6, "Escuela de pruebas"),
        )

    fun establishments(data: BreakUiData): List<PublicEstablishment> =
        when (data) {
            BreakUiData.DEMO ->
                listOf(
                    establishment(10, "Cafetería Centro"),
                    establishment(11, "Cafetería Norte", label = "Número de empleado", required = false),
                )
            BreakUiData.PEOR -> peorEstablishments
            BreakUiData.VACIO -> emptyList()
            BreakUiData.UNO -> listOf(peorEstablishments[1])
            BreakUiData.MUCHOS ->
                (0 until 300).map {
                    establishment(
                        100 + it,
                        peorEstablishments[
                            it %
                                peorEstablishments.size,
                        ].name,
                    )
                }
        }

    private fun movement(
        index: Int,
        type: String,
        description: String,
        amount: String,
        after: String,
        secondsAgo: Long,
    ) = WalletMovement("mov-$index", type, description, amount, after, orderId = null, createdAt = ago(secondsAgo))

    fun wallet(data: BreakUiData): WalletRemoteUiState {
        val (balance, movements) =
            when (data) {
                BreakUiData.DEMO ->
                    "140.00" to
                        listOf(
                            movement(1, "recarga_efectivo", "Recarga en caja", "100.00", "140.00", 3_600),
                            movement(2, "compra", "Compra", "-60.00", "40.00", 7_200),
                        )
                BreakUiData.PEOR ->
                    "12345.67" to
                        listOf(
                            movement(
                                1,
                                "recarga_stripe",
                                "Recarga con tarjeta de María Guadalupe Hernández Villaseñor (tutora principal)",
                                "10000.00",
                                "12345.67",
                                60,
                            ),
                            movement(
                                2,
                                "compra",
                                "Compra · Paquete fiesta de fin de curso para 30 alumnos (incluye pastel, aguas frescas y bocadillos)",
                                "-4850.00",
                                "2345.67",
                                86_000,
                            ),
                            movement(
                                3,
                                "devolucion",
                                "Devolución del pedido 104382 por artículo quitado en cocina",
                                "12.00",
                                "7195.67",
                                90_000,
                            ),
                            movement(4, "cashback", "Cashback", "0.01", "7183.67", 400_000),
                        )
                BreakUiData.VACIO -> "0.00" to emptyList()
                BreakUiData.UNO ->
                    "1.00" to
                        listOf(movement(1, "recarga_efectivo", "Recarga en caja", "1.00", "1.00", 120))
                BreakUiData.MUCHOS ->
                    "987.50" to
                        (0 until 500).map {
                            movement(
                                it,
                                if (it % 4 ==
                                    0
                                ) {
                                    "recarga_efectivo"
                                } else {
                                    "compra"
                                },
                                if (it % 4 ==
                                    0
                                ) {
                                    "Recarga en caja"
                                } else {
                                    "Compra"
                                },
                                if (it % 4 ==
                                    0
                                ) {
                                    "100.00"
                                } else {
                                    "-25.00"
                                },
                                "987.50",
                                it * 3_600L,
                            )
                        }
            }
        return WalletRemoteUiState(
            data = WalletData(WalletSnapshot("w-1", "u-1", "e-1", balance, ago(60)), movements),
        )
    }

    private fun child(
        index: Int,
        name: String,
        group: String? = "3°B",
        studentId: String? = "A-%03d".format(index),
        balance: String? = "40.00",
        low: String? = null,
        awaiting: Boolean = false,
        consent: Boolean = false,
        guardians: List<ChildGuardian> = emptyList(),
        principal: Boolean = true,
    ) = Dependent(
        userId = "hijo-$index",
        name = name,
        group = group,
        photoUrl = null,
        studentId = studentId,
        hasOwnAccount = index % 2 == 0,
        balance = balance?.let(::BigDecimal),
        controls = if (awaiting) null else DependentControls(lowBalanceAlert = low?.let(::BigDecimal)),
        isAwaitingSchool = awaiting,
        isPrincipal = principal,
        guardians = guardians,
        needsConsent = consent,
    )

    fun children(data: BreakUiData): List<Dependent> =
        when (data) {
            BreakUiData.DEMO ->
                listOf(
                    child(1, "Ana López", balance = "40.00"),
                    child(2, "Leo López", group = "1°A", balance = "120.00"),
                )
            BreakUiData.VACIO -> emptyList()
            BreakUiData.UNO -> listOf(child(1, "Jo", group = null, studentId = null, balance = "1.00"))
            BreakUiData.PEOR ->
                listOf(
                    child(
                        1,
                        "María Guadalupe Hernández Villaseñor de la Fuente",
                        group = "6°B Vespertino (Grupo de alto rendimiento)",
                        studentId = "A-2026-00012345",
                        balance = "12345.67",
                        guardians =
                            listOf(
                                ChildGuardian("t1", "Bartolomé Fitzgerald Ramírez", isPrincipal = true, isYou = false),
                                ChildGuardian("t2", "Jo", isPrincipal = false, isYou = true),
                            ),
                        principal = false,
                    ),
                    child(2, "Jo", group = null, studentId = null, balance = "0.00", low = "50.00"),
                    child(3, "🦄 Sofi Ramírez", balance = "3.50", low = "50.00"),
                    child(4, "Juan Pablo Hernández", balance = null, awaiting = true),
                    child(5, "Ximena Ñúñez", balance = "9999.99", consent = true),
                    child(6, "Iñaki Zúñiga Castañeda", group = "Kínder 3", balance = "40.00"),
                )
            BreakUiData.MUCHOS ->
                (1..9).map {
                    child(
                        it,
                        peorChildNames[it % peorChildNames.size],
                        balance = "${it * 10}.00",
                    )
                }
        }

    private val peorChildNames =
        listOf("Ana López", "Leo López", "Jo", "Iñaki Zúñiga", "Ximena Ñúñez", "María José de la Fuente")

    /** Lo que ve el alumno con tutores, como lo arma la app con sus propias reglas. */
    fun caretaker(data: BreakUiData): MyGuardianControls? =
        when (data) {
            BreakUiData.VACIO -> null
            BreakUiData.DEMO ->
                MyGuardianControls(
                    listOf("Marta"),
                    BigDecimal("60.00"),
                    BigDecimal("20.00"),
                    emptySet(),
                    emptySet(),
                    allergens = emptyList(),
                )
            BreakUiData.UNO ->
                MyGuardianControls(
                    listOf("Jo"),
                    BigDecimal("1.00"),
                    BigDecimal("1.00"),
                    setOf(1),
                    emptySet(),
                    allergens = emptyList(),
                )
            BreakUiData.PEOR, BreakUiData.MUCHOS ->
                MyGuardianControls(
                    tutorNames = listOf("María Guadalupe", "Bartolomé", "Jo", "Ximena"),
                    dailyLimit = BigDecimal("1500.00"),
                    availableToday = BigDecimal("0.00"),
                    blockedProductIds = (1..37).toSet(),
                    allergenProductIds = (100..112).toSet(),
                    allergens = emptyList(),
                )
        }

    private fun table(
        id: Int,
        name: String,
        call: TableCall? = null,
        orders: List<BoardOrder> = emptyList(),
    ) = BoardTable(space = TableSpace(id = id, name = name, type = "mesa"), call = call, orders = orders)

    private fun boardOrder(
        id: String,
        folio: Int,
        state: OrderState,
        client: String?,
        items: String,
    ) = BoardOrder(
        id = id,
        folio = folio,
        state = state,
        version = 1,
        clientName = client,
        itemsSummary = items,
        updatedAt = ago(1_380),
    )

    private fun call(
        id: String,
        space: TableSpace,
        status: CallStatus,
        reason: CallReason,
        client: String?,
        taker: String? = null,
        secondsAgo: Long = 222,
    ) = TableCall(
        id = id,
        space = space,
        orderId = null,
        reason = reason,
        status = status,
        clientName = client,
        takenBy = taker?.let { TableCallTaker("u-$it", it) },
        createdAt = ago(secondsAgo),
        takenAt = if (status == CallStatus.EN_CAMINO) ago(60) else null,
        closedAt = null,
        version = 1,
    )

    fun waiterBoard(data: BreakUiData): WaiterUiState {
        if (data == BreakUiData.DEMO) return ScreenshotFixtures.waiterBoardState()
        val terraza = TableSpace(77, "Terraza de la cancha de básquetbol (junto a la tienda escolar)", "mesa")
        val longItems =
            "12× Chilaquiles verdes con pollo deshebrado, crema, queso fresco, cebolla morada, " +
                "aguacate y frijoles refritos de la olla, " +
                "3× 🌮 Tacos al pastor (orden de 5), 1× Té, 30× Vaso con agua natural"
        val tables =
            when (data) {
                BreakUiData.VACIO -> emptyList()
                BreakUiData.UNO -> listOf(table(1, "1"))
                BreakUiData.PEOR ->
                    listOf(
                        BoardTable(
                            space = terraza,
                            call =
                                call(
                                    "c1",
                                    terraza,
                                    CallStatus.PENDIENTE,
                                    CallReason.PROBLEMA,
                                    "María Guadalupe Hernández Villaseñor de la Fuente",
                                    secondsAgo = 5_400,
                                ),
                            orders =
                                listOf(
                                    boardOrder(
                                        "o1",
                                        104_382,
                                        OrderState.READY,
                                        "María Guadalupe Hernández Villaseñor de la Fuente",
                                        longItems,
                                    ),
                                ),
                        ),
                        table(
                            2,
                            "M2",
                            call =
                                call(
                                    "c2",
                                    TableSpace(2, "M2", "mesa"),
                                    CallStatus.EN_CAMINO,
                                    CallReason.UTENSILIOS,
                                    null,
                                    taker = "Bartolomé Fitzgerald Ramírez",
                                ),
                        ),
                        table(
                            3,
                            "BARRA_EXTERIOR_LADO_ESTACIONAMIENTO",
                            orders =
                                (0 until 6).map {
                                    boardOrder(
                                        "o3-$it",
                                        999_990 + it,
                                        orderStates[it % orderStates.size],
                                        if (it % 2 ==
                                            0
                                        ) {
                                            "Jo"
                                        } else {
                                            null
                                        },
                                        "1× Té",
                                    )
                                },
                        ),
                        table(4, "Mesa 4"),
                        table(5, "Salón de usos múltiples, mesa larga del fondo junto a las ventanas"),
                    )
                else ->
                    (1..150).map {
                        table(
                            it,
                            if (it % 10 == 0) terraza.name else "Mesa $it",
                            call =
                                if (it % 7 ==
                                    0
                                ) {
                                    call(
                                        "c$it",
                                        TableSpace(it, "Mesa $it", "mesa"),
                                        CallStatus.PENDIENTE,
                                        CallReason.ATENCION,
                                        "Jo",
                                    )
                                } else {
                                    null
                                },
                            orders =
                                if (it % 3 ==
                                    0
                                ) {
                                    listOf(boardOrder("o$it", 100_000 + it, OrderState.PREPARING, "Ana", "2× Té"))
                                } else {
                                    emptyList()
                                },
                        )
                    }
            }
        return WaiterUiState(tables = tables, callsEnabled = true, lastBoardRefreshAt = now)
    }
}
