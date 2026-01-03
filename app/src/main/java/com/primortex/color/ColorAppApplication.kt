package com.primortex.color


import android.app.Application
import com.primortex.color.service.PaletteService
import com.primortex.color.service.RecentPicksService
import com.primortex.color.service.SettingsService

class ColorAppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RecentPicksService.init(applicationContext)
        PaletteService.init(applicationContext)
        SettingsService.init(applicationContext)
    }
}