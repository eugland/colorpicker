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
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Favorite
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import android.content.Context
import androidx.core.graphics.ColorUtils
import com.primortex.color.R
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.service.ColorServices
import com.primortex.color.service.PaletteService
import com.primortex.color.service.RecentPicksService
import com.primortex.color.ui.components.ColorDetailsBottomSheet
import com.primortex.color.ui.components.ScreenScaffold
import com.primortex.color.ui.util.argbToHex
import kotlinx.coroutines.CoroutineScope
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
    val context = LocalContext.current

    var argb by remember { mutableIntStateOf(0xFF7C3AED.toInt()) }
    val savedColors by RecentPicksService.saved.collectAsState()
    val palettes by PaletteService.palettes.collectAsState()
    var showPalettePicker by remember { mutableStateOf(false) }
    var showColorDetails by remember { mutableStateOf(false) }
    val colorNameService = remember(context) {
        ColorServices.ensure(context)
        ColorServices.colorNames
    }

    val nearestName = remember(argb) { colorNameService.localNameFromArgb(argb) }
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
        titleRes = R.string.color_slider,
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
                        val message = if (isSaved) {
                            context.getString(R.string.removed_from_my_colors)
                        } else {
                            context.getString(R.string.saved_to_my_colors)
                        }
                        showSnackbar(snackbarHostState, scope, message)
                    }
                ) {
                    Icon(
                        if (isSaved) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isSaved) stringResource(R.string.saved) else stringResource(R.string.save))
                }

                Button(
                    onClick = {
                        if (palettes.isEmpty()) {
                            showSnackbar(snackbarHostState, scope, context, R.string.no_palettes_available)
                        } else {
                            showPalettePicker = true
                        }
                    }
                ) {
                    Icon(
                        Icons.Outlined.Palette,
                        contentDescription = stringResource(R.string.add_to_palette)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_to_palette))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.rgb_label), style = MaterialTheme.typography.titleMedium)
                LabeledSlider(
                    label = stringResource(R.string.red_label),
                    value = rgb.first.toFloat(),
                    valueRange = 0f..255f,
                    onValueChange = { value ->
                        argb = toArgb(value.toInt(), rgb.second, rgb.third)
                    }
                )
                LabeledSlider(
                    label = stringResource(R.string.green_label),
                    value = rgb.second.toFloat(),
                    valueRange = 0f..255f,
                    onValueChange = { value ->
                        argb = toArgb(rgb.first, value.toInt(), rgb.third)
                    }
                )
                LabeledSlider(
                    label = stringResource(R.string.blue_label),
                    value = rgb.third.toFloat(),
                    valueRange = 0f..255f,
                    onValueChange = { value ->
                        argb = toArgb(rgb.first, rgb.second, value.toInt())
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.hsl_label), style = MaterialTheme.typography.titleMedium)
                LabeledSlider(
                    label = stringResource(R.string.hue_label),
                    value = hsl[0],
                    valueRange = 0f..360f,
                    steps = 0,
                    onValueChange = { value ->
                        argb = ColorUtils.HSLToColor(floatArrayOf(value, hsl[1], hsl[2]))
                    }
                )
                LabeledSlider(
                    label = stringResource(R.string.saturation_label),
                    value = hsl[1] * 100f,
                    valueRange = 0f..100f,
                    onValueChange = { value ->
                        argb = ColorUtils.HSLToColor(floatArrayOf(hsl[0], value / 100f, hsl[2]))
                    },
                    valueFormatter = { v -> "${v.toInt()}%" }
                )
                LabeledSlider(
                    label = stringResource(R.string.lightness_label),
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
        val alreadyInPaletteMessage = stringResource(R.string.already_in_palette)
        val paletteFullMessage = stringResource(R.string.palette_full)
        val addedToPaletteMessage = stringResource(R.string.added_to_palette)

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
                Text(stringResource(R.string.select_palette), style = MaterialTheme.typography.titleLarge)

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
                                    scope = scope,
                                    alreadyInPaletteMessage = alreadyInPaletteMessage,
                                    paletteFullMessage = paletteFullMessage,
                                    addedToPaletteMessage = addedToPaletteMessage
                                )
                                showPalettePicker = false
                            }
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(palette.name)
                                Text(
                                    stringResource(R.string.palette_color_count, palette.colors.size),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }

                TextButton(onClick = { showPalettePicker = false }) { Text(stringResource(R.string.close)) }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT: color + texts
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(argb))
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(name, style = MaterialTheme.typography.titleLarge)
                        Text(hex, fontFamily = FontFamily.Monospace)
                    }
                }

                // RIGHT: "more info" hint
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = stringResource(R.string.more_details),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
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
    scope: CoroutineScope,
    alreadyInPaletteMessage: String,
    paletteFullMessage: String,
    addedToPaletteMessage: String,
) {
    when {
        palette.colors.any { it.argb == color.argb } ->
            scope.launch { snackbarHostState.showSnackbar(alreadyInPaletteMessage) }

        palette.colors.size >= 10 ->
            scope.launch { snackbarHostState.showSnackbar(paletteFullMessage) }

        else -> {
            PaletteService.update(
                id = palette.id,
                colors = palette.colors + color
            )
            RecentPicksService.addPick(color)
            scope.launch { snackbarHostState.showSnackbar(addedToPaletteMessage) }
        }
    }
}

private fun showSnackbar(
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    message: String
) {
    scope.launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(message)
    }
}

private fun showSnackbar(
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    context: Context,
    @StringRes messageResId: Int,
    vararg formatArgs: Any
) {
    showSnackbar(
        snackbarHostState = snackbarHostState,
        scope = scope,
        message = context.getString(messageResId, *formatArgs)
    )
}
