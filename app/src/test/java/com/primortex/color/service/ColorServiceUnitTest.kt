package com.primortex.color.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ColorServiceUnitTest {

    @Test
    fun buildSnapshot_filtersInvalidBlankAndDuplicateEntries() {
        val service = ColorService(
            listOf(
                ColorSeed(name = "ValidRed", hex = "#FF0000"),
                ColorSeed(name = "  ", hex = "#00FF00"),
                ColorSeed(name = "BrokenHex", hex = "#GGGGGG"),
                ColorSeed(name = "validred", hex = "#FF0000")
            )
        )

        val all = service.allColors()
        assertEquals(1, all.size)
        assertEquals("ValidRed", all.first().name)
        assertEquals("#FF0000", service.hexFromName("ValidRed"))
        assertNull(service.hexFromName("BrokenHex"))
    }

    @Test
    fun hexFromName_isCaseInsensitiveAndTrimmed() {
        val service = ColorService(
            listOf(
                ColorSeed(name = "Ocean Blue", hex = "#1E90FF")
            )
        )

        assertEquals("#1E90FF", service.hexFromName("ocean blue"))
        assertEquals("#1E90FF", service.hexFromName("  OCEAN BLUE "))
    }

    @Test
    fun setColors_replacesCatalogAndClearsLookupState() {
        val service = ColorService(
            listOf(ColorSeed(name = "OldColor", hex = "#112233"))
        )
        assertEquals("OldColor", service.nameFromArgb(0xFF112233.toInt()))

        service.setColors(listOf(ColorSeed(name = "NewColor", hex = "#445566")))

        assertNull(service.hexFromName("OldColor"))
        assertEquals("#445566", service.hexFromName("NewColor"))
        assertEquals("NewColor", service.nameFromArgb(0xFF445566.toInt()))
    }

    @Test
    fun search_prioritizesStartsWithBeforeContains_andHonorsLimit() {
        val service = ColorService(
            listOf(
                ColorSeed(name = "Blue", hex = "#0000FF"),
                ColorSeed(name = "BlueGray", hex = "#6699CC"),
                ColorSeed(name = "DarkBlue", hex = "#00008B"),
                ColorSeed(name = "NotRelated", hex = "#123456")
            )
        )

        val results = service.search("blue", limit = 2)

        assertEquals(2, results.size)
        assertEquals("Blue", results[0].name)
        assertEquals("BlueGray", results[1].name)
    }

    @Test
    fun search_removesDuplicateArgbEntries() {
        val service = ColorService(
            listOf(
                ColorSeed(name = "Sky Blue", hex = "#87CEEB"),
                ColorSeed(name = "Light Sky", hex = "#87CEEB")
            )
        )

        val results = service.search("sky", limit = 10)

        assertEquals(1, results.size)
        assertEquals(0xFF87CEEB.toInt(), results.first().argb)
    }

    @Test
    fun nameFromArgb_returnsNearestColorName_andLocalNameMatches() {
        val service = ColorService(
            listOf(
                ColorSeed(name = "Red", hex = "#FF0000"),
                ColorSeed(name = "Green", hex = "#00FF00"),
                ColorSeed(name = "Blue", hex = "#0000FF")
            )
        )

        val nearest = service.nameFromArgb(0xFFFF1010.toInt())
        assertEquals("Red", nearest)
        assertEquals(nearest, service.localNameFromArgb(0xFFFF1010.toInt()))
    }

    @Test
    fun search_blankQuery_returnsEmpty() {
        val service = ColorService(
            listOf(ColorSeed(name = "Blue", hex = "#0000FF"))
        )

        assertTrue(service.search("   ").isEmpty())
    }

    @Test
    fun emptyCatalog_returnsFallbackRecord() {
        val service = ColorService(emptyList())

        assertEquals("", service.nameFromArgb(0xFF123456.toInt()))
        assertTrue(service.allColors().isEmpty())
        assertNull(service.hexFromName("anything"))
    }
}
