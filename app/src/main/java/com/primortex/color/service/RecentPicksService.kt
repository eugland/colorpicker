package com.primortex.color.service

import com.primortex.color.app.PickedColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

object RecentPicksService {
    private const val MAX = 50

    private val _history = MutableStateFlow<List<PickedColor>>(emptyList())
    val history: StateFlow<List<PickedColor>> = _history

    fun addPick(argb: Int, source: String) {
        _history.update { prev ->
            val next = listOf(PickedColor(argb = argb, source = source)) + prev
            next.take(MAX)
        }
    }

    fun clear() {
        _history.value = emptyList()
    }
}