// com/primortex/color/screens/LiveCameraScreen.kt
package com.primortex.color.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.primortex.color.service.RecentPicksService
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun LiveCameraScreen(
    onBack: () -> Unit,
    onOpenPhotoPick: (String) -> Unit
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPerm by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPerm = it
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) onOpenPhotoPick(uri.toString())
    }

    LaunchedEffect(Unit) {
        permLauncher.launch(Manifest.permission.CAMERA)
    }

    var currentArgb by remember { mutableIntStateOf(0xFF7B8266.toInt()) }
    val hex = argbToHex(currentArgb)

    val previewView = remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        if (hasCameraPerm) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { previewView },
                update = {
                    bindCamera(
                        context = ctx,
                        lifecycleOwner = lifecycleOwner,
                        previewView = previewView,
                        onImageCaptureReady = { imageCapture = it },
                        onCenterSampleArgb = { sampled -> currentArgb = sampled },
                        cameraExecutor = cameraExecutor
                    )
                }
            )
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera permission required")
                Spacer(Modifier.height(10.dp))
                Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) { Text("Grant") }
            }
        }

        // Crosshair circle center
        Box(
            Modifier.align(Alignment.Center).size(86.dp)
                .clip(CircleShape).background(Color(currentArgb).copy(alpha = 0.35f))
                .padding(6.dp).clip(CircleShape).background(Color.Transparent)
        )

        // Top color bar (tap to add to recent)
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp).fillMaxWidth(0.92f)
                .clickable { RecentPicksService.addPick(currentArgb, "camera_live") },
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(Color(currentArgb)))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Live color", style = MaterialTheme.typography.labelLarge)
                    Text(hex, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { RecentPicksService.addPick(currentArgb, "camera_live") }) { Text("Add") }
            }
        }

        // Bottom controls
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("Close") }

            Spacer(Modifier.weight(1f))

            ShutterButton(
                onClick = {
                    val cap = imageCapture ?: return@ShutterButton
                    captureToTempFile(ctx, cap) { uriString ->
                        onOpenPhotoPick(uriString)
                    }
                }
            )

            Spacer(Modifier.weight(1f))

            IconButton(onClick = {
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            }) {
                Icon(Icons.Filled.Collections, contentDescription = "Album")
            }
        }
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(74.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f))
            .padding(6.dp).clip(CircleShape).background(Color.White).clickable(onClick = onClick)
    )
}

private fun bindCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onCenterSampleArgb: (Int) -> Unit,
    cameraExecutor: ExecutorService
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        onImageCaptureReady(capture)

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysis.setAnalyzer(cameraExecutor) { image ->
            // sample center pixel from YUV without converting full bitmap
            val sampled = sampleCenterArgb(image)
            if (sampled != null) onCenterSampleArgb(sampled)
            image.close()
        }

        val selector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture, analysis)
        } catch (_: Exception) {
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun captureToTempFile(ctx: Context, capture: ImageCapture, onDone: (String) -> Unit) {
    val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: ctx.filesDir
    val file = File(dir, "cap_${System.currentTimeMillis()}.jpg")
    val output = ImageCapture.OutputFileOptions.Builder(file).build()

    capture.takePicture(
        output,
        ContextCompat.getMainExecutor(ctx),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val uri = outputFileResults.savedUri ?: Uri.fromFile(file)
                onDone(uri.toString())
            }
            override fun onError(exception: ImageCaptureException) {}
        }
    )
}

/**
 * Samples center pixel from YUV_420_888 ImageProxy.
 * Returns ARGB (alpha = 255).
 */
private fun sampleCenterArgb(image: ImageProxy): Int? {
    if (image.format != android.graphics.ImageFormat.YUV_420_888) return null
    val w = image.width
    val h = image.height
    val x = w / 2
    val y = h / 2

    val yPlane = image.planes[0]
    val uPlane = image.planes[1]
    val vPlane = image.planes[2]

    val yBuf = yPlane.buffer
    val uBuf = uPlane.buffer
    val vBuf = vPlane.buffer

    val yRowStride = yPlane.rowStride
    val yPixelStride = yPlane.pixelStride

    val uRowStride = uPlane.rowStride
    val uPixelStride = uPlane.pixelStride

    val vRowStride = vPlane.rowStride
    val vPixelStride = vPlane.pixelStride

    val yIndex = yRowStride * y + yPixelStride * x
    val uvX = x / 2
    val uvY = y / 2
    val uIndex = uRowStride * uvY + uPixelStride * uvX
    val vIndex = vRowStride * uvY + vPixelStride * uvX

    if (yIndex >= yBuf.limit() || uIndex >= uBuf.limit() || vIndex >= vBuf.limit()) return null

    val Y = (yBuf.get(yIndex).toInt() and 0xFF)
    val U = (uBuf.get(uIndex).toInt() and 0xFF)
    val V = (vBuf.get(vIndex).toInt() and 0xFF)

    // YUV -> RGB (BT.601)
    val yf = Y.toFloat()
    val uf = (U - 128).toFloat()
    val vf = (V - 128).toFloat()

    var r = (yf + 1.402f * vf).toInt()
    var g = (yf - 0.344136f * uf - 0.714136f * vf).toInt()
    var b = (yf + 1.772f * uf).toInt()

    r = r.coerceIn(0, 255)
    g = g.coerceIn(0, 255)
    b = b.coerceIn(0, 255)

    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}
