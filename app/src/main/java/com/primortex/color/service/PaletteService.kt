// com/primortex/color/service/PaletteService.kt
package com.primortex.color.service

import android.content.Context
import com.primortex.color.analytics.AnalyticsTracker
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.data.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

object PaletteService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var appContext: Context
    private lateinit var repository: DataRepository
    private val _palettes = MutableStateFlow<List<Palette>>(emptyList())
    val palettes: StateFlow<List<Palette>> = _palettes

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        repository = DataRepository.getInstance(appContext)
        scope.launch {
            repository.seedPalettesIfNeeded()
            repository.observePalettes().collect { _palettes.value = it }
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
            createdAt = now,
            updatedAt = now
        )
        scope.launch { repository.upsertPalette(p) }
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
        val existing = _palettes.value.firstOrNull { it.id == id } ?: return
        val updated = existing.copy(
            name = name ?: existing.name,
            colors = (colors ?: existing.colors).distinctBy { it.argb },
            tags = tags ?: existing.tags,
            note = note ?: existing.note,
            updatedAt = now
        )
        scope.launch { repository.upsertPalette(updated) }
    }

    fun delete(id: String) {
        scope.launch { repository.deletePalette(id) }
    }

    fun clear() {
        scope.launch { repository.clearPalettes() }
    }
}
