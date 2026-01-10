package com.primortex.color.service

import android.graphics.Color as AColor
import androidx.core.graphics.ColorUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class Rgb(val r: Int, val g: Int, val b: Int)
data class Hsv(val h: Float, val s: Float, val v: Float)
data class Hsl(val h: Float, val s: Float, val l: Float)

data class Lab(val l: Float, val a: Float, val b: Float)

fun argbToRgb(argb: Int): Rgb {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return Rgb(r, g, b)
}

fun argbToHsv(argb: Int): Hsv {
    val rgb = argbToRgb(argb)
    val hsv = FloatArray(3)
    AColor.RGBToHSV(rgb.r, rgb.g, rgb.b, hsv)
    return Hsv(hsv[0], hsv[1], hsv[2])
}

fun rgbToHsl(rgb: Rgb): Hsl {
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

fun luminance(rgb: Rgb): Float {
    fun linearize(c: Int): Float {
        val s = c / 255f
        return if (s <= 0.04045f) (s / 12.92f) else ((s + 0.055f) / 1.055f).pow(2.4f)
    }
    val r = linearize(rgb.r)
    val g = linearize(rgb.g)
    val b = linearize(rgb.b)
    return (0.2126f * r + 0.7152f * g + 0.0722f * b).coerceIn(0f, 1f)
}

fun hueShift(argb: Int, degrees: Float): Int {
    val rgb = argbToRgb(argb)
    val hsv = FloatArray(3)
    AColor.RGBToHSV(rgb.r, rgb.g, rgb.b, hsv)
    hsv[0] = (hsv[0] + degrees).mod(360f)
    return AColor.HSVToColor(0xFF, hsv)
}

fun rgbDistanceSq(a: Rgb, b: Rgb): Int {
    val dr = a.r - b.r
    val dg = a.g - b.g
    val db = a.b - b.b
    return dr * dr + dg * dg + db * db
}

fun rgbDistSq(a: Int, b: Int): Int {
    val ar = (a shr 16) and 0xFF
    val ag = (a shr 8) and 0xFF
    val ab = a and 0xFF
    val br = (b shr 16) and 0xFF
    val bg = (b shr 8) and 0xFF
    val bb = b and 0xFF
    val dr = ar - br
    val dg = ag - bg
    val db = ab - bb
    return dr * dr + dg * dg + db * db
}

fun argbToHex(argb: Int): String {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return "#%02X%02X%02X".format(r, g, b)
}

fun hexToArgb(hex: String): Int {
    val clean = hex.trim().removePrefix("#")
    require(clean.matches(Regex("^[0-9A-Fa-f]{6}$"))) { "Invalid hex: $hex" }
    return (0xFF shl 24) or clean.toInt(16)
}

fun normalizeArgb(argb: Int): Int = argb or (0xFF shl 24)

fun normalizeName(name: String): String = name.trim().lowercase()

fun argbToLab(argb: Int): Lab {
    val r = ((argb ushr 16) and 0xFF) / 255f
    val g = ((argb ushr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f

    fun pivot(u: Float): Float =
        if (u > 0.04045f) (((u + 0.055f) / 1.055f).toDouble().pow(2.4)).toFloat() else (u / 12.92f)

    val rr = pivot(r)
    val gg = pivot(g)
    val bb = pivot(b)

    val x = (0.4124f * rr + 0.3576f * gg + 0.1805f * bb) / 0.95047f
    val y = (0.2126f * rr + 0.7152f * gg + 0.0722f * bb)
    val z = (0.0193f * rr + 0.1192f * gg + 0.9505f * bb) / 1.08883f

    fun f(t: Float): Float =
        if (t > 0.008856f) cbrt(t) else (7.787f * t + 16f / 116f)

    val fx = f(x)
    val fy = f(y)
    val fz = f(z)

    val l = 116f * fy - 16f
    val a = 500f * (fx - fy)
    val bVal = 200f * (fy - fz)
    return Lab(l, a, bVal)
}

fun deltaE76(p: Lab, q: Lab): Float {
    val dl = p.l - q.l
    val da = p.a - q.a
    val db = p.b - q.b
    return sqrt(dl * dl + da * da + db * db)
}

fun argbToHslString(argb: Int): String {
    val rgb = argbToRgb(argb)
    val hsl = FloatArray(3)
    ColorUtils.RGBToHSL(rgb.r, rgb.g, rgb.b, hsl)
    val h = hsl[0].toInt()
    val s = (hsl[1] * 100).toInt()
    val l = (hsl[2] * 100).toInt()
    return "HSL($h°, $s%, $l%)"
}

private fun cbrt(x: Float): Float = Math.cbrt(x.toDouble()).toFloat()
