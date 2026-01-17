// com/primortex/color/screens/ColorDetailsScreen.kt
package com.primortex.color.screens

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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.primortex.color.R
import com.primortex.color.app.PickedColor
import com.primortex.color.service.ColorDetails
import com.primortex.color.service.ColorDetailsService
import com.primortex.color.service.ColorServices
import com.primortex.color.service.PaletteService
import com.primortex.color.service.argbToHex
import com.primortex.color.ui.LocalSnackbarService
import androidx.core.graphics.ColorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorDetailsScreen(
    argb: Int,
    nameHint: String? = null,
    onBack: () -> Unit,
    onOpenColorDetail: (PickedColor) -> Unit = {}, // open similar colors
    onOpenPalette: (String, Boolean) -> Unit = { _, _ -> }
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val snackbarService = LocalSnackbarService.current
    val palettes by PaletteService.palettes.collectAsState()
    var showPalettePicker by remember { mutableStateOf(false) }

    val details: ColorDetails = remember(argb) {
        ColorDetailsService.details(argb, similarLimit = 10)
    }
    val displayName = details.name.ifBlank { nameHint ?: stringResource(R.string.color_label) }
    val pickedColor = remember(details.argb, displayName) {
        PickedColor(details.argb, displayName)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // Big swatch card
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                val baseColor = Color(details.argb)
                val baseHsl = remember(details.argb) {
                    FloatArray(3).apply { ColorUtils.colorToHSL(details.argb, this) }
                }
                val transition = rememberInfiniteTransition(label = "breathingTint")
                val lightnessShift by transition.animateFloat(
                    initialValue = 0.06f,
                    targetValue = 0.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 8000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "lightnessShift"
                )
                val tintColor = remember(baseHsl, lightnessShift) {
                    Color(
                        ColorUtils.HSLToColor(
                            floatArrayOf(
                                baseHsl[0],
                                baseHsl[1],
                                (baseHsl[2] + lightnessShift).coerceIn(0f, 1f)
                            )
                        )
                    )
                }
                val shadeColor = remember(baseHsl, lightnessShift) {
                    Color(
                        ColorUtils.HSLToColor(
                            floatArrayOf(
                                baseHsl[0],
                                baseHsl[1],
                                (baseHsl[2] - lightnessShift).coerceIn(0f, 1f)
                            )
                        )
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    tintColor,
                                    baseColor,
                                    shadeColor
                                )
                            )
                        )
                )
            }

            Spacer(Modifier.height(14.dp))

            // Header: swatch + name + hex
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(details.argb))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        displayName,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        details.hex,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(details.hex))
                    snackbarService.showMessage(context.getString(R.string.hex_copied))
                }) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(R.string.copy_hex)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Actions
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {
                    clipboard.setText(AnnotatedString(details.hex))
                    snackbarService.showMessage(context.getString(R.string.hex_copied))
                }) { Text(stringResource(R.string.copy_hex)) }

                OutlinedButton(onClick = {
                    clipboard.setText(AnnotatedString(displayName))
                    snackbarService.showMessage(context.getString(R.string.name_copied))
                }) { Text(stringResource(R.string.copy_name)) }

                OutlinedButton(onClick = { showPalettePicker = true }) {
                    Text(stringResource(R.string.add_to_palette))
                }
            }

            Spacer(Modifier.height(14.dp))

            // Info chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(stringResource(R.string.luma_label, (details.luminance * 100).toInt()))
                    }
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            if (details.isDark) {
                                stringResource(R.string.dark_label)
                            } else {
                                stringResource(R.string.light_label)
                            }
                        )
                    }
                )
                AssistChip(
                    onClick = {},
                    label = {
                        val textColor = if (details.recommendedOnColor == 0xFFFFFFFF.toInt()) {
                            stringResource(R.string.white_label)
                        } else {
                            stringResource(R.string.black_label)
                        }
                        Text(stringResource(R.string.text_recommendation, textColor))
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // RGB / HSV / HSL
            KeyValueGrid(
                items = listOf(
                    stringResource(R.string.rgb_label) to "${details.rgb.r}, ${details.rgb.g}, ${details.rgb.b}",
                    stringResource(R.string.hsv_label) to "${details.hsv.h.toInt()}°, ${(details.hsv.s * 100).toInt()}%, ${(details.hsv.v * 100).toInt()}%",
                    stringResource(R.string.hsl_label) to "${details.hsl.h.toInt()}°, ${(details.hsl.s * 100).toInt()}%, ${(details.hsl.l * 100).toInt()}%"
                )
            )

            Spacer(Modifier.height(14.dp))

            // Harmonies
            Text(stringResource(R.string.harmonies), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            HarmonyRow(
                label = stringResource(R.string.harmony_complement),
                argbs = details.complements
            )
            Spacer(Modifier.height(8.dp))
            HarmonyRow(label = stringResource(R.string.harmony_triad), argbs = details.triads)
            Spacer(Modifier.height(8.dp))
            HarmonyRow(
                label = stringResource(R.string.harmony_analogous),
                argbs = details.analogous
            )

            Spacer(Modifier.height(14.dp))

            Text(
                stringResource(R.string.color_shades_tints_tones_title, displayName),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(10.dp))
            ShadeToneRow(
                label = stringResource(R.string.tints_label),
                argbs = details.tints,
                onOpenPalette = {
                    val saved = PaletteService.create(
                        name = "${displayName} ${context.getString(R.string.tints_label)}",
                        colors = (listOf(details.argb) + details.tints).toPickedColors(),
                        tags = listOf("details"),
                        saveOnCreate = false,
                        creationSource = "color_details"
                    )
                    ColorServices.selectedPalette = saved
                    onOpenPalette(saved.id, false)
                }
            )
            Spacer(Modifier.height(8.dp))
            ShadeToneRow(
                label = stringResource(R.string.shades_label),
                argbs = details.shades,
                onOpenPalette = {
                    val saved = PaletteService.create(
                        name = "${displayName} ${context.getString(R.string.shades_label)}",
                        colors = (listOf(details.argb) + details.shades).toPickedColors(),
                        tags = listOf("details"),
                        saveOnCreate = false,
                        creationSource = "color_details"
                    )
                    ColorServices.selectedPalette = saved
                    onOpenPalette(saved.id, false)
                }
            )
            Spacer(Modifier.height(8.dp))
            ShadeToneRow(
                label = stringResource(R.string.tones_label),
                argbs = details.tones,
                onOpenPalette = {
                    val saved = PaletteService.create(
                        name = "${displayName} ${context.getString(R.string.tones_label)}",
                        colors = (listOf(details.argb) + details.tones).toPickedColors(),
                        tags = listOf("details"),
                        saveOnCreate = false,
                        creationSource = "color_details"
                    )
                    ColorServices.selectedPalette = saved
                    onOpenPalette(saved.id, false)
                }
            )

            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.color_palettes_title, displayName),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(10.dp))
            val paletteSchemes = listOf(
                PaletteScheme(
                    title = stringResource(R.string.palette_scheme_complementary),
                    colors = listOf(details.argb) + details.complements
                ),
                PaletteScheme(
                    title = stringResource(R.string.palette_scheme_analogous),
                    colors = listOf(details.argb) + details.analogous
                ),
                PaletteScheme(
                    title = stringResource(R.string.palette_scheme_split_complementary),
                    colors = listOf(details.argb) + details.splitComplements
                ),
                PaletteScheme(
                    title = stringResource(R.string.palette_scheme_triadic),
                    colors = listOf(details.argb) + details.triads
                ),
                PaletteScheme(
                    title = stringResource(R.string.palette_scheme_tetradic),
                    colors = listOf(details.argb) + details.tetrads
                ),
                PaletteScheme(
                    title = stringResource(R.string.palette_scheme_square),
                    colors = listOf(details.argb) + details.squares
                )
            )

            paletteSchemes.forEach { scheme ->
                PaletteSchemeCard(
                    scheme = scheme,
                    onOpenPalette = {
                        val saved = PaletteService.create(
                            name = "${displayName} ${scheme.title}",
                            colors = scheme.colors.toPickedColors(),
                            tags = listOf("details"),
                            saveOnCreate = false,
                            creationSource = "color_details"
                        )
                        ColorServices.selectedPalette = saved
                        onOpenPalette(saved.id, false)
                    }
                )
                Spacer(Modifier.height(10.dp))
            }

            // Similar colors
            Text(
                stringResource(R.string.similar_colors),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                details.similarColors.forEach { s ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(84.dp)
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(s.argb))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = s.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = argbToHex(s.argb),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }

    if (showPalettePicker) {
        PalettePickerDialog(
            palettes = palettes,
            pickedColor = pickedColor,
            onDismiss = { showPalettePicker = false },
            onPaletteUpdated = { message ->
                snackbarService.showMessage(message)
                showPalettePicker = false
            },
            onPaletteCreated = { message ->
                snackbarService.showMessage(message)
                showPalettePicker = false
            }
        )
    }
}

