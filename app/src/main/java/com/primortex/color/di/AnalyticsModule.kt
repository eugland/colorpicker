package com.primortex.color.di

import com.primortex.color.analytics.AnalyticsClient
import com.primortex.color.analytics.FirebaseAnalyticsClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {
    @Binds
    @Singleton
    abstract fun bindAnalyticsClient(client: FirebaseAnalyticsClient): AnalyticsClient
}

