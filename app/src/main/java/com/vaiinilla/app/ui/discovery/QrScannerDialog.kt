package com.vaiinilla.app.ui.discovery

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialSecondaryButton
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.screens.TABLE_CODE_LENGTH
import com.vaiinilla.app.ui.screens.shouldAutoResolveTableCode
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/** Otra forma de seguir sin cámara, distinta según quién abre el escáner. */
data class QrScannerAlternative(
    val label: String,
    val onClick: () -> Unit,
)

/** La hoja de abajo del escáner del cliente: los 4 números junto al QR o pedir sin mesa. */
data class QrScannerTableCode(
    val onSubmit: (String) -> Unit,
    /** Lugar donde se pide sin mesa; `null` mientras no ha elegido ninguno. */
    val venueName: String?,
    val onOrderWithoutTable: () -> Unit,
)

// El escáner siempre va sobre la cámara oscurecida: usa la paleta de noche aunque la app esté en Día.
private val ScannerBackdrop = Color(0xFF0B0C0A)
private val ScannerSheet = Color(0xFF1D1E1C)
private val ScannerTile = Color(0xFF292A27)
private val ScannerInk = Color(0xFFF5F1E5)
private val ScannerInk2 = Color(0xFFDEDBD0)
private val ScannerMuted = Color(0xFFAAA99F)
private val ScannerLine = Color.White.copy(alpha = 0.1f)
private val ScannerAccent = Color(0xFFB9D86D)
private val ScannerChrome = Color.Black.copy(alpha = 0.45f)
private val ScanEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
private val MinFrameSide = 140.dp

