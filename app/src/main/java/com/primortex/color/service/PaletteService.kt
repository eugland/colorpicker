// com/primortex/color/service/PaletteService.kt
package com.primortex.color.service

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.paletteDataStore by preferencesDataStore(name = "palettes")

object PaletteService {
    private val KEY = stringPreferencesKey("palettes_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var appContext: Context

    private val _palettes = MutableStateFlow<List<Palette>>(emptyList())
    val palettes: StateFlow<List<Palette>> = _palettes

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        scope.launch {
            val saved = runCatching {
                val prefs = appContext.paletteDataStore.data.first()
                prefs[KEY]?.let { json.decodeFromString<List<Palette>>(it) } ?: emptyList()
            }.getOrDefault(emptyList())
            _palettes.value = saved
        }
    }

    fun create(name: String, colors: List<PickedColor>, tags: List<String> = emptyList(), note: String = "") {
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
    }

    fun update(id: String, name: String? = null, colors: List<PickedColor>? = null, tags: List<String>? = null, note: String? = null) {
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

    private fun persist() {
        val snapshot = _palettes.value
        scope.launch {
            val payload = runCatching { json.encodeToString(snapshot) }.getOrElse { "[]" }
            appContext.paletteDataStore.edit { it[KEY] = payload }
        }
    }
}
