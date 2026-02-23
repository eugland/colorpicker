package com.primortex.color.features.photopick

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primortex.color.R
import com.primortex.color.app.PickedColor
import com.primortex.color.i18n.AppStrings
import com.primortex.color.service.ColorDetails
import com.primortex.color.service.ColorDetailsService
import com.primortex.color.service.ColorService
import com.primortex.color.service.PaletteSelectionStore
import com.primortex.color.service.PaletteService
import com.primortex.color.service.RecentPicksService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class PhotoPickLocalState(
    val pickedArgb: Int = 0xFF7B8266.toInt(),
    val palette: List<PickedColor> = emptyList(),
    val frozen: Boolean = false,
    val detailPick: PickedColor? = null
)

@HiltViewModel
class PhotoPickViewModel @Inject constructor(
    private val recentPicksService: RecentPicksService,
    private val paletteService: PaletteService,
    private val paletteSelectionStore: PaletteSelectionStore,
    private val colorService: ColorService,
    private val colorDetailsService: ColorDetailsService
) : ViewModel() {
    private val local = MutableStateFlow(PhotoPickLocalState())
    private val _effects = MutableSharedFlow<PhotoPickEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<PhotoPickEffect> = _effects.asSharedFlow()

    val uiState: StateFlow<PhotoPickUiState> = combine(recentPicksService.history, local) { recents, state ->
        val current = toPickedColor(state.pickedArgb)
        PhotoPickUiState(
            pickedColor = current,
            recents = recents,
            palette = state.palette,
            frozen = state.frozen,
            detailPick = state.detailPick
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        initialValue = PhotoPickUiState(
            pickedColor = toPickedColor(local.value.pickedArgb),
            recents = recentPicksService.history.value,
            palette = local.value.palette,
            frozen = local.value.frozen,
            detailPick = null
        )
    )

    fun onAction(action: PhotoPickUiAction) {
        when (action) {
            PhotoPickUiAction.ToggleFreeze -> local.update { it.copy(frozen = !it.frozen) }
            PhotoPickUiAction.ShowCurrentDetails -> {
                local.update { it.copy(detailPick = toPickedColor(it.pickedArgb)) }
            }

            is PhotoPickUiAction.ShowDetails -> local.update { it.copy(detailPick = action.pick) }
            PhotoPickUiAction.DismissDetails -> local.update { it.copy(detailPick = null) }
            is PhotoPickUiAction.SampleColor -> sampleColor(action.argb)
            PhotoPickUiAction.AddCurrentToPalette -> addCurrentToPalette()
            PhotoPickUiAction.SavePalette -> savePalette()
        }
    }

    fun detailsFor(argb: Int): ColorDetails = colorDetailsService.details(argb, similarLimit = 10)

    private fun sampleColor(argb: Int) {
        if (local.value.frozen) return
        val pick = toPickedColor(argb)
        local.update { it.copy(pickedArgb = argb) }
        recentPicksService.addPick(pick, source = "photo_library")
    }

    private fun addCurrentToPalette() {
        val state = local.value
        val picked = toPickedColor(state.pickedArgb)
        val updated = when {
            state.palette.any { it.argb == picked.argb } -> {
                emitMessage(AppStrings.get(R.string.photo_already_in_palette, picked.name))
                state.palette
            }

            state.palette.size >= PaletteService.MAX_COLORS_PER_PALETTE -> {
                emitMessage(AppStrings.get(R.string.photo_palette_full_message))
                state.palette
            }

            else -> state.palette + picked
        }
        local.update { it.copy(palette = updated) }
    }

    private fun savePalette() {
        val palette = local.value.palette
        if (palette.isEmpty()) {
            emitMessage(AppStrings.get(R.string.photo_palette_empty))
            return
        }

        val saved = paletteService.create(
            name = AppStrings.get(
                R.string.palette_default_name,
                paletteService.palettes.value.size + 1
            ),
            tags = listOf(
                AppStrings.get(R.string.palette_tag_photo),
                AppStrings.get(R.string.palette_tag_pick)
            ),
            colors = palette,
            creationSource = "photo_library"
        )
        paletteSelectionStore.select(saved)
        local.update { it.copy(palette = emptyList()) }
        emitMessage(AppStrings.get(R.string.photo_palette_saved))
        emitEffect(PhotoPickEffect.OpenPalette(saved.id, true))
    }

    private fun toPickedColor(argb: Int): PickedColor {
        return PickedColor(argb = argb, name = colorService.localNameFromArgb(argb))
    }

    private fun emitMessage(message: String) {
        emitEffect(PhotoPickEffect.ShowMessage(message))
    }

    private fun emitEffect(effect: PhotoPickEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
