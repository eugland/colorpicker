package com.primortex.color


import android.app.Application
import com.primortex.color.analytics.AnalyticsTracker
import com.primortex.color.i18n.AppStrings
import com.primortex.color.service.SettingsService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ColorAppApplication : Application() {
    @Inject
    lateinit var settingsService: SettingsService

    override fun onCreate() {
        super.onCreate()
        AnalyticsTracker.init(this)
        AppStrings.init(applicationContext)
    }
}

