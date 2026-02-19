package com.primortex.color.app

import androidx.compose.runtime.staticCompositionLocalOf
import com.primortex.color.service.ColorDetailsService
import com.primortex.color.service.ColorService
import com.primortex.color.service.PaletteSelectionStore
import com.primortex.color.service.PaletteService
import com.primortex.color.service.RecentPicksService
import com.primortex.color.service.SettingsService

val LocalColorService = staticCompositionLocalOf<ColorService> {
    error("ColorService not provided")
}

val LocalColorDetailsService = staticCompositionLocalOf<ColorDetailsService> {
    error("ColorDetailsService not provided")
}

val LocalPaletteSelectionStore = staticCompositionLocalOf<PaletteSelectionStore> {
    error("PaletteSelectionStore not provided")
}

val LocalPaletteService = staticCompositionLocalOf<PaletteService> {
    error("PaletteService not provided")
}

val LocalRecentPicksService = staticCompositionLocalOf<RecentPicksService> {
    error("RecentPicksService not provided")
}

val LocalSettingsService = staticCompositionLocalOf<SettingsService> {
    error("SettingsService not provided")
}
