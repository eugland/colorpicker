package com.primortex.color.service.palette

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PaletteEntity::class],
    version = 2,
    exportSchema = false
)
abstract class PaletteDatabase : RoomDatabase() {
    abstract fun paletteDao(): PaletteDao
}
