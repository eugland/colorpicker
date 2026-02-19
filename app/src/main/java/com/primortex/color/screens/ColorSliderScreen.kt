package com.primortex.color.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import com.primortex.color.i18n.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.primortex.color.R
import com.primortex.color.app.PickedColor
import com.primortex.color.service.ColorServices
import com.primortex.color.service.RecentPicksService
import com.primortex.color.service.argbToHex
import com.primortex.color.ui.components.ColorDetailsBottomSheet
import com.primortex.color.ui.components.ScreenScaffold
import android.graphics.Color as AndroidColor

private enum class SliderMode { RGB, HSL, HSV, CMYK }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorSliderScreen(
    innerPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenColorDetail: (PickedColor) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var argb by remember { mutableIntStateOf(0xFF7C3AED.toInt()) }
    var showColorDetails by remember { mutableStateOf(false) }
    var sliderMode by remember { mutableStateOf(SliderMode.RGB) }
    val colorNameService = remember(context) {
        ColorServices.ensure(context)
        ColorServices.colors
    }

    val nearestName = remember(argb) { colorNameService.localNameFromArgb(argb) }
    val picked = remember(argb, nearestName) { PickedColor(argb = argb, name = nearestName) }
    val hex = remember(argb) { argbToHex(argb) }
    val rgb = remember(argb) {
        Triple(AndroidColor.red(argb), AndroidColor.green(argb), AndroidColor.blue(argb))
    }
    val hsv = remember(argb) {
        FloatArray(3).also { AndroidColor.RGBToHSV(rgb.first, rgb.second, rgb.third, it) }
    }
    val hsl = remember(argb) {
        FloatArray(3).also { ColorUtils.colorToHSL(argb, it) }
    }
    val cmyk = remember(argb) { rgbToCmyk(rgb.first, rgb.second, rgb.third) }

    ScreenScaffold(
        titleRes = R.string.color_slider,
        innerPadding = innerPadding,
        onBack = onBack
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ColorPreviewCard(
                name = nearestName,
                hex = hex,
                argb = argb,
                onClick = { showColorDetails = true }
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val rgbText = "RGB ${rgb.first}, ${rgb.second}, ${rgb.third}"
                val hslText =
                    "HSL ${hsl[0].toInt()}°, ${(hsl[1] * 100).toInt()}%, ${(hsl[2] * 100).toInt()}%"
                val hsvText =
                    "HSV ${hsv[0].toInt()}°, ${(hsv[1] * 100).toInt()}%, ${(hsv[2] * 100).toInt()}%"
                val cmykText = "CMYK ${cmyk.c}%, ${cmyk.m}%, ${cmyk.y}%, ${cmyk.k}%"

                AssistChip(
                    onClick = {
                        clipboard.setText(AnnotatedString(rgbText))
                        RecentPicksService.addPick(picked, source = "color_slider_copy")
                    },
                    label = { Text(rgbText) }
                )
                AssistChip(
                    onClick = {
                        clipboard.setText(AnnotatedString(cmykText))
                        RecentPicksService.addPick(picked, source = "color_slider_copy")
                    },
                    label = { Text(cmykText) }
                )
                AssistChip(
                    onClick = {
                        clipboard.setText(AnnotatedString(hslText))
                        RecentPicksService.addPick(picked, source = "color_slider_copy")
                    },
                    label = { Text(hslText) }
                )
                AssistChip(
                    onClick = {
                        clipboard.setText(AnnotatedString(hsvText))
                        RecentPicksService.addPick(picked, source = "color_slider_copy")
                    },
                    label = { Text(hsvText) }
                )

            }

            Text(
                stringResource(R.string.sliders_label),
                style = MaterialTheme.typography.titleMedium
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SliderMode.entries.forEach { mode ->
                    FilterChip(
                        selected = sliderMode == mode,
                        onClick = { sliderMode = mode },
                        label = { Text(mode.name) }
                    )
                }
            }

            when (sliderMode) {
                SliderMode.RGB -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.rgb_label),
                        style = MaterialTheme.typography.titleMedium
                    )
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

                SliderMode.HSL -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.hsl_label),
                        style = MaterialTheme.typography.titleMedium
                    )
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

                SliderMode.HSV -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.hsv_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    LabeledSlider(
                        label = stringResource(R.string.hue_label),
                        value = hsv[0],
                        valueRange = 0f..360f,
                        steps = 0,
                        onValueChange = { value ->
                            argb = AndroidColor.HSVToColor(floatArrayOf(value, hsv[1], hsv[2]))
                        }
                    )
                    LabeledSlider(
                        label = stringResource(R.string.saturation_label),
                        value = hsv[1] * 100f,
                        valueRange = 0f..100f,
                        onValueChange = { value ->
                            argb =
                                AndroidColor.HSVToColor(floatArrayOf(hsv[0], value / 100f, hsv[2]))
                        },
                        valueFormatter = { v -> "${v.toInt()}%" }
                    )
                    LabeledSlider(
                        label = stringResource(R.string.value_label),
                        value = hsv[2] * 100f,
                        valueRange = 0f..100f,
                        onValueChange = { value ->
                            argb =
                                AndroidColor.HSVToColor(floatArrayOf(hsv[0], hsv[1], value / 100f))
                        },
                        valueFormatter = { v -> "${v.toInt()}%" }
                    )
                }

                SliderMode.CMYK -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.cmyk_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    LabeledSlider(
                        label = stringResource(R.string.cyan_label),
                        value = cmyk.c.toFloat(),
                        valueRange = 0f..100f,
                        onValueChange = { value ->
                            argb = cmykToArgb(value.toInt(), cmyk.m, cmyk.y, cmyk.k)
                        },
                        valueFormatter = { v -> "${v.toInt()}%" }
                    )
                    LabeledSlider(
                        label = stringResource(R.string.magenta_label),
                        value = cmyk.m.toFloat(),
                        valueRange = 0f..100f,
                        onValueChange = { value ->
                            argb = cmykToArgb(cmyk.c, value.toInt(), cmyk.y, cmyk.k)
                        },
                        valueFormatter = { v -> "${v.toInt()}%" }
                    )
                    LabeledSlider(
                        label = stringResource(R.string.yellow_label),
                        value = cmyk.y.toFloat(),
                        valueRange = 0f..100f,
                        onValueChange = { value ->
                            argb = cmykToArgb(cmyk.c, cmyk.m, value.toInt(), cmyk.k)
                        },
                        valueFormatter = { v -> "${v.toInt()}%" }
                    )
                    LabeledSlider(
                        label = stringResource(R.string.black_label),
                        value = cmyk.k.toFloat(),
                        valueRange = 0f..100f,
                        onValueChange = { value ->
                            argb = cmykToArgb(cmyk.c, cmyk.m, cmyk.y, value.toInt())
                        },
                        valueFormatter = { v -> "${v.toInt()}%" }
                    )
                }
            }
        }
    }

    if (showColorDetails) {
        ColorDetailsBottomSheet(
            picked = picked,
            onDismiss = { showColorDetails = false },
            onOpenColorDetail = onOpenColorDetail
        )
    }
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
            modifier = Modifier.height(28.dp),
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

