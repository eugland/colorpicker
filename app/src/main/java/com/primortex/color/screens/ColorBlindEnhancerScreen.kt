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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.primortex.color.R

private const val MONOCHROME_SHADER = """
uniform shader inner;

half4 main(float2 coord) {
    half4 c = inner.eval(coord);
    float y = dot(c.rgb, vec3(0.2126, 0.7152, 0.0722));
    return half4(vec3(y), c.a);
}
"""

private const val COLOR_BLIND_ENHANCER_SHADER = """
uniform shader inner;
uniform float intensity;

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

vec3 simulateDeuteranopia(vec3 lms) {
    return vec3(lms.x, 0.494207 * lms.x + 1.24827 * lms.z, lms.z);
}

vec3 redistributeError(vec3 error) {
    return vec3(error.g, 0.0, error.g);
}

half4 main(float2 coord) {
    half4 c = inner.eval(coord);
    vec3 rgb = c.rgb;
    vec3 lms = rgbToLms * rgb;
    vec3 simLms = simulateDeuteranopia(lms);
    vec3 simRgb = lmsToRgb * simLms;
    vec3 error = rgb - simRgb;
    vec3 corrected = clamp(rgb + redistributeError(error) * intensity, 0.0, 1.0);
    return half4(corrected, c.a);
}
"""

private const val DRASTIC_SHADER = """
uniform shader inner;

vec3 saturateColor(vec3 color, float saturation) {
    float y = dot(color, vec3(0.2126, 0.7152, 0.0722));
    return mix(vec3(y), color, saturation);
}

half4 main(float2 coord) {
    half4 c = inner.eval(coord);
    vec3 boosted = saturateColor(c.rgb, 2.2);
    float luma = dot(boosted, vec3(0.2126, 0.7152, 0.0722));
    float highlightTame = smoothstep(0.6, 1.0, luma);
    vec3 tamed = mix(boosted, boosted * 0.85, highlightTame);
    boosted = clamp(tamed + vec3(0.02), 0.0, 1.0);
    return half4(boosted, c.a);
}
"""

private const val EDGE_CONTRAST_SHADER = """
uniform shader inner;
uniform float2 invSize;

float luminance(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

half4 main(float2 coord) {
    vec3 center = inner.eval(coord).rgb;
    vec3 left = inner.eval(coord + vec2(-invSize.x, 0.0)).rgb;
    vec3 right = inner.eval(coord + vec2(invSize.x, 0.0)).rgb;
    vec3 up = inner.eval(coord + vec2(0.0, -invSize.y)).rgb;
    vec3 down = inner.eval(coord + vec2(0.0, invSize.y)).rgb;
    float edge = luminance(center) - (luminance(left) + luminance(right) + luminance(up) + luminance(down)) * 0.25;
    vec3 boosted = clamp(center + vec3(edge * 1.2), 0.0, 1.0);
    return half4(boosted, 1.0);
}
"""

private const val THERMAL_SHADER = """
uniform shader inner;

vec3 thermalRamp(float t) {
    if (t < 0.25) {
        return mix(vec3(0.02, 0.0, 0.2), vec3(0.0, 0.0, 0.8), t / 0.25);
    }
    if (t < 0.5) {
        return mix(vec3(0.0, 0.0, 0.8), vec3(0.0, 0.8, 0.6), (t - 0.25) / 0.25);
    }
    if (t < 0.75) {
        return mix(vec3(0.0, 0.8, 0.6), vec3(0.9, 0.8, 0.0), (t - 0.5) / 0.25);
    }
    return mix(vec3(0.9, 0.8, 0.0), vec3(1.0, 0.2, 0.0), (t - 0.75) / 0.25);
}

half4 main(float2 coord) {
    vec3 rgb = inner.eval(coord).rgb;
    float t = clamp(dot(rgb, vec3(0.2126, 0.7152, 0.0722)), 0.0, 1.0);
    return half4(thermalRamp(t), 1.0);
}
"""

private const val MRI_SHADER = """
uniform shader inner;

half4 main(float2 coord) {
    vec3 rgb = inner.eval(coord).rgb;
    float t = clamp(dot(rgb, vec3(0.2126, 0.7152, 0.0722)), 0.0, 1.0);
    vec3 cool = vec3(0.1, 0.2, 0.4);
    vec3 warm = vec3(0.9, 0.7, 0.5);
    vec3 mapped = mix(cool, warm, pow(t, 0.75));
    return half4(mapped, 1.0);
}
"""

