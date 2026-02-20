package com.primortex.color.features.colordetails

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.primortex.color.R
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.data.enums.PaletteSchemeType
import com.primortex.color.i18n.stringResource
import com.primortex.color.service.argbToHex

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorDetailsContent(uiState: ColorDetailsUiState, onAction: (ColorDetailsUiAction) -> Unit) {
    ColorHeroCard(argb = uiState.details.argb)
    Spacer(Modifier.height(14.dp))
    ColorHeaderRow(displayName = uiState.displayName, hex = uiState.details.hex, argb = uiState.details.argb, onAction = onAction)
    Spacer(Modifier.height(12.dp))
    ColorActionsRow(isSaved = uiState.isSaved, onAction = onAction)
    Spacer(Modifier.height(14.dp))
    ColorInfoChips(uiState = uiState)
    Spacer(Modifier.height(12.dp))
    KeyValueGrid(items = metricsFor(uiState))
    Spacer(Modifier.height(14.dp))
    Text(stringResource(R.string.harmonies), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    HarmonyRow(label = stringResource(R.string.harmony_complement), argbs = uiState.details.complements)
    Spacer(Modifier.height(8.dp))
    HarmonyRow(label = stringResource(R.string.harmony_triad), argbs = uiState.details.triads)
    Spacer(Modifier.height(8.dp))
    HarmonyRow(label = stringResource(R.string.harmony_analogous), argbs = uiState.details.analogous)
    Spacer(Modifier.height(14.dp))
    Text(stringResource(R.string.color_shades_tints_tones_title, uiState.displayName), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))
    ShadeToneRow(label = stringResource(R.string.tints_label), argbs = uiState.details.tints) { onAction(ColorDetailsUiAction.OpenTintsPalette) }
    Spacer(Modifier.height(8.dp))
    ShadeToneRow(label = stringResource(R.string.shades_label), argbs = uiState.details.shades) { onAction(ColorDetailsUiAction.OpenShadesPalette) }
    Spacer(Modifier.height(8.dp))
    ShadeToneRow(label = stringResource(R.string.tones_label), argbs = uiState.details.tones) { onAction(ColorDetailsUiAction.OpenTonesPalette) }
    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.color_palettes_title, uiState.displayName), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))
    uiState.paletteSchemes.forEach { scheme ->
        PaletteSchemeCard(scheme = scheme) { onAction(ColorDetailsUiAction.OpenPaletteScheme(scheme.type)) }
        Spacer(Modifier.height(10.dp))
    }
    Text(stringResource(R.string.similar_colors), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    SimilarColorsSection(similarColors = uiState.details.similarColors)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun metricsFor(uiState: ColorDetailsUiState): List<Pair<String, String>> {
    val d = uiState.details
    return listOf(
        stringResource(R.string.rgb_label) to "${d.rgb.r}, ${d.rgb.g}, ${d.rgb.b}",
        stringResource(R.string.hsv_label) to "${d.hsv.h.toInt()}\u00B0, ${(d.hsv.s * 100).toInt()}%, ${(d.hsv.v * 100).toInt()}%",
        stringResource(R.string.hsl_label) to "${d.hsl.h.toInt()}\u00B0, ${(d.hsl.s * 100).toInt()}%, ${(d.hsl.l * 100).toInt()}%"
    )
}

@Composable
private fun ColorHeroCard(argb: Int) {
    Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        val baseColor = Color(argb)
        val baseHsl = remember(argb) { FloatArray(3).apply { ColorUtils.colorToHSL(argb, this) } }
        val transition = rememberInfiniteTransition(label = "breathingTint")
        val shift by transition.animateFloat(
            initialValue = 0.06f,
            targetValue = 0.1f,
            animationSpec = infiniteRepeatable(animation = tween(durationMillis = 8000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
            label = "lightnessShift"
        )
        val tint = remember(baseHsl, shift) { Color(ColorUtils.HSLToColor(floatArrayOf(baseHsl[0], baseHsl[1], (baseHsl[2] + shift).coerceIn(0f, 1f)))) }
        val shade = remember(baseHsl, shift) { Color(ColorUtils.HSLToColor(floatArrayOf(baseHsl[0], baseHsl[1], (baseHsl[2] - shift).coerceIn(0f, 1f)))) }
        Box(Modifier.fillMaxWidth().height(160.dp).background(Brush.linearGradient(colors = listOf(tint, baseColor, shade))))
    }
}

@Composable
private fun ColorHeaderRow(displayName: String, hex: String, argb: Int, onAction: (ColorDetailsUiAction) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(64.dp).background(Color(argb), CircleShape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(displayName, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(hex, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
        }
        IconButton(onClick = { onAction(ColorDetailsUiAction.CopyHexClicked) }) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.copy_hex))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorActionsRow(isSaved: Boolean, onAction: (ColorDetailsUiAction) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isSaved) {
            Button(onClick = { onAction(ColorDetailsUiAction.ToggleSavedClicked) }) {
                Icon(Icons.Filled.Favorite, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.saved))
            }
        } else {
            OutlinedButton(onClick = { onAction(ColorDetailsUiAction.ToggleSavedClicked) }) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.save))
            }
        }
        OutlinedButton(onClick = { onAction(ColorDetailsUiAction.ShowPalettePicker) }) { Text(stringResource(R.string.add_to_palette)) }
    }
}

