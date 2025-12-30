package com.primortex.color.screens

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.primortex.color.app.PickedColor
import com.primortex.color.service.RecentPicksService
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPickScreen(
    photoUri: String,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current

    var pickedArgb by remember { mutableIntStateOf(0xFF7B8266.toInt()) }


    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(ctx)
            .data(photoUri)
            .allowHardware(false) // IMPORTANT: must be false to access Bitmap pixels
            .build()
    )

    val bitmap: Bitmap? = (painter.state as? coil.compose.AsyncImagePainter.State.Success)
        ?.result
        ?.drawable
        ?.let { it as? BitmapDrawable }
        ?.bitmap

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pick color") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(Color.Black)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val maxW = constraints.maxWidth
                val maxH = constraints.maxHeight

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(bitmap, maxW, maxH) {
                            detectTapGestures { tap ->
                                val bmp = bitmap ?: return@detectTapGestures
                                val sampled = sampleBitmapAtTapFit(
                                    tap = tap,
                                    containerW = maxW.toFloat(),
                                    containerH = maxH.toFloat(),
                                    bmp = bmp
                                )
                                if (sampled != null) pickedArgb = sampled
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
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Bottom info card
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(pickedArgb), shape = MaterialTheme.shapes.large)
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Picked", style = MaterialTheme.typography.titleMedium)
                        Text(
                            pickedArgb.toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(onClick = { RecentPicksService.addPick(PickedColor(pickedArgb,"photo")) }) {
                        Text("Add")
                    }
                }
            }
        }
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

    val scale = minOf(containerW / bmpW, containerH / bmpH)
    val drawW = bmpW * scale
    val drawH = bmpH * scale

    val offsetX = (containerW - drawW) / 2f
    val offsetY = (containerH - drawH) / 2f

    val xIn = tap.x - offsetX
    val yIn = tap.y - offsetY

    // Tap outside the drawn image area
    if (xIn < 0f || yIn < 0f || xIn > drawW || yIn > drawH) return null

    val bx = (xIn / scale).roundToInt().coerceIn(0, bmp.width - 1)
    val by = (yIn / scale).roundToInt().coerceIn(0, bmp.height - 1)

    return bmp.getPixel(bx, by)
}