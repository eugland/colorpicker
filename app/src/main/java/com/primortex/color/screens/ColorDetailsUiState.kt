package com.primortex.color.screens

import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.data.enums.PaletteSchemeType
import com.primortex.color.service.ColorDetails

data class PaletteSchemeUiModel(
    val type: PaletteSchemeType,
    val colors: List<Int>
)

data class ColorDetailsUiState(
    val details: ColorDetails,
    val displayName: String,
    val pickedColor: PickedColor,
    val isSaved: Boolean,
    val paletteSchemes: List<PaletteSchemeUiModel>,
    val palettes: List<Palette>,
    val showPalettePicker: Boolean = false
)

