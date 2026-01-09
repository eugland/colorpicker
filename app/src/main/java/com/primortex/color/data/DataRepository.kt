package com.primortex.color.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.data.entity.PaletteEntity
import com.primortex.color.data.entity.RecentHistoryEntity
import com.primortex.color.data.entity.SavedColorEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataRepository private constructor(private val database: AppDatabase) {
    private val recentPickDao = database.recentPickDao()
    private val paletteDao = database.paletteDao()

    fun observeHistory(limit: Int): Flow<List<PickedColor>> {
        return recentPickDao.observeHistory(limit).map { list ->
            list.map { PickedColor(it.argb, it.name) }
        }
    }

    fun observeSaved(limit: Int): Flow<List<PickedColor>> {
        return recentPickDao.observeSaved(limit).map { list ->
            list.map { PickedColor(it.argb, it.name) }
        }
    }

    suspend fun addHistory(pick: PickedColor, limit: Int) {
        val entity = RecentHistoryEntity(
            argb = pick.argb,
            name = pick.name,
            createdAt = System.currentTimeMillis()
        )
        recentPickDao.upsertHistory(entity)
        recentPickDao.trimHistory(limit)
    }

    suspend fun clearHistory() {
        recentPickDao.clearHistory()
    }

    suspend fun addSaved(pick: PickedColor, limit: Int) {
        val entity = SavedColorEntity(
            argb = pick.argb,
            name = pick.name,
            createdAt = System.currentTimeMillis()
        )
        recentPickDao.upsertSaved(entity)
        recentPickDao.trimSaved(limit)
    }

    suspend fun removeSaved(argb: Int) {
        recentPickDao.deleteSaved(argb)
    }

    suspend fun clearSaved() {
        recentPickDao.clearSaved()
    }

    fun observePalettes(): Flow<List<Palette>> {
        return paletteDao.observePalettes().map { list ->
            list.map { entity ->
                Palette(
                    id = entity.id,
                    name = entity.name,
                    colors = entity.colors,
                    tags = entity.tags,
                    note = entity.note,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt
                )
            }
        }
    }

    suspend fun upsertPalette(palette: Palette) {
        paletteDao.upsertPalette(palette.toEntity())
    }

    suspend fun deletePalette(id: String) {
        paletteDao.deletePalette(id)
    }

    suspend fun clearPalettes() {
        paletteDao.clearPalettes()
    }

    suspend fun seedRecentsIfNeeded(limit: Int) {
        database.withTransaction {
            if (recentPickDao.historyCount() == 0) {
                val initHistory = listOf(
                    PickedColor(0xFF0F172A.toInt(), "Slate 900"),
                    PickedColor(0xFF475569.toInt(), "Slate 600"),
                    PickedColor(0xFFA1A1AA.toInt(), "Zinc 400"),
                    PickedColor(0xFF0EA5E9.toInt(), "Sky 500"),
                    PickedColor(0xFF10B981.toInt(), "Emerald 500"),
                )
                val baseTime = System.currentTimeMillis()
                recentPickDao.upsertHistory(
                    initHistory.mapIndexed { index, pick ->
                        RecentHistoryEntity(
                            argb = pick.argb,
                            name = pick.name,
                            createdAt = baseTime - index
                        )
                    }
                )
                recentPickDao.trimHistory(limit)
            }
            if (recentPickDao.savedCount() == 0) {
                val initSaved = listOf(
                    PickedColor(0xFF2F5D50.toInt(), "Forest"),
                    PickedColor(0xFF7A9B76.toInt(), "Moss"),
                    PickedColor(0xFFE6D5B8.toInt(), "Sand"),
                    PickedColor(0xFFC97C5D.toInt(), "Clay"),
                    PickedColor(0xFF3A3A3A.toInt(), "Ink"),
                )
                val baseTime = System.currentTimeMillis()
                recentPickDao.upsertSaved(
                    initSaved.mapIndexed { index, pick ->
                        SavedColorEntity(
                            argb = pick.argb,
                            name = pick.name,
                            createdAt = baseTime - index
                        )
                    }
                )
                recentPickDao.trimSaved(limit)
            }
        }
    }

    suspend fun seedPalettesIfNeeded() {
        database.withTransaction {
            if (paletteDao.paletteCount() != 0) return@withTransaction

            val now = System.currentTimeMillis()

            val uiNeutrals = Palette(
                id = java.util.UUID.randomUUID().toString(),
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

            fun mutedNaturePalette(): Palette {
                return Palette(
                    id = java.util.UUID.randomUUID().toString(),
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
            }

            val palettes = listOf(
                uiNeutrals,
                mutedNaturePalette(),
                mutedNaturePalette(),
                mutedNaturePalette(),
                mutedNaturePalette(),
                mutedNaturePalette(),
                mutedNaturePalette(),
                mutedNaturePalette(),
                mutedNaturePalette(),
                mutedNaturePalette(),
            ).mapIndexed { index, palette ->
                palette.copy(createdAt = now - index, updatedAt = now - index)
            }

            paletteDao.upsertPalettes(palettes.map { it.toEntity() })
        }
    }

    private fun Palette.toEntity(): PaletteEntity {
        return PaletteEntity(
            id = id,
            name = name,
            colors = colors,
            tags = tags,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        @Volatile
        private var instance: DataRepository? = null

        fun getInstance(context: Context): DataRepository {
            return instance ?: synchronized(this) {
                instance ?: DataRepository(
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "colorpicker.db"
                    ).build()
                ).also { instance = it }
            }
        }
    }
}
