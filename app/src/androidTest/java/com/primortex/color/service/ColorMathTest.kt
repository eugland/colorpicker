package com.primortex.color.service

import android.graphics.Color as AColor
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ColorMathTest {
    private fun assertClose(actual: Float, expected: Float, tolerance: Float = 0.01f) {
        assertTrue("Expected $actual to be within $tolerance of $expected", abs(actual - expected) <= tolerance)
    }

    @Test
    fun argbToRgbExtractsChannels() {
        val rgb = argbToRgb(0xFFA1B2C3.toInt())
        assertEquals(0xA1, rgb.r)
        assertEquals(0xB2, rgb.g)
        assertEquals(0xC3, rgb.b)
    }

    @Test
    fun argbToHsvMatchesAndroidConversion() {
        val hsv = argbToHsv(0xFFFF0000.toInt())
        assertClose(hsv.h, 0f, 0.01f)
        assertClose(hsv.s, 1f, 0.001f)
        assertClose(hsv.v, 1f, 0.001f)
    }

    @Test
    fun rgbToHslHandlesPrimaryAndGrayColors() {
        val redHsl = rgbToHsl(Rgb(255, 0, 0))
        assertClose(redHsl.h, 0f, 0.01f)
        assertClose(redHsl.s, 1f, 0.001f)
        assertClose(redHsl.l, 0.5f, 0.001f)

        val grayHsl = rgbToHsl(Rgb(128, 128, 128))
        assertClose(grayHsl.h, 0f, 0.01f)
        assertClose(grayHsl.s, 0f, 0.001f)
        assertClose(grayHsl.l, 128f / 255f, 0.001f)
    }

    @Test
    fun luminanceMatchesKnownValues() {
        assertClose(luminance(Rgb(0, 0, 0)), 0f, 0.0001f)
        assertClose(luminance(Rgb(255, 255, 255)), 1f, 0.0001f)
        assertClose(luminance(Rgb(255, 0, 0)), 0.2126f, 0.001f)
    }

    @Test
    fun hueShiftRotatesHueAndWraps() {
        val shiftedGreen = hueShift(0xFFFF0000.toInt(), 120f)
        assertEquals(0xFF00FF00.toInt(), shiftedGreen)

        val shiftedWrap = hueShift(0xFF0000FF.toInt(), 30f)
        val hsv = FloatArray(3)
        AColor.RGBToHSV(0, 0, 255, hsv)
        hsv[0] = (hsv[0] + 30f) % 360f
        val expected = AColor.HSVToColor(0xFF, hsv)
        assertEquals(expected, shiftedWrap)
    }

    @Test
    fun rgbDistanceSqCalculatesSquaredDistance() {
        val distance = rgbDistanceSq(Rgb(10, 20, 30), Rgb(13, 24, 26))
        assertEquals(3 * 3 + 4 * 4 + (-4) * (-4), distance)
    }

    @Test
    fun rgbDistSqCalculatesSquaredDistanceFromArgb() {
        val distance = rgbDistSq(0xFF000000.toInt(), 0xFF010203.toInt())
        assertEquals(1 * 1 + 2 * 2 + 3 * 3, distance)
    }

    @Test
    fun argbToHexFormatsRgb() {
        assertEquals("#1A2B3C", argbToHex(0xFF1A2B3C.toInt()))
    }

    @Test
    fun hexToArgbParsesAndValidates() {
        assertEquals(0xFF1A2B3C.toInt(), hexToArgb("#1a2b3c"))
        assertThrows(IllegalArgumentException::class.java) {
            hexToArgb("#XYZ")
        }
    }

    @Test
    fun normalizeArgbForcesOpaqueAlpha() {
        assertEquals(0xFF112233.toInt(), normalizeArgb(0x00112233))
        assertEquals(0xFF445566.toInt(), normalizeArgb(0xFF445566.toInt()))
    }

    @Test
    fun normalizeNameTrimsAndLowercases() {
        assertEquals("cool blue", normalizeName("  Cool Blue  "))
    }

    @Test
    fun argbToLabMatchesReferenceValues() {
        val whiteLab = argbToLab(0xFFFFFFFF.toInt())
        assertClose(whiteLab.l, 100f, 0.01f)
        assertClose(whiteLab.a, 0f, 0.02f)
        assertClose(whiteLab.b, 0f, 0.02f)

        val blackLab = argbToLab(0xFF000000.toInt())
        assertClose(blackLab.l, 0f, 0.01f)
        assertClose(blackLab.a, 0f, 0.02f)
        assertClose(blackLab.b, 0f, 0.02f)

        val redLab = argbToLab(0xFFFF0000.toInt())
        assertClose(redLab.l, 53.23f, 0.05f)
        assertClose(redLab.a, 80.11f, 0.05f)
        assertClose(redLab.b, 67.22f, 0.05f)
    }

    @Test
    fun deltaE76ComputesDistance() {
        assertClose(deltaE76(Lab(0f, 0f, 0f), Lab(0f, 3f, 4f)), 5f, 0.0001f)
    }

    @Test
    fun argbToHslStringFormatsText() {
        assertEquals("HSL(120°, 100%, 50%)", argbToHslString(0xFF00FF00.toInt()))
    }
}
