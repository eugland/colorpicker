package com.primortex.color.features.colordetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primortex.color.R
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.data.enums.PaletteSchemeType
import com.primortex.color.i18n.AppStrings
import com.primortex.color.service.ColorDetails
import com.primortex.color.service.ColorDetailsService
import com.primortex.color.service.ColorService
import com.primortex.color.service.PaletteSelectionStore
import com.primortex.color.service.PaletteService
import com.primortex.color.service.RecentPicksService
import com.primortex.color.service.argbToHex
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class ColorDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val colorService: ColorService,
    private val colorDetailsService: ColorDetailsService,
    private val paletteService: PaletteService,
    private val recentPicksService: RecentPicksService,
    private val paletteSelectionStore: PaletteSelectionStore
) : ViewModel() {
    private val argb: Int = savedStateHandle.get<Int>("argb") ?: 0
    private val nameHint: String? = savedStateHandle.get<String>("name")
        ?.takeIf { it.isNotBlank() }
        ?.let { encoded -> runCatching { java.net.URLDecoder.decode(encoded, "UTF-8") }.getOrDefault(encoded) }

    private val details: ColorDetails = colorDetailsService.details(argb, similarLimit = 10)
    private val displayName = details.name.ifBlank { nameHint?.takeIf { it.isNotBlank() } ?: AppStrings.get(R.string.color_label) }
    private val pickedColor = PickedColor(details.argb, displayName)
    private val showPalettePicker = MutableStateFlow(false)

    private val _uiState = MutableStateFlow(
        buildUiState(
            palettes = paletteService.palettes.value,
            savedColors = recentPicksService.saved.value,
            pickerVisible = false
        )
    )
    val uiState: StateFlow<ColorDetailsUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ColorDetailsEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<ColorDetailsEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(paletteService.palettes, recentPicksService.saved, showPalettePicker) { palettes, savedColors, pickerVisible ->
                buildUiState(palettes, savedColors, pickerVisible)
            }.collect { _uiState.value = it }
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

            ColorDetailsUiAction.ShowPalettePicker -> showPalettePicker.value = true
            ColorDetailsUiAction.DismissPalettePicker -> showPalettePicker.value = false
            is ColorDetailsUiAction.AddColorToPalette -> addToPalette(action.paletteId)
            ColorDetailsUiAction.CreatePaletteFromColor -> createPaletteFromColor()
            ColorDetailsUiAction.OpenTintsPalette -> openColorSetPalette(
                suffix = AppStrings.get(R.string.tints_label),
                colors = listOf(details.argb) + details.tints
            )

            ColorDetailsUiAction.OpenShadesPalette -> openColorSetPalette(
                suffix = AppStrings.get(R.string.shades_label),
                colors = listOf(details.argb) + details.shades
            )

            ColorDetailsUiAction.OpenTonesPalette -> openColorSetPalette(
                suffix = AppStrings.get(R.string.tones_label),
                colors = listOf(details.argb) + details.tones
            )

            is ColorDetailsUiAction.OpenPaletteScheme -> {
                val scheme = _uiState.value.paletteSchemes.firstOrNull { it.type == action.schemeType } ?: return
                openColorSetPalette(suffix = paletteSchemeLabel(action.schemeType), colors = scheme.colors)
            }
        }
    }

    private fun buildUiState(
        palettes: List<Palette>,
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
                PaletteSchemeUiModel(PaletteSchemeType.Complementary, listOf(details.argb) + details.complements),
                PaletteSchemeUiModel(PaletteSchemeType.Analogous, listOf(details.argb) + details.analogous),
                PaletteSchemeUiModel(PaletteSchemeType.SplitComplementary, listOf(details.argb) + details.splitComplements),
                PaletteSchemeUiModel(PaletteSchemeType.Triadic, listOf(details.argb) + details.triads),
                PaletteSchemeUiModel(PaletteSchemeType.Tetradic, listOf(details.argb) + details.tetrads),
                PaletteSchemeUiModel(PaletteSchemeType.Square, listOf(details.argb) + details.squares)
            ),
            palettes = palettes,
            showPalettePicker = pickerVisible
        )
    }

    private fun addToPalette(paletteId: String) {
        val palette = _uiState.value.palettes.firstOrNull { it.id == paletteId } ?: return
        val message = when {
            palette.colors.any { it.argb == pickedColor.argb } -> AppStrings.get(R.string.already_in_palette)
            palette.colors.size >= 10 -> AppStrings.get(R.string.palette_full)
            else -> {
                paletteService.update(id = palette.id, colors = palette.colors + pickedColor)
                showPalettePicker.value = false
                AppStrings.get(R.string.added_to_palette)
            }
        }
        viewModelScope.launch { _effects.emit(ColorDetailsEffect.ShowMessage(message)) }
    }

    private fun createPaletteFromColor() {
        val newPaletteName = AppStrings.get(R.string.palette_name_with_color, pickedColor.name)
        val hasDuplicateName = _uiState.value.palettes.any { it.name.equals(newPaletteName, ignoreCase = true) }
        val message = if (hasDuplicateName) {
            AppStrings.get(R.string.already_in_palette)
        } else {
            paletteService.create(
                name = newPaletteName,
                colors = listOf(pickedColor),
                tags = listOf("details"),
                creationSource = "color_details"
            )
            showPalettePicker.value = false
            AppStrings.get(R.string.palette_saved)
        }
        viewModelScope.launch { _effects.emit(ColorDetailsEffect.ShowMessage(message)) }
    }

    private fun openColorSetPalette(suffix: String, colors: List<Int>) {
        val saved = paletteService.create(
            name = "$displayName $suffix",
            colors = colors.toPickedColors(),
            tags = listOf("details"),
            saveOnCreate = false,
            creationSource = "color_details"
        )
        paletteSelectionStore.select(saved)
        viewModelScope.launch { _effects.emit(ColorDetailsEffect.OpenPalette(saved.id, false)) }
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

    private fun List<Int>.toPickedColors(): List<PickedColor> {
        return distinct().map { argb ->
            val name = colorService.localNameFromArgb(argb)
            PickedColor(argb, if (name.isBlank()) argbToHex(argb) else name)
        }
    }
}

