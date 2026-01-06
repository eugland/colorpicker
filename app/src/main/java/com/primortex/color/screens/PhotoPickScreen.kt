// com/primortex/color/screens/PhotoPickScreen.kt
package com.primortex.color.screens

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.primortex.color.app.PickedColor
import com.primortex.color.service.ColorServices
import com.primortex.color.service.PaletteService
import com.primortex.color.service.RecentPicksService
import com.primortex.color.ui.components.ActiveColorSheet
import com.primortex.color.ui.components.ColorDetailsBottomSheet
import com.primortex.color.ui.components.PaletteBar
import com.primortex.color.ui.util.argbToHex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPickScreen(
    photoUri: String,
    onBack: () -> Unit,
    onOpenPalette: (String, Boolean) -> Unit,
) {
    val ctx = LocalContext.current
    val colorNameService = remember(ctx) {
        ColorServices.ensure(ctx)
        ColorServices.colorNames
    }
    val uiScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var pickedArgb by remember { mutableIntStateOf(0xFF7B8266.toInt()) }
    val pickedColor by remember {
        derivedStateOf {
            PickedColor(argb = pickedArgb, name = colorNameService.localNameFromArgb(pickedArgb))
        }
    }
    argbToHex(pickedArgb)
    var containerWpx by remember { mutableFloatStateOf(0f) }
    var containerHpx by remember { mutableFloatStateOf(0f) }
    val palette = remember { mutableStateListOf<PickedColor>() }
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    val recents by RecentPicksService.history.collectAsState()
    var detailPick by remember { mutableStateOf<PickedColor?>(null) }
    var frozen by remember { mutableStateOf(false) }
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(ctx)
            .data(photoUri)
            .allowHardware(false)
            .build()
    )
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    fun showSnack(msg: String) {
        uiScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 168.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            ActiveColorSheet(
                picked = pickedColor,
                recents = recents,
                onTapPick = { pick ->
                    detailPick = pick
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back")
                    }
                    Text(
                        "Photo picking",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { frozen = !frozen }) {
                        Icon(
                            imageVector = if (frozen) Icons.Outlined.PlayCircle else Icons.Outlined.PauseCircle,
                            contentDescription = "Freeze"
                        )
                    }

                    IconButton(onClick = { detailPick = pickedColor }) {
                        Icon(
                            imageVector = Icons.Outlined.Colorize,
                            contentDescription = "More info"
                        )
                    }
                }
            }
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        containerWpx = it.width.toFloat()
                        containerHpx = it.height.toFloat()
                    }
                    .pointerInput(bitmap, containerWpx, containerHpx) {
                        detectTapGestures { tap ->
                            if (frozen) return@detectTapGestures
                            Log.d(
                                "PhotoPickScreen",
                                "tap=$tap, bitmap=${painter.state}"
                            )
                            val bmp = bitmap ?: return@detectTapGestures
                            if (containerWpx <= 1f || containerHpx <= 1f) return@detectTapGestures

                            val sampled = sampleBitmapAtTapFit(
                                tap = tap,
                                containerW = containerWpx,
                                containerH = containerHpx,
                                bmp = bmp
                            )
                            sampled?.let { pickedArgb = it }
                            RecentPicksService.addPick(pickedColor)
                        }
                    }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx)
                        .data(photoUri)
                        .allowHardware(false)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    onSuccess = { success ->
                        bitmap = (success.result.drawable as? BitmapDrawable)?.bitmap
                    }
                )
            }

            PaletteBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                palette = palette,
                onAddColor = {
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
                        tags = listOf("photo", "pick"),
                        colors = palette
                    )
                    palette.clear()
                    showSnack("Palette saved ✅")
                    onOpenPalette(saved.id, true)
                }
            )
        }
    }

    detailPick?.let { picked ->
        ColorDetailsBottomSheet(
            picked = picked,
            onDismiss = { detailPick = null },
            onOpenColorDetail = { p -> detailPick = p } // tap similar colors -> jump
        )
    }
}

private fun sampleBitmapAtTapFit(
    tap: Offset,
    containerW: Float,
    containerH: Float,
    bmp: Bitmap
): Int? {
    val bmpW = bmp.width.toFloat()
    val bmpH = bmp.height.toFloat()
    if (bmpW <= 0f || bmpH <= 0f) return null

    val fitScale = minOf(containerW / bmpW, containerH / bmpH)
    val drawW = bmpW * fitScale
    val drawH = bmpH * fitScale

    val offsetX = (containerW - drawW) / 2f
    val offsetY = (containerH - drawH) / 2f

    val xIn = tap.x - offsetX
    val yIn = tap.y - offsetY

    // tap outside the drawn image area
    if (xIn < 0f || yIn < 0f || xIn > drawW || yIn > drawH) return null

    val bx = (xIn / fitScale).roundToInt().coerceIn(0, bmp.width - 1)
    val by = (yIn / fitScale).roundToInt().coerceIn(0, bmp.height - 1)

    return bmp.getPixel(bx, by)
}
