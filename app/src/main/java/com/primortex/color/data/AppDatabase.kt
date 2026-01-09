package com.primortex.color.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.primortex.color.data.converters.RoomConverters
import com.primortex.color.data.dao.PaletteDao
import com.primortex.color.data.dao.RecentPickDao
import com.primortex.color.data.entity.PaletteEntity
import com.primortex.color.data.entity.RecentHistoryEntity
import com.primortex.color.data.entity.SavedColorEntity

@Database(
    entities = [
        RecentHistoryEntity::class,
        SavedColorEntity::class,
        PaletteEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recentPickDao(): RecentPickDao

    abstract fun paletteDao(): PaletteDao
}
