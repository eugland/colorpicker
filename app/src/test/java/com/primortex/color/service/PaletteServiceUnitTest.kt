package com.primortex.color.service

import com.primortex.color.analytics.AnalyticsClient
import com.primortex.color.analytics.AnalyticsTracker
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class PaletteServiceUnitTest {

    @Test
    fun create_deduplicatesByHash_andKeepsUniqueColors() {
        val events = mutableListOf<String>()
        val service = PaletteService(
            context = RuntimeEnvironment.getApplication(),
            analyticsTracker = AnalyticsTracker(testAnalyticsClient(events))
        )
        waitForInit()
        service.clear()

        val colors = listOf(
            PickedColor(0xFFAA0000.toInt(), "A"),
            PickedColor(0xFFAA0000.toInt(), "A-dup")
        )

        val created = service.create(name = "", colors = colors, creationSource = "unit")
        val duplicate = service.create(name = "another", colors = colors, creationSource = "unit")

        assertEquals("Palette", created.name)
        assertEquals(1, created.colors.size)
        assertEquals(created.id, duplicate.id)
        assertEquals(1, service.palettes.value.size)
        assertTrue(events.contains("palette_created"))
    }

    @Test
    fun toggleSaved_movesPreviewPaletteIntoSavedAndBack() {
        val service = PaletteService(
            context = RuntimeEnvironment.getApplication(),
            analyticsTracker = AnalyticsTracker(testAnalyticsClient(mutableListOf()))
        )
        waitForInit()
        service.clear()

        val preview = service.create(
            name = "Preview",
            colors = listOf(PickedColor(0xFF00AA00.toInt(), "Green")),
            saveOnCreate = false,
            creationSource = "unit"
        )

        assertEquals(1, service.previewPalettes.value.size)
        assertEquals(0, service.palettes.value.size)

        service.toggleSaved(preview)
        assertEquals(0, service.previewPalettes.value.size)
        assertEquals(1, service.palettes.value.size)

        service.toggleSaved(preview)
        assertEquals(0, service.palettes.value.size)
    }

    @Test
    fun update_andDelete_modifySavedPalettes() {
        val service = PaletteService(
            context = RuntimeEnvironment.getApplication(),
            analyticsTracker = AnalyticsTracker(testAnalyticsClient(mutableListOf()))
        )
        waitForInit()
        service.clear()

        val palette = service.create(
            name = "Initial",
            colors = listOf(PickedColor(0xFF0000AA.toInt(), "Blue")),
            creationSource = "unit"
        )

        service.update(
            id = palette.id,
            name = "Updated",
            colors = listOf(
                PickedColor(0xFF0000AA.toInt(), "Blue"),
                PickedColor(0xFF0000AA.toInt(), "Blue-dup")
            ),
            tags = listOf("tag"),
            note = "note"
        )

        val updated = service.palettes.value.single()
        assertEquals("Updated", updated.name)
        assertEquals(1, updated.colors.size)
        assertEquals(listOf("tag"), updated.tags)
        assertEquals("note", updated.note)

        service.delete(updated.id)
        assertTrue(service.palettes.value.isEmpty())
    }

    private fun waitForInit() {
        Thread.sleep(300)
    }

    private fun testAnalyticsClient(events: MutableList<String>) = object : AnalyticsClient {
        override fun logColorPicked(pick: PickedColor, source: String) = Unit
        override fun logColorSaved(pick: PickedColor, action: String) = Unit
        override fun logPaletteCreated(palette: Palette, source: String) { events += "palette_created" }
        override fun logPaletteUpdated(palette: Palette) = Unit
        override fun logPaletteDeleted(paletteId: String) = Unit
        override fun logRecentsCleared() = Unit
        override fun logSavedCleared() = Unit
        override fun logFirstColorPick(pick: PickedColor) = Unit
        override fun logFirstColorSaved(pick: PickedColor) = Unit
        override fun logFirstPaletteCreated(palette: Palette) = Unit
        override fun logFirstUse() = Unit
        override fun logScreenView(screenName: String, screenClass: String) = Unit
    }
}
