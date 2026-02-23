package com.primortex.color.service

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.primortex.color.analytics.AnalyticsTracker
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.service.palette.PaletteDatabase
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
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.Executors

@Singleton
class PaletteService @Inject constructor(
    @ApplicationContext context: Context,
    private val analyticsTracker: AnalyticsTracker
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
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS palette_meta")
            }
        }
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
                    .addMigrations(MIGRATION_1_2)
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
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _palettes = MutableStateFlow<List<Palette>>(emptyList())
    val palettes: StateFlow<List<Palette>> = _palettes
    private val _previewPalettes = MutableStateFlow<List<Palette>>(emptyList())
    val previewPalettes: StateFlow<List<Palette>> = _previewPalettes
    private var firstPaletteLogged = false

    init {
        scope.launch {
            _palettes.value = dao.loadAll().map { it.toDomain() }
        }
        firstPaletteLogged = prefs.getBoolean(PREF_KEY_FIRST_PALETTE_LOGGED, false)
    }

    suspend fun seedPalettesIfEmpty(palettes: List<Palette>) {
        if (dao.loadAll().isEmpty()) {
            _palettes.value = palettes
            dao.clearPalettes()
            if (palettes.isNotEmpty()) {
                dao.insertAll(palettes.map { it.toEntity() })
            }
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

    fun addColorToDraftPalette(
        colors: List<PickedColor>,
        pickedColor: PickedColor
    ): AddColorResult {
        val normalized = colors.distinctBy { it.argb }.take(MAX_COLORS_PER_PALETTE)
        if (normalized.any { it.argb == pickedColor.argb }) {
            return AddColorResult.Duplicate
        }
        if (normalized.size >= MAX_COLORS_PER_PALETTE) {
            return AddColorResult.Full
        }
        return AddColorResult.Added(normalized + pickedColor)
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

