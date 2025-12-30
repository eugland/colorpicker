package com.primortex.color.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.primortex.color.app.PickedColor
import com.primortex.color.service.ColorNameLookup
import com.primortex.color.service.RecentPicksService
import com.primortex.color.ui.util.argbToHex
import com.primortex.color.ui.util.rgbDistSq
import com.primortex.color.ui.util.sampleCenterArgb
import java.util.concurrent.*

@OptIn(ExperimentalMaterial3Api::class)
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
    LaunchedEffect(Unit) { permLauncher.launch(Manifest.permission.CAMERA) }

    // live color
    var currentArgb by remember { mutableIntStateOf(0xFF7B8266.toInt()) }
    val hex = argbToHex(currentArgb)
    var nearest by remember { mutableStateOf(ColorNameLookup.nearestName(currentArgb)) }
    var lastLookupArgb by remember { mutableIntStateOf(currentArgb) }

    // camera lifecycle
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val previewView = remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // debounce name lookup
    LaunchedEffect(currentArgb) {
        val snapshot = currentArgb
        kotlinx.coroutines.delay(10)
        if (snapshot != currentArgb) return@LaunchedEffect
        if (rgbDistSq(snapshot, lastLookupArgb) < 12 * 12) return@LaunchedEffect
        nearest = ColorNameLookup.nearestName(snapshot)
        lastLookupArgb = snapshot
    }

    // stop camera immediately when leaving
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

    // ---- DATA: recents + palette ----
    // Replace this with your real RecentPicksService state if you have it (StateFlow/MutableState/etc.)
    val recents by RecentPicksService.history.collectAsState()

    // palette being built by user
    val palette = remember { mutableStateListOf<Int>() } // store argb list

    fun addCurrentToPaletteAndRecents() {
        val pick = PickedColor(argb = currentArgb, name = nearest.name)
        // recents: newest first
        RecentPicksService.addPick(pick) // keep your existing persistence call
        // palette: avoid duplicates if you want
        if (palette.firstOrNull() != currentArgb) palette.add(0, currentArgb)
    }

    // ---- Bottom sheet scaffold ----
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )
    val scaffoldState = rememberBottomSheetScaffoldState(sheetState)

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 168.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetShadowElevation = 12.dp,
        sheetContent = {
            RecentSheet(
                latest = recents.firstOrNull(),
                recents = recents,
                onTapPick = { /* optional: currentArgb = it.argb */ }
            )
        }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner).background(Color.Black)) {

            // camera preview
            if (hasCameraPerm) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { previewView },
                    update = {
                        bindCamera(
                            context = ctx,
                            lifecycleOwner = lifecycleOwner,
                            previewView = previewView,
                            onCameraProviderReady = { cameraProvider = it },
                            onImageCaptureReady = { imageCapture = it },
                            onCenterSampleArgb = { sampled -> currentArgb = sampled },
                            cameraExecutor = cameraExecutor
                        )
                    }
                )
            } else {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Camera permission required", color = Color.White)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) { Text("Grant") }
                }
            }

            // SOLID top bar (not translucent)
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

                    // color swatch
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(currentArgb))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )

                    Spacer(Modifier.width(10.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            nearest.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            hex,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // center sample dot (clean)
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(currentArgb))
                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )

            // Palette builder bar ABOVE the sheet
            PaletteBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp) // sits above peek sheet
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                palette = palette,
                onAddColor  = { addCurrentToPaletteAndRecents() },
                onAddPalette = { /* optional: addCurrentToPaletteAndRecents() */ }
            )
        }
    }
}
@Composable
private fun PaletteBar(
    modifier: Modifier,
    palette: List<Int>,
    onAddColor: () -> Unit,
    onAddPalette: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT AREA (clickable): becomes "Add palette"
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    .clickable(onClick = onAddPalette)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = "Palette",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                StackedColorsPreview(colors = palette)
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Open palette",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

            }

            Spacer(Modifier.width(10.dp))

            // RIGHT BUTTON: always "Add color"
            Button(
                onClick = onAddColor,
                shape = RoundedCornerShape(44.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Colorize,
                    contentDescription = "Add color",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}


@Composable
private fun StackedColorsPreview(colors: List<Int>) {
    // Draw up to N circles slightly overlapped (looks like stack)
    val shown = if (colors.isEmpty()) listOf(0xFFBDBDBD.toInt()) else colors
    Box(Modifier.height(32.dp).width(72.dp)) {
        shown.take(10).forEachIndexed { i, argb ->
            Box(
                Modifier
                    .offset(x = (i * 16).dp, y = 0.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentSheet(
    latest: PickedColor?,
    recents: List<PickedColor>,
    onTapPick: (PickedColor) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {

        // Collapsed intent: show only latest at top area always
        Text(
            "Recent colors",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        if (latest != null) {
            RecentRow(latest, onTapPick)
        } else {
            Text(
                "No colors yet. Tap \uD83E\uDDEA to pick a color.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Divider(Modifier.padding(top = 6.dp))

        // Expanded list: user pulls up to see more
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 420.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(recents.drop(1)) { pick ->
                RecentRow(pick, onTapPick)
            }
        }
    }
}

@Composable
private fun RecentRow(pick: PickedColor, onTapPick: (PickedColor) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onTapPick(pick) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(28.dp)
                .clip(CircleShape)
                .background(Color(pick.argb))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                pick.name ?: "Unknown",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                String.format("#%06X", pick.argb and 0xFFFFFF),
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun bindCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onCameraProviderReady: (ProcessCameraProvider) -> Unit,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onCenterSampleArgb: (Int) -> Unit,
    cameraExecutor: ExecutorService
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        onCameraProviderReady(cameraProvider)

        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }

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
            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture, analysis)
        } catch (_: Exception) {}
    }, ContextCompat.getMainExecutor(context))
}
