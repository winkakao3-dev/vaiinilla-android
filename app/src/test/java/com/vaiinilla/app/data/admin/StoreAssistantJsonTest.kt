package com.vaiinilla.app.data.admin

import com.vaiinilla.app.data.inventory.InventoryUnit
import com.vaiinilla.app.data.inventory.RecipeLine
import com.vaiinilla.app.domain.admin.AssistantLabels
import com.vaiinilla.app.domain.admin.AssistantMessage
import com.vaiinilla.app.domain.admin.AssistantProduct
import com.vaiinilla.app.domain.admin.AssistantProductPhoto
import com.vaiinilla.app.domain.admin.AssistantRecipeLine
import com.vaiinilla.app.domain.admin.AssistantStep
import com.vaiinilla.app.domain.admin.steps
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreAssistantJsonTest {
    private val json = StoreAssistantJson()

    @Test
    fun `manda la platica con los roles del servidor`() {
        val body =
            Json
                .parseToJsonElement(
                    json.encodeMessages(
                        listOf(AssistantMessage(false, "¿Qué negocio es?"), AssistantMessage(true, "Un boliche")),
                    ),
                ).jsonObject
        val mensajes = body["mensajes"]!!.jsonArray
        assertEquals("asistente", mensajes[0].jsonObject["rol"]!!.jsonPrimitive.content)
        assertEquals("dueno", mensajes[1].jsonObject["rol"]!!.jsonPrimitive.content)
        assertEquals("Un boliche", mensajes[1].jsonObject["texto"]!!.jsonPrimitive.content)
    }

    @Test
    fun `lee una pregunta sin propuesta`() {
        val reply =
            json.parseReply(
                """{"data":{"mensaje":"¿Pagan al final?","opciones":["Sí","No"],"propuesta":null,"quedan_mensajes":37}}""",
            )
        assertEquals("¿Pagan al final?", reply.message)
        assertEquals(listOf("Sí", "No"), reply.options)
        assertNull(reply.proposal)
        assertEquals(37, reply.remainingMessages)
    }

    @Test
    fun `lee la propuesta en palabras y guarda el cuerpo del PATCH tal cual`() {
        val reply =
            json.parseReply(
                """
                {"data":{"mensaje":"Así quedaría.","opciones":[],"quedan_mensajes":20,
                "propuesta":{"cambios":{"tipo":"bar","permite_pago_al_final":true,"entrega_en_espacio":"ambas",
                "franjas_pedido":[{"desde":"16:00","hasta":"01:00"}],"gracia_liberacion_min":10,"fondo_sugerido":"500.00"},
                "motivos":{"tipo":"Tienes barra."}}}}
                """.trimIndent(),
            )
        val proposal = reply.proposal!!
        val byField = proposal.changes.associateBy { it.field }
        assertEquals("Bar o cantina", byField.getValue("tipo").value)
        assertEquals("Tienes barra.", byField.getValue("tipo").reason)
        assertEquals("Al final, con cuenta abierta", byField.getValue("permite_pago_al_final").value)
        assertEquals("Las dos: el cliente elige", byField.getValue("entrega_en_espacio").value)
        assertEquals("16:00 a 01:00", byField.getValue("franjas_pedido").value)
        assertEquals("10 min", byField.getValue("gracia_liberacion_min").value)
        assertEquals("$500.00", byField.getValue("fondo_sugerido").value)
        val patch = Json.parseToJsonElement(proposal.patchBody!!).jsonObject
        assertEquals("bar", patch["tipo"]!!.jsonPrimitive.content)
        assertEquals(6, patch.size)
    }

    @Test
    fun `manda los ids de las fotos con la platica`() {
        val body =
            Json
                .parseToJsonElement(
                    json.encodeMessages(listOf(AssistantMessage(true, "Mi logo [foto1]")), listOf("foto1")),
                ).jsonObject
        assertEquals("foto1", body["fotos"]!!.jsonArray[0].jsonPrimitive.content)
    }

    @Test
    fun `lee mesas, menu, equipo y fotos aunque no cambie ningun ajuste, y los ordena para aplicarlos`() {
        val proposal =
            json
                .parseReply(
                    """
                    {"data":{"mensaje":"Así quedaría.","opciones":[],"quedan_mensajes":20,
                    "propuesta":{"cambios":{},"motivos":{"espacios":"Tus pistas."},
                    "espacios":[{"tipo":"cancha","prefijo":"Pista","cantidad":12,"precio_hora":"350.00"},
                      {"tipo":"mesa","prefijo":"Mesa","cantidad":4,"precio_hora":null}],
                    "menu":[{"categoria":"Bebidas","categoria_id":7,"productos":[
                      {"nombre":"Cerveza","precio":"45.00","descripcion":null,"prepara":"ninguna","foto":"foto2"}]}],
                    "personal":[{"email":"ana@correo.com","rol":"cajero"}],
                    "fotos":{"logo":"foto1"}}}}
                    """.trimIndent(),
                ).proposal!!
        assertNull(proposal.patchBody)
        assertEquals("Tus pistas.", proposal.sectionReasons["espacios"])
        assertEquals(listOf("350.00", null), proposal.spaces.map { it.hourlyPrice })
        assertEquals(7, proposal.menu.single().existingId)
        assertEquals(
            "foto2",
            proposal.menu
                .single()
                .products
                .single()
                .photoId,
        )
        val steps = proposal.steps()
        assertTrue(steps[0] is AssistantStep.Spaces)
        assertTrue(steps[1] is AssistantStep.Spaces)
        assertTrue(steps[2] is AssistantStep.Menu)
        assertEquals(AssistantStep.StorePhoto("logo", "foto1"), steps[3])
        assertEquals("Invitar a ana@correo.com", steps[4].label)
    }

    @Test
    fun `lee fotos para productos que ya existen y las aplica despues del menu`() {
        val proposal =
            json
                .parseReply(
                    """
                    {"data":{"mensaje":"Le pongo foto.","opciones":[],"quedan_mensajes":20,
                    "propuesta":{"cambios":{},
                    "menu":[{"categoria":"Bebidas","productos":[
                      {"nombre":"Agua","precio":"20.00","descripcion":null,"prepara":"ninguna","foto":null}]}],
                    "fotos_productos":[{"producto_id":41,"nombre":"Cerveza","foto":"foto2"},
                      {"tipo":"foto_producto","producto_id":42,"nombre":"Nachos","foto":"foto3"},
                      {"producto_id":43,"nombre":"Sin foto"},
                      {"tipo":"otra","producto_id":44,"nombre":"Otra","foto":"foto4"}],
                    "fotos":{"logo":"foto1"}}}}
                    """.trimIndent(),
                ).proposal!!
        assertEquals(
            listOf(AssistantProductPhoto(41, "Cerveza", "foto2"), AssistantProductPhoto(42, "Nachos", "foto3")),
            proposal.productPhotos,
        )
        val steps = proposal.steps()
        assertTrue(steps[0] is AssistantStep.Menu)
        assertEquals(AssistantStep.ProductPhoto(AssistantProductPhoto(41, "Cerveza", "foto2")), steps[1])
        assertEquals("Foto de Nachos", steps[2].label)
        assertEquals(AssistantStep.StorePhoto("logo", "foto1"), steps[3])
    }

    @Test
    fun `una propuesta con solo fotos de productos no se pierde`() {
        val proposal =
            json
                .parseReply(
                    """
                    {"data":{"mensaje":"Listo.","opciones":[],
                    "propuesta":{"cambios":{},"fotos_productos":[{"producto_id":7,"nombre":"Café","foto":"foto1"}]}}}
                    """.trimIndent(),
                ).proposal
        assertEquals(7L, proposal!!.productPhotos.single().productId)
    }

    @Test
    fun `un producto se crea sin opciones y lo ya hecho no tarda`() {
        val body =
            Json
                .parseToJsonElement(
                    json.encodeProduct(7, AssistantProduct("Refresco", "25.00", null, "ninguna", null)),
                ).jsonObject
        assertEquals(7, body["categoria_id"]!!.jsonPrimitive.content.toInt())
        assertEquals("ninguna", body["estacion_preparacion"]!!.jsonPrimitive.content)
        assertEquals("0", body["tiempo_estimado_min"]!!.jsonPrimitive.content)
        assertEquals(0, body["grupos_opcion"]!!.jsonArray.size)
    }

    @Test
    fun `lee insumos, recetas y si la propuesta sigue`() {
        val reply =
            json.parseReply(
                """
                {"data":{"mensaje":"Así quedaría","opciones":[],"sigue":true,"propuesta":{"cambios":{},
                "motivos":{"recetas":"Lo que lleva cada uno."},
                "insumos":[{"nombre":"Clamato","unidad":"ml","existencia_inicial":3000,"minimo":500,"costo_unitario":null}],
                "recetas":[{"producto_id":31,"producto":"Michelada","lineas":[
                  {"insumo_id":5,"insumo":"Cerveza clara","unidad":"pieza","cantidad":1},
                  {"insumo_id":null,"insumo":"Clamato","unidad":"ml","cantidad":120}]},
                  {"producto_id":null,"producto":"Chelada","lineas":[]}]},"quedan_mensajes":50}}
                """.trimIndent(),
            )
        val proposal = reply.proposal!!
        assertTrue(proposal.continues)
        assertEquals("Clamato", proposal.supplies.single().name)
        assertEquals(3000.0, proposal.supplies.single().initialStock!!, 0.0)
        assertNull(proposal.supplies.single().unitCost)
        val recipe = proposal.recipes.single()
        assertEquals(31, recipe.productId)
        assertEquals(listOf(5L, null), recipe.lines.map { it.supplyId })
        assertEquals("Lo que lleva cada uno.", proposal.sectionReasons["recetas"])
        assertEquals("Cerveza clara 1 pz · Clamato 120 ml", AssistantLabels.recipe(recipe))
        assertEquals(
            "En mililitros · tienes 3,000 ml · te avisa con 500 ml",
            AssistantLabels.supply(proposal.supplies.single()),
        )
        // Insumos y recetas se guardan después del menú, y las recetas al final.
        assertEquals(
            listOf(AssistantStep.Supply::class, AssistantStep.Recipe::class),
            proposal.steps().map { it::class },
        )
    }

    @Test
    fun `una propuesta solo con recetas no se pierde`() {
        val reply =
            json.parseReply(
                """{"data":{"mensaje":"Listo","opciones":[],"propuesta":{"recetas":[{"producto_id":2,"producto":"Café",
                "lineas":[{"insumo_id":9,"insumo":"Café molido","unidad":"g","cantidad":"18.5"}]}]}}}""",
            )
        assertEquals(false, reply.proposal!!.continues)
        assertEquals(
            18.5,
            reply.proposal!!
                .recipes
                .single()
                .lines
                .single()
                .quantity,
            0.0,
        )
    }

    @Test
    fun `la receta conserva lo que ya tenia y cambia solo lo que dice el asistente`() {
        val current =
            listOf(
                RecipeLine(5, "Cerveza clara", InventoryUnit.PIECE, 2.0, null, null, 18.0),
                RecipeLine(7, "Sal", InventoryUnit.GRAM, 3.0, null, null, 0.1),
                RecipeLine(5, "Cerveza clara", InventoryUnit.PIECE, 1.0, 40, "Doble", 18.0),
            )
        val merged =
            mergeAssistantRecipe(
                current,
                listOf(
                    AssistantRecipeLine(5, "Cerveza clara", "pieza", 1.0),
                    AssistantRecipeLine(9, "Clamato", "ml", 120.0),
                ),
            )
        assertEquals(
            listOf(Triple(7L, null, 3.0), Triple(5L, 40, 1.0), Triple(5L, null, 1.0), Triple(9L, null, 120.0)),
            merged.map { Triple(it.supplyId, it.optionId, it.quantity) },
        )
        assertEquals(assistantNameKey("Limón "), assistantNameKey("limon"))
    }
}
