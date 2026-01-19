package com.primortex.color.data.repo

import com.primortex.color.app.Palette

interface PaletteRepository {
    suspend fun loadPalettes(): List<Palette>
    suspend fun upsertPalette(palette: Palette)
    suspend fun replacePalettes(palettes: List<Palette>)
    suspend fun deletePalette(id: String)
    suspend fun clear()
}
