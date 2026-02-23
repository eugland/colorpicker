package com.primortex.color.service.palette

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PaletteEntity::class, PaletteMetaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PaletteDatabase : RoomDatabase() {
    abstract fun paletteDao(): PaletteDao
}
