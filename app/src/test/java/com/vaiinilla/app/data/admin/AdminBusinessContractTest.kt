package com.vaiinilla.app.data.admin

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.domain.admin.CashbackRule
import com.vaiinilla.app.domain.admin.CashbackRules
import com.vaiinilla.app.domain.admin.InvitationState
import com.vaiinilla.app.domain.admin.SpacePatch
import com.vaiinilla.app.domain.admin.SpaceRules
import com.vaiinilla.app.domain.admin.StaffRole
import com.vaiinilla.app.domain.admin.StaffRules
import com.vaiinilla.app.domain.admin.StoreFlow
import com.vaiinilla.app.domain.admin.StoreRules
import com.vaiinilla.app.domain.admin.StoreTemplate
import com.vaiinilla.app.domain.admin.StripeSetup
import com.vaiinilla.app.domain.admin.StripeStep
import com.vaiinilla.app.domain.admin.stripeStep
import com.vaiinilla.app.domain.model.OrderWindow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminBusinessContractTest {
    private val json = AdminBusinessJson()

    private fun obj(raw: String) = Json.parseToJsonElement(raw).jsonObject

    @Test
    fun `invitations page keeps roles, states and the next cursor`() {
        val raw =
            """
            {"data":[{"id":"i1","email":"ana@x.mx","rol":"mesero","estado":"expirada","expira_en":"2026-10-19T00:00:00Z",
              "creado_en":"2026-10-16T00:00:00Z","reemplaza_invitacion_id":null,"membresia_id":null}],
             "meta":{"cursor":"c2"},"error":null}
            """.trimIndent()
        val page = json.parseInvitationsPage(raw)
        assertEquals("c2", page.cursor)
        val invitation = page.invitations.single()
        assertEquals(StaffRole.WAITER, invitation.role)
        assertEquals(InvitationState.EXPIRED, invitation.state)
        assertTrue(invitation.state!!.canResend)
        assertFalse(InvitationState.ACCEPTED.canResend)
    }

    @Test
    fun `staff email is normalised like the backend`() {
        assertEquals("ana@x.mx", StaffRules.email("  Ana@X.MX "))
        assertNull(StaffRules.email("ana"))
    }

    @Test
    fun `store config reads windows and videos, and the flow goes complete`() {
        val raw =
            """
            {"data":{"tipo":"restaurante","entrega_requiere_qr":false,"permite_pago_al_final":true,"permite_saldo":true,
              "gracia_liberacion_min":5,"franjas_pedido":[{"desde":"12:00","hasta":"15:00"}],
              "videos_local":[{"titulo":"Tour","url":"https://v.test/1"}],"visible_en_directorio":true,"caja_alerta_horas":24,
              "tienda_url":"https://app.test/e/x","imagen_url":null,"logo_url":null,"tipos_disponibles":["cafeteria"]}}
            """.trimIndent()
        val config = json.parseStoreConfig(raw)
        assertEquals(listOf(OrderWindow("12:00", "15:00")), config.orderWindows)
        assertEquals("Tour", config.videos.single().title)
        val flow = config.flow().applying(StoreTemplate.SCHOOL_CAFETERIA)
        val body = obj(json.encodeFlow(flow))
        assertEquals("cafeteria", body["tipo"]!!.jsonPrimitive.content)
        assertEquals("true", body["entrega_requiere_qr"]!!.jsonPrimitive.content)
        assertEquals(0, body["franjas_pedido"]!!.jsonArray.size)
        assertEquals("true", body["cocina_prepara_sin_cobrar"]!!.jsonPrimitive.content)
        assertEquals(7, body.size)
    }

    @Test
    fun `cocina prepara sin cobrar viaja en la configuracion y un servidor anterior la deja en si`() {
        val apagada =
            json.parseStoreConfig("""{"data":{"tipo":"cafeteria","cocina_prepara_sin_cobrar":false}}""")
        assertFalse(apagada.kitchenPreparesUnpaid)
        assertEquals(
            "false",
            obj(json.encodeFlow(apagada.flow()))["cocina_prepara_sin_cobrar"]!!.jsonPrimitive.content,
        )
        assertTrue(json.parseStoreConfig("""{"data":{"tipo":"cafeteria"}}""").kitchenPreparesUnpaid)
    }

    @Test
    fun `order windows reject overlaps, including across midnight`() {
        assertNull(StoreRules.windowsProblem(listOf(OrderWindow("12:00", "15:00"), OrderWindow("18:00", "20:00"))))
        assertNotNull(StoreRules.windowsProblem(listOf(OrderWindow("12:00", "15:00"), OrderWindow("14:00", "16:00"))))
        assertNotNull(StoreRules.windowsProblem(listOf(OrderWindow("22:00", "02:00"), OrderWindow("01:00", "03:00"))))
        assertNull(StoreRules.windowsProblem(listOf(OrderWindow("22:00", "02:00"), OrderWindow("08:00", "12:00"))))
        assertNotNull(StoreRules.windowsProblem(listOf(OrderWindow("10:00", "10:00"))))
        assertNotNull(StoreRules.problem(StoreFlow("cafeteria", true, false, true, 61, emptyList())))
    }

    @Test
    fun `stripe step follows charges and payouts`() {
        assertEquals(StripeStep.CONNECT, stripeStep(null))
        assertEquals(StripeStep.FINISH, stripeStep(StripeSetup(false, true, false, "en_revision", false)))
        assertEquals(StripeStep.READY, stripeStep(StripeSetup(false, true, true, "habilitada", false)))
        assertNull(json.parseStripeSetup("""{"data":null,"error":null}"""))
    }

    @Test
    fun `cashback rule trims seconds and is sent whole with explicit nulls`() {
        val raw =
            """
            {"data":{"id":"r1","nombre":"Regla principal","porcentaje":"5.00","hora_inicio":"08:00:00","hora_fin":"11:00:00",
              "dias_activos":[1,2],"vigencia_inicio":"2026-10-01","vigencia_fin":null,"activa":true,
              "creado_en":"2026-10-01T00:00:00Z","actualizado_en":"2026-10-02T00:00:00Z"}}
            """.trimIndent()
        val rule = json.parseCashbackRule(raw)!!
        assertEquals("08:00", rule.startTime)
        assertEquals("Lun, Mar", CashbackRules.daysLabel(rule.activeDays))
        val body =
            obj(json.encodeCashbackRule(CashbackRules.normalized(rule.copy(percent = "7.5", activeDays = emptyList()))))
        assertEquals("7.50", body["porcentaje"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, body["dias_activos"])
        assertEquals(JsonNull, body["vigencia_fin"])
        assertEquals(8, body.size)
    }

    @Test
    fun `cashback rules match the web form`() {
        val ok = CashbackRule("Regla", "5", null, null, null, null, null, true)
        assertNull(CashbackRules.problem(ok))
        assertNotNull(CashbackRules.problem(ok.copy(percent = "101")))
        assertNotNull(CashbackRules.problem(ok.copy(startTime = "08:00")))
        assertNotNull(CashbackRules.problem(ok.copy(startTime = "08:00", endTime = "08:00")))
        assertNotNull(CashbackRules.problem(ok.copy(validFrom = "2026-10-10", validTo = "2026-10-01")))
        assertEquals("100.00", CashbackRules.percent("100"))
    }

    @Test
    fun `space patch only sends what changes`() {
        assertEquals("""{"activo":false}""", json.encodeSpacePatch(SpacePatch(active = false)))
        val price = obj(json.encodeSpacePatch(SpacePatch(hourlyPrice = null, setsHourlyPrice = true)))
        assertEquals(JsonNull, price["precio_hora"])
        val profile =
            obj(json.encodeSpacePatch(SpacePatch(description = " ", features = listOf("Techada"), setsProfile = true)))
        assertEquals(JsonNull, profile["descripcion"])
        assertEquals(1, profile["caracteristicas"]!!.jsonArray.size)
    }

    @Test
    fun `space rules follow the backend limits`() {
        assertEquals(SpaceRules.HourlyPrice.Value("300.00"), SpaceRules.hourlyPrice("300"))
        assertEquals(SpaceRules.HourlyPrice.None, SpaceRules.hourlyPrice(" "))
        assertEquals(SpaceRules.HourlyPrice.Invalid, SpaceRules.hourlyPrice("0"))
        assertNull(SpaceRules.batchProblem("Asiento", 1, 100))
        assertNotNull(SpaceRules.batchProblem("Asiento", 1, 501))
        assertNotNull(SpaceRules.batchProblem("Asiento", 5, 1))
        assertEquals(listOf("Techada", "Con luz"), SpaceRules.features(listOf("Techada", " techada", "Con luz", "")))
    }

    @Test
    fun `managed spaces accept the short code as a number`() {
        val raw =
            """{"data":[{"id":7,"nombre":"Cancha 1","tipo":"cancha","activo":true,"qr_url":"https://q/7","codigo":1234,
               "precio_hora":"300.00","descripcion":null,"imagen_url":null,"caracteristicas":["Techada"]}]}"""
        val space = json.parseSpaces(raw).single()
        assertEquals("1234", space.code)
        assertTrue(space.isCourt)
        assertEquals("300.00", space.hourlyPrice)
    }

    @Test
    fun `live counts read invitations, cash session and active orders`() {
        val client =
            ScriptedClient(
                mapOf(
                    "personal/invitaciones" to
                        """{"data":[{"id":"a","email":"a@b.c","rol":"cajero","estado":"pendiente"}],"meta":{}}""",
                    "sesiones-caja/activa" to """{"data":null}""",
                    "pedidos" to """{"data":[{},{}],"meta":{"cursor":"x"}}""",
                ),
            )
        val counts = RemoteAdminStoreRepository(client).liveCounts().getOrThrow()
        assertEquals(1, counts.pendingInvitations)
        assertEquals(false, counts.cashOpen)
        assertEquals(2, counts.activeOrders)
        assertTrue(counts.moreActiveOrders)
    }

    @Test
    fun `forced close sends forzar with the optional note`() {
        val client = ScriptedClient(emptyMap())
        RemoteAdminSpacesRepository(client).closeWithoutCharging(4, " Se fue sin pagar ", "k1").getOrThrow()
        assertEquals("espacios/4/sesion/cierres", client.lastPath)
        assertEquals("""{"forzar":true,"nota":"Se fue sin pagar"}""", client.lastBody)
        assertEquals("k1", client.lastHeaders["Idempotency-Key"])
    }

    private class ScriptedClient(
        private val gets: Map<String, String>,
    ) : VaiinillaApiClient {
        override val baseUrl: String = "https://example.invalid/api/v1/"
        var lastPath: String? = null
        var lastBody: String? = null
        var lastHeaders: Map<String, String> = emptyMap()

        override fun get(
            path: String,
            query: Map<String, String>,
        ): Result<String> = gets[path]?.let { Result.success(it) } ?: Result.failure(AssertionError("GET $path"))

        override fun post(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> {
            lastPath = path
            lastBody = body
            lastHeaders = headers
            return Result.success("""{"data":{}}""")
        }
    }
}
