package com.vaiinilla.app.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.R
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.catalog.CatalogUiState
import com.vaiinilla.app.ui.theme.Cream
import com.vaiinilla.app.ui.theme.Ink
import com.vaiinilla.app.ui.theme.Lime

@Composable
fun CatalogScreen(
    state: CatalogUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        state.loading -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Cream),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Lime)
        }

        state.errorMessage != null -> ErrorState(state.errorMessage, onBack, onRetry)
        state.catalog != null -> CatalogContent(state, onBack)
    }
}

@Composable
private fun CatalogContent(state: CatalogUiState, onBack: () -> Unit) {
    val catalog = requireNotNull(state.catalog)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 24.dp,
            bottom = 36.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onBack,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = Ink,
                    ),
                ) {
                    Text("Atrás")
                }
                Surface(shape = CircleShape, color = Lime) {
                    Text(
                        text = state.dataSourceMode.name,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Text("Catálogo de fixtures", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Solo lectura para validar arquitectura y contrato. " +
                    "Las acciones de compra pertenecen a VAI-10.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(16.dp))
            OperationalCard(state)
        }

        items(catalog.products, key = Product::id) { product ->
            ProductCard(product)
        }
    }
}

@Composable
private fun OperationalCard(state: CatalogUiState) {
    val status = state.operationalStatus ?: return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Estado operativo", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(5.dp))
            Text(
                text = if (status.acceptingOrders) {
                    "Recibiendo pedidos · ${status.estimatedTimeMinutes} min estimados"
                } else {
                    "No disponible"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ProductCard(product: Product) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(productImage(product.imageUrl)),
                contentDescription = product.name,
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 15.dp, end = 6.dp),
            ) {
                Text(product.name, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "\$${product.digitalPrice}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = product.preparationStation.wireValue,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onBack: () -> Unit, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Fuente remota no disponible", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Reintentar")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text("Volver", color = Ink)
        }
    }
}

@DrawableRes
private fun productImage(imageUrl: String): Int = when (imageUrl.removePrefix("fixture://")) {
    "jamaica" -> R.drawable.jamaica
    "burrito_norteno" -> R.drawable.burrito_norteno
    "waffle" -> R.drawable.waffle
    else -> R.drawable.waffle
}
