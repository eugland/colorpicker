package com.primortex.color.data.repo

import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.data.db.ColorDatabaseApi
import com.primortex.color.data.db.dao.PaletteWithColors
import com.primortex.color.data.db.entities.PaletteColorEntity
import com.primortex.color.data.db.entities.PaletteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlPaletteRepository(
    private val database: ColorDatabaseApi
) : PaletteRepository {

    override suspend fun loadPalettes(): List<Palette> = withContext(Dispatchers.IO) {
        database.paletteDao().loadAll().map { it.toModel() }
    }

    override suspend fun upsertPalette(palette: Palette) = withContext(Dispatchers.IO) {
        database.runInTransaction {
            val dao = database.paletteDao()
            dao.upsertPalette(palette.toEntity())
            dao.deleteColors(palette.id)
            dao.insertColors(palette.colors.mapIndexed { index, color ->
                PaletteColorEntity(
                    paletteId = palette.id,
                    argb = color.argb,
                    name = color.name,
                    position = index
                )
            })
        }
    }

    override suspend fun replacePalettes(palettes: List<Palette>) = withContext(Dispatchers.IO) {
        database.runInTransaction {
            val dao = database.paletteDao()
            dao.clearColors()
            dao.clearPalettes()
            palettes.forEach { palette ->
                dao.upsertPalette(palette.toEntity())
                dao.insertColors(palette.colors.mapIndexed { index, color ->
                    PaletteColorEntity(
                        paletteId = palette.id,
                        argb = color.argb,
                        name = color.name,
                        position = index
                    )
                })
            }
        }
    }

    override suspend fun deletePalette(id: String) = withContext(Dispatchers.IO) {
        database.runInTransaction {
            val dao = database.paletteDao()
            dao.deleteColors(id)
            dao.deletePalette(id)
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        database.runInTransaction {
            val dao = database.paletteDao()
            dao.clearColors()
            dao.clearPalettes()
        }
    }

    private fun Palette.toEntity(): PaletteEntity {
        return PaletteEntity(
            id = id,
            name = name,
            tags = tags,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun PaletteWithColors.toModel(): Palette {
        val ordered = colors.sortedBy { it.position }
        return Palette(
            id = palette.id,
            name = palette.name,
            colors = ordered.map { PickedColor(it.argb, it.name) },
            tags = palette.tags,
            note = palette.note,
            createdAt = palette.createdAt,
            updatedAt = palette.updatedAt
        )
    }
}
