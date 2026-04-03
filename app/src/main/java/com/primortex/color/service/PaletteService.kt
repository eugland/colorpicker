package com.primortex.color.service

import android.content.Context
import com.primortex.color.R
import com.primortex.color.analytics.AnalyticsTracker
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.i18n.AppStrings
import com.primortex.color.service.palette.toDomain
import com.primortex.color.service.palette.toEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

@Singleton
class PaletteService @Inject constructor(
    @ApplicationContext context: Context,
    private val analyticsTracker: AnalyticsTracker,
    appDatabase: AppDatabase
) {
    sealed interface AddColorResult {
        data class Added(val colors: List<PickedColor>) : AddColorResult
        data object Duplicate : AddColorResult
        data object Full : AddColorResult
    }

    companion object {
        private const val PREFS_NAME = "palette_flags"
        private const val PREF_KEY_FIRST_PALETTE_LOGGED = "first_palette_logged_v1"
        const val MAX_COLORS_PER_PALETTE = 5
    }

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dao = appDatabase.paletteDao()
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val palettes: StateFlow<List<Palette>> = dao.observeAll()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private var firstPaletteLogged = false

    init {
        firstPaletteLogged = prefs.getBoolean(PREF_KEY_FIRST_PALETTE_LOGGED, false)
    }

    suspend fun seedPalettesIfEmpty(palettes: List<Palette>) {
        if (dao.loadAll().isEmpty() && palettes.isNotEmpty()) {
            dao.insertAll(palettes.map { it.toEntity() })
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
            name = name.ifBlank { AppStrings.get(R.string.palette) },
            colors = colors.distinctBy { it.argb }.take(MAX_COLORS_PER_PALETTE),
            tags = tags,
            note = note,
            createdAt = now,
            updatedAt = now
        )
        val hash = paletteHash(p)
        val existing = palettes.value.firstOrNull { paletteHash(it) == hash }
        if (existing != null) return existing

        scope.launch { dao.upsert(p.toEntity()) }

        analyticsTracker.logPaletteCreated(p, creationSource)
        if (!firstPaletteLogged) {
            analyticsTracker.logFirstPaletteCreated(p)
            firstPaletteLogged = true
            prefs.edit().putBoolean(PREF_KEY_FIRST_PALETTE_LOGGED, true).apply()
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
        val current = palettes.value.firstOrNull { it.id == id } ?: return
        val updated = current.copy(
            name = name ?: current.name,
            colors = (colors ?: current.colors).distinctBy { it.argb }.take(MAX_COLORS_PER_PALETTE),
            tags = tags ?: current.tags,
            note = note ?: current.note,
            updatedAt = System.currentTimeMillis()
        )
        scope.launch { dao.upsert(updated.toEntity()) }
        analyticsTracker.logPaletteUpdated(updated)
    }

    fun toggleSaved(palette: Palette, isCurrentlySaved: Boolean) {
        val targetHash = paletteHash(palette)
        if (isCurrentlySaved) {
            val existing = palettes.value.firstOrNull { paletteHash(it) == targetHash }
            existing?.let { scope.launch { dao.deletePaletteById(it.id) } }
        } else {
            scope.launch { dao.upsert(palette.toEntity()) }
        }
    }

    fun delete(id: String) {
        scope.launch { dao.deletePaletteById(id) }
        analyticsTracker.logPaletteDeleted(id)
    }

    fun clear() {
        scope.launch { dao.clearPalettes() }
    }

    fun addColorToDraftPalette(
        colors: List<PickedColor>,
        pickedColor: PickedColor
    ): AddColorResult {
        val normalized = colors.distinctBy { it.argb }.take(MAX_COLORS_PER_PALETTE)
        if (normalized.any { it.argb == pickedColor.argb }) return AddColorResult.Duplicate
        if (normalized.size >= MAX_COLORS_PER_PALETTE) return AddColorResult.Full
        return AddColorResult.Added(normalized + pickedColor)
    }

    fun paletteHash(palette: Palette): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val payload = palette.colors
            .map { it.argb }
            .sorted()
            .joinToString(separator = ",")
            .toByteArray()
        return digest.digest(payload).joinToString("") { "%02x".format(it) }
    }
}
