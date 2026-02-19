package com.primortex.color.service

import com.primortex.color.app.Palette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ephemeral cross-screen selection for the currently opened palette.
 */
@Singleton
class PaletteSelectionStore {
    @Inject
    constructor()

    private val _selected = MutableStateFlow<Palette?>(null)
    val selected: StateFlow<Palette?> = _selected

    fun select(palette: Palette?) {
        _selected.value = palette
    }

    fun clear() {
        _selected.value = null
    }
}
