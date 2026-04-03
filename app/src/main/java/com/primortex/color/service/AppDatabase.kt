package com.primortex.color.service

import androidx.room.Database
import androidx.room.RoomDatabase
import com.primortex.color.service.palette.PaletteDao
import com.primortex.color.service.palette.PaletteEntity
import com.primortex.color.service.recentpicks.RecentPickHistoryEntity
import com.primortex.color.service.recentpicks.RecentPicksDao
import com.primortex.color.service.recentpicks.SavedPickEntity

@Database(
    entities = [
        RecentPickHistoryEntity::class,
        SavedPickEntity::class,
        PaletteEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recentPicksDao(): RecentPicksDao
    abstract fun paletteDao(): PaletteDao
}
