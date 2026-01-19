package com.primortex.color.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.primortex.color.data.db.entities.PaletteColorEntity
import com.primortex.color.data.db.entities.PaletteEntity

@Dao
interface PaletteDao {
    @Transaction
    @Query("SELECT * FROM palettes ORDER BY updatedAt DESC")
    suspend fun loadAll(): List<PaletteWithColors>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsertPalette(entity: PaletteEntity)

    @Insert
    suspend fun insertColors(colors: List<PaletteColorEntity>)

    @Query("DELETE FROM palette_colors WHERE paletteId = :paletteId")
    suspend fun deleteColors(paletteId: String)

    @Query("DELETE FROM palettes WHERE id = :paletteId")
    suspend fun deletePalette(paletteId: String)

    @Query("DELETE FROM palette_colors")
    suspend fun clearColors()

    @Query("DELETE FROM palettes")
    suspend fun clearPalettes()
}

data class PaletteWithColors(
    @Embedded val palette: PaletteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "paletteId"
    )
    val colors: List<PaletteColorEntity>
)
