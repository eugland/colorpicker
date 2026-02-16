package com.primortex.color.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.primortex.color.R
import com.primortex.color.app.PickedColor
import com.primortex.color.data.enums.PaletteSchemeType
import com.primortex.color.i18n.AppStrings
import com.primortex.color.service.ColorDetails
import com.primortex.color.service.ColorDetailsService
import com.primortex.color.service.ColorServices
import com.primortex.color.service.PaletteService
import com.primortex.color.service.RecentPicksService
import com.primortex.color.service.argbToHex
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ColorDetailsViewModel(
    argb: Int,
    nameHint: String?
) : ViewModel() {

    private val details: ColorDetails = ColorDetailsService.details(argb, similarLimit = 10)
    private val displayName = details.name.ifBlank {
        nameHint?.takeIf { it.isNotBlank() } ?: AppStrings.get(R.string.color_label)
    }
    private val pickedColor = PickedColor(details.argb, displayName)
    private val showPalettePicker = MutableStateFlow(false)

    private val _uiState = MutableStateFlow(
        buildUiState(
            palettes = PaletteService.palettes.value,
            savedColors = RecentPicksService.saved.value,
            pickerVisible = false
        )
    )
    val uiState: StateFlow<ColorDetailsUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ColorDetailsEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<ColorDetailsEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                PaletteService.palettes,
                RecentPicksService.saved,
                showPalettePicker
            ) { palettes, savedColors, pickerVisible ->
                buildUiState(palettes, savedColors, pickerVisible)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onAction(action: ColorDetailsUiAction) {
        when (action) {
            ColorDetailsUiAction.CopyHexClicked -> {
                viewModelScope.launch {
                    _effects.emit(ColorDetailsEffect.CopyHex(details.hex))
                    _effects.emit(
                        ColorDetailsEffect.ShowMessage(
                            AppStrings.get(R.string.hex_copied)
                        )
                    )
                }
            }

            ColorDetailsUiAction.ToggleSavedClicked -> {
                RecentPicksService.toggleSaved(pickedColor)
                val message = if (_uiState.value.isSaved) {
                    AppStrings.get(R.string.removed_from_my_colors)
                } else {
                    AppStrings.get(R.string.saved_to_my_colors)
                }
                viewModelScope.launch {
                    _effects.emit(ColorDetailsEffect.ShowMessage(message))
                }
            }

            ColorDetailsUiAction.ShowPalettePicker -> {
                showPalettePicker.value = true
            }

            ColorDetailsUiAction.DismissPalettePicker -> {
                showPalettePicker.value = false
            }

            is ColorDetailsUiAction.AddColorToPalette -> {
                addToPalette(action.paletteId)
            }

            ColorDetailsUiAction.CreatePaletteFromColor -> {
                createPaletteFromColor()
            }

            ColorDetailsUiAction.OpenTintsPalette -> {
                openColorSetPalette(
                    suffix = AppStrings.get(R.string.tints_label),
                    colors = listOf(details.argb) + details.tints
                )
            }

            ColorDetailsUiAction.OpenShadesPalette -> {
                openColorSetPalette(
                    suffix = AppStrings.get(R.string.shades_label),
                    colors = listOf(details.argb) + details.shades
                )
            }

            ColorDetailsUiAction.OpenTonesPalette -> {
                openColorSetPalette(
                    suffix = AppStrings.get(R.string.tones_label),
                    colors = listOf(details.argb) + details.tones
                )
            }

            is ColorDetailsUiAction.OpenPaletteScheme -> {
                val scheme = _uiState.value.paletteSchemes.firstOrNull { it.type == action.schemeType }
                if (scheme != null) {
                    openColorSetPalette(
                        suffix = paletteSchemeLabel(action.schemeType),
                        colors = scheme.colors
                    )
                }
            }
        }
    }

    private fun buildUiState(
        palettes: List<com.primortex.color.app.Palette>,
        savedColors: List<PickedColor>,
        pickerVisible: Boolean
    ): ColorDetailsUiState {
        val isSaved = savedColors.any { it.argb == pickedColor.argb }
        return ColorDetailsUiState(
            details = details,
            displayName = displayName,
            pickedColor = pickedColor,
            isSaved = isSaved,
            paletteSchemes = listOf(
                PaletteSchemeUiModel(
                    type = PaletteSchemeType.Complementary,
                    colors = listOf(details.argb) + details.complements
                ),
                PaletteSchemeUiModel(
                    type = PaletteSchemeType.Analogous,
                    colors = listOf(details.argb) + details.analogous
                ),
                PaletteSchemeUiModel(
                    type = PaletteSchemeType.SplitComplementary,
                    colors = listOf(details.argb) + details.splitComplements
                ),
                PaletteSchemeUiModel(
                    type = PaletteSchemeType.Triadic,
                    colors = listOf(details.argb) + details.triads
                ),
                PaletteSchemeUiModel(
                    type = PaletteSchemeType.Tetradic,
                    colors = listOf(details.argb) + details.tetrads
                ),
                PaletteSchemeUiModel(
                    type = PaletteSchemeType.Square,
                    colors = listOf(details.argb) + details.squares
                )
            ),
            palettes = palettes,
            showPalettePicker = pickerVisible
        )
    }

    private fun addToPalette(paletteId: String) {
        val palette = _uiState.value.palettes.firstOrNull { it.id == paletteId } ?: return
        val message = when {
            palette.colors.any { it.argb == pickedColor.argb } ->
                AppStrings.get(R.string.already_in_palette)

            palette.colors.size >= 10 ->
                AppStrings.get(R.string.palette_full)

            else -> {
                PaletteService.update(
                    id = palette.id,
                    colors = palette.colors + pickedColor
                )
                showPalettePicker.value = false
                AppStrings.get(R.string.added_to_palette)
            }
        }
        viewModelScope.launch {
            _effects.emit(ColorDetailsEffect.ShowMessage(message))
        }
    }

    private fun createPaletteFromColor() {
        val newPaletteName = AppStrings.get(
            R.string.palette_name_with_color,
            pickedColor.name
        )
        val hasDuplicateName = _uiState.value.palettes.any { palette ->
            palette.name.equals(newPaletteName, ignoreCase = true)
        }

        val message = if (hasDuplicateName) {
            AppStrings.get(R.string.already_in_palette)
        } else {
            PaletteService.create(
                name = newPaletteName,
                colors = listOf(pickedColor),
                tags = listOf("details"),
                creationSource = "color_details"
            )
            showPalettePicker.value = false
            AppStrings.get(R.string.palette_saved)
        }
        viewModelScope.launch {
            _effects.emit(ColorDetailsEffect.ShowMessage(message))
        }
    }

    private fun openColorSetPalette(suffix: String, colors: List<Int>) {
        val saved = PaletteService.create(
            name = "$displayName $suffix",
            colors = colors.toPickedColors(),
            tags = listOf("details"),
            saveOnCreate = false,
            creationSource = "color_details"
        )
        ColorServices.selectedPalette = saved
        viewModelScope.launch {
            _effects.emit(ColorDetailsEffect.OpenPalette(saved.id, false))
        }
    }

    private fun paletteSchemeLabel(type: PaletteSchemeType): String {
        return when (type) {
            PaletteSchemeType.Complementary -> AppStrings.get(R.string.palette_scheme_complementary)
            PaletteSchemeType.Analogous -> AppStrings.get(R.string.palette_scheme_analogous)
            PaletteSchemeType.SplitComplementary -> AppStrings.get(R.string.palette_scheme_split_complementary)
            PaletteSchemeType.Triadic -> AppStrings.get(R.string.palette_scheme_triadic)
            PaletteSchemeType.Tetradic -> AppStrings.get(R.string.palette_scheme_tetradic)
            PaletteSchemeType.Square -> AppStrings.get(R.string.palette_scheme_square)
        }
    }

    companion object {
        fun factory(argb: Int, nameHint: String?): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ColorDetailsViewModel(argb, nameHint) as T
                }
            }
        }
    }
}

private fun List<Int>.toPickedColors(): List<PickedColor> {
    return distinct().map { argb ->
        val name = ColorServices.colors.localNameFromArgb(argb)
        PickedColor(argb, if (name.isBlank()) argbToHex(argb) else name)
    }
}

