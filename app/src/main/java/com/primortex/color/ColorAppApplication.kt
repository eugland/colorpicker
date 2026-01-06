package com.primortex.color


import android.app.Application
import com.primortex.color.service.ColorService
import com.primortex.color.service.PaletteService
import com.primortex.color.service.RecentPicksService
import com.primortex.color.service.SettingsService

class ColorAppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ColorService.init(applicationContext)
        RecentPicksService.init(applicationContext)
        PaletteService.init(applicationContext)
        SettingsService.init(applicationContext)
    }
}