@Composable
private fun HarmonyRow(label: String, argbs: List<Int>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(90.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(argbs) { a ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(a))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        argbToHex(a),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun ShadeToneRow(
    label: String,
    argbs: List<Int>,
    onOpenPalette: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenPalette() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(90.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(argbs) { a ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(a))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            argbToHex(a),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteSchemeCard(
    scheme: PaletteScheme,
    onOpenPalette: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenPalette() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(scheme.title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(scheme.colors) { a ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(a))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            argbToHex(a),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyValueGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (k, v) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    k,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(52.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    v,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun PalettePickerDialog(
    palettes: List<com.primortex.color.app.Palette>,
    pickedColor: PickedColor,
    onDismiss: () -> Unit,
    onPaletteUpdated: (String) -> Unit,
    onPaletteCreated: (String) -> Unit
) {
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_palette)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (palettes.isEmpty()) {
                    Text(stringResource(R.string.no_palettes_available))
                } else {
                    palettes.forEach { palette ->
                        TextButton(
                            onClick = {
                                when {
                                    palette.colors.any { it.argb == pickedColor.argb } ->
                                        onPaletteUpdated(ctx.getString(R.string.already_in_palette))

                                    palette.colors.size >= 10 ->
                                        onPaletteUpdated(ctx.getString(R.string.palette_full))

                                    else -> {
                                        PaletteService.update(
                                            id = palette.id,
                                            colors = palette.colors + pickedColor
                                        )
                                        onPaletteUpdated(ctx.getString(R.string.added_to_palette))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = palette.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        val newPaletteName = ctx.getString(
                            R.string.palette_name_with_color,
                            pickedColor.name
                        )
                        val hasDuplicateName = palettes.any { palette ->
                            palette.name.equals(newPaletteName, ignoreCase = true)
                        }
                        if (hasDuplicateName) {
                            onPaletteCreated(ctx.getString(R.string.already_in_palette))
                        } else {
                            PaletteService.create(
                                name = newPaletteName,
                                colors = listOf(pickedColor),
                                tags = listOf("details"),
                                creationSource = "color_details"
                            )
                            onPaletteCreated(ctx.getString(R.string.palette_saved))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.create_new_palette))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private data class PaletteScheme(val title: String, val colors: List<Int>)

private fun List<Int>.toPickedColors(): List<PickedColor> {
    return distinct().map { argb ->
        val name = ColorServices.colors.localNameFromArgb(argb)
        PickedColor(argb, if (name.isBlank()) argbToHex(argb) else name)
    }
}
