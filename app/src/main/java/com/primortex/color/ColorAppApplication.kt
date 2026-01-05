package com.primortex.color


import androidx.appcompat.app.AppCompatApplication
import com.primortex.color.service.PaletteService
import com.primortex.color.service.RecentPicksService
import com.primortex.color.service.SettingsService

class ColorAppApplication : AppCompatApplication() {
    override fun onCreate() {
        super.onCreate()
        RecentPicksService.init(applicationContext)
        PaletteService.init(applicationContext)
        SettingsService.init(applicationContext)
    }
}