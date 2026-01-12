package com.primortex.color.service

import com.primortex.color.app.PickedColor

data class ColorDetails(
    val argb: Int,
    val name: String,
    val hex: String,

    val rgb: Rgb,
    val hsv: Hsv,
    val hsl: Hsl,

    val luminance: Float,
    val isDark: Boolean,
    val recommendedOnColor: Int,

    val similarColors: List<PickedColor>,
    val complements: List<Int>,
    val triads: List<Int>,
    val analogous: List<Int>,
    val splitComplements: List<Int>,
    val tetrads: List<Int>,
    val squares: List<Int>,
    val tints: List<Int>,
    val shades: List<Int>,
    val tones: List<Int>
)

object ColorDetailsService {

    fun details(argb: Int, similarLimit: Int = 8): ColorDetails {
        val name = ColorServices.colors.localNameFromArgb(argb)

        val rgb = argbToRgb(argb)
        val hsv = argbToHsv(argb)
        val hsl = rgbToHsl(rgb)

        val lum = luminance(rgb)
        val dark = lum < 0.5f
        val on = if (dark) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()

        val complements = listOf(hueShift(argb, 180f))
        val triads = listOf(hueShift(argb, 120f), hueShift(argb, 240f))
        val analogous = listOf(hueShift(argb, -30f), hueShift(argb, 30f))
        val splitComplements = listOf(hueShift(argb, 150f), hueShift(argb, 210f))
        val tetrads = listOf(hueShift(argb, 60f), hueShift(argb, 180f), hueShift(argb, 240f))
        val squares = listOf(hueShift(argb, 90f), hueShift(argb, 180f), hueShift(argb, 270f))

        val tints = colorSeries(argb, 0xFFFFFFFF.toInt())
        val shades = colorSeries(argb, 0xFF000000.toInt())
        val tones = colorSeries(argb, 0xFF808080.toInt())

        val similar = similarColors(argb, similarLimit, excludeArgb = argb)

        return ColorDetails(
            argb = argb,
            name = name,
            hex = argbToHex(argb),
            rgb = rgb,
            hsv = hsv,
            hsl = hsl,
            luminance = lum,
            isDark = dark,
            recommendedOnColor = on,
            similarColors = similar,
            complements = complements,
            triads = triads,
            analogous = analogous,
            splitComplements = splitComplements,
            tetrads = tetrads,
            squares = squares,
            tints = tints,
            shades = shades,
            tones = tones
        )
    }

    // --- Similar colors: RGB Euclidean distance over your dataset ---
    private fun similarColors(argb: Int, limit: Int, excludeArgb: Int? = null): List<PickedColor> {
        val target = argbToRgb(argb)

        // You need a list of all colors in your dataset:
        val all = ColorServices.colors.allColors()

        return all.asSequence()
            .filter { excludeArgb == null || it.argb != excludeArgb }
            .sortedBy { rgbDistanceSq(target, argbToRgb(it.argb)) }
            .take(limit)
            .toList()
    }

    private fun colorSeries(base: Int, mix: Int): List<Int> {
        val steps = listOf(0.15f, 0.3f, 0.45f, 0.6f, 0.75f)
        return steps.map { blendColors(base, mix, it) }
    }

    private fun blendColors(base: Int, mix: Int, mixAmount: Float): Int {
        val baseRgb = argbToRgb(base)
        val mixRgb = argbToRgb(mix)
        val ratio = mixAmount.coerceIn(0f, 1f)
        val r = (baseRgb.r + (mixRgb.r - baseRgb.r) * ratio).toInt().coerceIn(0, 255)
        val g = (baseRgb.g + (mixRgb.g - baseRgb.g) * ratio).toInt().coerceIn(0, 255)
        val b = (baseRgb.b + (mixRgb.b - baseRgb.b) * ratio).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
