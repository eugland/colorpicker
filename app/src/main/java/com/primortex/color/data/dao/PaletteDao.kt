package com.primortex.color.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.primortex.color.data.entity.PaletteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaletteDao {
    @Query("SELECT * FROM palettes ORDER BY updatedAt DESC")
    fun observePalettes(): Flow<List<PaletteEntity>>

    @Query("SELECT COUNT(*) FROM palettes")
    suspend fun paletteCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPalette(entity: PaletteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPalettes(entities: List<PaletteEntity>)

    @Query("DELETE FROM palettes WHERE id = :id")
    suspend fun deletePalette(id: String)

    @Query("DELETE FROM palettes")
    suspend fun clearPalettes()
}
