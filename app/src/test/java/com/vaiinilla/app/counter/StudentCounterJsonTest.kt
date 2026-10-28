package com.vaiinilla.app.counter

import com.vaiinilla.app.core.network.ApiResponseParser
import com.vaiinilla.app.data.counter.StudentCounterJson
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.counter.StudentCounterMessages
import com.vaiinilla.app.domain.counter.StudentCredentialKind
import com.vaiinilla.app.domain.counter.StudentCredentialPayload
import com.vaiinilla.app.domain.counter.StudentPurchaseItem
import com.vaiinilla.app.domain.counter.StudentPurchaseRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.ZoneId

/** Los JSON de `/alumnos`, `/menu-dias/{fecha}/apartados` y `estado-operativo`, como los manda el backend de pruebas. */
class StudentCounterJsonTest {
    private val json = StudentCounterJson()

    @Test
    fun `la ficha del alumno trae saldo, alergias, PIN y lo de hoy`() {
        val raw =
            """
            {"data":[{"usuario_id":"2158f3c1-b686-4da4-b4e7-110d5e07d76f","nombre":"Tilin Ramírez","matricula":"A100",
            "grupo":"3B","foto_url":null,"tiene_cuenta":false,"saldo":"390.00","gastado_hoy":"0","limite_diario":"100.00",
            "alergenos":["cacahuate","crustaceos","nueces","nuevo_alergeno"],"pide_pin":true,
            "pedidos_hoy":[{"id":"p1","folio":12,"estado":"listo","version":3,"total":"45.00","resumen":"1× Torta"}]}],
            "meta":{"page":null,"total_pages":null,"total_items":null,"cursor":null},"error":null}
            """.trimIndent()
        val card = json.parseCards(raw).single()
        assertEquals("Tilin Ramírez", card.name)
        assertEquals("Matrícula A100 · 3B", card.identity)
        assertEquals(0, BigDecimal("390").compareTo(card.balance))
        assertEquals(0, BigDecimal("100").compareTo(card.spendableToday))
        assertEquals(listOf("cacahuate", "crustaceos", "nueces"), card.allergens)
        assertTrue(card.requiresPin)
        val order = card.todayOrders.single()
        assertTrue(order.isReady)
        assertEquals(3, order.version)
        assertEquals("1× Torta", order.summary)
    }

    @Test
    fun `la ficha trae lo bloqueado por el tutor y nada si el servidor no lo manda`() {
        val with =
            json
                .parseCards(
                    """{"data":[{"usuario_id":"u","nombre":"Leo","productos_bloqueados":[4],"categorias_bloqueadas":[2],
                    "productos_con_alergeno":[7],"productos_sin_revisar":[8]}],"error":null}""",
                ).single()
        assertEquals(setOf(4), with.blockedProducts)
        assertEquals(setOf(2), with.blockedCategories)
        assertEquals(setOf(7), with.allergenProducts)
        assertEquals(setOf(8), with.unreviewedProducts)
        val without = json.parseCards("""{"data":[{"usuario_id":"u","nombre":"Leo"}],"error":null}""").single()
        assertTrue(without.blockedProducts.isEmpty() && without.blockedCategories.isEmpty())
        assertTrue(without.allergenProducts.isEmpty() && without.unreviewedProducts.isEmpty())
    }

    @Test
    fun `sin limite puede gastar todo su saldo`() {
        val raw =
            """{"data":[{"usuario_id":"u","nombre":"Leo","saldo":"80.50","gastado_hoy":"20.00","limite_diario":null,
            "alergenos":[],"pide_pin":false,"pedidos_hoy":[]}],"error":null}"""
        val card = json.parseCards(raw).single()
        assertEquals(0, BigDecimal("80.50").compareTo(card.spendableToday))
        assertFalse(card.requiresPin)
    }

    @Test
    fun `la ficha trae la nota del tutor y nada si viene null o vacia`() {
        fun card(note: String) =
            json
                .parseCards(
                    """{"data":[{"usuario_id":"u","nombre":"Leo","saldo":"10.00","nota_tutor":$note}],"error":null}""",
                ).single()
        assertEquals("sin picante", card("\" sin picante \"").guardianNote)
        assertNull(card("null").guardianNote)
        assertNull(card("\"  \"").guardianNote)
        val older = json.parseCards("""{"data":[{"usuario_id":"u","nombre":"Leo","saldo":"10.00"}],"error":null}""")
        assertNull(older.single().guardianNote)
    }

    @Test
    fun `la compra lleva opcion_ids y el PIN solo si se pide`() {
        val items = listOf(StudentPurchaseItem(4, 2, listOf(9)), StudentPurchaseItem(5, 1))
        assertEquals(
            """{"items":[{"producto_id":4,"cantidad":2,"opcion_ids":[9]},{"producto_id":5,"cantidad":1,"opcion_ids":[]}]}""",
            json.encodePurchase(items, null),
        )
        assertTrue(json.encodePurchase(items, "1234").endsWith(""","pin":"1234"}"""))
    }

    @Test
    fun `la entrega con credencial manda credencial_alumno en lugar de qr_token`() {
        assertEquals(
            """{"estado_objetivo":"entregado","version_esperada":3,"credencial_alumno":"vaiinilla:alumno:abc"}""",
            json.encodeDelivery(3, "vaiinilla:alumno:abc"),
        )
        assertEquals("""{"estado_objetivo":"entregado","version_esperada":3}""", json.encodeDelivery(3, null))
    }

