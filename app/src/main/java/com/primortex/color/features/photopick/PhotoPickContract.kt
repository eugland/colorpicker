package com.primortex.color.features.photopick

import com.primortex.color.app.PickedColor

data class PhotoPickUiState(
    val pickedColor: PickedColor,
    val recents: List<PickedColor>,
    val palette: List<PickedColor>,
    val frozen: Boolean,
    val detailPick: PickedColor? = null
)

sealed interface PhotoPickUiAction {
    data object ToggleFreeze : PhotoPickUiAction
    data object ShowCurrentDetails : PhotoPickUiAction
    data class ShowDetails(val pick: PickedColor) : PhotoPickUiAction
    data object DismissDetails : PhotoPickUiAction
    data class SampleColor(val argb: Int) : PhotoPickUiAction
    data object AddCurrentToPalette : PhotoPickUiAction
    data object SavePalette : PhotoPickUiAction
}

sealed interface PhotoPickEffect {
    data class ShowMessage(val message: String) : PhotoPickEffect
    data class OpenPalette(val id: String, val edit: Boolean) : PhotoPickEffect
}
