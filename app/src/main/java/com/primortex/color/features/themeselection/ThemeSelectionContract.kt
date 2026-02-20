package com.primortex.color.features.themeselection

import com.primortex.color.data.enums.ThemeMode

data class ThemeSelectionUiState(
    val selectedTheme: ThemeMode
)

sealed interface ThemeSelectionUiAction {
    data class SelectTheme(val mode: ThemeMode) : ThemeSelectionUiAction
}
