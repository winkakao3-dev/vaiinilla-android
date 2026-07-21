package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.ui.theme.Cream
import com.vaiinilla.app.ui.theme.Ink
import com.vaiinilla.app.ui.theme.Lime

@Composable
fun StartScreen(
    dataSourceMode: DataSourceMode,
    onOpenCatalog: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(horizontal = 22.dp, vertical = 30.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Text(
                        text = "VAI-5 · ${dataSourceMode.name}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Spacer(Modifier.height(26.dp))
                Text(
                    text = "Vaiinilla\nAndroid base",
                    style = MaterialTheme.typography.displayMedium,
                    color = Ink,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Navegación, dominio y datos desacoplados. " +
                        "Esta entrega todavía no implementa carrito, cobro ni seguimiento.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "Fixture compatible con CONTRACTS.md v1.0",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onOpenCatalog,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Lime,
                        contentColor = Ink,
                    ),
                ) {
                    Text(
                        text = "Abrir navegación de prueba",
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    }
}
