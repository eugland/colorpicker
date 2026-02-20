package com.primortex.color.features.swatchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primortex.color.app.PickedColor
import com.primortex.color.data.enums.SwatchListType
import com.primortex.color.service.RecentPicksService
import com.primortex.color.service.argbToHex
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SwatchListViewModel @Inject constructor(
    private val recentPicksService: RecentPicksService
) : ViewModel() {
    private val type = MutableStateFlow(SwatchListType.RECENT)
    private val query = MutableStateFlow("")
    private val _effects = MutableSharedFlow<SwatchListEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<SwatchListEffect> = _effects.asSharedFlow()

    val uiState: StateFlow<SwatchListUiState> = combine(
        type,
        query,
        recentPicksService.history,
        recentPicksService.saved
    ) { currentType, currentQuery, history, saved ->
        val picks = when (currentType) {
            SwatchListType.RECENT -> history
            SwatchListType.SAVED -> saved
        }
        val filtered = filterPicks(picks, currentQuery)
        SwatchListUiState(
            type = currentType,
            picks = picks,
            filtered = filtered,
            query = currentQuery
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SwatchListUiState(
            type = SwatchListType.RECENT,
            picks = recentPicksService.history.value,
            filtered = recentPicksService.history.value,
            query = ""
        )
    )

    fun onAction(action: SwatchListUiAction) {
        when (action) {
            is SwatchListUiAction.BindType -> type.value = action.type
            is SwatchListUiAction.QueryChanged -> query.value = action.value
            SwatchListUiAction.ClearQuery -> query.value = ""
            SwatchListUiAction.DeleteAll -> clearCurrentType()
            is SwatchListUiAction.OpenColorDetail -> {
                viewModelScope.launch { _effects.emit(SwatchListEffect.OpenColorDetail(action.pick)) }
            }
        }
    }

    private fun clearCurrentType() {
        when (type.value) {
            SwatchListType.RECENT -> recentPicksService.clear()
            SwatchListType.SAVED -> recentPicksService.clearSaved()
        }
    }

    private fun filterPicks(picks: List<PickedColor>, query: String): List<PickedColor> {
        val lowered = query.trim().lowercase()
        if (lowered.isBlank()) return picks
        return picks.filter {
            it.name.lowercase().contains(lowered) ||
                argbToHex(it.argb).lowercase().contains(lowered)
        }
    }
}
