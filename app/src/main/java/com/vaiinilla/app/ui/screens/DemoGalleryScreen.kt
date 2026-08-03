package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

data class DemoGalleryItem(
    val id: String,
    val label: String,
)

data class DemoGallerySection(
    val title: String,
    val items: List<DemoGalleryItem>,
)

val demoGallerySections =
    listOf(
        DemoGallerySection(
            title = "Arranque",
            items =
                listOf(
                    DemoGalleryItem("splash", "Splash"),
                    DemoGalleryItem("01", "Selector de roles"),
                ),
        ),
        DemoGallerySection(
            title = "Menú",
            items =
                listOf(
                    DemoGalleryItem("02", "Catálogo"),
                    DemoGalleryItem("05", "Banner pedido activo"),
                    DemoGalleryItem("06", "Búsqueda vacía"),
                    DemoGalleryItem("07", "Producto (sheet)"),
                ),
        ),
        DemoGallerySection(
            title = "Asistente",
            items =
                listOf(
                    DemoGalleryItem("09", "Hub asistente"),
                    DemoGalleryItem("57", "Chat"),
                ),
        ),
        DemoGallerySection(
            title = "Carrito / checkout",
            items =
                listOf(
                    DemoGalleryItem("12", "Carrito vacío"),
                    DemoGalleryItem("13", "Llevar + efectivo"),
                    DemoGalleryItem("14", "Mesa + saldo"),
                    DemoGalleryItem("15", "Llevar + tarjeta"),
                ),
        ),
        DemoGallerySection(
            title = "Confirmación",
            items =
                listOf(
                    DemoGalleryItem("16", "Confirm efectivo"),
                    DemoGalleryItem("17", "Confirm saldo"),
                    DemoGalleryItem("18", "Confirm tarjeta"),
                ),
        ),
        DemoGallerySection(
            title = "Pedidos",
            items =
                listOf(
                    DemoGalleryItem("19", "Sin pedidos"),
                    DemoGalleryItem("20", "Por cobrar"),
                    DemoGalleryItem("21", "Cobrado"),
                    DemoGalleryItem("22", "Preparando"),
                    DemoGalleryItem("23", "Listo"),
                    DemoGalleryItem("24", "Entregado"),
                ),
        ),
        DemoGallerySection(
            title = "Cartera",
            items =
                listOf(
                    DemoGalleryItem("25", "Hub"),
                    DemoGalleryItem("26", "Añadir dinero"),
                    DemoGalleryItem("27", "SPEI"),
                    DemoGalleryItem("28", "Métodos"),
                    DemoGalleryItem("29", "Agregar tarjeta"),
                    DemoGalleryItem("30", "Mi cuenta"),
                ),
        ),
        DemoGallerySection(
            title = "Stickers",
            items =
                listOf(
                    DemoGalleryItem("51", "Editorial"),
                    DemoGalleryItem("52", "Core"),
                    DemoGalleryItem("53", "Limited"),
                    DemoGalleryItem("54", "Breakfast"),
                    DemoGalleryItem("55", "QR Live"),
                    DemoGalleryItem("56", "Térmico"),
                ),
        ),
        DemoGallerySection(
            title = "Ops",
            items =
                listOf(
                    DemoGalleryItem("caja", "Caja"),
                    DemoGalleryItem("cocina", "Cocina"),
                    DemoGalleryItem("mesero", "Mesero"),
                ),
        ),
        DemoGallerySection(
            title = "VAI-27 · Accesos autorizados",
            items =
                listOf(
                    DemoGalleryItem("vai27-invitation", "Invitación válida"),
                    DemoGalleryItem("vai27-modes", "Tres modos autorizados"),
                    DemoGalleryItem("vai27-external-revoke", "Revocación externa → Alumno"),
                ),
        ),
    )

@Composable
fun DemoGalleryScreen(
    onBack: () -> Unit,
    onItemSelected: (String) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .statusBarsPadding(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = colors.ink,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Galería demo",
                    color = colors.ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "Solo pruebas · sin backend",
                    color = colors.muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            demoGallerySections.forEach { section ->
                item(key = "header-${section.title}") {
                    Text(
                        section.title.uppercase(),
                        color = colors.muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.1.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
                items(section.items, key = { it.id }) { item ->
                    DemoGalleryRow(
                        item = item,
                        onClick = { onItemSelected(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoGalleryRow(
    item: DemoGalleryItem,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .physicalPress(onClick = onClick),
        color = colors.paper2,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    item.id,
                    color = colors.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(
                    item.label,
                    color = colors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.muted,
                )
            }
        }
    }
}
