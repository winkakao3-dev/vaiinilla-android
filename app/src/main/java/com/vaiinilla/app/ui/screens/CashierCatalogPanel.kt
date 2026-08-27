package com.vaiinilla.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.vaiinilla.app.core.io.readBytesLimited
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.CatalogProductDraft
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.ui.components.AuthInkSubmitButton
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigDecimal

@Composable
fun CashierCatalogPanel(
    catalog: Catalog?,
    acting: Boolean,
    enabled: Boolean,
    onToggleAvailable: (productId: Int, available: Boolean) -> Unit,
    onCreateProduct: (CatalogProductDraft, ByteArray?, String?, String?) -> Unit,
    onUploadImage: (productId: Int, bytes: ByteArray, filename: String, mimeType: String) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val context = LocalContext.current
    val products = catalog?.products.orEmpty().sortedBy { it.name.lowercase() }
    val categories = catalog?.categories.orEmpty().sortedBy { it.order }
    var composing by remember { mutableStateOf(false) }
    var imageHint by remember { mutableStateOf<String?>(null) }
    var photoTargetId by remember { mutableStateOf<Int?>(null) }
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    val gallery =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            val productId = photoTargetId
            photoTargetId = null
            uri ?: return@rememberLauncherForActivityResult
            if (productId == null) return@rememberLauncherForActivityResult
            val prepared = prepareProductImage(context, uri)
            if (prepared == null) {
                imageHint = "No pude leer esa foto. Prueba JPEG/PNG, o una más ligera."
                return@rememberLauncherForActivityResult
            }
            onUploadImage(productId, prepared.bytes, prepared.filename, prepared.mimeType)
        }
    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            val productId = photoTargetId
            val uri = captureUri
            photoTargetId = null
            if (!ok || uri == null || productId == null) return@rememberLauncherForActivityResult
            val prepared = prepareProductImage(context, uri)
            if (prepared == null) {
                imageHint = "La cámara no dejó una foto usable. Intenta de nuevo."
                return@rememberLauncherForActivityResult
            }
            onUploadImage(productId, prepared.bytes, prepared.filename, prepared.mimeType)
        }
    val cameraPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                imageHint = "Sin permiso de cámara no se puede tomar la foto."
                photoTargetId = null
                return@rememberLauncherForActivityResult
            }
            captureUri = createCaptureUri(context)
            captureUri?.let { camera.launch(it) }
        }

    fun launchGallery(productId: Int) {
        photoTargetId = productId
        gallery.launch("image/*")
    }

    fun launchCamera(productId: Int) {
        photoTargetId = productId
        val granted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) {
            captureUri = createCaptureUri(context)
            captureUri?.let { camera.launch(it) }
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Menú del establecimiento", color = colors.muted, fontWeight = FontWeight.Black)
        Text(
            "Crea productos, súbeles foto y apágalos si se agotan.",
            color = colors.muted,
        )
        if (enabled) {
            AuthInkSubmitButton(
                text = if (composing) "Cerrar alta" else "Nuevo producto",
                onClick = { composing = !composing },
                enabled = !acting,
            )
        }
        if (composing && categories.isNotEmpty()) {
            CashierCreateProductForm(
                categories = categories,
                acting = acting,
                onImageHint = { imageHint = it },
                onCreate = { draft, bytes, name, mime ->
                    onCreateProduct(draft, bytes, name, mime)
                },
            )
        } else if (composing) {
            Text("No hay categorías. Un admin tiene que crearlas primero.", color = colors.muted)
        }
        imageHint?.let { Text(it, color = colors.muted) }
        if (products.isEmpty()) {
            Text("Aún no hay productos en este menú.", color = colors.muted)
        }
        products.forEach { product ->
            key(product.id) {
                CashierProductRow(
                    product = product,
                    acting = acting,
                    enabled = enabled,
                    onToggleAvailable = onToggleAvailable,
                    onPickGallery = { launchGallery(product.id) },
                    onTakePhoto = { launchCamera(product.id) },
                )
            }
        }
    }
}

