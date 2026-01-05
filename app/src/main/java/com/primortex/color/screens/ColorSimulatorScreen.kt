package com.primortex.color.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.primortex.color.R
import com.primortex.color.service.ColorNameLookup
import com.primortex.color.ui.components.ScreenScaffold
import com.primortex.color.ui.util.argbToHex
import android.graphics.Color as AndroidColor

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
    var argb by remember {
        mutableIntStateOf(
            ColorUtils.HSLToColor(
                floatArrayOf(
                    hue,
                    saturation,
                    lightness
                )
            )
        )
    }
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
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                        Text(
                            stringResource(R.string.tweak_the_shade),
                            style = MaterialTheme.typography.titleMedium
                        )
                        LabeledSlider(
                            label = stringResource(R.string.hue),
                            value = hue,
                            valueRange = 0f..360f,
                            onValueChange = {
                                hue = it
                                argb =
                                    ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness))
                            }
                        )
                        LabeledSlider(
                            label = stringResource(R.string.saturation),
                            value = saturation * 100f,
                            valueRange = 0f..100f,
                            onValueChange = {
                                saturation = it / 100f
                                argb =
                                    ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness))
                            },
                            valueFormatter = { v -> "${v.toInt()}%" }
                        )
                        LabeledSlider(
                            label = stringResource(R.string.lightness),
                            value = lightness * 100f,
                            valueRange = 0f..100f,
                            onValueChange = {
                                lightness = it / 100f
                                argb =
                                    ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness))
                            },
                            valueFormatter = { v -> "${v.toInt()}%" }
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.simulated_surfaces),
                style = MaterialTheme.typography.titleMedium
            )

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

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                scenes.forEach { scene ->
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
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                SceneIllustration(scene = scene, color = color)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = scene.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(stringResource(scene.title), style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    stringResource(scene.subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
private fun SceneIllustration(scene: SimulationScene, color: Color) {
    val neutral = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val shadow = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    val accent = color.copy(alpha = 0.88f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        when (scene.title) {
            R.string.scene_dress_title -> DressIllustration(neutral, accent, shadow)
            R.string.scene_shoes_title -> ShoesIllustration(neutral, accent, shadow)
            R.string.scene_building_title -> BuildingIllustration(neutral, accent, shadow)
            else -> HouseIllustration(neutral, accent, shadow)
        }
    }
}

@Composable
private fun DressIllustration(neutral: Color, accent: Color, shadow: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(color = Color(0xFFE8E8E8))
        drawRect(
            color = Color(0xFFD6D6D6),
            topLeft = Offset(0f, h * 0.68f),
            size = Size(w, h * 0.32f)
        )

        drawRoundRect(
            color = shadow.copy(alpha = 0.28f),
            topLeft = Offset(w * 0.34f, h * 0.78f),
            size = Size(w * 0.32f, h * 0.08f),
            cornerRadius = CornerRadius(w * 0.2f)
        )

        val skin = Color(0xFFDCC3A9)
        val hair = Color(0xFF5A4A3F)
        val boot = Color(0xFF2A2A2A)

        drawRoundRect(
            color = skin,
            topLeft = Offset(w * 0.44f, h * 0.22f),
            size = Size(w * 0.07f, h * 0.06f),
            cornerRadius = CornerRadius(w * 0.04f)
        )

        drawRoundRect(
            color = skin,
            topLeft = Offset(w * 0.41f, h * 0.27f),
            size = Size(w * 0.08f, h * 0.16f),
            cornerRadius = CornerRadius(w * 0.05f)
        )

        drawRoundRect(
            color = skin,
            topLeft = Offset(w * 0.35f, h * 0.46f),
            size = Size(w * 0.07f, h * 0.33f),
            cornerRadius = CornerRadius(w * 0.035f)
        )
        drawRoundRect(
            color = skin,
            topLeft = Offset(w * 0.46f, h * 0.43f),
            size = Size(w * 0.06f, h * 0.36f),
            cornerRadius = CornerRadius(w * 0.03f)
        )

        drawRoundRect(
            color = hair,
            topLeft = Offset(w * 0.43f, h * 0.19f),
            size = Size(w * 0.09f, h * 0.09f),
            cornerRadius = CornerRadius(w * 0.06f)
        )

        drawRoundRect(
            color = boot,
            topLeft = Offset(w * 0.33f, h * 0.72f),
            size = Size(w * 0.1f, h * 0.15f),
            cornerRadius = CornerRadius(w * 0.03f)
        )
        drawRoundRect(
            color = boot,
            topLeft = Offset(w * 0.46f, h * 0.73f),
            size = Size(w * 0.11f, h * 0.16f),
            cornerRadius = CornerRadius(w * 0.03f)
        )
        drawRoundRect(
            color = shadow.copy(alpha = 0.4f),
            topLeft = Offset(w * 0.32f, h * 0.87f),
            size = Size(w * 0.27f, h * 0.03f),
            cornerRadius = CornerRadius(w * 0.08f)
        )
        drawRoundRect(
            color = shadow.copy(alpha = 0.32f),
            topLeft = Offset(w * 0.46f, h * 0.88f),
            size = Size(w * 0.3f, h * 0.03f),
            cornerRadius = CornerRadius(w * 0.08f)
        )

        val dressPath = Path().apply {
            moveTo(w * 0.36f, h * 0.32f)
            cubicTo(w * 0.38f, h * 0.27f, w * 0.45f, h * 0.26f, w * 0.48f, h * 0.3f)
            lineTo(w * 0.53f, h * 0.46f)
            lineTo(w * 0.52f, h * 0.6f)
            cubicTo(w * 0.47f, h * 0.66f, w * 0.4f, h * 0.65f, w * 0.36f, h * 0.59f)
            lineTo(w * 0.32f, h * 0.5f)
            close()
        }

        drawPath(
            path = dressPath,
            brush = Brush.verticalGradient(
                colors = listOf(accent.copy(alpha = 0.9f), accent.copy(alpha = 0.7f)),
                startY = h * 0.3f,
                endY = h * 0.6f
            )
        )

        drawPath(
            path = dressPath,
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(w * 0.46f, h * 0.42f),
                radius = w * 0.18f
            )
        )

        drawPath(
            path = dressPath,
            color = shadow.copy(alpha = 0.35f),
            style = Stroke(width = w * 0.006f)
        )

        drawRoundRect(
            color = accent.copy(alpha = 0.2f),
            topLeft = Offset(w * 0.46f, h * 0.42f),
            size = Size(w * 0.12f, h * 0.12f),
            cornerRadius = CornerRadius(w * 0.05f)
        )
    }
}

@Composable
private fun ShoesIllustration(neutral: Color, accent: Color, shadow: Color) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(accent)
            )
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(accent.copy(alpha = 0.75f))
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .align(Alignment.BottomCenter)
                .background(shadow)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-4).dp)
                .background(neutral)
        )
    }
}

@Composable
private fun BuildingIllustration(neutral: Color, accent: Color, shadow: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .size(width = 70.dp, height = 70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(neutral)
            )
            Box(
                modifier = Modifier
                    .size(width = 90.dp, height = 110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent)
            )
            Box(
                modifier = Modifier
                    .size(width = 70.dp, height = 90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.8f))
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .align(Alignment.BottomCenter)
                .background(shadow)
        )
    }
}

@Composable
private fun HouseIllustration(neutral: Color, accent: Color, shadow: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(width = 150.dp, height = 70.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accent)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-40).dp)
                .size(width = 100.dp, height = 40.dp)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(neutral)
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-44).dp)
                .size(width = 170.dp, height = 70.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(accent.copy(alpha = 0.8f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(width = 60.dp, height = 40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shadow)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 6.dp)
                .size(width = 220.dp, height = 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(shadow.copy(alpha = 0.6f))
        )
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
