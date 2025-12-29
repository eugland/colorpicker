package com.primortex.color.ui.util

import android.graphics.Color
import kotlin.random.Random

fun argbToHex(argb: Int): String {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = (argb) and 0xFF
    return String.format("#%02X%02X%02X", r, g, b)
}

fun randomColorArgb(): Int {
    val r = Random.nextInt(0, 256)
    val g = Random.nextInt(0, 256)
    val b = Random.nextInt(0, 256)
    val argb = Color.rgb(r, g, b)
    return argb
}

fun colorName(argb: Int): String {
    val alpha = (argb shr 24) and 0xFF
    val red = (argb shr 16) and 0xFF
    val green = (argb shr 8) and 0xFF
    val blue = argb and 0xFF

    // Handle transparency
    if (alpha < 50) return "Transparent"
    if (alpha < 200) return "Semi-transparent " + getOpaqueColorName(red, green, blue)

    return getOpaqueColorName(red, green, blue)
}

private fun getOpaqueColorName(r: Int, g: Int, b: Int): String {
    // Convert to HSL for better color perception
    val hsl = rgbToHsl(r, g, b)
    val hue = hsl[0]
    val saturation = hsl[1]
    val lightness = hsl[2]

    // Gray/Black/White detection
    if (saturation < 0.1) {
        return when {
            lightness > 0.95 -> "White"
            lightness > 0.85 -> "Off-white"
            lightness > 0.75 -> "Light Gray"
            lightness > 0.5 -> "Gray"
            lightness > 0.25 -> "Dark Gray"
            lightness > 0.1 -> "Charcoal"
            else -> "Black"
        }
    }

    // Color naming based on hue and saturation
    return when {
        hue < 15 -> if (saturation > 0.7) "Red" else if (lightness > 0.7) "Light Red" else "Dark Red"
        hue < 45 -> "Orange"
        hue < 65 -> "Yellow"
        hue < 160 -> if (saturation > 0.6) "Green" else "Olive"
        hue < 195 -> "Cyan"
        hue < 260 -> if (saturation > 0.7) "Blue" else if (lightness > 0.7) "Light Blue" else "Dark Blue"
        hue < 295 -> "Purple"
        hue < 330 -> if (saturation > 0.7) "Magenta" else "Mauve"
        else -> "Red"
    }
}

private fun rgbToHsl(r: Int, g: Int, b: Int): FloatArray {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f

    val max = maxOf(rf, gf, bf)
    val min = minOf(rf, gf, bf)
    var h: Float
    val s: Float
    val l = (max + min) / 2f

    if (max == min) {
        h = 0f
        s = 0f
    } else {
        val d = max - min
        s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        h = when (max) {
            rf -> (gf - bf) / d + (if (gf < bf) 6f else 0f)
            gf -> (bf - rf) / d + 2f
            else -> (rf - gf) / d + 4f
        }
        h *= 60f
    }

    return floatArrayOf(h, s, l)
}
