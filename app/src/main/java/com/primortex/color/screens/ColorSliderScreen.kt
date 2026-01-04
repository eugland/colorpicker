package com.primortex.color.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.service.ColorNameLookup
import com.primortex.color.service.PaletteService
import com.primortex.color.service.RecentPicksService
import com.primortex.color.ui.components.ColorDetailsBottomSheet
import com.primortex.color.ui.components.ScreenScaffold
import com.primortex.color.ui.util.argbToHex
import kotlinx.coroutines.launch
import android.graphics.Color as AndroidColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSliderScreen(
    innerPadding: PaddingValues,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var argb by remember { mutableIntStateOf(0xFF7C3AED.toInt()) }
    val savedColors by RecentPicksService.saved.collectAsState()
    val palettes by PaletteService.palettes.collectAsState()
    var showPalettePicker by remember { mutableStateOf(false) }
    var showColorDetails by remember { mutableStateOf(false) }

    val nearestName = remember(argb) { ColorNameLookup.nearestName(argb).name }
    val picked = remember(argb, nearestName) { PickedColor(argb = argb, name = nearestName) }
    val hex = remember(argb) { argbToHex(argb) }
    val rgb = remember(argb) {
        Triple(AndroidColor.red(argb), AndroidColor.green(argb), AndroidColor.blue(argb))
    }
    val hsl = remember(argb) {
        FloatArray(3).also { ColorUtils.colorToHSL(argb, it) }
    }
    val isSaved = savedColors.any { it.argb == argb }

    ScreenScaffold(
        title = "Color slider",
        innerPadding = innerPadding,
        onBack = onBack,

        snackbarHostState = snackbarHostState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ColorPreviewCard(
                name = nearestName,
                hex = hex,
                argb = argb,
                onClick = { showColorDetails = true }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = {
                        RecentPicksService.addPick(picked)
                        RecentPicksService.toggleSaved(picked)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (isSaved) "Removed from My colors" else "Saved to My colors"
                            )
                        }
                    }
                ) { 
                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isSaved) "Saved" else "Save to My colors")
                }

                Button(
                    onClick = {
                        if (palettes.isEmpty()) {
                            scope.launch { snackbarHostState.showSnackbar("No palettes available. Create one first.") }
                        } else {
                            showPalettePicker = true
                        }
                    }
                ) {
                    Icon(Icons.Outlined.Palette, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add to palette")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("RGB", style = MaterialTheme.typography.titleMedium)
                LabeledSlider(
                    label = "Red",
                    value = rgb.first.toFloat(),
                    valueRange = 0f..255f,
                    onValueChange = { value ->
                        argb = toArgb(value.toInt(), rgb.second, rgb.third)
                    }
                )
                LabeledSlider(
                    label = "Green",
                    value = rgb.second.toFloat(),
                    valueRange = 0f..255f,
                    onValueChange = { value ->
                        argb = toArgb(rgb.first, value.toInt(), rgb.third)
                    }
                )
                LabeledSlider(
                    label = "Blue",
                    value = rgb.third.toFloat(),
                    valueRange = 0f..255f,
                    onValueChange = { value ->
                        argb = toArgb(rgb.first, rgb.second, value.toInt())
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("HSL", style = MaterialTheme.typography.titleMedium)
                LabeledSlider(
                    label = "Hue",
                    value = hsl[0],
                    valueRange = 0f..360f,
                    steps = 0,
                    onValueChange = { value ->
                        argb = ColorUtils.HSLToColor(floatArrayOf(value, hsl[1], hsl[2]))
                    }
                )
                LabeledSlider(
                    label = "Saturation",
                    value = hsl[1] * 100f,
                    valueRange = 0f..100f,
                    onValueChange = { value ->
                        argb = ColorUtils.HSLToColor(floatArrayOf(hsl[0], value / 100f, hsl[2]))
                    },
                    valueFormatter = { v -> "${v.toInt()}%" }
                )
                LabeledSlider(
                    label = "Lightness",
                    value = hsl[2] * 100f,
                    valueRange = 0f..100f,
                    onValueChange = { value ->
                        argb = ColorUtils.HSLToColor(floatArrayOf(hsl[0], hsl[1], value / 100f))
                    },
                    valueFormatter = { v -> "${v.toInt()}%" }
                )
            }
        }
    }

    if (showPalettePicker) {
        val paletteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showPalettePicker = false },
            sheetState = paletteSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Select palette", style = MaterialTheme.typography.titleLarge)

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(palettes) { palette ->
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                addToPalette(
                                    palette = palette,
                                    color = picked,
                                    snackbarHostState = snackbarHostState,
                                    scope = scope
                                )
                                showPalettePicker = false
                            }
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(palette.name)
                                Text(
                                    "${palette.colors.size} colors",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                TextButton(onClick = { showPalettePicker = false }) { Text("Close") }
            }
        }
    }

    if (showColorDetails) {
        ColorDetailsBottomSheet(
            picked = picked,
            onDismiss = { showColorDetails = false }
        )
    }
}

@Preview
@Composable
private fun previewPreviewCard() {
    ColorPreviewCard("Blue-violet", "#883AED", 0xFF7C3AED.toInt()) {}
}

@Composable
private fun ColorPreviewCard(
    name: String,
    hex: String,
    argb: Int,
    onClick: () -> Unit,
) {
    Card(shape = MaterialTheme.shapes.extraLarge) {
        val rgb = remember(argb) {
            Triple(AndroidColor.red(argb), AndroidColor.green(argb), AndroidColor.blue(argb))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(argb)),
                    contentAlignment = Alignment.Center
                ) {}
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name, style = MaterialTheme.typography.titleLarge)
                    Text(hex, fontFamily = FontFamily.Monospace)
                    Text(
                        "RGB ${rgb.first}, ${rgb.second}, ${rgb.third}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Tap to view details",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueFormatter: (Float) -> String = { v -> v.toInt().toString() },
    steps: Int = 0,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f))
            Text(valueFormatter(value), fontFamily = FontFamily.Monospace)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

private fun toArgb(r: Int, g: Int, b: Int): Int {
    val rr = r.coerceIn(0, 255)
    val gg = g.coerceIn(0, 255)
    val bb = b.coerceIn(0, 255)
    return (0xFF shl 24) or (rr shl 16) or (gg shl 8) or bb
}

private fun addToPalette(
    palette: Palette,
    color: PickedColor,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    when {
        palette.colors.any { it.argb == color.argb } ->
            scope.launch { snackbarHostState.showSnackbar("Already in palette") }
        palette.colors.size >= 10 ->
            scope.launch { snackbarHostState.showSnackbar("Palette full (10 colors)") }
        else -> {
            PaletteService.update(
                id = palette.id,
                colors = palette.colors + color
            )
            RecentPicksService.addPick(color)
            scope.launch { snackbarHostState.showSnackbar("Added to palette") }
        }
    }
}
