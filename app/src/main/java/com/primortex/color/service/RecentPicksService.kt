package com.primortex.color.service

import android.content.Context
import com.primortex.color.analytics.AnalyticsTracker
import com.primortex.color.app.PickedColor
import com.primortex.color.data.db.DatabaseProvider
import com.primortex.color.data.repo.RecentPicksRepository
import com.primortex.color.data.repo.SqlRecentPicksRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object RecentPicksService {

    private const val MAX = 100
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _history = MutableStateFlow<List<PickedColor>>(emptyList())
    val history: StateFlow<List<PickedColor>> = _history
    private val _saved = MutableStateFlow<List<PickedColor>>(emptyList())
    val saved: StateFlow<List<PickedColor>> = _saved
    private var firstPickLogged = false
    private var firstSavedLogged = false
    private lateinit var repository: RecentPicksRepository

    fun init(context: Context) {
        if (::repository.isInitialized) return
        DatabaseProvider.init(context.applicationContext)
        val database = DatabaseProvider.getDatabase()
        repository = SqlRecentPicksRepository(database)

        scope.launch {
            val saved = repository.loadHistory(MAX)
            val savedColors = repository.loadSaved(MAX)

            _history.value = saved
            _saved.value = savedColors

        }
    }

    // ---- public API ----
    fun addPick(pick: PickedColor, source: String = "unknown") {
        AnalyticsTracker.logColorPicked(pick, source)
        if (!firstPickLogged) {
            AnalyticsTracker.logFirstColorPick(pick)
            firstPickLogged = true
        }
        _history.update { prev ->
            (listOf(pick) + prev)
                .distinctBy { it.argb } // optional: dedupe by color
                .take(MAX)
        }
        scope.launch {
            repository.addHistory(pick, MAX)
        }
    }

    fun clear() {
        _history.value = emptyList()
        AnalyticsTracker.logRecentsCleared()
        scope.launch {
            repository.clearHistory()
        }
    }

    fun clearSaved() {
        _saved.value = emptyList()
        AnalyticsTracker.logSavedCleared()
        scope.launch {
            repository.clearSaved()
        }
    }

    fun addSaved(pick: PickedColor) {
        _saved.update { prev ->
            (listOf(pick) + prev)
                .distinctBy { it.argb }
                .take(MAX)
        }
        AnalyticsTracker.logColorSaved(pick, action = "saved")
        if (!firstSavedLogged) {
            AnalyticsTracker.logFirstColorSaved(pick)
            firstSavedLogged = true
        }
        scope.launch {
            repository.addSaved(pick, MAX)
        }
    }

    fun removeSaved(argb: Int) {
        val color = _saved.value.firstOrNull { it.argb == argb }
        _saved.update { prev -> prev.filterNot { it.argb == argb } }
        color?.let { AnalyticsTracker.logColorSaved(it, action = "removed") }
        scope.launch {
            repository.removeSaved(argb)
        }
    }

    fun toggleSaved(pick: PickedColor) {
        val exists = _saved.value.any { it.argb == pick.argb }
        if (exists) removeSaved(pick.argb) else addSaved(pick)
    }
}
