package com.primortex.color


import android.app.Application
import com.primortex.color.service.PaletteService
import com.primortex.color.service.RecentPicksService

class ColorAppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RecentPicksService.init(applicationContext)
        PaletteService.init(applicationContext)
    }
}