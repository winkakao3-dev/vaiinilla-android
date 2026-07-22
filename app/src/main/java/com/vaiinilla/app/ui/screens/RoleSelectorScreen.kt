package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.RoomService
import androidx.compose.material.icons.rounded.SentimentSatisfiedAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.ui.theme.Cream
import com.vaiinilla.app.ui.theme.Ink
import com.vaiinilla.app.ui.theme.Lime
import com.vaiinilla.app.ui.theme.MutedInk

@Composable
fun RoleSelectorScreen(
    onRoleSelected: (OperationalRole) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Vaiinilla", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(
                "Entrega 01 · demo local VAI-11",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedInk,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Elige un rol para recorrer el flujo. Las transiciones usan fixtures hasta que Saúl libere el backend.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedInk,
            )
        }
        items(roleOptions) { option ->
            RoleCard(option = option, onClick = { onRoleSelected(option.role) })
        }
    }
}

private data class RoleOption(
    val role: OperationalRole,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

private val roleOptions = listOf(
    RoleOption(
        role = OperationalRole.CLIENT,
        title = "Alumno",
        subtitle = "Catálogo, pedido y seguimiento",
        icon = Icons.Rounded.SentimentSatisfiedAlt,
    ),
    RoleOption(
        role = OperationalRole.CASHIER,
        title = "Caja",
        subtitle = "Cobrar y entregar para llevar",
        icon = Icons.Rounded.PointOfSale,
    ),
    RoleOption(
        role = OperationalRole.KITCHEN,
        title = "Cocina",
        subtitle = "Comandas y preparación",
        icon = Icons.Rounded.LocalDining,
    ),
    RoleOption(
        role = OperationalRole.WAITER,
        title = "Mesero",
        subtitle = "Entregas en espacio",
        icon = Icons.Rounded.RoomService,
    ),
)

@Composable
private fun RoleCard(
    option: RoleOption,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = Lime.copy(alpha = 0.35f),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = Lime,
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(option.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(option.subtitle, style = MaterialTheme.typography.bodyMedium, color = MutedInk)
            }
        }
    }
}
