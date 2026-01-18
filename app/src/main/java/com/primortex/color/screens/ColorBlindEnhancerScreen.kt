package com.primortex.color.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.primortex.color.R

private const val COLOR_BLIND_SHADER = """
uniform shader inner;
uniform float2 invSize;
uniform float intensity;
uniform float mode;
uniform float edgeEnabled;
uniform float edgeStrength;

const mat3 rgbToLms = mat3(
    0.31399022, 0.63951294, 0.04649755,
    0.15537241, 0.75789446, 0.08670142,
    0.01775239, 0.10944209, 0.87256922
);
const mat3 lmsToRgb = mat3(
    5.47221206, -4.6419601, 0.16963708,
    -1.1252419, 2.29317094, -0.1678952,
    0.02980165, -0.19318073, 1.16364789
);

float luminance(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

vec3 simulateDeficiency(vec3 lms, float mode) {
    if (mode < 0.5) {
        return vec3(2.02344 * lms.y - 2.52581 * lms.z, lms.y, lms.z);
    }
    if (mode < 1.5) {
        return vec3(lms.x, 0.494207 * lms.x + 1.24827 * lms.z, lms.z);
    }
    return vec3(lms.x, lms.y, -0.395913 * lms.x + 0.801109 * lms.y);
}

vec3 redistributeError(vec3 error, float mode) {
    if (mode < 0.5) {
        return vec3(0.0, error.r, error.r);
    }
    if (mode < 1.5) {
        return vec3(error.g, 0.0, error.g);
    }
    return vec3(error.b, error.b, 0.0);
}

half4 main(float2 coord) {
    half4 c = inner.eval(coord);
    vec3 rgb = c.rgb;
    vec3 lms = rgbToLms * rgb;
    vec3 simLms = simulateDeficiency(lms, mode);
    vec3 simRgb = lmsToRgb * simLms;
    vec3 error = rgb - simRgb;
    vec3 corrected = clamp(rgb + redistributeError(error, mode) * intensity, 0.0, 1.0);

    if (edgeEnabled > 0.5) {
        vec3 left = inner.eval(coord + vec2(-invSize.x, 0.0)).rgb;
        vec3 right = inner.eval(coord + vec2(invSize.x, 0.0)).rgb;
        vec3 up = inner.eval(coord + vec2(0.0, -invSize.y)).rgb;
        vec3 down = inner.eval(coord + vec2(0.0, invSize.y)).rgb;
        float lumCenter = luminance(corrected);
        float lumAvg = (luminance(left) + luminance(right) + luminance(up) + luminance(down)) * 0.25;
        float edge = lumCenter - lumAvg;
        corrected = clamp(corrected + edge * edgeStrength, 0.0, 1.0);
    }

    return half4(corrected, c.a);
}
"""

private const val MONOCHROME_SHADER = """
uniform shader inner;

half4 main(float2 coord) {
    half4 c = inner.eval(coord);
    float y = dot(c.rgb, vec3(0.2126, 0.7152, 0.0722));
    return half4(vec3(y), c.a);
}
"""

