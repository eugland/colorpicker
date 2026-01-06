package com.primortex.color.service

import android.graphics.Color as AColor
import com.primortex.color.app.PickedColor
import kotlin.math.*

data class Rgb(val r: Int, val g: Int, val b: Int)
data class Hsv(val h: Float, val s: Float, val v: Float)
data class Hsl(val h: Float, val s: Float, val l: Float)

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
        val name = ColorServices.colorNames.localNameFromArgb(argb)

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
        val all = ColorNameIndex.allColors()

        return all.asSequence()
            .filter { excludeArgb == null || it.argb != excludeArgb }
            .sortedBy { rgbDistanceSq(target, argbToRgb(it.argb)) }
            .take(limit)
            .toList()
    }

    private fun rgbDistanceSq(a: Rgb, b: Rgb): Int {
        val dr = a.r - b.r
        val dg = a.g - b.g
        val db = a.b - b.b
        return dr * dr + dg * dg + db * db
    }

    // --- Conversions ---
    private fun argbToRgb(argb: Int): Rgb {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = (argb) and 0xFF
        return Rgb(r, g, b)
    }

    private fun argbToHsv(argb: Int): Hsv {
        val rgb = argbToRgb(argb)
        val hsv = FloatArray(3)
        AColor.RGBToHSV(rgb.r, rgb.g, rgb.b, hsv)
        return Hsv(hsv[0], hsv[1], hsv[2])
    }

    private fun rgbToHsl(rgb: Rgb): Hsl {
        val r = rgb.r / 255f
        val g = rgb.g / 255f
        val b = rgb.b / 255f

        val max = max(r, max(g, b))
        val min = min(r, min(g, b))
        val d = max - min

        val l = (max + min) / 2f

        val s = if (d == 0f) 0f else d / (1f - abs(2f * l - 1f))

        val h = when {
            d == 0f -> 0f
            max == r -> ((g - b) / d).mod(6f) * 60f
            max == g -> (((b - r) / d) + 2f) * 60f
            else -> (((r - g) / d) + 4f) * 60f
        }

        return Hsl(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
    }

    private fun luminance(rgb: Rgb): Float {
        fun linearize(c: Int): Float {
            val s = c / 255f
            return if (s <= 0.04045f) (s / 12.92f) else ((s + 0.055f) / 1.055f).pow(2.4f)
        }
        val r = linearize(rgb.r)
        val g = linearize(rgb.g)
        val b = linearize(rgb.b)
        return (0.2126f * r + 0.7152f * g + 0.0722f * b).coerceIn(0f, 1f)
    }

    private fun hueShift(argb: Int, degrees: Float): Int {
        val rgb = argbToRgb(argb)
        val hsv = FloatArray(3)
        AColor.RGBToHSV(rgb.r, rgb.g, rgb.b, hsv)
        hsv[0] = (hsv[0] + degrees).mod(360f)
        return AColor.HSVToColor(0xFF, hsv)
    }

    // --- Hex (ARGB always stored, output #RRGGBB) ---
    private fun argbToHex(argb: Int): String {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = (argb) and 0xFF
        return "#%02X%02X%02X".format(r, g, b)
    }
}