@Composable
private fun ColorInfoChips(uiState: ColorDetailsUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        AssistChip(onClick = {}, label = { Text(stringResource(R.string.luma_label, (uiState.details.luminance * 100).toInt())) })
        AssistChip(onClick = {}, label = { Text(if (uiState.details.isDark) stringResource(R.string.dark_label) else stringResource(R.string.light_label)) })
        AssistChip(
            onClick = {},
            label = {
                val textColor = if (uiState.details.recommendedOnColor == 0xFFFFFFFF.toInt()) stringResource(R.string.white_label) else stringResource(R.string.black_label)
                Text(stringResource(R.string.text_recommendation, textColor))
            }
        )
    }
}

@Composable
private fun SimilarColorsSection(similarColors: List<PickedColor>) {
    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        similarColors.forEach { s ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(84.dp)) {
                Box(Modifier.size(44.dp).background(Color(s.argb), CircleShape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
                Spacer(Modifier.height(6.dp))
                Text(text = s.name, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(text = argbToHex(s.argb), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun HarmonyRow(label: String, argbs: List<Int>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(90.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(argbs) { a ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).background(Color(a), CircleShape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(argbToHex(a), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun ShadeToneRow(label: String, argbs: List<Int>, onOpenPalette: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth().clickable { onOpenPalette() }) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(90.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(argbs) { a ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(28.dp).background(Color(a), CircleShape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
                        Spacer(Modifier.height(4.dp))
                        Text(argbToHex(a), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteSchemeCard(scheme: PaletteSchemeUiModel, onOpenPalette: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth().clickable { onOpenPalette() }) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(schemeTitle(scheme.type), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(scheme.colors) { a ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(34.dp).background(Color(a), CircleShape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
                        Spacer(Modifier.height(4.dp))
                        Text(argbToHex(a), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun schemeTitle(type: PaletteSchemeType): String {
    return when (type) {
        PaletteSchemeType.Complementary -> stringResource(R.string.palette_scheme_complementary)
        PaletteSchemeType.Analogous -> stringResource(R.string.palette_scheme_analogous)
        PaletteSchemeType.SplitComplementary -> stringResource(R.string.palette_scheme_split_complementary)
        PaletteSchemeType.Triadic -> stringResource(R.string.palette_scheme_triadic)
        PaletteSchemeType.Tetradic -> stringResource(R.string.palette_scheme_tetradic)
        PaletteSchemeType.Square -> stringResource(R.string.palette_scheme_square)
    }
}

@Composable
private fun KeyValueGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (k, v) ->
            Row(
                Modifier.fillMaxWidth().background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    RoundedCornerShape(12.dp)
                ).padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(k, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(52.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(v, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
internal fun PalettePickerDialog(
    palettes: List<Palette>,
    onDismiss: () -> Unit,
    onSelectPalette: (String) -> Unit,
    onCreatePalette: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_palette)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (palettes.isEmpty()) {
                    Text(stringResource(R.string.no_palettes_available))
                } else {
                    palettes.forEach { palette ->
                        TextButton(onClick = { onSelectPalette(palette.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(text = palette.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                OutlinedButton(onClick = onCreatePalette, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.create_new_palette))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

