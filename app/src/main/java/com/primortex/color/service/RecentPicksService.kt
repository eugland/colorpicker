package com.primortex.color.service

import android.content.Context
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

    // ---- app context (safe) ----
    private lateinit var appContext: Context

    // ---- coroutine scope ----
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ---- serialization ----
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ---- state ----
    private val _history = MutableStateFlow<List<PickedColor>>(emptyList())
    val history: StateFlow<List<PickedColor>> = _history

    // ---- init (call once at app startup) ----
    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext

        scope.launch {
            val saved = runCatching {
                val prefs = appContext.recentsDataStore.data.first()
                prefs[KEY_HISTORY]
                    ?.let { json.decodeFromString<List<PickedColor>>(it) }
                    ?: emptyList()
            }.getOrDefault(emptyList())

            _history.value = saved.take(MAX)
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
