package com.primortex.color.features.palette

import android.util.Log
import androidx.lifecycle.ViewModel
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.service.ColorDetailsService
import com.primortex.color.service.ColorService
import com.primortex.color.service.PaletteSelectionStore
import com.primortex.color.service.PaletteService
import com.primortex.color.service.RecentPicksService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

private const val TAG = "PaletteFlow"

object ColorQueryResolver {

    fun search(
        query: String,
        limit: Int = 10,
        nearestName: (Int) -> String,
        searchByName: (String, Int) -> List<PickedColor>
    ): List<PickedColor> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        if (isHex(q)) {
            val rgb = q.removePrefix("#").toLong(16).toInt() and 0x00FFFFFF
            val argb = (0xFF shl 24) or rgb
            val nearest = nearestName(argb)
            Log.d("ColorSearch", "HEX pressed, top=${nearest} #${argb.toString(16)}")
            return listOf(PickedColor(argb = argb, name = nearest))
        }

        return searchByName(q, limit)
    }

    private fun isHex(input: String): Boolean {
        val s = input.removePrefix("#")
        return s.length in setOf(3, 6, 8) &&
                s.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
    }
}

@HiltViewModel
class PaletteViewModel @Inject constructor(
    private val colorService: ColorService,
    private val paletteService: PaletteService,
    private val recentPicksService: RecentPicksService,
    val colorDetailsService: ColorDetailsService,
    private val paletteSelectionStore: PaletteSelectionStore
) : ViewModel() {
    val recents: StateFlow<List<PickedColor>> = recentPicksService.history
    val savedColors: StateFlow<List<PickedColor>> = recentPicksService.saved
    val savedPalettes: StateFlow<List<Palette>> = paletteService.palettes

    fun selectPalette(palette: Palette) {
        Log.d(TAG, "selectPalette source=palette_tab id=${palette.id} name=${palette.name}")
        paletteSelectionStore.select(palette)
    }

    suspend fun searchSuggestions(query: String, limit: Int = 10): List<PickedColor> {
        return withContext(Dispatchers.Default) {
            ColorQueryResolver.search(
                query = query,
                limit = limit,
                nearestName = colorService::localNameFromArgb,
                searchByName = colorService::search
            )
        }
    }

    fun addSearchPick(pick: PickedColor) {
        recentPicksService.addPick(pick, source = "palette_search")
    }

    fun detailsFor(argb: Int) = colorDetailsService.details(argb, similarLimit = 10)

    fun clearPalettes() {
        paletteService.clear()
    }
}
