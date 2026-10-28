package com.vaiinilla.app.data.guardianship

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.domain.guardianship.ChildRestriction
import com.vaiinilla.app.domain.guardianship.DependentContextGrant
import com.vaiinilla.app.domain.guardianship.DependentControls
import com.vaiinilla.app.domain.guardianship.PlannedItem
import com.vaiinilla.app.domain.guardianship.RefundMethod
import com.vaiinilla.app.domain.guardianship.ScheduledDayOrder
import com.vaiinilla.app.domain.guardianship.StudentRequestKind
import com.vaiinilla.app.domain.guardianship.StudentRequestStatus
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.school.SchoolAllergen
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class GuardianshipContractTest {
    private val json = GuardianshipJson()

    private fun envelope(data: String) = """{"data":$data,"meta":{"page":null},"error":null}"""

    @Test
    fun pendingChildKeepsBalanceAndControlsAsNullNotZero() {
        val raw =
            envelope(
                """[{"usuario_id":"u1","nombre":"Tilin Ramírez","grupo":"3B","foto_url":null,"matricula":"A100",
                "tiene_cuenta":false,"estado":"pendiente","requiere_consentimiento":true,"saldo":null,"controles":null}]""",
            )
        val child = json.parseDependents(raw).single()
        assertTrue(child.isAwaitingSchool)
        assertNull(child.balance)
        assertNull(child.controls)
        assertTrue(child.needsConsent)
        assertFalse(child.isLowBalance)
        assertEquals("3B · A100", child.schoolLine)
    }

    @Test
    fun approvedChildReadsGuardiansControlsAndCredentials() {
        val raw =
            envelope(
                """{"usuario_id":"u2","nombre":"Ana López","grupo":null,"foto_url":null,"matricula":"A-001",
                "tiene_cuenta":false,"estado":"aprobada","requiere_consentimiento":false,"soy_principal":false,
                "tutores":[{"tutela_id":"t1","nombre":"Luis López","principal":true,"eres_tu":false},
                           {"tutela_id":"t2","nombre":"Marta","principal":false,"eres_tu":true}],
                "tiene_pin":true,"credenciales":{"qr":false,"nfc":true},"saldo":"40.00",
                "controles":{"limite_diario":"60.00","umbral_saldo_bajo":"50.00","notas":null,
                "productos_bloqueados":[3],"categorias_bloqueadas":[2],"alergenos":["huevo","desconocido"]}}""",
            )
        val child = json.parseDependent(raw)
        assertEquals(BigDecimal("40.00"), child.balance)
        assertEquals("Luis", child.principalFirstName)
        assertTrue(child.hasPin)
        assertTrue(child.hasCredential)
        assertTrue(child.isLowBalance)
        val controls = child.controls!!
        assertEquals(BigDecimal("60.00"), controls.dailyLimit)
        assertEquals(setOf(SchoolAllergen.HUEVO), controls.allergens)
        assertEquals(setOf(2), controls.blockedCategoryIds)
    }

    @Test
    fun controlsTravelWithNullsSoTheLimitCanBeRemoved() {
        val encoded =
            Json
                .parseToJsonElement(
                    json.encodeControls(
                        DependentControls(
                            dailyLimit = null,
                            lowBalanceAlert = BigDecimal("30.00"),
                            notes = "  ",
                            blockedCategoryIds = setOf(5, 1),
                            allergens = setOf(SchoolAllergen.SOYA, SchoolAllergen.HUEVO),
                        ),
                    ),
                ).jsonObject
        assertEquals(JsonNull, encoded["limite_diario"])
        assertEquals("30.00", encoded["umbral_saldo_bajo"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, encoded["notas"])
        assertEquals(
            listOf(1, 5),
            encoded["categorias_bloqueadas"]!!.jsonArray.map { it.jsonPrimitive.content.toInt() },
        )
        assertEquals(listOf("huevo", "soya"), encoded["alergenos"]!!.jsonArray.map { it.jsonPrimitive.content })
        val lowOnly = Json.parseToJsonElement(json.encodeLowBalanceAlert(null)).jsonObject
        assertEquals(setOf("umbral_saldo_bajo"), lowOnly.keys)
    }

    @Test
    fun reloadRangeComparesAsNumbersNotText() {
        val wallet =
            json.parseChildWallet(
                envelope(
                    """{"recarga_tarjeta":{"minima":"50.00","maxima":"2000.00"},"cliente":{"usuario_id":"u"},
                    "wallet":{"saldo":"12.50"},"movimientos":[{"id":"m","tipo":"recarga_stripe","descripcion":"Recarga con tarjeta",
                    "monto":"-30.00","saldo_posterior":"12.50","creado_en":"2026-10-21T10:00:00Z"}]}""",
                ),
            )
        val range = wallet.reloadRange!!
        assertNull(range.problem(BigDecimal("300")))
        assertEquals("El mínimo es \$50.", range.problem(BigDecimal("20")))
        assertEquals("El máximo es \$2,000.", range.problem(BigDecimal("2500")))
        assertEquals("De \$50 a \$2,000", range.label)
        assertTrue(wallet.movements.single().isDebit)
    }

    @Test
    fun consentNoticeAndAcceptanceReplaceTheList() {
        val notice =
            json.parsePrivacyNotice(
                envelope("""{"version":"2026-10-21","borrador":true,"titulo":"Aviso","parrafos":["Uno","Dos"]}"""),
            )
        assertEquals("2026-10-21", notice.version)
        assertTrue(notice.isDraft)
        assertEquals(2, notice.paragraphs.size)
        val body = Json.parseToJsonElement(json.encodeConsent(notice.version)).jsonObject
        assertEquals("2026-10-21", body["consentimiento_version"]!!.jsonPrimitive.content)
        val link = Json.parseToJsonElement(json.encodeLink("ABC234", "v1")).jsonObject
        assertEquals(setOf("codigo", "consentimiento_version"), link.keys)
    }

    @Test
    fun myControlsNullWithoutGuardianAndRestrictionsOtherwise() {
        assertNull(json.parseMyControls(envelope("""{"tutores":[],"controles":null}""")))
        val controls =
            json.parseMyControls(
                envelope(
                    """{"tutores":[{"nombre":"Ana"},{"nombre":"Luis"}],"controles":{"limite_diario":"60.00",
                    "gastado_hoy":"20.00","disponible_hoy":"40.00","productos_bloqueados":[1,2],
                    "productos_con_alergeno":[2,3],"productos_sin_revisar":[4],"alergenos":["huevo"]}}""",
                ),
            )!!
        assertEquals("Te cuidan Ana y Luis", controls.caretakerLine)
        assertEquals(ChildRestriction.Blocked, controls.restriction(2))
        assertEquals(ChildRestriction.Allergen, controls.restriction(3))
        assertEquals(ChildRestriction.AllergensUnknown, controls.restriction(4))
        assertNull(controls.guardianRestriction(1))
        assertEquals(ChildRestriction.ChildAllergen, controls.guardianRestriction(2))
        // EQUIPO-27: sin revisar en el servidor, pero la ficha dice huevo: el mismo motivo que en Caja.
        val dedos =
            Product(
                id = 4,
                categoryId = 1,
                preparationStation = PreparationStation.KITCHEN,
                name = "Dedos de queso",
                description = "",
                ingredients = "",
                allergens = "Lácteos, huevo",
                estimatedTimeMinutes = 5,
                counterPrice = "40.00",
                digitalPrice = "40.00",
                available = true,
                imageUrl = "",
                optionGroups = emptyList(),
            )
        assertEquals("Contiene huevo", controls.restriction(dedos)?.label)
        assertEquals("Contiene huevo", controls.guardianRestriction(dedos)?.label)
        assertTrue(controls.fitsToday(BigDecimal("40.00")))
        assertFalse(controls.fitsToday(BigDecimal("40.50")))
    }

    @Test
    fun menuCalendarAndWeekBody() {
        val calendar =
            json.parseMenuCalendar(
                envelope(
                    """{"programacion":{"hoy":"2026-10-21","dias_adelanto_pedido":5,"hora_corte_pedido":"09:00:00",
                    "hora_corte_cancelacion":"08:00","franjas":[]},"dias":[{"fecha":"2026-10-21","producto_ids":[2,3]}],
                    "sin_servicio":[{"fecha":"2026-10-23T00:00:00.000Z","motivo":"Junta de maestros"}]}""",
                ),
            )
        assertEquals("09:00", calendar.schedule.orderCutoff)
        assertEquals("Junta de maestros", calendar.closure("2026-10-23"))
        assertEquals(setOf(2, 3), calendar.menu("2026-10-21"))
        assertNull(calendar.menu("2026-10-22"))
        val body =
            Json
                .parseToJsonElement(
                    json.encodeWeek(
                        listOf(ScheduledDayOrder("2026-10-22", null, listOf(PlannedItem(7, 2, listOf(11))))),
                    ),
                ).jsonObject
        val day = body["pedidos"]!!.jsonArray.single().jsonObject
        assertEquals("2026-10-22", day["fecha_entrega"]!!.jsonPrimitive.content)
        assertFalse("franja_entrega" in day)
        val item = day["items"]!!.jsonArray.single().jsonObject
        assertEquals(
            "11",
            item["opcion_ids"]!!
                .jsonArray
                .single()
                .jsonPrimitive.content,
        )
    }

    @Test
    fun studentRequestsSkipUnknownKinds() {
        val requests =
            json.parseStudentRequests(
                envelope(
                    """[{"id":"r1","tipo":"devolucion","estado":"resuelta","como_devolver":"efectivo","saldo_al_pedir":"80.00",
                    "monto_devuelto":"75.00"},{"id":"r2","tipo":"otra","estado":"pendiente"}]""",
                ),
            )
        val request = requests.single()
        assertEquals(StudentRequestKind.REFUND, request.kind)
        assertEquals(StudentRequestStatus.RESOLVED, request.status)
        assertEquals(RefundMethod.CASH, request.refundMethod)
        assertEquals(BigDecimal("75.00"), request.refunded)
    }

    @Test
    fun childCallsUseTheChildTokenAndRenewOnlyItOn401() {
        val bearers = mutableListOf<String>()
        var issued = 0
        val client =
            object : VaiinillaApiClient {
                override val baseUrl = "https://example.test/api/v1/"

                override fun get(
                    path: String,
                    query: Map<String, String>,
                ): Result<String> = error("El hijo nunca usa la sesión del tutor")

                override fun post(
                    path: String,
                    body: String,
                    headers: Map<String, String>,
                ): Result<String> = error("El hijo nunca usa la sesión del tutor")

                override fun getWithBearer(
                    bearer: String,
                    path: String,
                    query: Map<String, String>,
                ): Result<String> {
                    bearers += bearer
                    return if (bearer == "hijo-1") {
                        Result.failure(ApiClientException("UNAUTHENTICATED", "vencido", 401))
                    } else {
                        Result.success(envelope("""{"tutores":[],"controles":null}"""))
                    }
                }
            }
        val session =
            DependentContextSession("u1", {
                issued++
                Result.success(DependentContextGrant("hijo-$issued", 900))
            })
        val repository = RemoteDependentAccountRepository(client, session)
        assertTrue(repository.controls().isSuccess)
        assertEquals(listOf("hijo-1", "hijo-2"), bearers)
    }
}
