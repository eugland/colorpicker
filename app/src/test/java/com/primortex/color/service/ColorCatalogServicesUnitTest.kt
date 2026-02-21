package com.primortex.color.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ColorCatalogServicesUnitTest {
    private val context = RuntimeEnvironment.getApplication()
    private val importService = ColorCatalogImportService()

    @Test
    fun importService_loadsDefaultCatalog_whenNoOverride() {
        val seeds = importService.loadLocaleSeeds(context, null)
        assertEquals(1013, seeds.size)
    }

    @Test
    fun importService_loadsLocaleSpecificCatalog_whenOverrideProvided() {
        val jaSeeds = importService.loadLocaleSeeds(context, "ja")
        assertEquals(143, jaSeeds.size)
    }

    @Test
    fun coordinator_appliesLoadedCatalog_toColorService() {
        val repository = ColorCatalogRepository(context, importService)
        val colorService = ColorService(emptyList())
        val coordinator = ColorCatalogCoordinator(repository, colorService)

        coordinator.setLanguageOverride("zh")

        val all = colorService.allColors()
        assertFalse(all.isEmpty())
        assertEquals(165, all.size)
    }
}
