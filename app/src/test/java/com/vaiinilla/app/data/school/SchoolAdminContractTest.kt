package com.vaiinilla.app.data.school

import com.vaiinilla.app.data.admin.AdminBusinessJson
import com.vaiinilla.app.data.admin.AdminContractJson
import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.data.school.SchoolAdminJson.JoinCodeChange
import com.vaiinilla.app.domain.admin.AdminProductInput
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.school.RefundMethod
import com.vaiinilla.app.domain.school.RosterStudent
import com.vaiinilla.app.domain.school.StudentRequestKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Los JSON salen de las respuestas reales del backend de pruebas (rama padres-hijos, sandbox SANDBX). */
class SchoolAdminContractTest {
    private val json = SchoolAdminJson()

    private fun obj(raw: String) = Json.parseToJsonElement(raw).jsonObject

    @Test
    fun `settings keep the join code, approval and the reload range as field text`() {
        val raw =
            """
            {"data":{"tipo":"cafeteria","modo_escuela":true,"codigo_union":"SANDBX","union_requiere_codigo":false,
              "tutores_requieren_aprobacion":true,"recarga_minima":"50.00","recarga_maxima":"2000.00",
              "dias_adelanto_pedido":5,"hora_corte_pedido":"09:00","hora_corte_cancelacion":"08:00"},
             "meta":{"cursor":null},"error":null}
            """.trimIndent()
        val settings = json.parseSettings(raw)
        assertEquals("SANDBX", settings.joinCode)
        assertFalse(settings.joinRequiresCode)
        assertTrue(settings.guardiansNeedApproval)
        assertEquals("50", settings.reloadMinimum)
        assertEquals("2000", settings.reloadMaximum)
        assertEquals(5, settings.daysAhead)
        assertEquals("09:00", settings.orderCutoff)
        assertEquals("08:00", settings.cancelCutoff)
    }

