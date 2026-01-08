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
    val analogous: List<Int>
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
            analogous = analogous
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
}
