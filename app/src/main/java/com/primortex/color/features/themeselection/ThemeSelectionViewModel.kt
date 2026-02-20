package com.primortex.color.features.themeselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primortex.color.service.SettingsService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ThemeSelectionViewModel @Inject constructor(
    private val settingsService: SettingsService
) : ViewModel() {
    val uiState: StateFlow<ThemeSelectionUiState> = settingsService.themeMode
        .map { selected -> ThemeSelectionUiState(selectedTheme = selected) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeSelectionUiState(selectedTheme = settingsService.themeMode.value)
        )

    fun onAction(action: ThemeSelectionUiAction) {
        when (action) {
            is ThemeSelectionUiAction.SelectTheme -> settingsService.setThemeMode(action.mode)
        }
    }
}