private data class Cmyk(val c: Int, val m: Int, val y: Int, val k: Int)

private fun rgbToCmyk(r: Int, g: Int, b: Int): Cmyk {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val k = 1f - maxOf(rf, gf, bf)
    if (k >= 1f) {
        return Cmyk(0, 0, 0, 100)
    }
    val c = ((1f - rf - k) / (1f - k) * 100f).coerceIn(0f, 100f)
    val m = ((1f - gf - k) / (1f - k) * 100f).coerceIn(0f, 100f)
    val y = ((1f - bf - k) / (1f - k) * 100f).coerceIn(0f, 100f)
    return Cmyk(c.toInt(), m.toInt(), y.toInt(), (k * 100f).toInt())
}

private fun cmykToArgb(c: Int, m: Int, y: Int, k: Int): Int {
    val cf = c.coerceIn(0, 100) / 100f
    val mf = m.coerceIn(0, 100) / 100f
    val yf = y.coerceIn(0, 100) / 100f
    val kf = k.coerceIn(0, 100) / 100f
    val r = (255f * (1f - cf) * (1f - kf)).toInt()
    val g = (255f * (1f - mf) * (1f - kf)).toInt()
    val b = (255f * (1f - yf) * (1f - kf)).toInt()
    return toArgb(r, g, b)
}
