package com.primortex.color.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Data access for loading locale-specific color seeds.
 */
@Singleton
class ColorCatalogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val colorCatalogImportService: ColorCatalogImportService
) {
    @Volatile
    private var languageOverrideTag: String? = null

    fun setLanguageOverride(languageTag: String?) {
        languageOverrideTag = languageTag
    }

    fun loadSelectedColors(): List<ColorSeed> {
        return colorCatalogImportService.loadLocaleSeeds(context, languageOverrideTag)
    }
}

/**
 * Orchestrates catalog lifecycle and applies loaded seeds to ColorService.
 */
@Singleton
class ColorCatalogCoordinator @Inject constructor(
    private val repository: ColorCatalogRepository,
    val colorService: ColorService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        refreshColors()
    }

    @Synchronized
    fun setLanguageOverride(languageTag: String? = null) {
        repository.setLanguageOverride(languageTag)
        refreshColors()
    }

    suspend fun loadNow(languageTag: String? = null) {
        repository.setLanguageOverride(languageTag)
        withContext(Dispatchers.Default) {
            colorService.setColors(repository.loadSelectedColors())
        }
    }

    private fun refreshColors() {
        scope.launch {
            colorService.setColors(repository.loadSelectedColors())
        }
    }
}

