package com.primortex.color


import android.app.Application
import com.primortex.color.analytics.AnalyticsTracker
import com.primortex.color.i18n.AppStrings
import com.primortex.color.service.ColorCatalogImportService
import com.primortex.color.service.ColorServices
import com.primortex.color.service.PaletteService
import com.primortex.color.service.RecentPicksService
import com.primortex.color.service.SettingsService

class ColorAppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AnalyticsTracker.init(this)
        val bootstrapCatalogs = ColorCatalogImportService.bootstrapDefault(applicationContext)
        ColorServices.setCatalogSelection(bootstrapCatalogs)
        ColorServices.init(applicationContext)
        RecentPicksService.init(applicationContext)
        PaletteService.init(applicationContext)
        SettingsService.init(applicationContext)
        AppStrings.init(applicationContext)
    }
}
