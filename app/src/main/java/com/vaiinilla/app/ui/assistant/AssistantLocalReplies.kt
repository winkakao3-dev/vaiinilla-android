package com.vaiinilla.app.ui.assistant

import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.components.moneyLabel
import java.math.BigDecimal

object AssistantLocalReplies {
    fun reply(userMessage: String, products: List<Product>): String {
        val normalized = userMessage.trim().lowercase()
        return when {
            normalized.contains("gluten") -> glutenReply(products)
            normalized.contains("ligero") || normalized.contains("fresco") -> lightReply(products)
            normalized.contains("recomienda") -> recommendReply(products)
            else -> recommendReply(products)
        }
    }

    private fun glutenReply(products: List<Product>): String {
        val safe = products.filter { product ->
            val allergens = product.allergens.lowercase()
            !allergens.contains("gluten") && !allergens.contains("trigo")
        }
        return if (safe.isEmpty()) {
            "Prueba frutas o jamaica; confirma alérgenos en cocina."
        } else {
            val names = safe.take(2).joinToString(" o ") { it.name }
            "Sin gluten detectable: $names. Confirma alérgenos en cocina antes de pedir."
        }
    }

    private fun lightReply(products: List<Product>): String {
        val light = products.filter { product ->
            val name = product.name.lowercase()
            name.contains("jamaica") || name.contains("fruta") || name.contains("agua")
        }
        val picks = if (light.isNotEmpty()) {
            light.take(2).joinToString(" y ") { "${it.name} (${moneyLabel(it.digitalPrice)})" }
        } else {
            "Agua de jamaica o fruta de temporada"
        }
        return "Para algo ligero y fresco te sugiero $picks. Perfecto si quieres acompañar sin sentirte pesado."
    }

    private fun recommendReply(products: List<Product>): String {
        val burrito = products.firstOrNull { it.name.contains("burrito", ignoreCase = true) }
            ?: products.firstOrNull { it.categoryId == 20 }
            ?: products.firstOrNull()
        return if (burrito != null) {
            "Te recomiendo el ${burrito.name} por ${moneyLabel(burrito.digitalPrice)}. " +
                "Es de los favoritos del menú y llega en unos ${burrito.estimatedTimeMinutes} min."
        } else {
            "Explora el menú de hoy: hay opciones rápidas y llenadoras. ¿Buscas algo específico?"
        }
    }

    fun filterByChip(chip: String, products: List<Product>): List<AssistantRecommendation> {
        val available = products.filter(Product::available)
        return when (chip) {
            "Menos de \$60" -> available
                .filter { parsePrice(it.digitalPrice) < BigDecimal("60") }
                .take(3)
                .map { it.toRecommendation() }
            "Algo ligero" -> available
                .filter {
                    val name = it.name.lowercase()
                    name.contains("jamaica") || name.contains("fruta") || name.contains("agua") ||
                        it.estimatedTimeMinutes <= 5
                }
                .take(3)
                .map { it.toRecommendation() }
            "Combo con bebida" -> {
                val food = available.filter { it.categoryId == 20 }.take(2)
                val drink = available.filter { it.categoryId == 10 }.firstOrNull()
                (food + listOfNotNull(drink)).map { it.toRecommendation() }
            }
            else -> defaultRecommendations(available)
        }
    }

    private fun defaultRecommendations(products: List<Product>): List<AssistantRecommendation> {
        val demoNames = listOf("torta", "burrito", "quesadilla")
        val matched = demoNames.mapNotNull { keyword ->
            products.firstOrNull { it.name.contains(keyword, ignoreCase = true) }
        }
        val result = if (matched.size >= 2) {
            matched.take(3)
        } else {
            products.take(3)
        }
        return result.map { it.toRecommendation() }.ifEmpty { fallbackRecommendations() }
    }

    private fun fallbackRecommendations(): List<AssistantRecommendation> = listOf(
        AssistantRecommendation(
            productId = null,
            name = "Torta de jamón",
            meta = "5–7 min · Comida",
            price = "45",
            imageUrl = "fixture://torta",
        ),
        AssistantRecommendation(
            productId = null,
            name = "Burrito norteño",
            meta = "8–10 min · Comida",
            price = "64",
            imageUrl = "fixture://burrito_norteno",
        ),
        AssistantRecommendation(
            productId = null,
            name = "Quesadillas (2)",
            meta = "6–8 min · Comida",
            price = "40",
            imageUrl = "fixture://quesa",
        ),
    )

    private fun Product.toRecommendation(): AssistantRecommendation {
        val categoryLabel = if (categoryId == 10) "Bebida" else "Comida"
        val minTime = (estimatedTimeMinutes - 2).coerceAtLeast(1)
        return AssistantRecommendation(
            productId = id,
            name = name,
            meta = "$minTime–$estimatedTimeMinutes min · $categoryLabel",
            price = digitalPrice,
            imageUrl = imageUrl,
        )
    }

    private fun parsePrice(value: String): BigDecimal = runCatching {
        BigDecimal(value)
    }.getOrDefault(BigDecimal.ZERO)
}

data class AssistantRecommendation(
    val productId: Int?,
    val name: String,
    val meta: String,
    val price: String,
    val imageUrl: String,
)