@Composable
fun QrScannerDialog(
    onClose: () -> Unit,
    onPayload: (String) -> Unit,
    title: String,
    subtitle: String? = null,
    alternative: QrScannerAlternative? = null,
    tableCode: QrScannerTableCode? = null,
    /**
     * Lo que se entrega, debajo del visor (C08): el cajero ve a quién busca mientras apunta. Recibe el margen de la
     * barra del sistema para que su fondo llegue hasta el borde, como la hoja del código de mesa.
     */
    footer: (@Composable (bottomInsets: WindowInsets) -> Unit)? = null,
) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionGranted = it
            if (it) cameraError = null
        }

    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    // Quien activa la cámara en Ajustes y vuelve no debe seguir viendo el aviso de permiso (COMPRA-14).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    val systemBars = rememberHostSystemBars()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = ScannerBackdrop) {
            if (permissionGranted && cameraError == null) {
                var camera by remember { mutableStateOf<Camera?>(null) }
                var torchOn by remember { mutableStateOf(false) }
                Box(Modifier.fillMaxSize()) {
                    CameraQrPreview(
                        onPayload = onPayload,
                        onCameraError = { cameraError = it },
                        onCameraReady = { camera = it },
                    )
                    QrScannerOverlay(
                        title = title,
                        subtitle = subtitle,
                        systemBars = systemBars,
                        onClose = onClose,
                        torchAvailable = camera?.cameraInfo?.hasFlashUnit() == true,
                        torchOn = torchOn,
                        onToggleTorch = {
                            val bound = camera ?: return@QrScannerOverlay
                            val wanted = !torchOn
                            torchOn = wanted
                            val result = bound.cameraControl.enableTorch(wanted)
                            result.addListener(
                                { if (runCatching { result.get() }.isFailure) torchOn = !wanted },
                                ContextCompat.getMainExecutor(context),
                            )
                        },
                        tableCode = tableCode,
                        footer = footer,
                    )
                }
            } else if (permissionGranted && cameraError != null) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(systemBars)
                            .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No se pudo abrir el escáner.", color = Color.White)
                    Text(
                        cameraError.orEmpty(),
                        color = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    EditorialAccentButton(
                        text = "Intentar de nuevo",
                        onClick = { cameraError = null },
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    alternative?.let { alt ->
                        TextButton(onClick = alt.onClick) {
                            Text(alt.label, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    EditorialSecondaryButton(
                        text = "Cerrar",
                        onClick = onClose,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(systemBars)
                            .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Para leer el QR necesitamos la cámara",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Si ya la negaste, actívala en los ajustes del teléfono.",
                        color = Color.White.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    EditorialAccentButton(
                        text = "Permitir cámara",
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    TextButton(onClick = { context.startActivity(appSettingsIntent(context)) }) {
                        Text("Abrir ajustes", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    alternative?.let { alt ->
                        TextButton(onClick = alt.onClick) {
                            Text(alt.label, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = onClose) {
                        Text("Cerrar", color = Color.White.copy(alpha = 0.72f))
                    }
                }
            }
        }
    }
}

private fun appSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

@Composable
@SuppressLint("UnsafeOptInUsageError")
private fun CameraQrPreview(
    onPayload: (String) -> Unit,
    onCameraError: (String) -> Unit,
    onCameraReady: (Camera) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnPayload by rememberUpdatedState(onPayload)
    val currentOnCameraError by rememberUpdatedState(onCameraError)
    val currentOnCameraReady by rememberUpdatedState(onCameraReady)
    val previewView =
        remember {
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val delivered = remember { AtomicBoolean(false) }

    DisposableEffect(lifecycleOwner, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val disposed = AtomicBoolean(false)
        var cameraProvider: ProcessCameraProvider? = null
        var analysis: ImageAnalysis? = null
        val scanner =
            BarcodeScanning.getClient(
                BarcodeScannerOptions
                    .Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build(),
            )

        fun reportCameraError(message: String) {
            if (!disposed.get()) currentOnCameraError(message)
        }

        cameraProviderFuture.addListener(
            {
                if (disposed.get()) return@addListener
                val provider =
                    runCatching { cameraProviderFuture.get() }
                        .getOrElse {
                            reportCameraError("No se pudo preparar la cámara del dispositivo.")
                            return@addListener
                        }
                if (disposed.get()) {
                    runCatching { provider.unbindAll() }
                    return@addListener
                }
                cameraProvider = provider
                val hasRearCamera =
                    runCatching { provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }
                        .getOrElse {
                            reportCameraError("No se pudo comprobar la cámara trasera.")
                            return@addListener
                        }
                if (!hasRearCamera) {
                    reportCameraError("No encontramos una cámara trasera disponible en este dispositivo.")
                    return@addListener
                }
                val preview =
                    Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                analysis =
                    ImageAnalysis
                        .Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                val currentAnalysis = analysis ?: return@addListener
                currentAnalysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (disposed.get() || mediaImage == null || delivered.get()) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val image =
                        InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees,
                        )
                    scanner
                        .process(image)
                        .addOnSuccessListener { barcodes ->
                            val value =
                                barcodes
                                    .firstOrNull { !it.rawValue.isNullOrBlank() }
                                    ?.rawValue
                                    ?.trim()
                            if (!value.isNullOrBlank() && delivered.compareAndSet(false, true)) {
                                ContextCompat.getMainExecutor(context).execute {
                                    if (!disposed.get()) currentOnPayload(value)
                                }
                            }
                        }.addOnCompleteListener {
                            imageProxy.close()
                        }
                }
                if (disposed.get()) return@addListener
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        currentAnalysis,
                    )
                }.onSuccess { camera ->
                    if (!disposed.get()) currentOnCameraReady(camera)
                }.onFailure {
                    currentAnalysis.clearAnalyzer()
                    runCatching { provider.unbindAll() }
                    reportCameraError("No se pudo iniciar la cámara trasera.")
                }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            disposed.set(true)
            analysis?.clearAnalyzer()
            runCatching { cameraProvider?.unbindAll() }
            scanner.close()
            executor.shutdownNow()
        }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize(),
    )
}

/** Lo que va encima de la cámara (L01 de Doop): cerrar, linterna, título, marco y, al cliente, la hoja del código. */
@Composable
internal fun QrScannerOverlay(
    title: String,
    subtitle: String?,
    systemBars: WindowInsets,
    onClose: () -> Unit,
    torchAvailable: Boolean,
    torchOn: Boolean,
    onToggleTorch: () -> Unit,
    tableCode: QrScannerTableCode?,
    modifier: Modifier = Modifier,
    footer: (@Composable (bottomInsets: WindowInsets) -> Unit)? = null,
) {
    val reduced = reducedMotion()
    val appear = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.84f, stiffness = 320f)) }

    Column(
        modifier
            .fillMaxSize()
            // brightness(.42) del dibujo: la cámara se ve pero no compite con el marco.
            .background(Color.Black.copy(alpha = 0.58f)),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .windowInsetsPadding(systemBars.only(WindowInsetsSides.Top))
                    .padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ScannerChrome)
                            .physicalPress(scale = PhysicalPressScale.Small, onClick = onClose)
                            .semantics { contentDescription = "Cerrar" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = null, tint = ScannerInk, modifier = Modifier.size(18.dp))
                }
                if (torchAvailable) TorchPill(on = torchOn, onClick = onToggleTorch)
            }
            Text(
                title,
                color = ScannerInk,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 30.dp, start = 20.dp, end = 20.dp)
                        .graphicsLayer {
                            alpha = appear.value
                            translationY = (1f - appear.value) * 10.dp.toPx()
                        },
            )
            subtitle?.let {
                Text(
                    it,
                    color = ScannerInk2,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, start = 24.dp, end = 24.dp)
                            .graphicsLayer { alpha = appear.value },
                )
            }
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                // Con el teclado arriba ya no cabe: el marco se encoge con la hoja y se desvanece antes de estorbar.
                val side = minOf(250.dp, maxHeight - 46.dp, maxWidth - 40.dp)
                if (side >= MinFrameSide) {
                    val room = ((side - MinFrameSide) / 60.dp).coerceIn(0f, 1f)
                    ScanFrame(
                        reduced = reduced,
                        modifier =
                            Modifier
                                .padding(top = 26.dp)
                                .size(side)
                                .graphicsLayer {
                                    val s = 0.92f + 0.08f * appear.value
                                    scaleX = s
                                    scaleY = s
                                    alpha = appear.value * room
                                },
                    )
                }
            }
        }
        if (tableCode != null) {
            TableCodeSheet(
                tableCode = tableCode,
                systemBars = systemBars,
                modifier =
                    Modifier.graphicsLayer {
                        translationY = (1f - appear.value) * size.height
                    },
            )
        } else if (footer != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationY = (1f - appear.value) * size.height },
            ) { footer(systemBars.only(WindowInsetsSides.Bottom)) }
        } else {
            Spacer(Modifier.windowInsetsPadding(systemBars.only(WindowInsetsSides.Bottom)).height(24.dp))
        }
    }
}

