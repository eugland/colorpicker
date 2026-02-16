package com.primortex.color.screens

import com.primortex.color.data.enums.PaletteSchemeType

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

