// com/primortex/color/service/PaletteService.kt
package com.primortex.color.service

import android.content.Context
import com.primortex.color.analytics.AnalyticsTracker
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.data.db.DatabaseProvider
import com.primortex.color.data.repo.PaletteRepository
import com.primortex.color.data.repo.SqlPaletteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

object PaletteService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _palettes = MutableStateFlow<List<Palette>>(emptyList())
    val palettes: StateFlow<List<Palette>> = _palettes
    private val _previewPalettes = MutableStateFlow<List<Palette>>(emptyList())
    val previewPalettes: StateFlow<List<Palette>> = _previewPalettes
    private var firstPaletteLogged = false
    private lateinit var repository: PaletteRepository

    fun init(context: Context) {
        if (::repository.isInitialized) return
        DatabaseProvider.init(context.applicationContext)
        val database = DatabaseProvider.getDatabase()
        repository = SqlPaletteRepository(database)
        scope.launch {
            val saved = repository.loadPalettes()
            _palettes.value = saved
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
            scope.launch {
                repository.upsertPalette(p)
            }
        } else {
            _previewPalettes.update { listOf(p) + it }
        }
        AnalyticsTracker.logPaletteCreated(p, creationSource)
        if (saveOnCreate && !firstPaletteLogged) {
            AnalyticsTracker.logFirstPaletteCreated(p)
            firstPaletteLogged = true
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
        updatedPalette?.let { palette ->
            scope.launch {
                repository.upsertPalette(palette)
            }
        }
        updatedPalette?.let { AnalyticsTracker.logPaletteUpdated(it) }
    }

    fun toggleSaved(palette: Palette) {
        val targetHash = paletteHash(palette)
        val exists = _palettes.value.any { paletteHash(it) == targetHash }
        if (exists) {
            _palettes.update { list -> list.filterNot { paletteHash(it) == targetHash } }
            scope.launch {
                repository.deletePalette(palette.id)
            }
        } else {
            _palettes.update { listOf(palette) + it }
            _previewPalettes.update { it.filterNot { p -> p.id == palette.id } }
            scope.launch {
                repository.upsertPalette(palette)
            }
        }
    }

    fun delete(id: String) {
        _palettes.update { it.filterNot { p -> p.id == id } }
        scope.launch {
            repository.deletePalette(id)
        }
        AnalyticsTracker.logPaletteDeleted(id)
    }

    fun clear() {
        _palettes.value = emptyList()
        _previewPalettes.value = emptyList()
        scope.launch {
            repository.clear()
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
