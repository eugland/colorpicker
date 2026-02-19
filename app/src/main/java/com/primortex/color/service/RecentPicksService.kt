package com.primortex.color.service

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.primortex.color.analytics.AnalyticsTracker
import com.primortex.color.app.PickedColor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val Context.recentsDataStore by preferencesDataStore(name = "recent_picks")

@Singleton
class RecentPicksService @Inject constructor(
    @ApplicationContext context: Context
) {
    private companion object {
        const val MAX = 100
    }

    private val KEY_HISTORY = stringPreferencesKey("history_json")
    private val KEY_SAVED = stringPreferencesKey("saved_json")
    private val KEY_SEEDED = booleanPreferencesKey("seeded_v1")
    private val KEY_FIRST_PICK_LOGGED = booleanPreferencesKey("first_pick_logged_v1")
    private val KEY_FIRST_SAVE_LOGGED = booleanPreferencesKey("first_saved_logged_v1")
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val _history = MutableStateFlow<List<PickedColor>>(emptyList())
    val history: StateFlow<List<PickedColor>> = _history
    private val _saved = MutableStateFlow<List<PickedColor>>(emptyList())
    val saved: StateFlow<List<PickedColor>> = _saved
    private var firstPickLogged = false
    private var firstSavedLogged = false

    init {
        scope.launch {
            val prefs = appContext.recentsDataStore.data.first()

            val savedHistory = runCatching {
                prefs[KEY_HISTORY]?.let { json.decodeFromString<List<PickedColor>>(it) }
                    ?: emptyList()
            }.getOrDefault(emptyList())

            val savedColors = runCatching {
                prefs[KEY_SAVED]?.let { json.decodeFromString<List<PickedColor>>(it) }
                    ?: emptyList()
            }.getOrDefault(emptyList())

            Log.d(
                "RecentPicksService",
                "Loaded ${savedHistory.size} recents and ${savedColors.size} saved colors"
            )

            _history.value = savedHistory.take(MAX)
            _saved.value = savedColors.take(MAX)
            firstPickLogged = prefs[KEY_FIRST_PICK_LOGGED] == true
            firstSavedLogged = prefs[KEY_FIRST_SAVE_LOGGED] == true

            seedIfNeeded(prefs)
        }
    }

    private suspend fun seedIfNeeded(prefs: Preferences) {
        if (prefs[KEY_SEEDED] == true) return

        val hasHistory = prefs[KEY_HISTORY] != null
        val hasSaved = prefs[KEY_SAVED] != null

        val initHistory = listOf(
            PickedColor(0xFF0F172A.toInt(), "Slate 900"),
            PickedColor(0xFF475569.toInt(), "Slate 600"),
            PickedColor(0xFFA1A1AA.toInt(), "Zinc 400"),
            PickedColor(0xFF0EA5E9.toInt(), "Sky 500"),
            PickedColor(0xFF10B981.toInt(), "Emerald 500"),
        )

        val initSaved = listOf(
            PickedColor(0xFF2F5D50.toInt(), "Forest"),
            PickedColor(0xFF7A9B76.toInt(), "Moss"),
            PickedColor(0xFFE6D5B8.toInt(), "Sand"),
            PickedColor(0xFFC97C5D.toInt(), "Clay"),
            PickedColor(0xFF3A3A3A.toInt(), "Ink"),
        ).take(MAX)

        if (!hasHistory) {
            _history.value = initHistory
        }
        if (!hasSaved) {
            _saved.value = initSaved
        }

        appContext.recentsDataStore.edit { updated ->
            updated[KEY_SEEDED] = true
            if (!hasHistory) {
                updated[KEY_HISTORY] = runCatching { json.encodeToString(initHistory) }
                    .getOrElse { "[]" }
            }
            if (!hasSaved) {
                updated[KEY_SAVED] = runCatching { json.encodeToString(initSaved) }
                    .getOrElse { "[]" }
            }
        }
    }

    fun addPick(pick: PickedColor, source: String = "unknown") {
        AnalyticsTracker.logColorPicked(pick, source)
        if (!firstPickLogged) {
            AnalyticsTracker.logFirstColorPick(pick)
            firstPickLogged = true
            scope.launch {
                appContext.recentsDataStore.edit { prefs ->
                    prefs[KEY_FIRST_PICK_LOGGED] = true
                }
            }
        }
        _history.update { prev ->
            (listOf(pick) + prev)
                .distinctBy { it.argb }
                .take(MAX)
        }
        persistHistory()
    }

    fun clear() {
        _history.value = emptyList()
        AnalyticsTracker.logRecentsCleared()
        persistHistory()
    }

    fun clearSaved() {
        _saved.value = emptyList()
        AnalyticsTracker.logSavedCleared()
        persistSaved()
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
            scope.launch {
                appContext.recentsDataStore.edit { prefs ->
                    prefs[KEY_FIRST_SAVE_LOGGED] = true
                }
            }
        }
        persistSaved()
    }

    fun removeSaved(argb: Int) {
        val color = _saved.value.firstOrNull { it.argb == argb }
        _saved.update { prev -> prev.filterNot { it.argb == argb } }
        color?.let { AnalyticsTracker.logColorSaved(it, action = "removed") }
        persistSaved()
    }

    fun toggleSaved(pick: PickedColor) {
        val exists = _saved.value.any { it.argb == pick.argb }
        if (exists) removeSaved(pick.argb) else addSaved(pick)
    }

    private fun persistHistory() {
        val snapshot = _history.value
        scope.launch {
            val payload = runCatching { json.encodeToString(snapshot) }.getOrElse { "[]" }
            appContext.recentsDataStore.edit { prefs ->
                prefs[KEY_HISTORY] = payload
            }
        }
    }

    private fun persistSaved() {
        val snapshot = _saved.value
        scope.launch {
            val payload = runCatching { json.encodeToString(snapshot) }.getOrElse { "[]" }
            appContext.recentsDataStore.edit { prefs ->
                prefs[KEY_SAVED] = payload
            }
        }
    }
}
