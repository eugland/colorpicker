package com.primortex.color.features.colordetails

import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.data.enums.PaletteSchemeType
import com.primortex.color.service.ColorDetails

data class PaletteSchemeUiModel(val type: PaletteSchemeType, val colors: List<Int>)

data class ColorDetailsUiState(
    val details: ColorDetails,
    val displayName: String,
    val pickedColor: PickedColor,
    val isSaved: Boolean,
    val paletteSchemes: List<PaletteSchemeUiModel>,
    val palettes: List<Palette>,
    val showPalettePicker: Boolean = false
)

sealed interface ColorDetailsUiAction {
    data object CopyHexClicked : ColorDetailsUiAction
    data object ToggleSavedClicked : ColorDetailsUiAction
    data object ShowPalettePicker : ColorDetailsUiAction
    data object DismissPalettePicker : ColorDetailsUiAction
    data class AddColorToPalette(val paletteId: String) : ColorDetailsUiAction
    data object CreatePaletteFromColor : ColorDetailsUiAction
    data object OpenTintsPalette : ColorDetailsUiAction
    data object OpenShadesPalette : ColorDetailsUiAction
    data object OpenTonesPalette : ColorDetailsUiAction
    data class OpenPaletteScheme(val schemeType: PaletteSchemeType) : ColorDetailsUiAction
}

sealed interface ColorDetailsEffect {
    data class CopyHex(val hex: String) : ColorDetailsEffect
    data class ShowMessage(val message: String) : ColorDetailsEffect
    data class OpenPalette(val id: String, val edit: Boolean) : ColorDetailsEffect
}

