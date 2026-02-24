package com.primortex.color.features.swatchlist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.primortex.color.app.PickedColor
import com.primortex.color.data.enums.SwatchListType
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SwatchListRoute(
    innerPadding: PaddingValues,
    type: SwatchListType,
    onBack: () -> Unit,
    onOpenColorDetail: (PickedColor) -> Unit
) {
    val viewModel: SwatchListViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(type) {
        viewModel.onAction(SwatchListUiAction.BindType(type))
    }

    LaunchedEffect(viewModel, onOpenColorDetail) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is SwatchListEffect.OpenColorDetail -> onOpenColorDetail(effect.pick)
            }
        }
    }

    SwatchListScreen(
        uiState = uiState,
        innerPadding = innerPadding,
        onBack = onBack,
        onAction = viewModel::onAction
    )
}
