package com.primortex.color.screens

import android.Manifest
import android.content.Context
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.primortex.color.ui.LocalSnackbarService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
    val scope = rememberCoroutineScope()

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

    var monochromeEnabled by remember { mutableStateOf(false) }
    var enhancerEnabled by remember { mutableStateOf(false) }
    var drasticEnabled by remember { mutableStateOf(true) }
    var edgeContrastEnabled by remember { mutableStateOf(false) }
    var thermalEnabled by remember { mutableStateOf(false) }
    var mriEnabled by remember { mutableStateOf(false) }
    var xrayEnabled by remember { mutableStateOf(false) }
    var animateEnabled by remember { mutableStateOf(false) }
    var cyberEnabled by remember { mutableStateOf(false) }

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
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val snackbarService = LocalSnackbarService.current

    val scaffoldState = rememberBottomSheetScaffoldState()
    val sheetState = scaffoldState.bottomSheetState
    val sheetExpanded by remember {
        derivedStateOf { sheetState.currentValue == SheetValue.Expanded }
    }
    val currentMode by remember(
        monochromeEnabled,
        enhancerEnabled,
        drasticEnabled,
        edgeContrastEnabled,
        animateEnabled,
        cyberEnabled,
        thermalEnabled,
        mriEnabled,
        xrayEnabled
    ) {
        derivedStateOf {
            when {
                drasticEnabled -> EnhancerMode.Drastic
                enhancerEnabled -> EnhancerMode.Enhance
                monochromeEnabled -> EnhancerMode.Monochrome
                edgeContrastEnabled -> EnhancerMode.Edge
                animateEnabled -> EnhancerMode.Animate
                cyberEnabled -> EnhancerMode.Cyber
                thermalEnabled -> EnhancerMode.Thermal
                mriEnabled -> EnhancerMode.Mri
                xrayEnabled -> EnhancerMode.Xray
                else -> EnhancerMode.Normal
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            imageCapture = null
            cameraExecutor.shutdown()
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
                supportsShader = supportsShader,
                monochromeEnabled = monochromeEnabled,
                onToggleMonochrome = { enabled ->
                    monochromeEnabled = enabled
                    if (enabled) {
                        enhancerEnabled = false
                        drasticEnabled = false
                        edgeContrastEnabled = false
                        thermalEnabled = false
                        mriEnabled = false
                        xrayEnabled = false
                        animateEnabled = false
                        cyberEnabled = false
                    }
                },
                enhancerEnabled = enhancerEnabled,
                onToggleEnhancer = { enabled ->
                    enhancerEnabled = enabled
                    if (enabled) {
                        monochromeEnabled = false
                        drasticEnabled = false
                        edgeContrastEnabled = false
                        thermalEnabled = false
                        mriEnabled = false
                        xrayEnabled = false
                        animateEnabled = false
                        cyberEnabled = false
                    }
                },
                drasticEnabled = drasticEnabled,
                onToggleDrastic = { enabled ->
                    drasticEnabled = enabled
                    if (enabled) {
                        monochromeEnabled = false
                        enhancerEnabled = false
                        edgeContrastEnabled = false
                        thermalEnabled = false
                        mriEnabled = false
                        xrayEnabled = false
                        animateEnabled = false
                        cyberEnabled = false
                    }
                },
                edgeContrastEnabled = edgeContrastEnabled,
                onToggleEdgeContrast = { enabled ->
                    edgeContrastEnabled = enabled
                    if (enabled) {
                        monochromeEnabled = false
                        enhancerEnabled = false
                        drasticEnabled = false
                        thermalEnabled = false
                        mriEnabled = false
                        xrayEnabled = false
                        animateEnabled = false
                        cyberEnabled = false
                    }
                },
                thermalEnabled = thermalEnabled,
                onToggleThermal = { enabled ->
                    thermalEnabled = enabled
                    if (enabled) {
                        monochromeEnabled = false
                        enhancerEnabled = false
                        drasticEnabled = false
                        edgeContrastEnabled = false
                        mriEnabled = false
                        xrayEnabled = false
                        animateEnabled = false
                        cyberEnabled = false
                    }
                },
                mriEnabled = mriEnabled,
                onToggleMri = { enabled ->
                    mriEnabled = enabled
                    if (enabled) {
                        monochromeEnabled = false
                        enhancerEnabled = false
                        drasticEnabled = false
                        edgeContrastEnabled = false
                        thermalEnabled = false
                        xrayEnabled = false
                        animateEnabled = false
                        cyberEnabled = false
                    }
                },
                xrayEnabled = xrayEnabled,
                onToggleXray = { enabled ->
                    xrayEnabled = enabled
                    if (enabled) {
                        monochromeEnabled = false
                        enhancerEnabled = false
                        drasticEnabled = false
                        edgeContrastEnabled = false
                        thermalEnabled = false
                        mriEnabled = false
                        animateEnabled = false
                        cyberEnabled = false
                    }
                },
                animateEnabled = animateEnabled,
                onToggleAnimate = { enabled ->
                    animateEnabled = enabled
                    if (enabled) {
                        monochromeEnabled = false
                        enhancerEnabled = false
                        drasticEnabled = false
                        edgeContrastEnabled = false
                        thermalEnabled = false
                        mriEnabled = false
                        xrayEnabled = false
                        cyberEnabled = false
                    }
                },
                cyberEnabled = cyberEnabled,
                onToggleCyber = { enabled ->
                    cyberEnabled = enabled
                    if (enabled) {
                        monochromeEnabled = false
                        enhancerEnabled = false
                        drasticEnabled = false
                        edgeContrastEnabled = false
                        thermalEnabled = false
                        mriEnabled = false
                        xrayEnabled = false
                        animateEnabled = false
                    }
                }
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
                        .fillMaxSize(),
                    factory = { previewView },
                    update = { view ->
                        if (supportsShader && monochromeEnabled) {
                            if (runtimeShader != null) {
                                view.setRenderEffect(renderEffect)
                            } else {
                                view.setRenderEffect(null)
                            }
                        } else if (supportsShader && enhancerEnabled) {
                            if (enhancerShader != null) {
                                enhancerShader.setFloatUniform("intensity", 1.0f)
                                view.setRenderEffect(enhancerRenderEffect)
                            } else {
                                view.setRenderEffect(null)
                            }
                        } else if (supportsShader && drasticEnabled) {
                            if (drasticShader != null) {
                                view.setRenderEffect(drasticRenderEffect)
                            } else {
                                view.setRenderEffect(null)
                            }
                        } else if (supportsShader && edgeContrastEnabled) {
                            if (edgeContrastShader != null) {
                                val width = view.width.coerceAtLeast(1)
                                val height = view.height.coerceAtLeast(1)
                                edgeContrastShader.setFloatUniform("invSize", 1f / width, 1f / height)
                                view.setRenderEffect(edgeContrastRenderEffect)
                            } else {
                                view.setRenderEffect(null)
                            }
                        } else if (supportsShader && thermalEnabled) {
                            if (thermalShader != null) {
                                view.setRenderEffect(thermalRenderEffect)
                            } else {
                                view.setRenderEffect(null)
                            }
                        } else if (supportsShader && mriEnabled) {
                            if (mriShader != null) {
                                view.setRenderEffect(mriRenderEffect)
                            } else {
                                view.setRenderEffect(null)
                            }
                        } else if (supportsShader && xrayEnabled) {
                            if (xrayShader != null) {
                                view.setRenderEffect(xrayRenderEffect)
                            } else {
                                view.setRenderEffect(null)
                            }
                        } else if (supportsShader && animateEnabled) {
                            if (animateShader != null) {
                                view.setRenderEffect(animateRenderEffect)
                            } else {
                                view.setRenderEffect(null)
                            }
                        } else if (supportsShader && cyberEnabled) {
                            if (cyberShader != null) {
                                view.setRenderEffect(cyberRenderEffect)
                            } else {
                                view.setRenderEffect(null)
                            }
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
                        onImageCaptureReady = { imageCapture = it },
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

            if (sheetExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                )
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

            if (!sheetExpanded) {
                EnhancerControls(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .systemBarsPadding()
                        .padding(bottom = 12.dp),
                    currentMode = currentMode,
                    onModeSelected = { mode ->
                        when (mode) {
                            EnhancerMode.Normal -> {
                                monochromeEnabled = false
                                enhancerEnabled = false
                                drasticEnabled = false
                                edgeContrastEnabled = false
                                thermalEnabled = false
                                mriEnabled = false
                                xrayEnabled = false
                                animateEnabled = false
                                cyberEnabled = false
                            }

                            EnhancerMode.Drastic -> onToggleMode(
                                on = { drasticEnabled = true },
                                off = {
                                    monochromeEnabled = false
                                    enhancerEnabled = false
                                    edgeContrastEnabled = false
                                    thermalEnabled = false
                                    mriEnabled = false
                                    xrayEnabled = false
                                    animateEnabled = false
                                    cyberEnabled = false
                                }
                            )

                            EnhancerMode.Enhance -> onToggleMode(
                                on = { enhancerEnabled = true },
                                off = {
                                    monochromeEnabled = false
                                    drasticEnabled = false
                                    edgeContrastEnabled = false
                                    thermalEnabled = false
                                    mriEnabled = false
                                    xrayEnabled = false
                                    animateEnabled = false
                                    cyberEnabled = false
                                }
                            )

                            EnhancerMode.Monochrome -> onToggleMode(
                                on = { monochromeEnabled = true },
                                off = {
                                    enhancerEnabled = false
                                    drasticEnabled = false
                                    edgeContrastEnabled = false
                                    thermalEnabled = false
                                    mriEnabled = false
                                    xrayEnabled = false
                                    animateEnabled = false
                                    cyberEnabled = false
                                }
                            )

                            EnhancerMode.Edge -> onToggleMode(
                                on = { edgeContrastEnabled = true },
                                off = {
                                    monochromeEnabled = false
                                    enhancerEnabled = false
                                    drasticEnabled = false
                                    thermalEnabled = false
                                    mriEnabled = false
                                    xrayEnabled = false
                                    animateEnabled = false
                                    cyberEnabled = false
                                }
                            )

                            EnhancerMode.Animate -> onToggleMode(
                                on = { animateEnabled = true },
                                off = {
                                    monochromeEnabled = false
                                    enhancerEnabled = false
                                    drasticEnabled = false
                                    edgeContrastEnabled = false
                                    thermalEnabled = false
                                    mriEnabled = false
                                    xrayEnabled = false
                                    cyberEnabled = false
                                }
                            )

                            EnhancerMode.Cyber -> onToggleMode(
                                on = { cyberEnabled = true },
                                off = {
                                    monochromeEnabled = false
                                    enhancerEnabled = false
                                    drasticEnabled = false
                                    edgeContrastEnabled = false
                                    thermalEnabled = false
                                    mriEnabled = false
                                    xrayEnabled = false
                                    animateEnabled = false
                                }
                            )

                            EnhancerMode.Thermal -> onToggleMode(
                                on = { thermalEnabled = true },
                                off = {
                                    monochromeEnabled = false
                                    enhancerEnabled = false
                                    drasticEnabled = false
                                    edgeContrastEnabled = false
                                    mriEnabled = false
                                    xrayEnabled = false
                                    animateEnabled = false
                                    cyberEnabled = false
                                }
                            )

                            EnhancerMode.Mri -> onToggleMode(
                                on = { mriEnabled = true },
                                off = {
                                    monochromeEnabled = false
                                    enhancerEnabled = false
                                    drasticEnabled = false
                                    edgeContrastEnabled = false
                                    thermalEnabled = false
                                    xrayEnabled = false
                                    animateEnabled = false
                                    cyberEnabled = false
                                }
                            )

                            EnhancerMode.Xray -> onToggleMode(
                                on = { xrayEnabled = true },
                                off = {
                                    monochromeEnabled = false
                                    enhancerEnabled = false
                                    drasticEnabled = false
                                    edgeContrastEnabled = false
                                    thermalEnabled = false
                                    mriEnabled = false
                                    animateEnabled = false
                                    cyberEnabled = false
                                }
                            )
                        }
                    },
                    onCapture = {
                        val capture = imageCapture ?: return@EnhancerControls
                        val name = "CB_${
                            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        }"
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ColorPicker")
                        }
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(
                            ctx.contentResolver,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                        ).build()
                        capture.takePicture(
                            outputOptions,
                            cameraExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    ContextCompat.getMainExecutor(ctx).execute {
                                        snackbarService.showMessage(
                                            ctx.getString(R.string.photo_saved_to_album)
                                        )
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    ContextCompat.getMainExecutor(ctx).execute {
                                        snackbarService.showMessage(
                                            ctx.getString(R.string.photo_save_failed)
                                        )
                                    }
                                }
                            }
                        )
                    },
                    onExpandFilters = {
                        scope.launch {
                            sheetState.expand()
                        }
                    }
                )
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

private fun onToggleMode(on: () -> Unit, off: () -> Unit) {
    off()
    on()
}

@Composable
private fun EnhancerControls(
    modifier: Modifier,
    currentMode: EnhancerMode,
    onModeSelected: (EnhancerMode) -> Unit,
    onCapture: () -> Unit,
    onExpandFilters: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconButton(
            onClick = onCapture,
            modifier = Modifier
                .size(76.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = stringResource(R.string.capture_photo),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        FilledTonalButton(
            onClick = onExpandFilters,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowUp,
                contentDescription = null
            )
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.filters_pull_up))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EnhancerMode.values().forEach { mode ->
                val selected = mode == currentMode
                if (selected) {
                    Button(onClick = { onModeSelected(mode) }) {
                        Text(stringResource(mode.labelRes))
                    }
                } else {
                    FilledTonalButton(onClick = { onModeSelected(mode) }) {
                        Text(stringResource(mode.labelRes))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorBlindEnhancerSheet(
    supportsShader: Boolean,
    monochromeEnabled: Boolean,
    onToggleMonochrome: (Boolean) -> Unit,
    enhancerEnabled: Boolean,
    onToggleEnhancer: (Boolean) -> Unit,
    drasticEnabled: Boolean,
    onToggleDrastic: (Boolean) -> Unit,
    edgeContrastEnabled: Boolean,
    onToggleEdgeContrast: (Boolean) -> Unit,
    thermalEnabled: Boolean,
    onToggleThermal: (Boolean) -> Unit,
    mriEnabled: Boolean,
    onToggleMri: (Boolean) -> Unit,
    xrayEnabled: Boolean,
    onToggleXray: (Boolean) -> Unit,
    animateEnabled: Boolean,
    onToggleAnimate: (Boolean) -> Unit,
    cyberEnabled: Boolean,
    onToggleCyber: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
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
                text = stringResource(R.string.color_blind_enhancement),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = enhancerEnabled && supportsShader,
                onCheckedChange = { onToggleEnhancer(it) },
                enabled = supportsShader
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.monochrome_mode),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = monochromeEnabled && supportsShader,
                onCheckedChange = { onToggleMonochrome(it) },
                enabled = supportsShader
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.edge_contrast_mode),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = edgeContrastEnabled && supportsShader,
                onCheckedChange = { onToggleEdgeContrast(it) },
                enabled = supportsShader
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.drastic_mode),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = drasticEnabled && supportsShader,
                onCheckedChange = { onToggleDrastic(it) },
                enabled = supportsShader
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.animate_mode),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = animateEnabled && supportsShader,
                onCheckedChange = { onToggleAnimate(it) },
                enabled = supportsShader
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.cyber_mode),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = cyberEnabled && supportsShader,
                onCheckedChange = { onToggleCyber(it) },
                enabled = supportsShader
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.thermal_mode),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = thermalEnabled && supportsShader,
                onCheckedChange = { onToggleThermal(it) },
                enabled = supportsShader
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.mri_mode),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = mriEnabled && supportsShader,
                onCheckedChange = { onToggleMri(it) },
                enabled = supportsShader
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.xray_mode),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = xrayEnabled && supportsShader,
                onCheckedChange = { onToggleXray(it) },
                enabled = supportsShader
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
    onImageCaptureReady: (ImageCapture) -> Unit,
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

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        onImageCaptureReady(capture)

        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                capture
            )
            onCameraReady(camera)
        } catch (_: Exception) {
        }
    }, ContextCompat.getMainExecutor(context))
}