@Composable
private fun CashierProductRow(
    product: Product,
    acting: Boolean,
    enabled: Boolean,
    onToggleAvailable: (Int, Boolean) -> Unit,
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProductImage(
            imageUrl = product.imageUrl,
            contentDescription = product.name,
            modifier = Modifier.size(48.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, color = colors.ink, fontWeight = FontWeight.Bold)
            Text(
                if (product.available) "En menú · ${product.counterPrice}" else "Agotado · ${product.counterPrice}",
                color = colors.muted,
            )
        }
        IconButton(onClick = onPickGallery, enabled = enabled && !acting) {
            Icon(
                Icons.Outlined.PhotoLibrary,
                contentDescription = "Elegir foto",
                tint = colors.ink,
            )
        }
        IconButton(onClick = onTakePhoto, enabled = enabled && !acting) {
            Icon(
                Icons.Outlined.AddAPhoto,
                contentDescription = "Tomar foto",
                tint = colors.ink,
            )
        }
        Switch(
            checked = product.available,
            onCheckedChange = { onToggleAvailable(product.id, it) },
            enabled = enabled && !acting,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = colors.accentInk,
                    checkedTrackColor = colors.accent,
                    uncheckedThumbColor = colors.muted,
                    uncheckedTrackColor = colors.paper2,
                ),
        )
    }
}

