package com.primortex.color.service

import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaletteSelectionStoreUnitTest {
    @Test
    fun selectAndClear_updatesStateFlow() {
        val store = PaletteSelectionStore()
        val palette = Palette(
            id = "p1",
            name = "Warm",
            colors = listOf(PickedColor(0xFFFF0000.toInt(), "Red"))
        )

        assertNull(store.selected.value)
        store.select(palette)
        assertEquals(palette, store.selected.value)
        store.clear()
        assertNull(store.selected.value)
    }
}