    @Test
    fun `la credencial QR trae el valor a imprimir y la NFC manda el UID`() {
        val credential =
            json.parseCredential(
                """{"data":{"credencial":{"id":"c1","tipo":"qr","valor":"vaiinilla:alumno:xyz"}},"error":null}""",
            )
        assertEquals(StudentCredentialKind.QR, credential.kind)
        assertEquals("vaiinilla:alumno:xyz", credential.printableValue)
        assertEquals(
            """{"tipo":"nfc","valor":"04A1B2C3"}""",
            json.encodeCredential(StudentCredentialKind.NFC, "04A1B2C3"),
        )
        assertEquals("""{"tipo":"qr"}""", json.encodeCredential(StudentCredentialKind.QR, "ignorado"))
        assertEquals("04A1FF", StudentCredentialPayload.nfcUid(byteArrayOf(0x04, 0xA1.toByte(), 0xFF.toByte())))
        assertTrue(StudentCredentialPayload.isStudentCredential(" vaiinilla:alumno:xyz"))
        assertFalse(StudentCredentialPayload.isStudentCredential("https://vaiinilla.app/p/abc"))
    }

    @Test
    fun `para manana trae piezas por producto y el dia sin servicio`() {
        val day =
            json.parseReservedDay(
                """{"data":{"fecha":"2026-10-22","pedidos":3,"sin_servicio":null,"productos":[
                {"producto_id":4,"nombre":"Torta","cantidad":5,"pedidos":3},{"producto_id":7,"nombre":"Agua","cantidad":2,"pedidos":2}]},"error":null}""",
            )
        assertEquals("2026-10-22", day.date)
        assertEquals(7, day.units)
        assertNull(day.closedReason)
        val closed =
            json.parseReservedDay(
                """{"data":{"fecha":"2026-10-23","pedidos":0,"sin_servicio":"Junta de maestros","productos":[]},"error":null}""",
            )
        assertEquals("Junta de maestros", closed.closedReason)
    }

    @Test
    fun `la politica de la escuela sale del estado operativo`() {
        val policy =
            json.parseSchoolPolicy(
                """{"data":{"recibiendo_pedidos":true,"entrega_requiere_qr":false,"modo_escuela":true,"tipo":"cafeteria"},"error":null}""",
            )
        assertTrue(policy.isSchool)
        assertFalse(policy.deliveryRequiresQr)
        // Un servidor anterior no manda modo_escuela: no es escuela.
        assertFalse(json.parseSchoolPolicy("""{"data":{"recibiendo_pedidos":true},"error":null}""").isSchool)
    }

    @Test
    fun `el pedido trae las alergias del alumno para el ticket`() {
        val raw =
            """{"data":{"id":"p1","folio":12,"fecha_operativa":"2026-10-21","estado":"cobrado","metodo_pago":"saldo",
            "destino":"para_llevar","subtotal":"45.00","ahorro_combinado":"0.00","cashback_otorgado":"0.00",
            "total":"45.00","version":1,"creado_en":"2026-10-21T15:00:00Z","actualizado_en":"2026-10-21T15:00:00Z",
            "usuario":{"nombre":"Andres Ramirez","matricula":"959266","alergias":["huevo","lactosa"]},
            "notas_cocina":null,"items":[]},"meta":{},"error":null}"""
        val order = OrderContractJson().parseOrderDetail(raw)
        assertEquals(listOf("huevo", "lactosa"), order.user?.allergies)
    }

    @Test
    fun `los rechazos del PIN le dicen a la cajera cuantos intentos quedan y hasta que hora`() {
        val parser = ApiResponseParser()
        val wrong =
            parser.parseError(
                """{"data":null,"meta":{},"error":{"code":"PIN_INCORRECT","message":"El PIN no coincide; quedan 3 intentos.","details":[{"intentos_restantes":3}]}}""",
                422,
            )
        assertEquals("El PIN no coincide; quedan 3 intentos.", StudentCounterMessages.chargeFailure(wrong))
        val locked =
            parser.parseError(
                """{"data":null,"meta":{},"error":{"code":"PIN_LOCKED","message":"Se equivocaron 5 veces; espera 15 minutos o llama a su tutor.","details":[{"bloqueado_hasta":"2026-10-21T16:45:00.000Z"}]}}""",
                423,
            )
        assertEquals(
            "Se equivocaron 5 veces con su PIN. Se puede volver a intentar a las 10:45 o llama a su tutor.",
            StudentCounterMessages.chargeFailure(locked, ZoneId.of("America/Mexico_City")),
        )
        val allergy =
            parser.parseError(
                """{"data":null,"meta":{},"error":{"code":"PRODUCT_ALLERGEN_BLOCKED","message":"Tilin es alérgico a algo de este producto."}}""",
                422,
            )
        assertEquals("Tilin es alérgico a algo de este producto.", StudentCounterMessages.chargeFailure(allergy))
    }

    @Test
    fun `el total suma el extra de cada opcion elegida`() {
        val total =
            StudentPurchaseRules.total(
                cart = mapOf(4 to 2, 5 to 1),
                prices = mapOf(4 to BigDecimal("30.00"), 5 to BigDecimal("12.50")),
                choices = mapOf(4 to listOf(9)),
                extras = mapOf(9 to BigDecimal("5.00")),
            )
        assertEquals(0, BigDecimal("82.50").compareTo(total))
        assertEquals(
            listOf("cacahuate"),
            StudentPurchaseRules.allergenWarnings("Contiene cacahuate y soya", listOf("cacahuate", "huevo")),
        )
        assertEquals(listOf("lacteos"), StudentPurchaseRules.allergenWarnings("LÁCTEOS", listOf("lacteos")))
    }
}