private enum class ColorBlindMode(val labelRes: Int, val shaderIndex: Float) {
    Protanopia(R.string.color_blind_mode_protan, 0f),
    Deuteranopia(R.string.color_blind_mode_deutan, 1f),
    Tritanopia(R.string.color_blind_mode_tritan, 2f)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorBlindEnhancerScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val supportsShader = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU }

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

    var useFrontCamera by remember { mutableStateOf(false) }
    var torchOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }

    var filterEnabled by remember { mutableStateOf(true) }
    var intensity by remember { mutableStateOf(0.7f) }
    var edgeEnabled by remember { mutableStateOf(false) }
    var edgeStrength by remember { mutableStateOf(0.15f) }
    var mode by remember { mutableStateOf(ColorBlindMode.Protanopia) }

    val runtimeShader = remember(supportsShader) {
        RuntimeShader(COLOR_BLIND_SHADER)
    }
    val renderEffect = remember(runtimeShader) {
        runtimeShader?.let { RenderEffect.createRuntimeShaderEffect(it, "inner") }
    }

    val previewView = remember {
        PreviewView(ctx).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val scaffoldState = rememberBottomSheetScaffoldState()

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    LaunchedEffect(torchOn, camera) {
        camera?.cameraControl?.enableTorch(torchOn)
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 88.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetShadowElevation = 8.dp,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            ColorBlindEnhancerSheet(
                scaffoldState = scaffoldState,
                supportsShader = supportsShader,
                filterEnabled = filterEnabled,
                onToggleFilter = { filterEnabled = it },
                mode = mode,
                onModeChange = { mode = it },
                intensity = intensity,
                onIntensityChange = { intensity = it },
                edgeEnabled = edgeEnabled,
                onEdgeEnabledChange = { edgeEnabled = it },
                edgeStrength = edgeStrength,
                onEdgeStrengthChange = { edgeStrength = it }
            )
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            if (hasCameraPerm) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { previewSize = it },
                    factory = { previewView },
                    update = { view ->
                        if (supportsShader && filterEnabled) {
                            val shader = runtimeShader ?: return@AndroidView
                            val width = previewSize.width.coerceAtLeast(1)
                            val height = previewSize.height.coerceAtLeast(1)
                            shader.setFloatUniform("intensity", intensity)
                            shader.setFloatUniform("mode", mode.shaderIndex)
                            shader.setFloatUniform("edgeEnabled", if (edgeEnabled) 1f else 0f)
                            shader.setFloatUniform("edgeStrength", edgeStrength)
                            shader.setFloatUniform("invSize", 1f / width, 1f / height)
                            view.setRenderEffect(renderEffect)
                        } else {
                            view.setRenderEffect(null)
                        }
                    }
                )

                LaunchedEffect(hasCameraPerm, useFrontCamera) {
                    if (!hasCameraPerm) return@LaunchedEffect
                    bindEnhancerCamera(
                        context = ctx,
                        lifecycleOwner = lifecycleOwner,
                        previewView = previewView,
                        cameraSelector = if (useFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        },
                        onCameraProviderReady = { cameraProvider = it },
                        onCameraReady = { camera = it }
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
                        IconButton(onClick = { torchOn = !torchOn }) {
                            Icon(
                                imageVector = if (torchOn) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                                contentDescription = if (torchOn) {
                                    stringResource(R.string.flash_on)
                                } else {
                                    stringResource(R.string.flash_off)
                                }
                            )
                        }
                        IconButton(onClick = {
                            useFrontCamera = !useFrontCamera
                            Log.d("ColorBlindEnhancer", "Flip camera $useFrontCamera")
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Cameraswitch,
                                contentDescription = stringResource(R.string.flip_camera)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorBlindEnhancerSheet(
    scaffoldState: BottomSheetScaffoldState,
    supportsShader: Boolean,
    filterEnabled: Boolean,
    onToggleFilter: (Boolean) -> Unit,
    mode: ColorBlindMode,
    onModeChange: (ColorBlindMode) -> Unit,
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    edgeEnabled: Boolean,
    onEdgeEnabledChange: (Boolean) -> Unit,
    edgeStrength: Float,
    onEdgeStrengthChange: (Float) -> Unit
) {
    val sheetState = scaffoldState.bottomSheetState
    val isPartial = sheetState.currentValue == SheetValue.PartiallyExpanded

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.color_blind_enhancer_subtitle),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.color_blind_enhancer_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!supportsShader) {
            Text(
                text = stringResource(R.string.color_blind_enhancer_requires_android_13),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.filter_enabled),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = filterEnabled && supportsShader,
                onCheckedChange = { onToggleFilter(it) },
                enabled = supportsShader
            )
        }

        if (!isPartial) {
            Column {
                Text(
                    text = stringResource(R.string.deficiency_mode),
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ColorBlindMode.values().forEach { option ->
                        val selected = option == mode
                        val buttonModifier = Modifier.weight(1f)
                        if (selected) {
                            Button(
                                onClick = { onModeChange(option) },
                                enabled = filterEnabled && supportsShader,
                                modifier = buttonModifier
                            ) {
                                Text(stringResource(option.labelRes))
                            }
                        } else {
                            FilledTonalButton(
                                onClick = { onModeChange(option) },
                                enabled = filterEnabled && supportsShader,
                                modifier = buttonModifier
                            ) {
                                Text(stringResource(option.labelRes))
                            }
                        }
                    }
                }
            }

            Column {
                Text(
                    text = stringResource(R.string.filter_strength),
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = intensity,
                    onValueChange = { onIntensityChange(it) },
                    valueRange = 0f..1f,
                    enabled = filterEnabled && supportsShader
                )
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.edge_enhancement),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = edgeEnabled,
                        onCheckedChange = { onEdgeEnabledChange(it) },
                        enabled = filterEnabled && supportsShader
                    )
                }
                Slider(
                    value = edgeStrength,
                    onValueChange = { onEdgeStrengthChange(it) },
                    valueRange = 0f..0.5f,
                    enabled = filterEnabled && supportsShader && edgeEnabled
                )
            }
        }
    }
}

private fun bindEnhancerCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    cameraSelector: CameraSelector,
    onCameraProviderReady: (ProcessCameraProvider) -> Unit,
    onCameraReady: (androidx.camera.core.Camera) -> Unit
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

        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview
            )
            onCameraReady(camera)
        } catch (_: Exception) {
        }
    }, ContextCompat.getMainExecutor(context))
}