private const val XRAY_SHADER = """
uniform shader inner;

half4 main(float2 coord) {
    vec3 rgb = inner.eval(coord).rgb;
    float t = clamp(dot(rgb, vec3(0.2126, 0.7152, 0.0722)), 0.0, 1.0);
    float inv = 1.0 - t;
    vec3 mapped = vec3(inv * 0.9 + 0.1);
    return half4(mapped, 1.0);
}
"""

private const val ANIMATE_SHADER = """
uniform shader inner;

half4 main(float2 coord) {
    vec3 rgb = inner.eval(coord).rgb;
    float y = dot(rgb, vec3(0.2126, 0.7152, 0.0722));
    float q = floor(y * 6.0 + 0.5) / 6.0;
    vec3 boosted = mix(vec3(y), rgb, 1.6);
    vec3 toon = mix(vec3(q), boosted, 0.6);
    return half4(toon, 1.0);
}
"""

private const val CYBER_SHADER = """
uniform shader inner;

vec3 edgeGlow(vec3 center, vec3 left, vec3 right, vec3 up, vec3 down) {
    vec3 lap = (left + right + up + down) * 0.25 - center;
    return abs(lap);
}

half4 main(float2 coord) {
    vec3 center = inner.eval(coord).rgb;
    vec3 left = inner.eval(coord + vec2(-1.0, 0.0)).rgb;
    vec3 right = inner.eval(coord + vec2(1.0, 0.0)).rgb;
    vec3 up = inner.eval(coord + vec2(0.0, -1.0)).rgb;
    vec3 down = inner.eval(coord + vec2(0.0, 1.0)).rgb;
    vec3 glow = edgeGlow(center, left, right, up, down);
    vec3 cyber = clamp(center * vec3(0.8, 1.0, 1.3) + glow * 1.8, 0.0, 1.0);
    return half4(cyber, 1.0);
}
"""

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

    var selectedMode by remember { mutableStateOf(EnhancerMode.Drastic) }

    val runtimeShader = remember(supportsShader) {
        RuntimeShader(MONOCHROME_SHADER)
    }
    val enhancerShader = remember(supportsShader) {
        RuntimeShader(COLOR_BLIND_ENHANCER_SHADER)
    }
    val renderEffect = remember(runtimeShader) {
        runtimeShader?.let { RenderEffect.createRuntimeShaderEffect(it, "inner") }
    }
    val enhancerRenderEffect = remember(enhancerShader) {
        enhancerShader?.let { RenderEffect.createRuntimeShaderEffect(it, "inner") }
    }
    val drasticShader = remember(supportsShader) {
        RuntimeShader(DRASTIC_SHADER)
    }
    val drasticRenderEffect = remember(drasticShader) {
        drasticShader?.let { RenderEffect.createRuntimeShaderEffect(it, "inner") }
    }
    val edgeContrastShader = remember(supportsShader) {
        RuntimeShader(EDGE_CONTRAST_SHADER)
    }
    val edgeContrastRenderEffect = remember(edgeContrastShader) {
        edgeContrastShader?.let { RenderEffect.createRuntimeShaderEffect(it, "inner") }
    }
    val thermalShader = remember(supportsShader) {
        RuntimeShader(THERMAL_SHADER)
    }
    val thermalRenderEffect = remember(thermalShader) {
        thermalShader?.let { RenderEffect.createRuntimeShaderEffect(it, "inner") }
    }
    val mriShader = remember(supportsShader) {
        RuntimeShader(MRI_SHADER)
    }
    val mriRenderEffect = remember(mriShader) {
        mriShader?.let { RenderEffect.createRuntimeShaderEffect(it, "inner") }
    }
    val xrayShader = remember(supportsShader) {
        RuntimeShader(XRAY_SHADER)
    }
    val xrayRenderEffect = remember(xrayShader) {
        xrayShader?.let { RenderEffect.createRuntimeShaderEffect(it, "inner") }
    }
    val animateShader = remember(supportsShader) {
        RuntimeShader(ANIMATE_SHADER)
    }
    val animateRenderEffect = remember(animateShader) {
        animateShader?.let { RenderEffect.createRuntimeShaderEffect(it, "inner") }
    }
    val cyberShader = remember(supportsShader) {
        RuntimeShader(CYBER_SHADER)
    }
    val cyberRenderEffect = remember(cyberShader) {
        cyberShader?.let { RenderEffect.createRuntimeShaderEffect(it, "inner") }
    }

    val previewView = remember {
        PreviewView(ctx).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    var showModeGrid by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    LaunchedEffect(torchOn, camera) {
        camera?.cameraControl?.enableTorch(torchOn)
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
                        if (!supportsShader || selectedMode == EnhancerMode.Normal) {
                            view.setRenderEffect(null)
                            return@AndroidView
                        }
                        when (selectedMode) {
                            EnhancerMode.Monochrome -> {
                                if (runtimeShader != null) {
                                    view.setRenderEffect(renderEffect)
                                } else {
                                    view.setRenderEffect(null)
                                }
                            }

                            EnhancerMode.Enhance -> {
                                if (enhancerShader != null) {
                                    enhancerShader.setFloatUniform("intensity", 1.0f)
                                    view.setRenderEffect(enhancerRenderEffect)
                                } else {
                                    view.setRenderEffect(null)
                                }
                            }

                            EnhancerMode.Drastic -> {
                                if (drasticShader != null) {
                                    view.setRenderEffect(drasticRenderEffect)
                                } else {
                                    view.setRenderEffect(null)
                                }
                            }

                            EnhancerMode.Edge -> {
                                if (edgeContrastShader != null) {
                                    val width = view.width.coerceAtLeast(1)
                                    val height = view.height.coerceAtLeast(1)
                                    edgeContrastShader.setFloatUniform("invSize", 1f / width, 1f / height)
                                    view.setRenderEffect(edgeContrastRenderEffect)
                                } else {
                                    view.setRenderEffect(null)
                                }
                            }

                            EnhancerMode.Thermal -> {
                                if (thermalShader != null) {
                                    view.setRenderEffect(thermalRenderEffect)
                                } else {
                                    view.setRenderEffect(null)
                                }
                            }

                            EnhancerMode.Mri -> {
                                if (mriShader != null) {
                                    view.setRenderEffect(mriRenderEffect)
                                } else {
                                    view.setRenderEffect(null)
                                }
                            }

                            EnhancerMode.Xray -> {
                                if (xrayShader != null) {
                                    view.setRenderEffect(xrayRenderEffect)
                                } else {
                                    view.setRenderEffect(null)
                                }
                            }

                            EnhancerMode.Animate -> {
                                if (animateShader != null) {
                                    view.setRenderEffect(animateRenderEffect)
                                } else {
                                    view.setRenderEffect(null)
                                }
                            }

                            EnhancerMode.Cyber -> {
                                if (cyberShader != null) {
                                    view.setRenderEffect(cyberRenderEffect)
                                } else {
                                    view.setRenderEffect(null)
                                }
                            }

                            EnhancerMode.Normal -> view.setRenderEffect(null)
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

            if (showModeGrid) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { showModeGrid = false }
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .systemBarsPadding(),
                    tonalElevation = 6.dp,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.filter_grid_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.height(360.dp)
                        ) {
                            items(EnhancerMode.values()) { mode ->
                                ModeGridItem(
                                    mode = mode,
                                    selected = mode == selectedMode,
                                    onSelect = {
                                        selectedMode = mode
                                        showModeGrid = false
                                    }
                                )
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
                            .clickable { showModeGrid = true }
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
                            text = stringResource(selectedMode.labelRes),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

private enum class EnhancerMode(val labelRes: Int) {
    Normal(R.string.mode_normal),
    Drastic(R.string.drastic_mode),
    Enhance(R.string.color_blind_enhancement),
    Monochrome(R.string.monochrome_mode),
    Edge(R.string.edge_contrast_mode),
    Animate(R.string.animate_mode),
    Cyber(R.string.cyber_mode),
    Thermal(R.string.thermal_mode),
    Mri(R.string.mri_mode),
    Xray(R.string.xray_mode)
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
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        tonalElevation = if (selected) 6.dp else 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(previewColor, CircleShape)
            )
            Text(
                text = stringResource(mode.labelRes),
                style = MaterialTheme.typography.bodyMedium
            )
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
