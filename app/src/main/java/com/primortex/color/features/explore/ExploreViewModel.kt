package com.primortex.color.features.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primortex.color.service.SettingsService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ExploreViewModel @Inject constructor(
    settingsService: SettingsService
) : ViewModel() {
    val uiState: StateFlow<ExploreUiState> = combine(
        settingsService.themeMode,
        settingsService.appLanguage,
        settingsService.crosshairSize,
        settingsService.crosshairShape,
        settingsService.pickerSensitivity
    ) { themeMode, appLanguage, crosshairSize, crosshairShape, crosshairSensitivity ->
        ExploreUiState(
            themeMode = themeMode,
            appLanguage = appLanguage,
            crosshairSize = crosshairSize,
            crosshairShape = crosshairShape,
            crosshairSensitivity = crosshairSensitivity
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExploreUiState(
            themeMode = settingsService.themeMode.value,
            appLanguage = settingsService.appLanguage.value,
            crosshairSize = settingsService.crosshairSize.value,
            crosshairShape = settingsService.crosshairShape.value,
            crosshairSensitivity = settingsService.pickerSensitivity.value
        )
    )
}
