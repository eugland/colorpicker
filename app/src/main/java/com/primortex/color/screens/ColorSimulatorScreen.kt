package com.primortex.color.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.House
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.primortex.color.R
import com.primortex.color.service.ColorNameLookup
import com.primortex.color.ui.components.ScreenScaffold
import com.primortex.color.ui.util.argbToHex

private data class SimulationScene(
    val title: Int,
    val subtitle: Int,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSimulatorScreen(
    innerPadding: PaddingValues,
    onBack: () -> Unit
) {
    var hue by remember { mutableFloatStateOf(210f) }
    var saturation by remember { mutableFloatStateOf(0.55f) }
    var lightness by remember { mutableFloatStateOf(0.55f) }
    var argb by remember { mutableIntStateOf(ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness))) }
    var hexInput by remember { mutableStateOf(argbToHex(argb)) }

    val nearestName = remember(argb) { ColorNameLookup.nearestName(argb).name }
    LaunchedEffect(argb) {
        hexInput = argbToHex(argb)
    }

    ScreenScaffold(
        titleRes = R.string.color_simulator,
        innerPadding = innerPadding,
        onBack = onBack
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(argb))
                        )
                        Spacer(Modifier.size(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(nearestName, style = MaterialTheme.typography.titleLarge)
                            Text(hexInput, fontFamily = FontFamily.Monospace)
                        }
                    }

                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { value ->
                            val sanitized = value.uppercase().take(7)
                            hexInput = sanitized
                            val candidate = sanitized.removePrefix("#")
                            if (candidate.length == 6 && candidate.all { it.isDigit() || it in 'A'..'F' }) {
                                runCatching { AndroidColor.parseColor("#${candidate}") }
                                    .onSuccess { parsed ->
                                        argb = parsed or (0xFF shl 24)
                                        val hsl = FloatArray(3)
                                        ColorUtils.colorToHSL(argb, hsl)
                                        hue = hsl[0]
                                        saturation = hsl[1]
                                        lightness = hsl[2]
                                    }
                            }
                        },
                        label = { Text(stringResource(R.string.hex_label)) },
                        leadingIcon = { Icon(Icons.Outlined.Brush, contentDescription = null) },
                        singleLine = true,
                        supportingText = { Text(stringResource(R.string.hex_supporting_text)) }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.tweak_the_shade), style = MaterialTheme.typography.titleMedium)
                        LabeledSlider(
                            label = stringResource(R.string.hue),
                            value = hue,
                            valueRange = 0f..360f,
                            onValueChange = {
                                hue = it
                                argb = ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness))
                            }
                        )
                        LabeledSlider(
                            label = stringResource(R.string.saturation),
                            value = saturation * 100f,
                            valueRange = 0f..100f,
                            onValueChange = {
                                saturation = it / 100f
                                argb = ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness))
                            },
                            valueFormatter = { v -> "${v.toInt()}%" }
                        )
                        LabeledSlider(
                            label = stringResource(R.string.lightness),
                            value = lightness * 100f,
                            valueRange = 0f..100f,
                            onValueChange = {
                                lightness = it / 100f
                                argb = ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness))
                            },
                            valueFormatter = { v -> "${v.toInt()}%" }
                        )
                    }
                }
            }

            Text(stringResource(R.string.simulated_surfaces), style = MaterialTheme.typography.titleMedium)

            val scenes = remember {
                listOf(
                    SimulationScene(
                        title = R.string.scene_dress_title,
                        subtitle = R.string.scene_dress_subtitle,
                        icon = Icons.Outlined.Checkroom
                    ),
                    SimulationScene(
                        title = R.string.scene_shoes_title,
                        subtitle = R.string.scene_shoes_subtitle,
                        icon = Icons.Outlined.DirectionsWalk
                    ),
                    SimulationScene(
                        title = R.string.scene_building_title,
                        subtitle = R.string.scene_building_subtitle,
                        icon = Icons.Outlined.Apartment
                    ),
                    SimulationScene(
                        title = R.string.scene_house_title,
                        subtitle = R.string.scene_house_subtitle,
                        icon = Icons.Outlined.House
                    )
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(scenes) { scene ->
                    SimulationCard(scene = scene, color = Color(argb))
                }
            }

            Spacer(Modifier.height(12.dp))

            val saveHint = stringResource(R.string.simulator_save_hint)
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Brush,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        saveHint,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SimulationCard(scene: SimulationScene, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            color.copy(alpha = 0.75f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = scene.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Text(stringResource(scene.title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(scene.subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Text(
                        text = argbToHex(color.toArgb()),
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
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
            steps = steps
        )
    }
}
