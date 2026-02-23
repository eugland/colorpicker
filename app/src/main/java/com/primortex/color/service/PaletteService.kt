package com.primortex.color.service

import android.content.Context
import androidx.room.Room
import com.primortex.color.analytics.AnalyticsTracker
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.service.palette.PaletteDatabase
import com.primortex.color.service.palette.PaletteMetaEntity
import com.primortex.color.service.palette.toDomain
import com.primortex.color.service.palette.toEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.Executors

@Singleton
class PaletteService @Inject constructor(
    @ApplicationContext context: Context,
    private val analyticsTracker: AnalyticsTracker
) {
    companion object {
        const val META_KEY_SEEDED = "seeded_v1"
        const val META_KEY_FIRST_PALETTE_LOGGED = "first_palette_logged_v1"
        const val MAX_COLORS_PER_PALETTE = 5
    }

    private object PaletteDbHolder {
        val executor = Executors.newSingleThreadExecutor()
        @Volatile private var instance: PaletteDatabase? = null

        fun get(context: Context): PaletteDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PaletteDatabase::class.java,
                    "palettes.db"
                )
                    .setQueryExecutor(executor)
                    .setTransactionExecutor(executor)
                    .build()
                    .also { instance = it }
            }
        }
    }

    private val appContext = context.applicationContext
    private val dbExecutor = PaletteDbHolder.executor
    private val scope = CoroutineScope(SupervisorJob() + dbExecutor.asCoroutineDispatcher())
    private val dao = PaletteDbHolder.get(appContext).paletteDao()
    private val _palettes = MutableStateFlow<List<Palette>>(emptyList())
    val palettes: StateFlow<List<Palette>> = _palettes
    private val _previewPalettes = MutableStateFlow<List<Palette>>(emptyList())
    val previewPalettes: StateFlow<List<Palette>> = _previewPalettes
    private var firstPaletteLogged = false

    init {
        runBlocking(scope.coroutineContext) {
            _palettes.value = dao.loadAll().map { it.toDomain() }
            firstPaletteLogged = dao.getMeta(META_KEY_FIRST_PALETTE_LOGGED) == "true"
            seedIfNeeded()
        }
    }

    private suspend fun seedIfNeeded() {
        if (dao.getMeta(META_KEY_SEEDED) == "true") return

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

        val seeded = listOf(uiNeutrals, mutedNature)
        _palettes.value = seeded
        dao.clearPalettes()
        dao.insertAll(seeded.map { it.toEntity() })
        dao.upsertMeta(PaletteMetaEntity(META_KEY_SEEDED, "true"))
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
            colors = colors.distinctBy { it.argb }.take(MAX_COLORS_PER_PALETTE),
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
        analyticsTracker.logPaletteCreated(p, creationSource)
        if (saveOnCreate && !firstPaletteLogged) {
            analyticsTracker.logFirstPaletteCreated(p)
            firstPaletteLogged = true
            scope.launch {
                dao.upsertMeta(PaletteMetaEntity(META_KEY_FIRST_PALETTE_LOGGED, "true"))
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
                    colors = (colors ?: p.colors).distinctBy { it.argb }.take(MAX_COLORS_PER_PALETTE),
                    tags = tags ?: p.tags,
                    note = note ?: p.note,
                    updatedAt = now
                ).also { updatedPalette = it }
            }
        }
        persist()
        updatedPalette?.let { analyticsTracker.logPaletteUpdated(it) }
    }

    fun toggleSaved(palette: Palette, isCurrentlySaved: Boolean) {
        val targetHash = paletteHash(palette)
        if (isCurrentlySaved) {
            _palettes.update { list -> list.filterNot { paletteHash(it) == targetHash } }
        } else {
            _palettes.update { list ->
                listOf(palette) + list.filterNot { paletteHash(it) == targetHash }
            }
            _previewPalettes.update { it.filterNot { p -> p.id == palette.id } }
        }
        persist()
    }

    fun delete(id: String) {
        _palettes.update { it.filterNot { p -> p.id == id } }
        persist()
        analyticsTracker.logPaletteDeleted(id)
    }

    fun clear() {
        _palettes.value = emptyList()
        _previewPalettes.value = emptyList()
        persist()
    }

    private fun persist() {
        val snapshot = _palettes.value
        scope.launch {
            dao.clearPalettes()
            if (snapshot.isNotEmpty()) {
                dao.insertAll(snapshot.map { it.toEntity() })
            }
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

