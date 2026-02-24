package com.primortex.color.features.colordetails

import com.primortex.color.app.PickedColor
import com.primortex.color.service.ColorDetails

data class ColorDetailsUiState(
    val details: ColorDetails,
    val displayName: String,
    val pickedColor: PickedColor,
    val isSaved: Boolean
)

sealed interface ColorDetailsUiAction {
    data object CopyHexClicked : ColorDetailsUiAction
    data object ToggleSavedClicked : ColorDetailsUiAction
}

sealed interface ColorDetailsEffect {
    data class CopyHex(val hex: String) : ColorDetailsEffect
    data class ShowMessage(val message: String) : ColorDetailsEffect
}

