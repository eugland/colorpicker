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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
    boosted = clamp(boosted * 1.25 + vec3(0.05), 0.0, 1.0);
    return half4(boosted, c.a);
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

    var monochromeEnabled by remember { mutableStateOf(true) }
    var enhancerEnabled by remember { mutableStateOf(false) }
    var drasticEnabled by remember { mutableStateOf(false) }

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
                supportsShader = supportsShader,
                monochromeEnabled = monochromeEnabled,
                onToggleMonochrome = { monochromeEnabled = it },
                enhancerEnabled = enhancerEnabled,
                onToggleEnhancer = { enhancerEnabled = it },
                drasticEnabled = drasticEnabled,
                onToggleDrastic = { drasticEnabled = it }
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
                                enhancerShader.setFloatUniform("intensity", 0.7f)
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
    supportsShader: Boolean,
    monochromeEnabled: Boolean,
    onToggleMonochrome: (Boolean) -> Unit,
    enhancerEnabled: Boolean,
    onToggleEnhancer: (Boolean) -> Unit,
    drasticEnabled: Boolean,
    onToggleDrastic: (Boolean) -> Unit
) {
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
