package com.primortex.color.di

import android.content.Context
import android.os.Build
import androidx.room.Room
import com.primortex.color.service.AppDatabase
import com.primortex.color.service.ColorService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ColorModule {
    @Provides
    @Singleton
    fun provideColorService(): ColorService {
        return ColorService()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val builder = if (Build.FINGERPRINT.contains("robolectric", ignoreCase = true)) {
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        } else {
            Room.databaseBuilder(context, AppDatabase::class.java, "color_app.db")
        }
        return builder.build()
    }
}
