package com.primortex.color.service.recentpicks

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RecentPickHistoryEntity::class,
        SavedPickEntity::class,
        RecentPicksMetaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RecentPicksDatabase : RoomDatabase() {
    abstract fun recentPicksDao(): RecentPicksDao
}
