package com.primortex.color.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.primortex.color.R
import com.primortex.color.app.PickedColor
import com.primortex.color.i18n.stringResource
import com.primortex.color.ui.LocalSnackbarService
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorDetailsScreen(
    argb: Int,
    nameHint: String? = null,
    onBack: () -> Unit,
    onOpenColorDetail: (PickedColor) -> Unit = {},
    onOpenPalette: (String, Boolean) -> Unit = { _, _ -> }
) {
    val viewModel: ColorDetailsViewModel = viewModel(
        key = "color_details_$argb:${nameHint.orEmpty()}",
        factory = ColorDetailsViewModel.factory(argb, nameHint)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val clipboard = LocalClipboardManager.current
    val snackbarService = LocalSnackbarService.current

    LaunchedEffect(viewModel, clipboard, snackbarService, onOpenPalette, onOpenColorDetail) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is ColorDetailsEffect.CopyHex -> {
                    clipboard.setText(AnnotatedString(effect.hex))
                }

                is ColorDetailsEffect.ShowMessage -> snackbarService.showMessage(effect.message)
                is ColorDetailsEffect.OpenPalette -> onOpenPalette(effect.id, effect.edit)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        uiState.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            ColorDetailsContent(
                uiState = uiState,
                onAction = viewModel::onAction
            )
        }
    }

    if (uiState.showPalettePicker) {
        PalettePickerDialog(
            palettes = uiState.palettes,
            onDismiss = { viewModel.onAction(ColorDetailsUiAction.DismissPalettePicker) },
            onSelectPalette = { id ->
                viewModel.onAction(ColorDetailsUiAction.AddColorToPalette(id))
            },
            onCreatePalette = { viewModel.onAction(ColorDetailsUiAction.CreatePaletteFromColor) }
        )
    }
}

