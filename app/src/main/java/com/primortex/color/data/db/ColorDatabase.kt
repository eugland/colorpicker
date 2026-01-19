package com.primortex.color.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.primortex.color.data.db.dao.PaletteDao
import com.primortex.color.data.db.dao.RecentPickDao
import com.primortex.color.data.db.entities.PaletteColorEntity
import com.primortex.color.data.db.entities.PaletteEntity
import com.primortex.color.data.db.entities.RecentPickEntity

interface ColorDatabaseApi {
    fun recentPickDao(): RecentPickDao
    fun paletteDao(): PaletteDao
    suspend fun <T> runInTransaction(block: suspend () -> T): T
}

@Database(
    entities = [
        RecentPickEntity::class,
        PaletteEntity::class,
        PaletteColorEntity::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class ColorDatabase : RoomDatabase(), ColorDatabaseApi {
    abstract override fun recentPickDao(): RecentPickDao
    abstract override fun paletteDao(): PaletteDao

    override suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return androidx.room.withTransaction { block() }
    }
}
