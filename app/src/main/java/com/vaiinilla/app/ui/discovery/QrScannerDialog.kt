package com.vaiinilla.app.ui.discovery

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialPrimaryButton
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun QrScannerDialog(
    onClose: () -> Unit,
    onPayload: (String) -> Unit,
    helperText: String = "Apunta al QR del comedor o de la mesa",
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

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            if (permissionGranted && cameraError == null) {
                CameraQrPreview(
                    onClose = onClose,
                    onPayload = onPayload,
                    onCameraError = { cameraError = it },
                    helperText = helperText,
                )
            } else if (permissionGranted && cameraError != null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No se pudo abrir el escáner.", color = Color.White)
                    Text(
                        cameraError.orEmpty(),
                        color = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    EditorialPrimaryButton(
                        text = "Intentar de nuevo",
                        onClick = { cameraError = null },
                        background = Color.White,
                        contentColor = Color.Black,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    EditorialAccentButton(
                        text = "Cerrar",
                        onClick = onClose,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Necesitamos la cámara para leer el QR.", color = Color.White)
                    Text(
                        "También puedes cerrar y pegar el token manualmente.",
                        color = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    EditorialPrimaryButton(
                        text = "Intentar de nuevo",
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        background = Color.White,
                        contentColor = Color.Black,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    EditorialAccentButton(
                        text = "Cerrar",
                        onClick = onClose,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
@SuppressLint("UnsafeOptInUsageError")
private fun CameraQrPreview(
    onClose: () -> Unit,
    onPayload: (String) -> Unit,
    onCameraError: (String) -> Unit,
    helperText: String,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnPayload by rememberUpdatedState(onPayload)
    val currentOnCameraError by rememberUpdatedState(onCameraError)
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

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.26f)),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.78f),
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
            ) {
                Box(modifier = Modifier.padding(vertical = 110.dp))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            EditorialAccentButton(text = "Cerrar", onClick = onClose)
        }
        Text(
            helperText,
            color = Color.White,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 42.dp),
        )
    }
}
