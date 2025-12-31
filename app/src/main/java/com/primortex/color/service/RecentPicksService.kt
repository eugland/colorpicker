package com.primortex.color.service

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.primortex.color.app.PickedColor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

private val Context.recentsDataStore by preferencesDataStore(
    name = "recent_picks"
)

object RecentPicksService {

    private const val MAX = 50
    private val KEY_HISTORY = stringPreferencesKey("history_json")
    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val KEY_SEEDED = booleanPreferencesKey("seeded_v1")
    private val _history = MutableStateFlow<List<PickedColor>>(emptyList())
    val history: StateFlow<List<PickedColor>> = _history

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext

        scope.launch {
            val prefs = appContext.recentsDataStore.data.first()

            val saved = runCatching {
                prefs[KEY_HISTORY]?.let { json.decodeFromString<List<PickedColor>>(it) } ?: emptyList()
            }.getOrDefault(emptyList())

            _history.value = saved.take(MAX)

            seedIfNeeded(prefs)
        }
    }

    private suspend fun seedIfNeeded(prefs: Preferences) {
        if (prefs[KEY_SEEDED] == true) return

        val defaults = listOf(
            // Modern UI / Neutral
            PickedColor(0xFF0F172A.toInt(), "Slate 900"),
            PickedColor(0xFF475569.toInt(), "Slate 600"),
            PickedColor(0xFFA1A1AA.toInt(), "Zinc 400"),
            PickedColor(0xFF0EA5E9.toInt(), "Sky 500"),
            PickedColor(0xFF10B981.toInt(), "Emerald 500"),

            // Muted Nature
            PickedColor(0xFF2F5D50.toInt(), "Forest"),
            PickedColor(0xFF7A9B76.toInt(), "Moss"),
            PickedColor(0xFFE6D5B8.toInt(), "Sand"),
            PickedColor(0xFFC97C5D.toInt(), "Clay"),
            PickedColor(0xFF3A3A3A.toInt(), "Ink"),
        ).take(MAX)

        _history.value = defaults

        val payload = runCatching {
            json.encodeToString(defaults)
        }.getOrElse { "[]" }

        appContext.recentsDataStore.edit { p ->
            p[KEY_HISTORY] = payload
            p[KEY_SEEDED] = true
        }
    }

    // ---- public API ----
    fun addPick(pick: PickedColor) {
        _history.update { prev ->
            (listOf(pick) + prev)
                .distinctBy { it.argb } // optional: dedupe by color
                .take(MAX)
        }
        persist()
    }

    fun clear() {
        _history.value = emptyList()
        persist()
    }

    // ---- persistence ----
    private fun persist() {
        val snapshot = _history.value
        scope.launch {
            val payload = runCatching {
                json.encodeToString(snapshot)
            }.getOrElse { "[]" }

            appContext.recentsDataStore.edit { prefs ->
                prefs[KEY_HISTORY] = payload
            }
        }
    }
}
