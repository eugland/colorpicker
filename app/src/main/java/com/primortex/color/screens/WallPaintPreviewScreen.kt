package com.primortex.color.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.primortex.color.R
import com.primortex.color.service.hexToArgb
import com.primortex.color.ui.components.ScreenScaffold

private val suggestedWallColors = listOf(
    "#F97316",
    "#0EA5E9",
    "#10B981",
    "#A855F7",
    "#F59E0B"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WallPaintPreviewScreen(
    innerPadding: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            hasCameraPerm = it
        }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    var maskCenter by remember { mutableStateOf<Offset?>(null) }

    androidx.compose.runtime.LaunchedEffect(hasCameraPerm) {
        if (!hasCameraPerm) {
            permLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            cameraProvider = null
        }
    }

    var hexInput by remember { mutableStateOf(suggestedWallColors.first()) }
    val cleanHex = remember(hexInput) { hexInput.trim().removePrefix("#") }
    val isValid = remember(cleanHex) { cleanHex.matches(Regex("^[0-9A-Fa-f]{6}$")) }
    val previewArgb = remember(isValid, cleanHex) {
        if (isValid) {
            hexToArgb("#$cleanHex")
        } else {
            hexToArgb(suggestedWallColors.first())
        }
    }
    val previewColor = remember(previewArgb) { Color(previewArgb) }

    ScreenScaffold(
        titleRes = R.string.wall_paint_preview_title,
        innerPadding = innerPadding,
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                stringResource(R.string.wall_paint_preview_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.wall_paint_camera_preview_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        stringResource(R.string.wall_paint_camera_preview_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .pointerInput(hasCameraPerm) {
                                if (!hasCameraPerm) return@pointerInput
                                detectTapGestures { position ->
                                    maskCenter = position
                                }
                            }
                            .onSizeChanged { previewSize = it }
                    ) {
                        if (hasCameraPerm) {
                            AndroidView(
                                modifier = Modifier.matchParentSize(),
                                factory = { previewView },
                                update = {}
                            )
                            androidx.compose.runtime.LaunchedEffect(hasCameraPerm) {
                                bindWallPaintCamera(
                                    context = context,
                                    lifecycleOwner = lifecycleOwner,
                                    previewView = previewView,
                                    onCameraProviderReady = { cameraProvider = it }
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier.matchParentSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.camera_permission_required),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Canvas(modifier = Modifier.matchParentSize()) {
                            val center = maskCenter
                                ?: Offset(
                                    previewSize.width / 2f,
                                    previewSize.height / 2f
                                )
                            drawCircle(
                                color = previewColor.copy(alpha = 0.45f),
                                radius = size.minDimension * 0.35f,
                                center = center
                            )
                        }
                    }
                }
            }

            WallPreviewCard(selectedColor = previewColor)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.wall_paint_steps_title),
                    style = MaterialTheme.typography.titleMedium
                )
                WallPreviewStep(
                    icon = Icons.Outlined.PhotoCamera,
                    text = stringResource(R.string.wall_paint_step_camera)
                )
                WallPreviewStep(
                    icon = Icons.Outlined.TouchApp,
                    text = stringResource(R.string.wall_paint_step_mask)
                )
                WallPreviewStep(
                    icon = Icons.Outlined.ColorLens,
                    text = stringResource(R.string.wall_paint_step_color_pick)
                )
                WallPreviewStep(
                    icon = Icons.Outlined.Visibility,
                    text = stringResource(R.string.wall_paint_step_result)
                )
            }

            Divider()

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.wall_paint_pick_color_title),
                    style = MaterialTheme.typography.titleMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestedWallColors.forEach { colorHex ->
                        AssistChip(
                            onClick = { hexInput = colorHex },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(hexToArgb(colorHex)))
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outline,
                                                RoundedCornerShape(4.dp)
                                            )
                                    )
                                    Text(colorHex, fontFamily = FontFamily.Monospace)
                                }
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { hexInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.wall_paint_hex_label)) },
                    placeholder = { Text("#F97316") },
                    isError = !isValid,
                    supportingText = {
                        if (!isValid) {
                            Text(stringResource(R.string.wall_paint_hex_error))
                        } else {
                            Text(stringResource(R.string.wall_paint_hex_helper))
                        }
                    },
                    singleLine = true
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Outlined.FormatPaint,
                            contentDescription = null
                        )
                        Text(
                            stringResource(R.string.wall_paint_note_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Text(
                        stringResource(R.string.wall_paint_note_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WallPreviewCard(selectedColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.wall_paint_preview_title),
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                WallPreviewTile(
                    label = stringResource(R.string.wall_paint_preview_before),
                    baseColor = Color(0xFFE7DCCF),
                    modifier = Modifier.weight(1f)
                )
                WallPreviewTile(
                    label = stringResource(R.string.wall_paint_preview_after),
                    baseColor = selectedColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WallPreviewTile(
    label: String,
    baseColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(MaterialTheme.shapes.large)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            baseColor.copy(alpha = 0.95f),
                            baseColor.copy(alpha = 0.75f),
                            baseColor.copy(alpha = 1f)
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.05f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.12f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.35f))
            )
        }
    }
}

@Composable
private fun WallPreviewStep(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = null)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun bindWallPaintCamera(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    onCameraProviderReady: (ProcessCameraProvider) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        onCameraProviderReady(cameraProvider)

        val preview = Preview.Builder()
            .build()
            .also { it.surfaceProvider = previewView.surfaceProvider }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview
            )
        } catch (_: Exception) {
        }
    }, ContextCompat.getMainExecutor(context))
}
