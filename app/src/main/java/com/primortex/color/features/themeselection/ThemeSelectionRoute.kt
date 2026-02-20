package com.primortex.color.features.themeselection

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ThemeSelectionRoute(
    innerPadding: PaddingValues,
    onBack: () -> Unit
) {
    val viewModel: ThemeSelectionViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ThemeSelectionScreen(
        uiState = uiState,
        innerPadding = innerPadding,
        onBack = onBack,
        onAction = viewModel::onAction
    )
}
