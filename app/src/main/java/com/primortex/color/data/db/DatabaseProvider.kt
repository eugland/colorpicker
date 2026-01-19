package com.primortex.color.data.db

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    private const val DATABASE_NAME = "colorpicker.db"
    @Volatile
    private var database: ColorDatabase? = null

    fun init(context: Context) {
        if (database != null) return
        synchronized(this) {
            if (database == null) {
                database = Room.databaseBuilder(
                    context.applicationContext,
                    ColorDatabase::class.java,
                    DATABASE_NAME
                ).build()
            }
        }
    }

    fun getDatabase(): ColorDatabase {
        return checkNotNull(database) { "DatabaseProvider.init must be called first" }
    }
}
