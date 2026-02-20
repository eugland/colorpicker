package com.primortex.color.features.themeselection

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.primortex.color.service.SettingsService

@Composable
fun ThemeSelectionRoute(
    innerPadding: PaddingValues,
    settingsService: SettingsService,
    onBack: () -> Unit
) {
    val selectedTheme by settingsService.themeMode.collectAsStateWithLifecycle()

    ThemeSelectionScreen(
        selectedTheme = selectedTheme,
        innerPadding = innerPadding,
        onBack = onBack,
        onSelectTheme = settingsService::setThemeMode
    )
}
