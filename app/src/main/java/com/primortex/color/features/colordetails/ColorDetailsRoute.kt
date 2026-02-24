package com.primortex.color.features.colordetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.primortex.color.ui.LocalSnackbarController
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ColorDetailsRoute(
    onBack: () -> Unit,
    onOpenPalette: (String, Boolean) -> Unit = { _, _ -> }
) {
    val viewModel: ColorDetailsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val snackbarController = LocalSnackbarController.current

    LaunchedEffect(viewModel, clipboard, snackbarController) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is ColorDetailsEffect.CopyHex -> clipboard.setText(AnnotatedString(effect.hex))
                is ColorDetailsEffect.ShowMessage -> snackbarController.showMessage(effect.message)
            }
        }
    }

    ColorDetailsScreen(uiState = uiState, onBack = onBack, onAction = viewModel::onAction)
}

