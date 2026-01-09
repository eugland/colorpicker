package com.primortex.color.service

import android.content.Context
import com.primortex.color.analytics.AnalyticsTracker
import com.primortex.color.app.PickedColor
import com.primortex.color.data.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object RecentPicksService {

    private const val MAX = 100
    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: DataRepository
    private val _history = MutableStateFlow<List<PickedColor>>(emptyList())
    val history: StateFlow<List<PickedColor>> = _history
    private val _saved = MutableStateFlow<List<PickedColor>>(emptyList())
    val saved: StateFlow<List<PickedColor>> = _saved

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        repository = DataRepository.getInstance(appContext)

        scope.launch {
            repository.seedRecentsIfNeeded(MAX)
            repository.observeHistory(MAX).collect { _history.value = it }
        }
        scope.launch {
            repository.observeSaved(MAX).collect { _saved.value = it }
        }
    }

    // ---- public API ----
    fun addPick(pick: PickedColor, source: String = "unknown") {
        AnalyticsTracker.logColorPicked(pick, source)
        scope.launch { repository.addHistory(pick, MAX) }
    }

    fun clear() {
        scope.launch { repository.clearHistory() }
    }

    fun clearSaved() {
        scope.launch { repository.clearSaved() }
    }

    fun addSaved(pick: PickedColor) {
        AnalyticsTracker.logColorSaved(pick, action = "saved")
        scope.launch { repository.addSaved(pick, MAX) }
    }

    fun removeSaved(argb: Int) {
        val color = _saved.value.firstOrNull { it.argb == argb }
        color?.let { AnalyticsTracker.logColorSaved(it, action = "removed") }
        scope.launch { repository.removeSaved(argb) }
    }

    fun toggleSaved(pick: PickedColor) {
        val exists = _saved.value.any { it.argb == pick.argb }
        if (exists) removeSaved(pick.argb) else addSaved(pick)
    }
}
