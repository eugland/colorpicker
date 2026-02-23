package com.primortex.color.service.palette

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PaletteDao {
    @Query("SELECT * FROM palette ORDER BY updatedAt DESC")
    suspend fun loadAll(): List<PaletteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PaletteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: PaletteMetaEntity)

    @Query("SELECT value FROM palette_meta WHERE `key` = :key LIMIT 1")
    suspend fun getMeta(key: String): String?

    @Query("DELETE FROM palette")
    suspend fun clearPalettes()

    @Query("DELETE FROM palette WHERE id = :id")
    suspend fun deletePaletteById(id: String)
}
