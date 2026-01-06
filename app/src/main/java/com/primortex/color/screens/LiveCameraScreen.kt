package com.primortex.color.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.primortex.color.app.PickedColor
import com.primortex.color.service.ColorServices
import com.primortex.color.service.PaletteService
import com.primortex.color.service.RecentPicksService
import com.primortex.color.service.PickerSensitivity
import com.primortex.color.service.SettingsService
import com.primortex.color.ui.components.ActiveColorSheet
import com.primortex.color.ui.components.ColorDetailsBottomSheet
import com.primortex.color.ui.components.CrosshairIndicator
import com.primortex.color.ui.components.PaletteBar
import com.primortex.color.ui.util.sampleCenterArgb
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveCameraScreen(
    onBack: () -> Unit,
    onOpenPalette: (String, Boolean) -> Unit
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colorNameService = remember(ctx) {
        ColorServices.ensure(ctx)
        ColorServices.colorNames
    }
    var detailPick by remember { mutableStateOf<PickedColor?>(null) }

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            hasCameraPerm = it
        }

    LaunchedEffect(hasCameraPerm) {
        if (!hasCameraPerm) {
            permLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var currentArgb by remember { mutableIntStateOf(0xFF7B8266.toInt()) }
    val crosshairSize by SettingsService.crosshairSize.collectAsState()
    val crosshairShape by SettingsService.crosshairShape.collectAsState()
    val pickerSensitivity by SettingsService.pickerSensitivity.collectAsState()
    val pickedColor by remember {
        derivedStateOf {
            val argb = currentArgb
            PickedColor(
                argb = argb,
                name = colorNameService.localNameFromArgb(argb)
            )
        }
    }


    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val previewView =
        remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val uiScope = rememberCoroutineScope()
    var frozen by remember { mutableStateOf(false) }
    var torchOn by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var lastCommitMs by remember { mutableStateOf(0L) }
    var lastCommittedArgb by remember { mutableIntStateOf(currentArgb) }

    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var zoomRatio by remember { mutableStateOf(1f) }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            imageCapture = null
            cameraExecutor.shutdown()
        }
    }

    val exit: () -> Unit = {
        cameraProvider?.unbindAll()
        onBack()
    }

    LaunchedEffect(camera) {
        val info = camera?.cameraInfo ?: return@LaunchedEffect
        zoomRatio = info.zoomState.value?.zoomRatio ?: 1f
    }

    val recents by RecentPicksService.history.collectAsState()
    val palette = remember { mutableStateListOf<PickedColor>() }


    LaunchedEffect(torchOn, camera) {
        camera?.cameraControl?.enableTorch(torchOn)
    }

    fun showSnack(msg: String) {
        uiScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    CameraScreenLayout(
        pickedColor = pickedColor,
        recents = recents,
        onTapPick = { pick -> detailPick = pick },
        snackbarHostState = snackbarHostState
    ) {
        if (hasCameraPerm) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(camera) {
                        val cam = camera ?: return@pointerInput
                        var cur = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1f

                        detectTransformGestures { _, _, zoomChange, _ ->
                            val zs = cam.cameraInfo.zoomState.value
                                ?: return@detectTransformGestures
                            cur = (cur * zoomChange).coerceIn(
                                zs.minZoomRatio,
                                zs.maxZoomRatio
                            )
                            cam.cameraControl.setZoomRatio(cur)
                        }
                    },
                factory = { previewView },
                update = {}
            )

            LaunchedEffect(hasCameraPerm, useFrontCamera) {
                if (!hasCameraPerm) return@LaunchedEffect

                bindCamera(
                    context = ctx,
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    onCameraProviderReady = { cameraProvider = it },
                    onImageCaptureReady = { imageCapture = it },
                    onCenterSampleArgb = { sampled ->
                        if (frozen) return@bindCamera
                        val now = android.os.SystemClock.uptimeMillis()
                        val (minIntervalMs, minRgbDistance) = when (pickerSensitivity) {
                            PickerSensitivity.Low -> 140L to 24
                            PickerSensitivity.Medium -> 90L to 14
                            PickerSensitivity.High -> 60L to 8
                        }
                        val minDistSq = minRgbDistance * minRgbDistance

                        // debounce by time + ignore tiny color jitter
                        if (now - lastCommitMs < minIntervalMs) return@bindCamera
                        if (rgbDistSq(
                                sampled,
                                lastCommittedArgb
                            ) < minDistSq
                        ) return@bindCamera

                        lastCommitMs = now
                        lastCommittedArgb = sampled
                        currentArgb = sampled
                    },
                    cameraExecutor = cameraExecutor,
                    cameraSelector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,
                    onCameraReady = { camera = it },
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission required", color = Color.White)
                Spacer(Modifier.height(10.dp))
                Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(
                        "Grant"
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding(),
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = exit) {
                    Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back")
                }

                Text(
                    "Live picking",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { frozen = !frozen }) {
                    Icon(
                        imageVector = if (frozen) Icons.Outlined.PlayCircle else Icons.Outlined.PauseCircle,
                        contentDescription = if (frozen) "Resume" else "Freeze"
                    )
                }
                IconButton(onClick = { torchOn = !torchOn }) {
                    Icon(
                        imageVector = if (torchOn) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                        contentDescription = if (torchOn) "Flash on" else "Flash off"
                    )
                }
                IconButton(onClick = {
                    useFrontCamera = !useFrontCamera
                    Log.d("LiveCameraScreen", "Flip camera $useFrontCamera")
                }) {
                    Icon(Icons.Outlined.Cameraswitch, contentDescription = "Flip camera")
                }
            }
        }
        if (hasCameraPerm) {
            CrosshairIndicator(
                argb = currentArgb,
                size = crosshairSize,
                shape = crosshairShape,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        PaletteBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp) // sits above peek sheet
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            palette = palette,
            onAddColor = {
                RecentPicksService.addPick(pickedColor)
                when {
                    pickedColor in palette -> showSnack("${pickedColor.name} already in palette")
                    palette.size >= 10 -> showSnack("Palette is full (10 colors). Tap the palette to save it and start a new one.")
                    else -> {
                        palette.add(pickedColor)
                    }
                }
            },
            onAddPalette = {
                if (palette.isEmpty()) {
                    showSnack("Palette empty, start adding colors")
                    return@PaletteBar
                }
                val saved = PaletteService.create(
                    name = "Palette ${PaletteService.palettes.value.size + 1}",
                    tags = listOf("camera", "live-pick"),
                    colors = palette
                )
                palette.clear()
                showSnack("Palette saved ✅")
                onOpenPalette(saved.id, true)
            },
            onClearPalette = { palette.clear() }
        )
    }

    detailPick?.let { picked ->
        ColorDetailsBottomSheet(
            picked = picked,
            onDismiss = { detailPick = null },
            onOpenColorDetail = { p -> detailPick = p }
        )
    }
}