    @Test
    fun `settings patch sends money with two decimals, empty hours as null and the join code change`() {
        val settings =
            json.parseSettings(
                """{"data":{"codigo_union":null,"recarga_minima":"50.00","recarga_maxima":"2000.00"}}""",
            )
        val body =
            obj(
                json.encodeSettings(
                    settings.copy(reloadMinimum = "75.5", orderCutoff = "10:30"),
                    JoinCodeChange.GENERATE,
                ),
            )
        assertEquals("generar", body["codigo_union"]!!.jsonPrimitive.content)
        assertEquals("75.50", body["recarga_minima"]!!.jsonPrimitive.content)
        assertEquals("2000.00", body["recarga_maxima"]!!.jsonPrimitive.content)
        assertEquals("10:30", body["hora_corte_pedido"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, body["hora_corte_cancelacion"])
        assertEquals(0, body["dias_adelanto_pedido"]!!.jsonPrimitive.int)

        val removing = obj(json.encodeSettings(settings, JoinCodeChange.REMOVE))
        assertEquals(JsonNull, removing["codigo_union"])
        // Sin cambio de código no se manda la clave: el servidor lo dejaría igual.
        assertFalse(obj(json.encodeSettings(settings, null)).containsKey("codigo_union"))
        // Un mínimo vacío no se manda: el servidor no deja quitar el rango.
        assertFalse(obj(json.encodeSettings(settings.copy(reloadMinimum = ""), null)).containsKey("recarga_minima"))
    }

    @Test
    fun `menu calendar uses the school today, its menus and closed days`() {
        val raw =
            """
            {"data":{"programacion":{"hoy":"2026-10-21","dias_adelanto_pedido":5,"hora_corte_pedido":"09:00",
              "hora_corte_cancelacion":"08:00","franjas":[]},"dias":[{"fecha":"2026-10-21","producto_ids":[2,3,4]}],
              "sin_servicio":[{"fecha":"2026-10-23","motivo":"Junta de maestros"}]},"error":null}
            """.trimIndent()
        val calendar = json.parseMenuCalendar(raw)
        assertEquals(LocalDate.of(2026, 10, 21), calendar.today)
        assertEquals(setOf(2, 3, 4), calendar.menu(LocalDate.of(2026, 10, 21)))
        assertNull(calendar.menu(LocalDate.of(2026, 10, 22)))
        assertEquals("Junta de maestros", calendar.closure(LocalDate.of(2026, 10, 23)))
    }

    @Test
    fun `menu body sorts the product ids`() {
        val body = obj(json.encodeMenu(listOf(4, 2)))
        assertEquals(listOf(2, 4), body["producto_ids"]!!.jsonArray.map { it.jsonPrimitive.int })
    }

    @Test
    fun `closing a day reports cancelled, refunded and pending`() {
        val raw =
            """
            {"data":{"fecha":"2026-10-23","motivo":"Junta","cancelados":3,"devuelto":"100.00",
              "sin_cancelar":[{"pedido_id":"p1","code":"INTERNAL_ERROR"}]}}
            """.trimIndent()
        val closure = json.parseDayClosure(raw)
        assertEquals(3, closure.cancelled)
        assertEquals("100.00", closure.refunded)
        assertEquals(1, closure.pending)
        assertEquals("""{"motivo":"Junta"}""", json.encodeReason("  Junta "))
    }

    @Test
    fun `reserved day lists pieces per product`() {
        val raw =
            """
            {"data":{"fecha":"2026-10-21","pedidos":2,"sin_servicio":null,
              "productos":[{"producto_id":4,"nombre":"Galleta de avena","cantidad":3,"pedidos":2}]}}
            """.trimIndent()
        val day = json.parseReservedDay(raw)
        assertEquals(2, day.orders)
        assertNull(day.closedReason)
        assertEquals(3, day.lines.single().quantity)
        assertEquals("Galleta de avena", day.lines.single().name)
    }

    @Test
    fun `roster round trips with total`() {
        val roster =
            json.parseRoster(
                """{"data":{"total":1,"alumnos":[{"matricula":"A100","nombre":"Tilin Ramírez","grupo":"3B"}]}}""",
            )
        assertEquals(listOf(RosterStudent("A100", "Tilin Ramírez", "3B")), roster)
        val body = obj(json.encodeRoster(listOf(RosterStudent("A100", null, "3B"))))
        val first = body["alumnos"]!!.jsonArray.first().jsonObject
        assertEquals("A100", first["matricula"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, first["nombre"])
        assertEquals(12, json.parseRosterTotal("""{"data":{"total":12}}"""))
    }

    @Test
    fun `guardian requests keep roster, other guardians and account facts`() {
        val raw =
            """
            {"data":[{"tutela_id":"t1","creado_en":"2026-10-21T15:00:00.000Z",
              "tutor":{"nombre":"Rosa Ramírez","email":"rosa@x.mx"},
              "alumno":{"usuario_id":"u1","nombre":"Tilin Ramírez","matricula":"A100","grupo":"3B","tiene_cuenta":false},
              "en_padron":true,"otros_tutores":1}]}
            """.trimIndent()
        val request = json.parseGuardianRequests(raw).single()
        assertEquals("t1", request.id)
        assertEquals("Rosa Ramírez", request.guardianName)
        assertEquals("A100 · 3B", request.studentLine)
        assertEquals("En el padrón · Ya tiene 1 tutor · Sin cuenta propia", request.facts(hasRoster = true))
        assertEquals("Ya tiene 1 tutor · Sin cuenta propia", request.facts(hasRoster = false))
    }

    @Test
    fun `guardianships search groups by student with the principal first`() {
        val raw =
            """
            {"data":[
              {"tutela_id":"t2","principal":false,"tutor":{"nombre":null,"email":"papa@x.mx"},
               "alumno":{"usuario_id":"u1","nombre":"Tilin","matricula":"A100","grupo":"3B"}},
              {"tutela_id":"t1","principal":true,"tutor":{"nombre":"Rosa","email":"rosa@x.mx"},
               "alumno":{"usuario_id":"u1","nombre":"Tilin","matricula":"A100","grupo":"3B"}}]}
            """.trimIndent()
        val rows = json.parseGuardianships(raw)
        assertEquals("papa@x.mx", rows.first().guardianName)
        val grouped =
            com.vaiinilla.app.domain.school.SchoolGuardianship
                .byStudent(rows)
        assertEquals(listOf("t1", "t2"), grouped.single().map { it.id })
    }

    @Test
    fun `student requests keep both balances and skip unknown kinds`() {
        val raw =
            """
            {"data":[
              {"id":"s1","tipo":"devolucion","como_devolver":"efectivo","nota":"Se cambia de escuela",
               "creado_en":"2026-10-21T15:00:00.000Z","saldo_al_pedir":"150.00","saldo_actual":"120.00",
               "tutor":{"nombre":"Rosa","email":"rosa@x.mx"},
               "alumno":{"usuario_id":"u1","nombre":"Tilin","matricula":"A100","grupo":"3B"}},
              {"id":"s2","tipo":"otra_cosa","saldo_al_pedir":"0.00","saldo_actual":"0.00",
               "tutor":{"nombre":"Rosa"},"alumno":{"nombre":"Tilin"}}]}
            """.trimIndent()
        val request = json.parseStudentRequests(raw).single()
        assertEquals(StudentRequestKind.REFUND, request.kind)
        assertEquals(RefundMethod.CASH, request.refundMethod)
        assertEquals("120.00", request.amountToRefund)
        assertTrue(request.balanceChanged)
        assertTrue(request.isCashRefund)
        assertEquals("Matrícula A100 · 3B", request.studentLine)
    }

    @Test
    fun `store config reads modo_escuela and patches only it`() {
        val business = AdminBusinessJson()
        val config = business.parseStoreConfig("""{"data":{"tipo":"cafeteria","modo_escuela":true}}""")
        assertTrue(config.isSchool)
        assertFalse(business.parseStoreConfig("""{"data":{"tipo":"cafeteria"}}""").isSchool)
        assertEquals("""{"modo_escuela":false}""", business.encodeSchoolMode(false))
    }

    @Test
    fun `catalog product reads allergen codes and review, defaulting to reviewed`() {
        val parser = ContractResponseParser()
        val product =
            parser.parseProduct(
                """
                {"data":{"id":4,"categoria_id":3,"estacion_preparacion":"caja","nombre":"Galleta de avena",
                  "descripcion":null,"ingredientes":null,"alergenos":"{}","alergenos_codigos":["gluten"],
                  "alergenos_revisados":true,"tiempo_estimado_min":1,"precio_mostrador":"20.00",
                  "precio_digital":"20.00","disponible":true,"imagen_url":null,"grupos_opcion":[]},"error":null}
                """.trimIndent(),
            )
        assertEquals(listOf("gluten"), product.allergenCodes)
        assertTrue(product.allergensReviewed)
        val old =
            parser.parseProduct(
                """
                {"data":{"id":5,"categoria_id":3,"estacion_preparacion":"caja","nombre":"Agua",
                  "tiempo_estimado_min":1,"precio_mostrador":"10.00","precio_digital":"10.00","disponible":true}}
                """.trimIndent(),
            )
        assertTrue(old.allergenCodes.isEmpty())
        assertTrue(old.allergensReviewed)
    }

    @Test
    fun `product body sends alergenos_codigos only when the admin touched them`() {
        val input =
            AdminProductInput(
                categoryId = 3,
                preparationStation = PreparationStation.CASHIER,
                name = "Galleta",
                description = "",
                ingredients = "",
                allergens = "",
                estimatedTimeMinutes = 1,
                counterPrice = "20.00",
                available = true,
                optionGroups = emptyList(),
            )
        val admin = AdminContractJson()
        assertFalse(obj(admin.encodeProduct(input)).containsKey("alergenos_codigos"))
        val none = obj(admin.encodeProduct(input.copy(allergenCodes = emptySet())))
        assertEquals(0, none["alergenos_codigos"]!!.jsonArray.size)
        val some = obj(admin.encodeProduct(input.copy(allergenCodes = setOf("lacteos", "gluten"))))
        assertEquals(
            listOf("gluten", "lacteos"),
            some["alergenos_codigos"]!!.jsonArray.map { it.jsonPrimitive.content },
        )
    }
}
