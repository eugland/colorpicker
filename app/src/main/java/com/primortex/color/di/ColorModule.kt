package com.primortex.color.di

import com.primortex.color.service.ColorService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}

