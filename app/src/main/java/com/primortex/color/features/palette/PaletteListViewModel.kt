package com.primortex.color.features.palette

import android.util.Log
import androidx.lifecycle.ViewModel
import com.primortex.color.app.Palette
import com.primortex.color.service.PaletteSelectionStore
import com.primortex.color.service.PaletteService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class PaletteListViewModel @Inject constructor(
    private val paletteService: PaletteService,
    private val paletteSelectionStore: PaletteSelectionStore
) : ViewModel() {
    val savedPalettes: StateFlow<List<Palette>> = paletteService.palettes

    fun selectPalette(palette: Palette) {
        Log.d("PaletteFlow", "selectPalette source=palette_list id=${palette.id} name=${palette.name}")
        paletteSelectionStore.select(palette)
    }

    fun clearPalettes() {
        paletteService.clear()
    }
}