@Composable
private fun TorchPill(
    on: Boolean,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        if (on) ScannerInk else ScannerChrome,
        spring(dampingRatio = 0.9f, stiffness = 500f),
        label = "torch-bg",
    )
    val content by animateColorAsState(
        if (on) ScannerSheet else ScannerInk,
        spring(dampingRatio = 0.9f, stiffness = 500f),
        label = "torch-ink",
    )
    Row(
        modifier =
            Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(background)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .semantics { contentDescription = if (on) "Apagar linterna" else "Prender linterna" }
                .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Outlined.FlashlightOn,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(17.dp),
        )
        Text("Linterna", color = content, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** Cuatro esquinas de 44dp y la línea que barre del 8% al 88%, como el marco del dibujo. */
@Composable
private fun ScanFrame(
    reduced: Boolean,
    modifier: Modifier = Modifier,
) {
    val sweep =
        if (reduced) {
            0.48f
        } else {
            val transition = rememberInfiniteTransition(label = "scan-line")
            val value by transition.animateFloat(
                initialValue = 0.08f,
                targetValue = 0.88f,
                animationSpec = infiniteRepeatable(tween(2200, easing = ScanEasing), RepeatMode.Reverse),
                label = "scan-line-offset",
            )
            value
        }
    BoxWithConstraints(modifier) {
        val side = maxHeight
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val arm = 44.dp.toPx() * (side / 250.dp).coerceAtMost(1f)
            val radius = 18.dp.toPx() - stroke / 2
            val inset = stroke / 2
            val w = size.width
            val h = size.height
            listOf(
                Triple(Offset(inset, inset), 1f, 1f),
                Triple(Offset(w - inset, inset), -1f, 1f),
                Triple(Offset(inset, h - inset), 1f, -1f),
                Triple(Offset(w - inset, h - inset), -1f, -1f),
            ).forEach { (corner, dx, dy) ->
                val path =
                    Path().apply {
                        moveTo(corner.x, corner.y + dy * (arm - inset))
                        lineTo(corner.x, corner.y + dy * radius)
                        arcTo(
                            rect =
                                Rect(
                                    left = if (dx > 0) corner.x else corner.x - 2 * radius,
                                    top = if (dy > 0) corner.y else corner.y - 2 * radius,
                                    right = if (dx > 0) corner.x + 2 * radius else corner.x,
                                    bottom = if (dy > 0) corner.y + 2 * radius else corner.y,
                                ),
                            startAngleDegrees = if (dx > 0) 180f else 0f,
                            sweepAngleDegrees = if (dx * dy > 0) 90f else -90f,
                            forceMoveTo = false,
                        )
                        lineTo(corner.x + dx * (arm - inset), corner.y)
                    }
                drawPath(path, ScannerInk, style = Stroke(width = stroke))
            }
        }
        ScanLine(
            modifier =
                Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth()
                    .offset { IntOffset(0, (side.toPx() * sweep).roundToInt()) }
                    .height(3.dp),
        )
    }
}

