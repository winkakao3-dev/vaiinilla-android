package com.vaiinilla.app

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.catalog.RemoteCatalogRepository
import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.domain.model.CatalogProductDraft
import com.vaiinilla.app.domain.model.PreparationStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CashierCatalogRepositoryTest {
    private val parser = ContractResponseParser()
    private val productEnvelope =
        """
        {
          "data": {
            "id": 102,
            "categoria_id": 20,
            "estacion_preparacion": "cocina",
            "nombre": "Waffle de la casa",
            "descripcion": "Waffle dorado preparado al momento.",
            "ingredientes": "Harina, leche, huevo y fruta",
            "alergenos": "Gluten, leche y huevo",
            "tiempo_estimado_min": 9,
            "precio_mostrador": "55.00",
            "precio_digital": "60.00",
            "disponible": true,
            "imagen_url": "fixture://waffle",
            "grupos_opcion": []
          },
          "error": null
        }
        """.trimIndent()

    @Test
    fun `cashier draft encodes empty option groups and wire price`() {
        val json =
            parser.encodeProductDraft(
                CatalogProductDraft(
                    categoryId = 20,
                    preparationStation = PreparationStation.KITCHEN,
                    name = "Waffle de la casa",
                    estimatedTimeMinutes = 9,
                    counterPrice = "55.00",
                ),
            )
        assertTrue(json.contains("\"grupos_opcion\":[]"))
        assertTrue(json.contains("\"precio_mostrador\":\"55.00\""))
        assertTrue(json.contains("\"estacion_preparacion\":\"cocina\""))
        assertTrue(json.contains("\"disponible\":true"))
    }

    @Test
    fun `create product posts catalog path with idempotency key`() {
        val client = RecordingCatalogApiClient(productEnvelope)
        val repository = RemoteCatalogRepository(client, parser)
        val product =
            repository
                .createProduct(
                    CatalogProductDraft(
                        categoryId = 20,
                        preparationStation = PreparationStation.KITCHEN,
                        name = "Waffle de la casa",
                        estimatedTimeMinutes = 9,
                        counterPrice = "55.00",
                    ),
                    "idem-create",
                ).getOrThrow()

        assertEquals(102, product.id)
        assertEquals("55.00", product.counterPrice)
        assertEquals("60.00", product.digitalPrice)
        assertEquals("catalogo/productos", client.path)
        assertEquals("idem-create", client.headers["Idempotency-Key"])
        assertTrue(client.body.contains("\"grupos_opcion\":[]"))
    }

    @Test
    fun `availability posts boolean payload to product path`() {
        val client = RecordingCatalogApiClient(productEnvelope)
        val repository = RemoteCatalogRepository(client, parser)
        repository.setProductAvailability(102, false, "idem-avail").getOrThrow()

        assertEquals("catalogo/productos/102/disponibilidad", client.path)
        assertEquals("{\"disponible\":false}", client.body)
        assertEquals("idem-avail", client.headers["Idempotency-Key"])
    }

    @Test
    fun `image upload uses multipart put with imagen field`() {
        val client = RecordingCatalogApiClient(productEnvelope)
        val repository = RemoteCatalogRepository(client, parser)
        repository
            .uploadProductImage(
                productId = 102,
                bytes = byteArrayOf(1, 2, 3),
                filename = "waffle.jpg",
                mimeType = "image/jpeg",
                idempotencyKey = "idem-img",
            ).getOrThrow()

        assertEquals("catalogo/productos/102/imagen", client.multipartPath)
        assertEquals("imagen", client.multipartField)
        assertEquals("waffle.jpg", client.multipartFilename)
        assertEquals("idem-img", client.multipartHeaders["Idempotency-Key"])
    }

    private class RecordingCatalogApiClient(
        private val response: String,
    ) : VaiinillaApiClient {
        override val baseUrl: String = "https://example.invalid/api/v1/"
        var path: String? = null
        var body: String = ""
        var headers: Map<String, String> = emptyMap()
        var multipartPath: String? = null
        var multipartField: String? = null
        var multipartFilename: String? = null
        var multipartHeaders: Map<String, String> = emptyMap()

        override fun get(
            path: String,
            query: Map<String, String>,
        ): Result<String> = Result.failure(AssertionError("GET no esperado"))

        override fun post(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> {
            this.path = path
            this.body = body
            this.headers = headers
            return Result.success(response)
        }

        override fun putMultipart(
            path: String,
            fieldName: String,
            filename: String,
            mimeType: String,
            bytes: ByteArray,
            headers: Map<String, String>,
        ): Result<String> {
            multipartPath = path
            multipartField = fieldName
            multipartFilename = filename
            multipartHeaders = headers
            return Result.success(response)
        }
    }
}
