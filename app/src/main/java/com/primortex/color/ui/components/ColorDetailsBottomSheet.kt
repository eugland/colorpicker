package com.primortex.color.ui.components

import android.content.ClipData
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
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
import com.primortex.color.service.RecentPicksService
import com.primortex.color.service.argbToHex
import com.primortex.color.ui.LocalSnackbarService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorDetailsBottomSheet(
    picked: PickedColor,
    onDismiss: () -> Unit,
    onOpenColorDetail: (PickedColor) -> Unit = {},
    skipPartiallyExpanded: Boolean = true
) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val snackbarService = LocalSnackbarService.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded // ✅ opens “expanded” (no half height)
    )


    val details: ColorDetails = remember(picked.argb) {
        ColorDetailsService.details(picked.argb, similarLimit = 10)
    }
    val savedColors by RecentPicksService.saved.collectAsState()
    val isSaved = savedColors.any { it.argb == picked.argb }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp)
        ) {
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
                        details.name,
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
            }

            Spacer(Modifier.height(12.dp))

            // Actions
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onClick = {
                    scope.launch {
                        copyToClipboard(context, clipboard, R.string.copy_hex, details.hex)
                    }
                }) { Text(stringResource(R.string.copy_hex)) }

                OutlinedButton(onClick = {
                    scope.launch {
                        copyToClipboard(context, clipboard, R.string.copy_name, details.name)
                    }
                }) { Text(stringResource(R.string.copy_name)) }

                OutlinedButton(onClick = { RecentPicksService.toggleSaved(picked) }) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isSaved) {
                            stringResource(R.string.saved)
                        } else {
                            stringResource(R.string.save)
                        }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Info chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            HarmonyRow(stringResource(R.string.harmony_complement), details.complements)
            Spacer(Modifier.height(8.dp))
            HarmonyRow(stringResource(R.string.harmony_triad), details.triads)
            Spacer(Modifier.height(8.dp))
            HarmonyRow(stringResource(R.string.harmony_analogous), details.analogous)

            Spacer(Modifier.height(14.dp))

            Text(
                stringResource(R.string.color_shades_tints_tones_title, details.name),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.color_shades_tints_tones_description, details.name),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            ShadeToneRow(
                label = stringResource(R.string.tints_label),
                argbs = details.tints
            )
            Spacer(Modifier.height(8.dp))
            ShadeToneRow(
                label = stringResource(R.string.shades_label),
                argbs = details.shades
            )
            Spacer(Modifier.height(8.dp))
            ShadeToneRow(
                label = stringResource(R.string.tones_label),
                argbs = details.tones
            )

            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.color_plates_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.color_palettes_title, details.name),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.color_palettes_description, details.name),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    onSavePalette = {
                        PaletteService.create(
                            name = "${details.name} ${scheme.title}",
                            colors = scheme.colors.toPickedColors(),
                            tags = listOf("details"),
                            creationSource = "color_details"
                        )
                        snackbarService.showMessage(context.getString(R.string.palette_saved))
                    },
                    onDownloadPalette = {
                        clipboard.setClipEntry(
                            ClipData.newPlainText(
                                context.getString(R.string.export_css),
                                buildPaletteCss(scheme.colors)
                            ).toClipEntry()
                        )
                        snackbarService.showMessage(context.getString(R.string.exported_css))
                    }
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(6.dp))

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
                                .clickable { onOpenColorDetail(s) }
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
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            argbs.forEach { a ->
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(a))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
                Text(
                    argbToHex(a),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

@Composable
private fun ShadeToneRow(label: String, argbs: List<Int>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(90.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(argbs, key = { it }) { a ->
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

@Composable
private fun PaletteSchemeCard(
    scheme: PaletteScheme,
    onSavePalette: () -> Unit,
    onDownloadPalette: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(scheme.title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(scheme.colors, key = { it }) { a ->
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
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onSavePalette) {
                    Text(stringResource(R.string.save_palette))
                }
                OutlinedButton(onClick = onDownloadPalette) {
                    Text(stringResource(R.string.download_palette))
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

private suspend fun copyToClipboard(
    context: Context,
    clipboard: Clipboard,
    @StringRes labelResId: Int,
    text: String
) {
    clipboard.setClipEntry(
        ClipData.newPlainText(
            context.getString(labelResId),
            text
        ).toClipEntry()
    )
}

private data class PaletteScheme(val title: String, val colors: List<Int>)

private fun List<Int>.toPickedColors(): List<PickedColor> {
    return distinct().map { argb ->
        val name = ColorServices.colors.localNameFromArgb(argb)
        PickedColor(argb, if (name.isBlank()) argbToHex(argb) else name)
    }
}

private fun buildPaletteCss(colors: List<Int>): String {
    return buildString {
        appendLine(":root {")
        colors.forEachIndexed { index, color ->
            append("    --color-${index + 1}: ${argbToHex(color)};")
            append('\n')
        }
        append("}")
    }
}