@ExperimentalMaterial3Api
@Composable
fun CameraScreenLayout(
    pickedColor: PickedColor,
    recents: List<PickedColor>,
    onTapPick: (PickedColor) -> Unit,
    snackbarHostState: SnackbarHostState,
    content: @Composable BoxScope.(PaddingValues) -> Unit
) {
    val bottomSheetState = rememberBottomSheetScaffoldState()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        BottomSheetScaffold(
            scaffoldState = bottomSheetState,
            sheetPeekHeight = 168.dp,
            sheetContainerColor = MaterialTheme.colorScheme.surface,
            sheetShadowElevation = 12.dp,
            sheetContent = {
                ActiveColorSheet(
                    picked = pickedColor,
                    recents = recents,
                    onTapPick = onTapPick
                )
            }
        ) { inner ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .background(Color.Black)
            ) {
                content(inner) // (or combine padding like Option A)
            }
        }
    }
}

private fun bindCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    cameraSelector: CameraSelector,
    onCameraProviderReady: (ProcessCameraProvider) -> Unit,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onCenterSampleArgb: (Int) -> Unit,
    onCameraReady: (androidx.camera.core.Camera) -> Unit,
    cameraExecutor: ExecutorService
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        onCameraProviderReady(cameraProvider)

        val preview = Preview.Builder()
            .build()
            .also { it.surfaceProvider = previewView.surfaceProvider }

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        onImageCaptureReady(capture)

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysis.setAnalyzer(cameraExecutor) { image ->
            val sampled = sampleCenterArgb(image)
            if (sampled != null) onCenterSampleArgb(sampled)
            image.close()
        }

        try {
            cameraProvider.unbindAll()

            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                capture,
                analysis
            )

            onCameraReady(camera)
        } catch (_: Exception) {
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun rgbDistSq(a: Int, b: Int): Int {
    val ar = (a shr 16) and 0xFF
    val ag = (a shr 8) and 0xFF
    val ab = (a) and 0xFF
    val br = (b shr 16) and 0xFF
    val bg = (b shr 8) and 0xFF
    val bb = (b) and 0xFF
    val dr = ar - br
    val dg = ag - bg
    val db = ab - bb
    return dr * dr + dg * dg + db * db
}
