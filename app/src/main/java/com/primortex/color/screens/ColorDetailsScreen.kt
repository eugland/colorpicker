// com/primortex/color/screens/ColorDetailsScreen.kt
package com.primortex.color.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.outlined.ContentCopy
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.primortex.color.service.argbToHex
import com.primortex.color.ui.LocalSnackbarService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorDetailsScreen(
    argb: Int,
    nameHint: String? = null,
    onBack: () -> Unit,
    onOpenColorDetail: (PickedColor) -> Unit = {} // open similar colors
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val snackbarService = LocalSnackbarService.current

    val details: ColorDetails = remember(argb) {
        ColorDetailsService.details(argb, similarLimit = 10)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        details.name.ifBlank { nameHint ?: stringResource(R.string.color_label) },
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
        ) {

            // Big swatch card
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(details.argb),
                                    Color(details.argb)
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
                        details.name.ifBlank { nameHint ?: stringResource(R.string.color_label) },
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
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {
                    clipboard.setText(AnnotatedString(details.hex))
                    snackbarService.showMessage(context.getString(R.string.hex_copied))
                }) { Text(stringResource(R.string.copy_hex)) }

                OutlinedButton(onClick = {
                    clipboard.setText(AnnotatedString(details.name))
                    snackbarService.showMessage(context.getString(R.string.name_copied))
                }) { Text(stringResource(R.string.copy_name)) }
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
            items(argbs, key = { it }) { a ->
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