@Composable
private fun CashierCreateProductForm(
    categories: List<Category>,
    acting: Boolean,
    onImageHint: (String) -> Unit,
    onCreate: (CatalogProductDraft, ByteArray?, String?, String?) -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf(categories.first().id) }
    var station by remember { mutableStateOf(PreparationStation.KITCHEN) }
    var minutes by remember { mutableStateOf("8") }
    var pendingImage by remember { mutableStateOf<PreparedProductImage?>(null) }
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    val gallery =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            val prepared = prepareProductImage(context, uri)
            if (prepared == null) {
                onImageHint("No pude leer esa foto. Prueba JPEG/PNG, o una más ligera.")
                return@rememberLauncherForActivityResult
            }
            pendingImage = prepared
            onImageHint("Foto lista para subir")
        }
    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            val uri = captureUri
            if (!ok || uri == null) return@rememberLauncherForActivityResult
            val prepared = prepareProductImage(context, uri)
            if (prepared == null) {
                onImageHint("La cámara no dejó una foto usable. Intenta de nuevo.")
                return@rememberLauncherForActivityResult
            }
            pendingImage = prepared
            onImageHint("Foto lista para subir")
        }
    val cameraPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                captureUri = createCaptureUri(context)
                captureUri?.let { camera.launch(it) }
            }
        }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre", color = colors.muted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.ink,
                    unfocusedTextColor = colors.ink,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.line,
                ),
        )
        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("Precio mostrador", color = colors.muted) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.ink,
                    unfocusedTextColor = colors.ink,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.line,
                ),
        )
        Text(
            text =
                "El menú del alumno usa el precio digital que devuelve la API; " +
                    "puede ser distinto al precio mostrador.",
            color = colors.muted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        OutlinedTextField(
            value = minutes,
            onValueChange = { minutes = it.filter(Char::isDigit).take(3) },
            label = { Text("Minutos de preparación", color = colors.muted) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.ink,
                    unfocusedTextColor = colors.ink,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.line,
                ),
        )
        Text("Categoría", color = colors.muted, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { category ->
                val isSelected = category.id == categoryId
                FilterChip(
                    selected = isSelected,
                    onClick = { categoryId = category.id },
                    label = {
                        Text(
                            category.name,
                            color = if (isSelected) colors.accentInk else colors.ink,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accent,
                            containerColor = colors.paper2,
                        ),
                )
            }
        }
        Text("Estación", color = colors.muted, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val isKitchen = station == PreparationStation.KITCHEN
            FilterChip(
                selected = isKitchen,
                onClick = { station = PreparationStation.KITCHEN },
                label = {
                    Text(
                        "Cocina",
                        color = if (isKitchen) colors.accentInk else colors.ink,
                        fontWeight = if (isKitchen) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.accent,
                        containerColor = colors.paper2,
                    ),
            )
            val isCashier = station == PreparationStation.CASHIER
            FilterChip(
                selected = isCashier,
                onClick = { station = PreparationStation.CASHIER },
                label = {
                    Text(
                        "Caja",
                        color = if (isCashier) colors.accentInk else colors.ink,
                        fontWeight = if (isCashier) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.accent,
                        containerColor = colors.paper2,
                    ),
            )
        }
        Text(
            if (pendingImage == null) "Foto opcional" else "Foto lista para subir",
            color = colors.muted,
        )
        Row {
            IconButton(onClick = { gallery.launch("image/*") }) {
                Icon(
                    Icons.Outlined.PhotoLibrary,
                    contentDescription = "Elegir foto",
                    tint = colors.ink,
                )
            }
            IconButton(
                onClick = {
                    val granted =
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        captureUri = createCaptureUri(context)
                        captureUri?.let { camera.launch(it) }
                    } else {
                        cameraPermission.launch(Manifest.permission.CAMERA)
                    }
                },
            ) {
                Icon(
                    Icons.Outlined.AddAPhoto,
                    contentDescription = "Tomar foto",
                    tint = colors.ink,
                )
            }
        }
        AuthInkSubmitButton(
            text = "Publicar producto",
            onClick = {
                val wirePrice = formatCounterPrice(price) ?: return@AuthInkSubmitButton
                val wait = minutes.toIntOrNull() ?: 8
                onCreate(
                    CatalogProductDraft(
                        categoryId = categoryId,
                        preparationStation = station,
                        name = name.trim(),
                        estimatedTimeMinutes = wait.coerceIn(0, 240),
                        counterPrice = wirePrice,
                    ),
                    pendingImage?.bytes,
                    pendingImage?.filename,
                    pendingImage?.mimeType,
                )
            },
            enabled = !acting && name.isNotBlank() && formatCounterPrice(price) != null,
        )
    }
}

private fun formatCounterPrice(raw: String): String? {
    val parsed = raw.trim().replace(',', '.').toBigDecimalOrNull() ?: return null
    if (parsed < BigDecimal.ZERO) return null
    return Money.format(parsed)
}

private fun createCaptureUri(context: android.content.Context): Uri {
    val dir = File(context.cacheDir, "product_photos").apply { mkdirs() }
    val file = File(dir, "capture-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.product_photos", file)
}

private data class PreparedProductImage(
    val bytes: ByteArray,
    val filename: String,
    val mimeType: String,
)

private fun prepareProductImage(
    context: android.content.Context,
    uri: Uri,
): PreparedProductImage? {
    val original =
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytesLimited(MAX_SOURCE_PRODUCT_IMAGE_BYTES)
        } ?: return null
    if (original.isEmpty()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(original, 0, original.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val longest = maxOf(bounds.outWidth, bounds.outHeight)
    val sample = if (longest <= 1600) 1 else Integer.highestOneBit((longest - 1) / 1600)
    val decoded =
        BitmapFactory.decodeByteArray(
            original,
            0,
            original.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: return null
    return try {
        var quality = 88
        var bytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            if (!decoded.compress(Bitmap.CompressFormat.JPEG, quality, out)) return null
            bytes = out.toByteArray()
            quality -= 8
        } while (bytes.size > MAX_PRODUCT_IMAGE_BYTES && quality >= 50)
        if (bytes.size > MAX_PRODUCT_IMAGE_BYTES) return null
        PreparedProductImage(bytes, "producto.jpg", "image/jpeg")
    } finally {
        decoded.recycle()
    }
}

private const val MAX_PRODUCT_IMAGE_BYTES = 5 * 1024 * 1024
private const val MAX_SOURCE_PRODUCT_IMAGE_BYTES = 16 * 1024 * 1024
