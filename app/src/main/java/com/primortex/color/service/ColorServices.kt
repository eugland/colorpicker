package com.primortex.color.service

import android.content.Context

/**
 * Application-level service locator to share the color pipeline.
 *
 * Loads selected JSON color packs into memory and provides nearest-name matching.
 */
object ColorServices {
    private lateinit var appContext: Context
    private lateinit var colorService: ColorService
    @Volatile
    var selectedPalette: com.primortex.color.app.Palette? = null

    @Volatile
    private var selectedAssetIds: Set<String> = setOf("colors")

    fun init(context: Context) {
        if (::colorService.isInitialized) return
        appContext = context.applicationContext
        colorService = buildService(appContext)
    }

    fun ensure(context: Context) {
        if (!::colorService.isInitialized) {
            init(context)
        }
    }

    @Synchronized
    fun setCatalogSelection(assetIds: Set<String>) {
        selectedAssetIds = if (::appContext.isInitialized) {
            ColorCatalogImportService.normalizeSelection(appContext, assetIds)
        } else {
            assetIds.filter { it.isNotBlank() }.toSet()
        }

        if (::colorService.isInitialized) {
            colorService.setColors(loadSelectedColors(appContext))
        }
    }

    @Synchronized
    fun reloadCatalog() {
        if (::colorService.isInitialized) {
            colorService.setColors(loadSelectedColors(appContext))
        }
    }

    val colors: ColorService
        get() {
            check(::colorService.isInitialized) { "ColorServices.init must be called first" }
            return colorService
        }

    private fun buildService(context: Context): ColorService {
        selectedAssetIds = ColorCatalogImportService.normalizeSelection(context, selectedAssetIds)
        return ColorService(loadSelectedColors(context))
    }

    private fun loadSelectedColors(context: Context): List<ColorSeed> {
        return ColorCatalogImportService.loadSelectedSeeds(context, selectedAssetIds)
    }
}
