package com.primortex.color.features.explore

import com.primortex.color.data.enums.AppLanguage
import com.primortex.color.data.enums.CrosshairShape
import com.primortex.color.data.enums.CrosshairSize
import com.primortex.color.data.enums.PickerSensitivity
import com.primortex.color.data.enums.ThemeMode

data class ExploreUiState(
    val themeMode: ThemeMode,
    val appLanguage: AppLanguage,
    val crosshairSize: CrosshairSize,
    val crosshairShape: CrosshairShape,
    val crosshairSensitivity: PickerSensitivity
)

sealed interface ExploreUiAction {
    data object OpenThemeSettings : ExploreUiAction
    data object OpenLanguageSettings : ExploreUiAction
    data object OpenCrosshairSettings : ExploreUiAction
    data object OpenCopyright : ExploreUiAction
    data object OpenPrivacy : ExploreUiAction
    data object OpenTerms : ExploreUiAction
    data object OpenUsage : ExploreUiAction
    data object OpenFeedbackForm : ExploreUiAction
    data object OpenRateUs : ExploreUiAction
    data object ShareApp : ExploreUiAction
}

sealed interface ExploreDestination {
    data object ThemeSettings : ExploreDestination
    data object LanguageSettings : ExploreDestination
    data object CrosshairSettings : ExploreDestination
    data object Copyright : ExploreDestination
    data object Privacy : ExploreDestination
    data object Terms : ExploreDestination
    data object Usage : ExploreDestination
}
