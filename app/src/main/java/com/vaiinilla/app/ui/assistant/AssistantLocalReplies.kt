package com.vaiinilla.app.ui.assistant

import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.components.moneyLabel

object AssistantLocalReplies {
    fun reply(
        userMessage: String,
        products: List<Product>,
    ): String {
        val normalized = userMessage.trim().lowercase()
        return when {
            normalized.contains("gluten") -> glutenReply(products)
            normalized.contains("ligero") || normalized.contains("fresco") -> lightReply(products)
            normalized.contains("recomienda") -> recommendReply(products)
            else -> recommendReply(products)
        }
    }

    private fun glutenReply(products: List<Product>): String {
        val safe =
            products.filter { product ->
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
        val light =
            products.filter { product ->
                val name = product.name.lowercase()
                name.contains("jamaica") || name.contains("fruta") || name.contains("agua")
            }
        val picks =
            if (light.isNotEmpty()) {
                light.take(2).joinToString(" y ") { "${it.name} (${moneyLabel(it.digitalPrice)})" }
            } else {
                "Agua de jamaica o fruta de temporada"
            }
        return "Para algo ligero y fresco te sugiero $picks. Perfecto si quieres acompañar sin sentirte pesado."
    }

    private fun recommendReply(products: List<Product>): String {
        val burrito =
            products.firstOrNull { it.name.contains("burrito", ignoreCase = true) }
                ?: products.firstOrNull { it.categoryId == 20 }
                ?: products.firstOrNull()
        return if (burrito != null) {
            "Te recomiendo el ${burrito.name} por ${moneyLabel(burrito.digitalPrice)}. " +
                "Es de los favoritos del menú y llega en unos ${burrito.estimatedTimeMinutes} min."
        } else {
            "Explora el menú de hoy: hay opciones rápidas y llenadoras. ¿Buscas algo específico?"
        }
    }
}
