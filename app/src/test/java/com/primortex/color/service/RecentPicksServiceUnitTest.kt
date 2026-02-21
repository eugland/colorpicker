package com.primortex.color.service

import com.primortex.color.analytics.AnalyticsClient
import com.primortex.color.analytics.AnalyticsTracker
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class RecentPicksServiceUnitTest {

    @Test
    fun addPick_deduplicatesAndPlacesMostRecentFirst() {
        val events = mutableListOf<String>()
        val service = RecentPicksService(
            context = RuntimeEnvironment.getApplication(),
            analyticsTracker = AnalyticsTracker(testAnalyticsClient(events))
        )
        waitForInit()
        service.clear()

        val red = PickedColor(0xFFFF0000.toInt(), "Red")
        val blue = PickedColor(0xFF0000FF.toInt(), "Blue")

        service.addPick(red, source = "unit")
        service.addPick(blue, source = "unit")
        service.addPick(red, source = "unit")

        waitUntil { service.history.value == listOf(red, blue) }
        assertEquals(listOf(red, blue), service.history.value)
        assertTrue(events.contains("picked"))
    }

    @Test
    fun toggleSaved_addsAndRemovesSavedColor() {
        val service = RecentPicksService(
            context = RuntimeEnvironment.getApplication(),
            analyticsTracker = AnalyticsTracker(testAnalyticsClient(mutableListOf()))
        )
        waitForInit()
        service.clearSaved()

        val green = PickedColor(0xFF00FF00.toInt(), "Green")
        service.toggleSaved(green, isCurrentlySaved = false)
        waitUntil { service.saved.value == listOf(green) }
        assertEquals(listOf(green), service.saved.value)

        service.toggleSaved(green, isCurrentlySaved = true)
        waitUntil { service.saved.value.isEmpty() }
        assertTrue(service.saved.value.isEmpty())
    }

    @Test
    fun clear_andClearSaved_emptyLists() {
        val service = RecentPicksService(
            context = RuntimeEnvironment.getApplication(),
            analyticsTracker = AnalyticsTracker(testAnalyticsClient(mutableListOf()))
        )
        waitForInit()

        service.clear()
        service.clearSaved()

        waitUntil { service.history.value.isEmpty() && service.saved.value.isEmpty() }
        assertFalse(service.history.value.isNotEmpty())
        assertFalse(service.saved.value.isNotEmpty())
    }

    private fun waitForInit() {
        Thread.sleep(300)
    }

    private fun waitUntil(timeoutMs: Long = 2000, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw AssertionError("Timed out waiting for condition")
            }
            Thread.sleep(25)
        }
    }

    private fun testAnalyticsClient(events: MutableList<String>) = object : AnalyticsClient {
        override fun logColorPicked(pick: PickedColor, source: String) { events += "picked" }
        override fun logColorSaved(pick: PickedColor, action: String) = Unit
        override fun logPaletteCreated(palette: Palette, source: String) = Unit
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
