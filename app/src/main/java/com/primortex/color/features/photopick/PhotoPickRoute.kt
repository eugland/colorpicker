package com.primortex.color.features.photopick

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.primortex.color.app.PickedColor
import com.primortex.color.ui.LocalSnackbarController
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PhotoPickRoute(
    photoUri: String,
    onBack: () -> Unit,
    onOpenPalette: (String, Boolean) -> Unit,
    onOpenColorDetail: (PickedColor) -> Unit
) {
    val viewModel: PhotoPickViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarController = LocalSnackbarController.current

    LaunchedEffect(viewModel, snackbarController, onOpenPalette) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is PhotoPickEffect.ShowMessage -> snackbarController.showMessage(effect.message)
                is PhotoPickEffect.OpenPalette -> onOpenPalette(effect.id, effect.edit)
            }
        }
    }

    PhotoPickScreen(
        uiState = uiState,
        photoUri = photoUri,
        onBack = onBack,
        onAction = viewModel::onAction,
        detailsFor = viewModel::detailsFor,
        onOpenColorDetail = onOpenColorDetail
    )
}
