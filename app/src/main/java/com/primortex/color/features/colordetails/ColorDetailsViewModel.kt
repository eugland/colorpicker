package com.primortex.color.features.colordetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primortex.color.R
import com.primortex.color.app.PickedColor
import com.primortex.color.i18n.AppStrings
import com.primortex.color.service.ColorDetails
import com.primortex.color.service.ColorDetailsService
import com.primortex.color.service.RecentPicksService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ColorDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val colorDetailsService: ColorDetailsService,
    private val recentPicksService: RecentPicksService
) : ViewModel() {
    private val argb: Int = savedStateHandle.get<Int>("argb") ?: 0
    private val nameHint: String? = savedStateHandle.get<String>("name")
        ?.takeIf { it.isNotBlank() }
        ?.let { encoded -> runCatching { java.net.URLDecoder.decode(encoded, "UTF-8") }.getOrDefault(encoded) }

    private val details: ColorDetails = colorDetailsService.details(argb, similarLimit = 10)
    private val displayName = details.name.ifBlank { nameHint?.takeIf { it.isNotBlank() } ?: AppStrings.get(R.string.color_label) }
    private val pickedColor = PickedColor(details.argb, displayName)
    private val _uiState = MutableStateFlow(buildUiState(savedColors = recentPicksService.saved.value))
    val uiState: StateFlow<ColorDetailsUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ColorDetailsEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<ColorDetailsEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            recentPicksService.saved.collect { savedColors ->
                _uiState.value = buildUiState(savedColors)
            }
        }
    }

    fun onAction(action: ColorDetailsUiAction) {
        when (action) {
            ColorDetailsUiAction.CopyHexClicked -> viewModelScope.launch {
                _effects.emit(ColorDetailsEffect.CopyHex(details.hex))
                _effects.emit(ColorDetailsEffect.ShowMessage(AppStrings.get(R.string.hex_copied)))
            }

            ColorDetailsUiAction.ToggleSavedClicked -> {
                recentPicksService.toggleSaved(
                    pick = pickedColor,
                    isCurrentlySaved = _uiState.value.isSaved
                )
                val message = if (_uiState.value.isSaved) AppStrings.get(R.string.removed_from_my_colors)
                else AppStrings.get(R.string.saved_to_my_colors)
                viewModelScope.launch { _effects.emit(ColorDetailsEffect.ShowMessage(message)) }
            }
        }
    }

    private fun buildUiState(savedColors: List<PickedColor>): ColorDetailsUiState {
        val isSaved = savedColors.any { it.argb == pickedColor.argb }
        return ColorDetailsUiState(
            details = details,
            displayName = displayName,
            pickedColor = pickedColor,
            isSaved = isSaved
        )
    }
}

