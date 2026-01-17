// com/primortex/color/service/PaletteService.kt
package com.primortex.color.service

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.primortex.color.analytics.AnalyticsTracker
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.paletteDataStore by preferencesDataStore(name = "palettes")

object PaletteService {
    private val KEY = stringPreferencesKey("palettes_json")
    private val KEY_SAVED_IDS = stringPreferencesKey("saved_palette_ids_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var appContext: Context

    private val KEY_SEEDED = booleanPreferencesKey("seeded_v1")
    private val _palettes = MutableStateFlow<List<Palette>>(emptyList())
    val palettes: StateFlow<List<Palette>> = _palettes
    private val _savedIds = MutableStateFlow<Set<String>>(emptySet())
    val savedIds: StateFlow<Set<String>> = _savedIds

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        scope.launch {
            appContext.paletteDataStore.data
                .map { prefs ->
                    val saved = runCatching {
                        prefs[KEY]?.let { json.decodeFromString<List<Palette>>(it) } ?: emptyList()
                    }.getOrDefault(emptyList())

                    val seeded = prefs[KEY_SEEDED] == true
                    val savedIds = runCatching {
                        prefs[KEY_SAVED_IDS]?.let { json.decodeFromString<List<String>>(it) }
                            ?: emptyList()
                    }.getOrDefault(emptyList())
                    Quadruple(saved, savedIds, seeded, prefs)
                }
                .collect { (saved, savedIds, seeded, prefs) ->
                    val resolvedSavedIds = if (prefs[KEY_SAVED_IDS] == null && saved.isNotEmpty()) {
                        saved.map { it.id }
                    } else {
                        savedIds
                    }
                    _savedIds.value = resolvedSavedIds.toSet()
                    _palettes.value = saved.map { palette ->
                        palette.copy(isSaved = _savedIds.value.contains(palette.id))
                    }
                    if (!seeded) seedIfNeeded(prefs) // this writes once
                    seedSavedIdsIfNeeded(prefs, saved, resolvedSavedIds)
                }
        }
    }

    private suspend fun seedIfNeeded(prefs: Preferences) {
        if (prefs[KEY_SEEDED] == true) return

        val now = System.currentTimeMillis()

        val uiNeutrals = Palette(
            id = UUID.randomUUID().toString(),
            name = "Modern UI Neutrals",
            colors = listOf(
                PickedColor(0xFF0F172A.toInt(), "Slate 900"),
                PickedColor(0xFF475569.toInt(), "Slate 600"),
                PickedColor(0xFFA1A1AA.toInt(), "Zinc 400"),
                PickedColor(0xFF0EA5E9.toInt(), "Sky 500"),
                PickedColor(0xFF10B981.toInt(), "Emerald 500"),
            ),
            tags = listOf("ui", "neutral", "modern"),
            note = "Clean, flexible colors for modern interfaces",
            isSaved = true,
            createdAt = now,
            updatedAt = now
        )

        fun pa(): Palette {
            return Palette(
                id = UUID.randomUUID().toString(),
                name = "Muted Nature",
                colors = listOf(
                    PickedColor(0xFF2F5D50.toInt(), "Forest"),
                    PickedColor(0xFF7A9B76.toInt(), "Moss"),
                    PickedColor(0xFFE6D5B8.toInt(), "Sand"),
                    PickedColor(0xFFC97C5D.toInt(), "Clay"),
                    PickedColor(0xFF3A3A3A.toInt(), "Ink"),
                ),
                tags = listOf("nature", "muted", "warm"),
                note = "Soft, earthy tones for calm visual design",
                isSaved = true,
                createdAt = now,
                updatedAt = now
            )
        }

        val mutedNature = Palette(
            id = UUID.randomUUID().toString(),
            name = "Muted Nature",
            colors = listOf(
                PickedColor(0xFF2F5D50.toInt(), "Forest"),
                PickedColor(0xFF7A9B76.toInt(), "Moss"),
                PickedColor(0xFFE6D5B8.toInt(), "Sand"),
                PickedColor(0xFFC97C5D.toInt(), "Clay"),
                PickedColor(0xFF3A3A3A.toInt(), "Ink"),
            ),
            tags = listOf("nature", "muted", "warm"),
            note = "Soft, earthy tones for calm visual design",
            isSaved = true,
            createdAt = now,
            updatedAt = now
        )

        _palettes.value = listOf(
            uiNeutrals,
            mutedNature,
            pa(),
            pa(),
            pa(),
            pa(),
            pa(),
            pa(),
            pa(),
            pa(),
        )

        val payload = runCatching {
            json.encodeToString(_palettes.value)
        }.getOrElse { "[]" }

        appContext.paletteDataStore.edit { p ->
            p[KEY] = payload
            p[KEY_SEEDED] = true
        }
    }

