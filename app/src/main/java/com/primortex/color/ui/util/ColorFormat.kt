package com.primortex.color.ui.util

import android.graphics.Color
import android.graphics.ImageFormat
import androidx.camera.core.ImageProxy
import androidx.core.graphics.ColorUtils
import kotlin.random.Random

fun argbToHex(argb: Int): String {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = (argb) and 0xFF
    return String.format("#%02X%02X%02X", r, g, b)
}

fun argbToRgbString(argb: Int): String {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return "RGB($r, $g, $b)"
}

fun argbToHslString(argb: Int): String {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    val hsl = FloatArray(3)
    ColorUtils.RGBToHSL(r, g, b, hsl)
    val h = hsl[0].toInt()
    val s = (hsl[1] * 100).toInt()
    val l = (hsl[2] * 100).toInt()
    return "HSL($h°, $s%, $l%)"
}


fun sampleCenterArgb(image: ImageProxy): Int? {
    if (image.format != ImageFormat.YUV_420_888) return null
    val w = image.width
    val h = image.height
    val x = w / 2
    val y = h / 2

    val yPlane = image.planes[0]
    val uPlane = image.planes[1]
    val vPlane = image.planes[2]

    val yBuf = yPlane.buffer
    val uBuf = uPlane.buffer
    val vBuf = vPlane.buffer

    val yRowStride = yPlane.rowStride
    val yPixelStride = yPlane.pixelStride

    val uRowStride = uPlane.rowStride
    val uPixelStride = uPlane.pixelStride

    val vRowStride = vPlane.rowStride
    val vPixelStride = vPlane.pixelStride

    val yIndex = yRowStride * y + yPixelStride * x
    val uvX = x / 2
    val uvY = y / 2
    val uIndex = uRowStride * uvY + uPixelStride * uvX
    val vIndex = vRowStride * uvY + vPixelStride * uvX

    if (yIndex >= yBuf.limit() || uIndex >= uBuf.limit() || vIndex >= vBuf.limit()) return null

    val Y = (yBuf.get(yIndex).toInt() and 0xFF)
    val U = (uBuf.get(uIndex).toInt() and 0xFF)
    val V = (vBuf.get(vIndex).toInt() and 0xFF)

    val yf = Y.toFloat()
    val uf = (U - 128).toFloat()
    val vf = (V - 128).toFloat()

    var r = (yf + 1.402f * vf).toInt()
    var g = (yf - 0.344136f * uf - 0.714136f * vf).toInt()
    var b = (yf + 1.772f * uf).toInt()

    r = r.coerceIn(0, 255)
    g = g.coerceIn(0, 255)
    b = b.coerceIn(0, 255)

    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
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