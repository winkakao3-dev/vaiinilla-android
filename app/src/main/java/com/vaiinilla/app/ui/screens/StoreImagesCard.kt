package com.vaiinilla.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.repository.StoreImageKind
import com.vaiinilla.app.domain.repository.StoreImages
import com.vaiinilla.app.ui.components.ProductImage

/**
 * Logo y foto del negocio dentro de Caja, como la tarjeta de "Flujo de mi tienda" en la web. Cada uno se sube o se
 * quita al momento; sin imagen, la app del cliente sigue con sus ilustraciones.
 */
@Composable
internal fun StoreImagesCard(
    images: StoreImages,
    busy: StoreImageKind?,
    notice: String?,
    enabled: Boolean,
    colors: OperationalColors,
    onUpload: (StoreImageKind, ByteArray, String, String) -> Unit,
    onRemove: (StoreImageKind) -> Unit,
    onPrepareFailed: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var picking by remember { mutableStateOf<StoreImageKind?>(null) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            val kind = picking
            picking = null
            if (uri == null || kind == null) return@rememberLauncherForActivityResult
            val prepared =
                runCatching {
                    if (kind ==
                        StoreImageKind.LOGO
                    ) {
                        prepareStoreLogo(context, uri)
                    } else {
                        prepareProductImage(context, uri)
                    }
                }.getOrNull()
            if (prepared == null) {
                onPrepareFailed("No se pudo preparar la imagen. Prueba otra de hasta 16 MB.")
            } else {
                onUpload(kind, prepared.bytes, prepared.filename, prepared.mimeType)
            }
        }

    Surface(
        modifier = modifier.fillMaxWidth().border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column {
                Text(
                    "Logo y foto de tu negocio",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colors.textPrimary,
                )
                Text(
                    "Tus clientes los ven en la app al pedir. JPG, PNG o WebP de hasta 5 MB.",
                    fontSize = 12.5.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StoreImageSlot(
                    title = "Logo",
                    help = "En el carrito y el seguimiento. Mejor cuadrado y con fondo transparente.",
                    url = images.logoUrl,
                    fit = ContentScale.Fit,
                    loading = busy == StoreImageKind.LOGO,
                    enabled = enabled && busy == null,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    onPick = {
                        picking = StoreImageKind.LOGO
                        picker.launch("image/*")
                    },
                    onRemove = { onRemove(StoreImageKind.LOGO) },
                )
                StoreImageSlot(
                    title = "Foto del lugar",
                    help = "Arriba en \"Mis pedidos\" y en el directorio. Mejor horizontal.",
                    url = images.photoUrl,
                    fit = ContentScale.Crop,
                    loading = busy == StoreImageKind.PHOTO,
                    enabled = enabled && busy == null,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    onPick = {
                        picking = StoreImageKind.PHOTO
                        picker.launch("image/*")
                    },
                    onRemove = { onRemove(StoreImageKind.PHOTO) },
                )
            }
            if (notice != null) {
                Text(notice, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun StoreImageSlot(
    title: String,
    help: String,
    url: String?,
    fit: ContentScale,
    loading: Boolean,
    enabled: Boolean,
    colors: OperationalColors,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(16.dp))
                .background(colors.cardInner)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = colors.textPrimary)
        Text(help, fontSize = 11.5.sp, lineHeight = 15.sp, color = colors.textSecondary)
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.background),
            contentAlignment = Alignment.Center,
        ) {
            if (url != null) {
                ProductImage(
                    imageUrl = url,
                    contentDescription = "$title actual",
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    contentScale = fit,
                )
            } else {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        ActionPill(
            label =
                if (loading) {
                    "Guardando…"
                } else if (url != null) {
                    "Cambiar"
                } else {
                    "Subir"
                },
            enabled = enabled,
            loading = loading,
            colors = colors,
            primary = false,
            onClick = onPick,
        )
        if (url != null) {
            Text(
                "Quitar",
                color = colors.textSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = enabled, onClickLabel = "Quitar ${title.lowercase()}", onClick = onRemove)
                        .padding(vertical = 4.dp, horizontal = 2.dp),
            )
        }
    }
}