    private suspend fun seedSavedIdsIfNeeded(
        prefs: Preferences,
        saved: List<Palette>,
        resolvedSavedIds: List<String>
    ) {
        if (prefs[KEY_SAVED_IDS] != null || saved.isEmpty()) return
        val payload = runCatching { json.encodeToString(resolvedSavedIds) }.getOrElse { "[]" }
        appContext.paletteDataStore.edit { p ->
            p[KEY_SAVED_IDS] = payload
        }
    }

    fun create(
        name: String,
        colors: List<PickedColor>,
        tags: List<String> = emptyList(),
        note: String = "",
        creationSource: String = "unknown"
    ): Palette {
        val now = System.currentTimeMillis()
        val p = Palette(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Palette" },
            colors = colors.distinctBy { it.argb },
            tags = tags,
            note = note,
            isSaved = true,
            createdAt = now,
            updatedAt = now
        )
        _palettes.update { listOf(p) + it }
        _savedIds.update { it + p.id }
        persist()
        persistSavedIds()
        AnalyticsTracker.logPaletteCreated(p, creationSource)
        return p
    }

    fun update(
        id: String,
        name: String? = null,
        colors: List<PickedColor>? = null,
        tags: List<String>? = null,
        note: String? = null
    ) {
        val now = System.currentTimeMillis()
        _palettes.update { list ->
            list.map { p ->
                if (p.id != id) p
                else p.copy(
                    name = name ?: p.name,
                    colors = (colors ?: p.colors).distinctBy { it.argb },
                    tags = tags ?: p.tags,
                    note = note ?: p.note,
                    isSaved = p.isSaved,
                    updatedAt = now
                )
            }
        }
        persist()
    }

    fun toggleSaved(id: String) {
        val isSaved = _savedIds.value.contains(id)
        setSaved(id, !isSaved)
    }

    fun setSaved(id: String, isSaved: Boolean) {
        _savedIds.update { ids ->
            if (isSaved) ids + id else ids - id
        }
        _palettes.update { list ->
            list.map { palette ->
                if (palette.id != id) palette else palette.copy(isSaved = isSaved)
            }
        }
        persist()
        persistSavedIds()
    }

    fun delete(id: String) {
        _palettes.update { it.filterNot { p -> p.id == id } }
        _savedIds.update { it - id }
        persist()
        persistSavedIds()
    }

    fun clear() {
        _palettes.value = emptyList()
        _savedIds.value = emptySet()
        persist()
        persistSavedIds()
    }

    private fun persist() {
        val snapshot = _palettes.value
        scope.launch {
            val payload = runCatching { json.encodeToString(snapshot) }.getOrElse { "[]" }
            appContext.paletteDataStore.edit { it[KEY] = payload }
        }
    }

    private fun persistSavedIds() {
        val snapshot = _savedIds.value.toList()
        scope.launch {
            val payload = runCatching { json.encodeToString(snapshot) }.getOrElse { "[]" }
            appContext.paletteDataStore.edit { it[KEY_SAVED_IDS] = payload }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
