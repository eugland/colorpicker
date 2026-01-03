package com.primortex.color.service

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.primortex.color.app.PickedColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val Context.recentsDataStore by preferencesDataStore(
    name = "recent_picks"
)

object RecentPicksService {

    private const val MAX = 100
    private val KEY_HISTORY = stringPreferencesKey("history_json")
    private val KEY_SAVED = stringPreferencesKey("saved_json")
    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val KEY_SEEDED = booleanPreferencesKey("seeded_v1")
    private val _history = MutableStateFlow<List<PickedColor>>(emptyList())
    val history: StateFlow<List<PickedColor>> = _history
    private val _saved = MutableStateFlow<List<PickedColor>>(emptyList())
    val saved: StateFlow<List<PickedColor>> = _saved

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext

        scope.launch {
            val prefs = appContext.recentsDataStore.data.first()

            val saved = runCatching {
                prefs[KEY_HISTORY]?.let { json.decodeFromString<List<PickedColor>>(it) }
                    ?: emptyList()
            }.getOrDefault(emptyList())

            val savedColors = runCatching {
                prefs[KEY_SAVED]?.let { json.decodeFromString<List<PickedColor>>(it) }
                    ?: emptyList()
            }.getOrDefault(emptyList())

            Log.d(
                "RecentPicksService",
                "Loaded ${saved.size} recents and ${savedColors.size} saved colors"
            )

            _history.value = saved.take(MAX)
            _saved.value = savedColors.take(MAX)

            seedIfNeeded(prefs)

            //Testing use: -------------------------------------------------------------------------
//            val defaults = List(100) { i ->
//                val hue = (i * 37f) % 360f              // golden-angle spread
//                val saturation = 0.45f + (i % 3) * 0.15f
//                val value = 0.65f + (i % 4) * 0.08f
//
//                val hsv = floatArrayOf(hue, saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
//                val argb = android.graphics.Color.HSVToColor(hsv)
//
//                PickedColor(
//                    argb = argb,
//                    name = "Color ${i + 1}"
//                )
//            }.take(MAX)
//            _history.value = defaults
            // testing use end ---------------------------------------------------------------------
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
        persistHistory()
    }

    fun clear() {
        _history.value = emptyList()
        persistHistory()
    }

    fun clearSaved() {
        _saved.value = emptyList()
        persistSaved()
    }

    fun addSaved(pick: PickedColor) {
        _saved.update { prev ->
            (listOf(pick) + prev)
                .distinctBy { it.argb }
                .take(MAX)
        }
        persistSaved()
    }

    fun removeSaved(argb: Int) {
        _saved.update { prev -> prev.filterNot { it.argb == argb } }
        persistSaved()
    }

    fun toggleSaved(pick: PickedColor) {
        val exists = _saved.value.any { it.argb == pick.argb }
        if (exists) removeSaved(pick.argb) else addSaved(pick)
    }

    // ---- persistence ----
    private fun persistHistory() {
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

    private fun persistSaved() {
        val snapshot = _saved.value
        scope.launch {
            val payload = runCatching { json.encodeToString(snapshot) }
                .getOrElse { "[]" }

            appContext.recentsDataStore.edit { prefs ->
                prefs[KEY_SAVED] = payload
            }
        }
    }
}
