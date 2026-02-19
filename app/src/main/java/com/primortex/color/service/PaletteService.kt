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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID

private val Context.paletteDataStore by preferencesDataStore(name = "palettes")

@Singleton
class PaletteService @Inject constructor(
    @ApplicationContext context: Context
) {
    private val KEY = stringPreferencesKey("palettes_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val appContext = context.applicationContext

    private val KEY_SEEDED = booleanPreferencesKey("seeded_v1")
    private val KEY_FIRST_PALETTE_LOGGED = booleanPreferencesKey("first_palette_logged_v1")
    private val _palettes = MutableStateFlow<List<Palette>>(emptyList())
    val palettes: StateFlow<List<Palette>> = _palettes
    private val _previewPalettes = MutableStateFlow<List<Palette>>(emptyList())
    val previewPalettes: StateFlow<List<Palette>> = _previewPalettes
    private var firstPaletteLogged = false

    init {
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
                    firstPaletteLogged = prefs[KEY_FIRST_PALETTE_LOGGED] == true
                    if (!seeded) seedIfNeeded(prefs)
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

        val payload = runCatching { json.encodeToString(_palettes.value) }.getOrElse { "[]" }

        appContext.paletteDataStore.edit { p ->
            p[KEY] = payload
            p[KEY_SEEDED] = true
        }
    }

    fun create(
        name: String,
        colors: List<PickedColor>,
        tags: List<String> = emptyList(),
        note: String = "",
        saveOnCreate: Boolean = true,
        creationSource: String = "unknown"
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
        val hash = paletteHash(p)
        if (saveOnCreate) {
            val existing = _palettes.value.firstOrNull { paletteHash(it) == hash }
            if (existing != null) {
                return existing
            }
            _palettes.update { listOf(p) + it }
            persist()
        } else {
            _previewPalettes.update { listOf(p) + it }
        }
        AnalyticsTracker.logPaletteCreated(p, creationSource)
        if (saveOnCreate && !firstPaletteLogged) {
            AnalyticsTracker.logFirstPaletteCreated(p)
            firstPaletteLogged = true
            scope.launch {
                appContext.paletteDataStore.edit { prefs ->
                    prefs[KEY_FIRST_PALETTE_LOGGED] = true
                }
            }
        }
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
        var updatedPalette: Palette? = null
        _palettes.update { list ->
            list.map { p ->
                if (p.id != id) p
                else p.copy(
                    name = name ?: p.name,
                    colors = (colors ?: p.colors).distinctBy { it.argb },
                    tags = tags ?: p.tags,
                    note = note ?: p.note,
                    updatedAt = now
                ).also { updatedPalette = it }
            }
        }
        persist()
        updatedPalette?.let { AnalyticsTracker.logPaletteUpdated(it) }
    }

    fun toggleSaved(palette: Palette) {
        val targetHash = paletteHash(palette)
        val exists = _palettes.value.any { paletteHash(it) == targetHash }
        if (exists) {
            _palettes.update { list -> list.filterNot { paletteHash(it) == targetHash } }
        } else {
            _palettes.update { listOf(palette) + it }
            _previewPalettes.update { it.filterNot { p -> p.id == palette.id } }
        }
        persist()
    }

    fun delete(id: String) {
        _palettes.update { it.filterNot { p -> p.id == id } }
        persist()
        AnalyticsTracker.logPaletteDeleted(id)
    }

    fun clear() {
        _palettes.value = emptyList()
        _previewPalettes.value = emptyList()
        persist()
    }

    private fun persist() {
        val snapshot = _palettes.value
        scope.launch {
            val payload = runCatching { json.encodeToString(snapshot) }.getOrElse { "[]" }
            appContext.paletteDataStore.edit { it[KEY] = payload }
        }
    }

    fun paletteHash(palette: Palette): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val payload = palette.colors
            .map { it.argb }
            .sorted()
            .joinToString(separator = ",")
            .toByteArray()
        val hashBytes = digest.digest(payload)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
