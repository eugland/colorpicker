// com/primortex/color/service/PaletteService.kt
package com.primortex.color.service

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var appContext: Context

    private val KEY_SEEDED = booleanPreferencesKey("seeded_v1")
    private val _palettes = MutableStateFlow<List<Palette>>(emptyList())
    val palettes: StateFlow<List<Palette>> = _palettes

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
                    Triple(saved, seeded, prefs)
                }
                .collect { (saved, seeded, prefs) ->
                    _palettes.value = saved
                    if (!seeded) seedIfNeeded(prefs) // this writes once
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
            createdAt = now,
            updatedAt = now
        )

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
            createdAt = now,
            updatedAt = now
        )

        _palettes.value = listOf(uiNeutrals, mutedNature)

        val payload = runCatching {
            json.encodeToString(_palettes.value)
        }.getOrElse { "[]" }

        appContext.paletteDataStore.edit { p ->
            p[KEY] = payload
            p[KEY_SEEDED] = true
        }
    }

    fun create(
        name: String,
        colors: List<PickedColor>,
        tags: List<String> = emptyList(),
        note: String = ""
    ): Palette {
        val now = System.currentTimeMillis()
        val p = Palette(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Palette" },
            colors = colors.distinctBy { it.argb },
            tags = tags,
            note = note,
            createdAt = now,
            updatedAt = now
        )
        _palettes.update { listOf(p) + it }
        persist()
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
                    updatedAt = now
                )
            }
        }
        persist()
    }

    fun delete(id: String) {
        _palettes.update { it.filterNot { p -> p.id == id } }
        persist()
    }

    fun clear() {
        _palettes.value = emptyList()
        persist()
    }

    private fun persist() {
        val snapshot = _palettes.value
        scope.launch {
            val payload = runCatching { json.encodeToString(snapshot) }.getOrElse { "[]" }
            appContext.paletteDataStore.edit { it[KEY] = payload }
        }
    }
}
