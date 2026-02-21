package com.primortex.color.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ColorDetailsServiceUnitTest {
    private val colorService = ColorService(
        listOf(
            ColorSeed("Red", "#FF0000"),
            ColorSeed("Green", "#00FF00"),
            ColorSeed("Blue", "#0000FF"),
            ColorSeed("White", "#FFFFFF"),
            ColorSeed("Black", "#000000")
        )
    )
    private val detailsService = ColorDetailsService(colorService)

    @Test
    fun details_computesCoreFieldsAndColorSchemes() {
        val details = detailsService.details(0xFFFF0000.toInt(), similarLimit = 3)

        assertEquals(0xFFFF0000.toInt(), details.argb)
        assertEquals("Red", details.name)
        assertEquals("#FF0000", details.hex)
        assertEquals(255, details.rgb.r)
        assertEquals(0xFFFFFFFF.toInt(), details.recommendedOnColor)
        assertEquals(1, details.complements.size)
        assertEquals(2, details.triads.size)
        assertEquals(2, details.analogous.size)
        assertEquals(2, details.splitComplements.size)
        assertEquals(3, details.tetrads.size)
        assertEquals(3, details.squares.size)
        assertEquals(5, details.tints.size)
        assertEquals(5, details.shades.size)
        assertEquals(5, details.tones.size)
    }

    @Test
    fun details_excludesBaseColorFromSimilarAndHonorsLimit() {
        val details = detailsService.details(0xFFFF0000.toInt(), similarLimit = 2)

        assertEquals(2, details.similarColors.size)
        assertTrue(details.similarColors.none { it.argb == 0xFFFF0000.toInt() })
    }
}
