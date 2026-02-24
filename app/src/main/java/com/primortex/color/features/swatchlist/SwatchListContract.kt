package com.primortex.color.features.swatchlist

import com.primortex.color.app.PickedColor
import com.primortex.color.data.enums.SwatchListType

data class SwatchListUiState(
    val type: SwatchListType,
    val picks: List<PickedColor>,
    val filtered: List<PickedColor>,
    val query: String
)

sealed interface SwatchListUiAction {
    data class BindType(val type: SwatchListType) : SwatchListUiAction
    data class QueryChanged(val value: String) : SwatchListUiAction
    data object ClearQuery : SwatchListUiAction
    data object DeleteAll : SwatchListUiAction
    data class OpenColorDetail(val pick: PickedColor) : SwatchListUiAction
}

sealed interface SwatchListEffect {
    data class OpenColorDetail(val pick: PickedColor) : SwatchListEffect
}