@Composable
private fun ScanLine(modifier: Modifier) {
    Box(
        modifier.drawBehind {
            // box-shadow 0 0 18px 4px del dibujo, en capas: BlurMaskFilter no pinta con aceleración antes de API 28.
            val glow = 18.dp.toPx()
            val spread = 4.dp.toPx()
            val layers = 10
            for (layer in layers downTo 1) {
                val grow = spread + glow * layer / layers
                drawRoundRect(
                    color = ScannerAccent.copy(alpha = 0.6f / (layers + 1)),
                    topLeft = Offset(-grow, -grow),
                    size = Size(size.width + 2 * grow, size.height + 2 * grow),
                    cornerRadius = CornerRadius(grow + size.height / 2),
                )
            }
            drawRoundRect(ScannerAccent, cornerRadius = CornerRadius(size.height / 2))
        },
    )
}

@Composable
private fun TableCodeSheet(
    tableCode: QrScannerTableCode,
    systemBars: WindowInsets,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberVaiinillaHaptics()
    var code by rememberSaveable { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val submit by rememberUpdatedState(tableCode.onSubmit)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(ScannerSheet)
                .windowInsetsPadding(WindowInsets.ime.union(systemBars.only(WindowInsetsSides.Bottom)))
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 18.dp),
    ) {
        Text("¿No ves un QR?", color = ScannerInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Escribe los $TABLE_CODE_LENGTH números que vienen junto al QR de tu mesa.",
            color = ScannerMuted,
            fontSize = 13.5.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 3.dp),
        )
        BasicTextField(
            value = code,
            onValueChange = { raw ->
                val next = raw.filter(Char::isDigit).take(TABLE_CODE_LENGTH)
                if (next == code) return@BasicTextField
                haptics.click()
                val complete = shouldAutoResolveTableCode(code, next, resolving = false)
                code = next
                if (complete) submit(next)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onDone = { if (code.length == TABLE_CODE_LENGTH) submit(code) }),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .onFocusChanged { focused = it.isFocused }
                    .semantics { contentDescription = "Código de mesa de $TABLE_CODE_LENGTH números" }
                    .testTag("scanner-table-code"),
            decorationBox = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                ) {
                    repeat(TABLE_CODE_LENGTH) { index ->
                        DigitBox(
                            digit = code.getOrNull(index)?.toString(),
                            current = index == code.length,
                            focused = focused,
                        )
                    }
                }
            },
        )
        Box(
            Modifier
                .padding(top = 20.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(ScannerLine),
        )
        Row(
            modifier =
                Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .physicalPress(onClick = tableCode.onOrderWithoutTable)
                    .testTag("scanner-without-table"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(ScannerTile),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = ScannerInk, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Pedir sin mesa", color = ScannerInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    tableCode.venueName?.let { "Para llevar o a domicilio en $it" }
                        ?: "Busca el lugar para pedir para llevar",
                    color = ScannerMuted,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = ScannerMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DigitBox(
    digit: String?,
    current: Boolean,
    focused: Boolean,
    width: Dp = 58.dp,
) {
    val border by animateColorAsState(
        if (current) ScannerAccent else Color.Transparent,
        spring(dampingRatio = 0.9f, stiffness = 600f),
        label = "digit-border",
    )
    val pop = remember { Animatable(1f) }
    LaunchedEffect(digit) {
        if (digit != null) {
            pop.snapTo(0.84f)
            pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 650f))
        }
    }
    val caretAlpha =
        if (current && focused) {
            val blink = rememberInfiniteTransition(label = "caret")
            val value by blink.animateFloat(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(tween(530, delayMillis = 250), RepeatMode.Reverse),
                label = "caret-alpha",
            )
            value
        } else if (current) {
            1f
        } else {
            0f
        }
    Box(
        modifier =
            Modifier
                .size(width = width, height = 66.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ScannerTile)
                .border(2.dp, border, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (digit != null) {
            Text(
                digit,
                color = ScannerInk,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                modifier =
                    Modifier.graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    },
            )
        } else {
            Box(
                Modifier
                    .width(2.dp)
                    .height(30.dp)
                    .graphicsLayer { alpha = caretAlpha }
                    .background(ScannerAccent),
            )
        }
    }
}

/**
 * The scanner is a full-screen Dialog, which lives in its own window. On some
 * OEM builds (One UI) that window reports zero system-bar insets, so the close
 * button slid under the status bar. The host activity's raw insets are always
 * right, so take the larger of the two.
 */
@Composable
internal fun rememberHostSystemBars(): WindowInsets {
    val hostView = LocalView.current
    val density = LocalDensity.current
    val hostBars =
        remember(hostView, density) {
            val bars =
                ViewCompat
                    .getRootWindowInsets(hostView)
                    ?.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            with(density) {
                WindowInsets(
                    top = (bars?.top ?: 0).toDp(),
                    bottom = (bars?.bottom ?: 0).toDp(),
                )
            }
        }
    return WindowInsets.safeDrawing
        .only(WindowInsetsSides.Top + WindowInsetsSides.Bottom)
        .union(hostBars)
}
