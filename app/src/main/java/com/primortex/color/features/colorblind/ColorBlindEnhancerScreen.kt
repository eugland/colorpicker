package com.primortex.color.features.colorblind

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.math.MathUtils
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.primortex.color.R
import com.primortex.color.i18n.stringResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

data class ColorBlindEnhancerUiState(
    val torchOn: Boolean = false,
    val useFrontCamera: Boolean = false,
    val selectedMode: EnhancerMode = EnhancerMode.Drastic,
    val showModeGrid: Boolean = false,
    val intrinsicMlEffect: RenderEffect? = null
)

@HiltViewModel
class ColorBlindEnhancerViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(ColorBlindEnhancerUiState())
    val uiState: StateFlow<ColorBlindEnhancerUiState> = _uiState.asStateFlow()

    private var smoothedGains: FloatArray? = null
    private val gainHistory = ArrayDeque<FloatArray>()

    fun onToggleTorch() {
        _uiState.update { it.copy(torchOn = !it.torchOn) }
    }

    fun onToggleCameraFacing() {
        _uiState.update { it.copy(useFrontCamera = !it.useFrontCamera) }
    }

    fun onOpenModeGrid() {
        _uiState.update { it.copy(showModeGrid = true) }
    }

    fun onCloseModeGrid() {
        _uiState.update { it.copy(showModeGrid = false) }
    }

    fun onSelectMode(mode: EnhancerMode) {
        val resetIntrinsic = mode != EnhancerMode.Intrinsic
        if (resetIntrinsic) {
            gainHistory.clear()
            smoothedGains = null
        }
        _uiState.update {
            it.copy(
                selectedMode = mode,
                showModeGrid = false,
                intrinsicMlEffect = if (resetIntrinsic) null else it.intrinsicMlEffect
            )
        }
    }

    fun onAnalyzeFrame(image: ImageProxy) {
        try {
            if (_uiState.value.selectedMode != EnhancerMode.Intrinsic) return
            val gains = estimateIlluminantGains(image) ?: return
            val medianGains = updateGainHistory(gainHistory, gains, maxSamples = 8)
            val blended = smoothGains(smoothedGains, medianGains, 0.2f)
            smoothedGains = blended
            val effect = RenderEffect.createColorFilterEffect(
                ColorMatrixColorFilter(createColorMatrix(blended))
            )
            _uiState.update { it.copy(intrinsicMlEffect = effect) }
        } finally {
            image.close()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorBlindEnhancerScreen(onBack: () -> Unit) {
    val viewModel: ColorBlindEnhancerViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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

    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    val runtimeShader = remember(ctx) {
        loadRuntimeShader(ctx, R.raw.monochrome_shader, "monochrome_shader")
    }
    val enhancerShader = remember(ctx) {
        loadRuntimeShader(
            context = ctx,
            rawResId = R.raw.color_blind_enhancer_shader,
            shaderName = "color_blind_enhancer_shader"
        )
    }
    val renderEffect = remember(runtimeShader) {
        runtimeShader?.let { RenderEffect.createRuntimeShaderEffect(it, "inner") }
    }
    val enhancerRenderEffect = remember(enhancerShader) {
        enhancerShader?.let { RenderEffect.createRuntimeShaderEffect(it, "inner") }
    }
    val drasticShader = remember(ctx) {
        loadRuntimeShader(ctx, R.raw.drastic_shader, "drastic_shader")
    }
    val drasticRenderEffect = remember(drasticShader) {
        drasticShader?.let { RenderEffect.createRuntimeShaderEffect(it, "inner") }
    }
    val edgeContrastShader = remember(ctx) {
        loadRuntimeShader(ctx, R.raw.edge_contrast_shader, "edge_contrast_shader")
    }
    val edgeContrastRenderEffect = remember(edgeContrastShader) {
        edgeContrastShader?.let { RenderEffect.createRuntimeShaderEffect(it, "inner") }
    }
    val thermalShader = remember(ctx) {
        loadRuntimeShader(ctx, R.raw.thermal_shader, "thermal_shader")
    }
    val thermalRenderEffect = remember(thermalShader) {
        thermalShader?.let { RenderEffect.createRuntimeShaderEffect(it, "inner") }
    }
    val mriShader = remember(ctx) {
        loadRuntimeShader(ctx, R.raw.mri_shader, "mri_shader")
    }
    val mriRenderEffect = remember(mriShader) {
        mriShader?.let { RenderEffect.createRuntimeShaderEffect(it, "inner") }
    }
    val xrayShader = remember(ctx) {
        loadRuntimeShader(ctx, R.raw.xray_shader, "xray_shader")
    }
    val xrayRenderEffect = remember(xrayShader) {
        xrayShader?.let { RenderEffect.createRuntimeShaderEffect(it, "inner") }
    }
    val animateShader = remember(ctx) {
        loadRuntimeShader(ctx, R.raw.animate_shader, "animate_shader")
    }
    val animateRenderEffect = remember(animateShader) {
        animateShader?.let { RenderEffect.createRuntimeShaderEffect(it, "inner") }
    }
    val cyberShader = remember(ctx) {
        loadRuntimeShader(ctx, R.raw.cyber_shader, "cyber_shader")
    }
    val cyberRenderEffect = remember(cyberShader) {
        cyberShader?.let { RenderEffect.createRuntimeShaderEffect(it, "inner") }
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val previewView = remember {
        PreviewView(ctx).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(uiState.torchOn, camera) {
        camera?.cameraControl?.enableTorch(uiState.torchOn)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPerm) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize(),
                factory = { previewView },
                update = { view ->
                    if (uiState.selectedMode == EnhancerMode.Normal) {
                        view.setRenderEffect(null)
                        return@AndroidView
                    }
                    when (uiState.selectedMode) {
                        EnhancerMode.Monochrome -> view.setRenderEffect(renderEffect)

                        EnhancerMode.Enhance -> {
                            enhancerShader?.setFloatUniform("intensity", 1.0f)
                            view.setRenderEffect(enhancerRenderEffect)
                        }

                        EnhancerMode.Drastic -> view.setRenderEffect(drasticRenderEffect)

                        EnhancerMode.Edge -> {
                            if (edgeContrastShader != null) {
                                val width = view.width.coerceAtLeast(1)
                                val height = view.height.coerceAtLeast(1)
                                edgeContrastShader.setFloatUniform(
                                    "invSize",
                                    1f / width,
                                    1f / height
                                )
                            }
                            view.setRenderEffect(edgeContrastRenderEffect)
                        }

                        EnhancerMode.Thermal -> view.setRenderEffect(thermalRenderEffect)

                        EnhancerMode.Mri -> view.setRenderEffect(mriRenderEffect)

                        EnhancerMode.Xray -> view.setRenderEffect(xrayRenderEffect)

                        EnhancerMode.Animate -> view.setRenderEffect(animateRenderEffect)

                        EnhancerMode.Cyber -> view.setRenderEffect(cyberRenderEffect)

                        EnhancerMode.Intrinsic -> view.setRenderEffect(uiState.intrinsicMlEffect)

                        EnhancerMode.Normal -> view.setRenderEffect(null)
                    }
                }
            )

            LaunchedEffect(hasCameraPerm, uiState.useFrontCamera) {
                if (!hasCameraPerm) return@LaunchedEffect
                bindEnhancerCamera(
                    context = ctx,
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    cameraSelector = if (uiState.useFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    },
                    onCameraProviderReady = { cameraProvider = it },
                    onCameraReady = { camera = it },
                    cameraExecutor = cameraExecutor,
                    onAnalyzeFrame = viewModel::onAnalyzeFrame
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.camera_permission_required), color = Color.White)
                Spacer(Modifier.height(10.dp))
                Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(R.string.grant))
                }
            }
        }


        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back)
                        )
                    }

                    Text(
                        stringResource(R.string.color_blind_enhancer_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = viewModel::onToggleTorch) {
                        Icon(
                            imageVector = if (uiState.torchOn) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                            contentDescription = if (uiState.torchOn) {
                                stringResource(R.string.flash_on)
                            } else {
                                stringResource(R.string.flash_off)
                            }
                        )
                    }
                    IconButton(onClick = {
                        viewModel.onToggleCameraFacing()
                        Log.d("ColorBlindEnhancer", "Flip camera ${!uiState.useFrontCamera}")
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Cameraswitch,
                            contentDescription = stringResource(R.string.flip_camera)
                        )
                    }
                }
            }
        }

        if (uiState.showModeGrid) {
            val modes = EnhancerMode.values().toList()
            val columns = 2

            // Tune these to match your ModeGridItem visual size
            val itemHeight = 108
            val vSpacing = 12
            val rows = (modes.size + columns - 1) / columns
            val gridHeight = rows * itemHeight + (rows - 1).coerceAtLeast(0) * vSpacing

            Box(modifier = Modifier.fillMaxSize()) {

                // Scrim (tap outside to close)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { viewModel.onCloseModeGrid() }
                )

                // Sheet
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .systemBarsPadding(),
                    tonalElevation = 10.dp,
                    shadowElevation = 10.dp,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            // prevent clicks inside sheet from closing it
                            .clickable(enabled = false) {}
                            .padding(horizontal = 20.dp)
                            .padding(top = 10.dp, bottom = 64.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Handle
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(44.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                        )

                        // Header row (title + close)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.filter_grid_title),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )

                            TextButton(onClick = viewModel::onCloseModeGrid) {
                                Text(stringResource(R.string.close))
                            }
                        }

                        // Grid (not scrollable; shows all items)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            userScrollEnabled = false,
                            verticalArrangement = Arrangement.spacedBy(vSpacing.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(gridHeight.dp)
                        ) {
                            items(modes) { mode ->
                                Box(modifier = Modifier.height(itemHeight.dp)) {
                                    ModeGridItem(
                                        mode = mode,
                                        selected = mode == uiState.selectedMode,
                                        onSelect = {
                                            viewModel.onSelectMode(mode)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }


        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .clickable { viewModel.onOpenModeGrid() }
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.filter_selector_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(uiState.selectedMode.labelRes),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}


enum class EnhancerMode(val labelRes: Int) {
    Normal(R.string.mode_normal),
    Drastic(R.string.drastic_mode),
    Enhance(R.string.color_blind_enhancement),
    Monochrome(R.string.monochrome_mode),
    Edge(R.string.edge_contrast_mode),
    Animate(R.string.animate_mode),
    Cyber(R.string.cyber_mode),
    Thermal(R.string.thermal_mode),
    Mri(R.string.mri_mode),
    Xray(R.string.xray_mode),
    Intrinsic(R.string.intrinsic_mode)
}

private fun loadRuntimeShader(context: Context, rawResId: Int, shaderName: String): RuntimeShader? {
    return runCatching {
        val source =
            context.resources.openRawResource(rawResId).bufferedReader().use { it.readText() }
        RuntimeShader(source)
    }.getOrElse { error ->
        Log.e("ColorBlindEnhancer", "Failed to load shader: $shaderName", error)
        null
    }
}

@Composable
private fun ModeGridItem(
    mode: EnhancerMode,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val previewColor = when (mode) {
        EnhancerMode.Normal -> MaterialTheme.colorScheme.surfaceVariant
        EnhancerMode.Drastic -> Color(0xFFEF6C00)
        EnhancerMode.Enhance -> Color(0xFF00897B)
        EnhancerMode.Monochrome -> Color(0xFF9E9E9E)
        EnhancerMode.Edge -> Color(0xFF5C6BC0)
        EnhancerMode.Animate -> Color(0xFFE91E63)
        EnhancerMode.Cyber -> Color(0xFF00BCD4)
        EnhancerMode.Thermal -> Color(0xFFFF7043)
        EnhancerMode.Mri -> Color(0xFF7E57C2)
        EnhancerMode.Xray -> Color(0xFF607D8B)
        EnhancerMode.Intrinsic -> Color(0xFF26A69A)
    }

    val shape = RoundedCornerShape(16.dp)
    val borderColor =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (selected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surface

    val elevation by animateDpAsState(
        targetValue = if (selected) 8.dp else 1.dp,
        label = "modeCardElevation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onSelect),
        color = containerColor,
        tonalElevation = elevation,
        shadowElevation = if (selected) 2.dp else 0.dp,
        shape = shape,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top row: label + selected indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(mode.labelRes),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (selected) {
                    // simple "check pill"
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.selected_str),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Preview strip (looks like a mini mode preview)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(previewColor.copy(alpha = 0.92f))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
            )

            // Optional: tiny helper text (if you have one)
            // Text(
            //     text = "Tap to apply",
            //     style = MaterialTheme.typography.labelMedium,
            //     color = MaterialTheme.colorScheme.onSurfaceVariant
            // )
        }
    }
}

private fun estimateIlluminantGains(image: ImageProxy): FloatArray? {
    if (image.format != android.graphics.ImageFormat.YUV_420_888) return null
    val samples = collectChromaSamples(image)
    if (samples.size < 8) return null

    val centers = kMeansChroma(samples, k = 2, iterations = 6)
    val neutral = floatArrayOf(1f / 3f, 1f / 3f, 1f / 3f)
    val chosen = centers.minByOrNull { distance(it, neutral) } ?: return null
    val gains = floatArrayOf(
        MathUtils.clamp(neutral[0] / chosen[0], 0.7f, 1.4f),
        MathUtils.clamp(neutral[1] / chosen[1], 0.7f, 1.4f),
        MathUtils.clamp(neutral[2] / chosen[2], 0.7f, 1.4f)
    )
    return gains
}

private fun collectChromaSamples(image: ImageProxy): List<FloatArray> {
    val samples = ArrayList<FloatArray>()
    val w = image.width
    val h = image.height
    val stepX = (w / 10).coerceAtLeast(1)
    val stepY = (h / 10).coerceAtLeast(1)
    var y = stepY / 2
    while (y < h) {
        var x = stepX / 2
        while (x < w) {
            val rgb = sampleRgb(image, x, y)
            if (rgb == null) {
                x += stepX
                continue
            }
            val sum = rgb[0] + rgb[1] + rgb[2]
            if (sum > 0.05f) {
                val chroma = floatArrayOf(rgb[0] / sum, rgb[1] / sum, rgb[2] / sum)
                val luma = luminance(rgb)
                val sat = length(rgb[0] - luma, rgb[1] - luma, rgb[2] - luma)
                val specular = luma > 0.85f && sat < 0.08f
                if (!specular && sat > 0.02f) {
                    samples.add(chroma)
                }
            }
            x += stepX
        }
        y += stepY
    }
    return samples
}

private fun kMeansChroma(
    samples: List<FloatArray>,
    k: Int,
    iterations: Int
): List<FloatArray> {
    val first = samples.first()
    var farthest = first
    var maxDist = 0f
    for (sample in samples) {
        val dist = distance(sample, first)
        if (dist > maxDist) {
            maxDist = dist
            farthest = sample
        }
    }
    val centers = MutableList(k) { idx ->
        if (idx == 0) first.copyOf() else farthest.copyOf()
    }

    repeat(iterations) {
        val sums = Array(k) { floatArrayOf(0f, 0f, 0f) }
        val counts = IntArray(k)
        for (sample in samples) {
            var best = 0
            var bestDist = Float.MAX_VALUE
            for (i in 0 until k) {
                val dist = distance(sample, centers[i])
                if (dist < bestDist) {
                    bestDist = dist
                    best = i
                }
            }
            sums[best][0] += sample[0]
            sums[best][1] += sample[1]
            sums[best][2] += sample[2]
            counts[best] += 1
        }
        for (i in 0 until k) {
            if (counts[i] > 0) {
                centers[i][0] = sums[i][0] / counts[i]
                centers[i][1] = sums[i][1] / counts[i]
                centers[i][2] = sums[i][2] / counts[i]
            }
        }
    }
    return centers
}

private fun sampleRgb(image: ImageProxy, x: Int, y: Int): FloatArray? {
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

    val yf = (yBuf.get(yIndex).toInt() and 0xFF).toFloat()
    val uf = (uBuf.get(uIndex).toInt() and 0xFF).toFloat() - 128f
    val vf = (vBuf.get(vIndex).toInt() and 0xFF).toFloat() - 128f

    val r = (yf + 1.402f * vf).coerceIn(0f, 255f)
    val g = (yf - 0.344136f * uf - 0.714136f * vf).coerceIn(0f, 255f)
    val b = (yf + 1.772f * uf).coerceIn(0f, 255f)

    return floatArrayOf(r / 255f, g / 255f, b / 255f)
}

private fun luminance(rgb: FloatArray): Float {
    return 0.2126f * rgb[0] + 0.7152f * rgb[1] + 0.0722f * rgb[2]
}

private fun length(x: Float, y: Float, z: Float): Float {
    return kotlin.math.sqrt(x * x + y * y + z * z)
}

private fun distance(a: FloatArray, b: FloatArray): Float {
    return length(a[0] - b[0], a[1] - b[1], a[2] - b[2])
}

private fun updateGainHistory(
    history: ArrayDeque<FloatArray>,
    gains: FloatArray,
    maxSamples: Int
): FloatArray {
    history.addLast(gains.copyOf())
    while (history.size > maxSamples) {
        history.removeFirst()
    }
    return medianGains(history)
}

private fun medianGains(history: ArrayDeque<FloatArray>): FloatArray {
    val r = ArrayList<Float>(history.size)
    val g = ArrayList<Float>(history.size)
    val b = ArrayList<Float>(history.size)
    for (sample in history) {
        r.add(sample[0])
        g.add(sample[1])
        b.add(sample[2])
    }
    return floatArrayOf(
        medianOf(r),
        medianOf(g),
        medianOf(b)
    )
}

private fun medianOf(values: ArrayList<Float>): Float {
    values.sort()
    val mid = values.size / 2
    return if (values.size % 2 == 0) {
        (values[mid - 1] + values[mid]) * 0.5f
    } else {
        values[mid]
    }
}

private fun smoothGains(previous: FloatArray?, current: FloatArray, alpha: Float): FloatArray {
    if (previous == null) return current.copyOf()
    val t = alpha.coerceIn(0f, 1f)
    return floatArrayOf(
        previous[0] + (current[0] - previous[0]) * t,
        previous[1] + (current[1] - previous[1]) * t,
        previous[2] + (current[2] - previous[2]) * t
    )
}

private fun createColorMatrix(gains: FloatArray): ColorMatrix {
    return ColorMatrix(
        floatArrayOf(
            gains[0], 0f, 0f, 0f, 0f,
            0f, gains[1], 0f, 0f, 0f,
            0f, 0f, gains[2], 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

private fun bindEnhancerCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    cameraSelector: CameraSelector,
    onCameraProviderReady: (ProcessCameraProvider) -> Unit,
    onCameraReady: (androidx.camera.core.Camera) -> Unit,
    cameraExecutor: ExecutorService,
    onAnalyzeFrame: (ImageProxy) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        onCameraProviderReady(cameraProvider)

        val preview = Preview.Builder()
            .setTargetResolution(Size(1280, 720))
            .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
            .build()
            .also { it.surfaceProvider = previewView.surfaceProvider }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysis.setAnalyzer(cameraExecutor) { image ->
            onAnalyzeFrame(image)
        }

        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                analysis
            )
            onCameraReady(camera)
        } catch (_: Exception) {
        }
    }, ContextCompat.getMainExecutor(context))
}


