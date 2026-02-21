package com.primortex.color.service

import com.primortex.color.analytics.AnalyticsClient
import com.primortex.color.analytics.AnalyticsTracker
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import com.primortex.color.data.enums.AppLanguage
import com.primortex.color.data.enums.CrosshairShape
import com.primortex.color.data.enums.CrosshairSize
import com.primortex.color.data.enums.PickerSensitivity
import com.primortex.color.data.enums.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class SettingsServiceUnitTest {

    @Test
    fun setters_updateInMemoryState() {
        val context = RuntimeEnvironment.getApplication()
        val service = SettingsService(
            context = context,
            colorCatalogCoordinator = ColorCatalogCoordinator(
                ColorCatalogRepository(context, ColorCatalogImportService()),
                ColorService(emptyList())
            ),
            analyticsTracker = AnalyticsTracker(testAnalyticsClient())
        )

        waitForInit()
        service.setCrosshairSize(CrosshairSize.Large)
        service.setCrosshairShape(CrosshairShape.Cross)
        service.setThemeMode(ThemeMode.DARK)
        service.setPickerSensitivity(PickerSensitivity.High)

        assertEquals(CrosshairSize.Large, service.crosshairSize.value)
        assertEquals(CrosshairShape.Cross, service.crosshairShape.value)
        assertEquals(ThemeMode.DARK, service.themeMode.value)
        assertEquals(PickerSensitivity.High, service.pickerSensitivity.value)
    }

    @Test
    fun setAppLanguage_updatesState_andReloadsCatalog() {
        val context = RuntimeEnvironment.getApplication()
        val colorService = ColorService(emptyList())
        val service = SettingsService(
            context = context,
            colorCatalogCoordinator = ColorCatalogCoordinator(
                ColorCatalogRepository(context, ColorCatalogImportService()),
                colorService
            ),
            analyticsTracker = AnalyticsTracker(testAnalyticsClient())
        )

        waitForInit()
        colorService.setColors(emptyList())
        assertFalse(colorService.allColors().isNotEmpty())

        service.setAppLanguage(AppLanguage.Japanese)

        waitUntil { colorService.allColors().isNotEmpty() }
        assertEquals(AppLanguage.Japanese, service.appLanguage.value)
        assertEquals(143, colorService.allColors().size)
    }

    private fun waitForInit() {
        Thread.sleep(300)
    }

    private fun waitUntil(timeoutMs: Long = 3000, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw AssertionError("Timed out waiting for condition")
            }
            Thread.sleep(25)
        }
    }

    private fun testAnalyticsClient() = object : AnalyticsClient {
        override fun logColorPicked(pick: PickedColor, source: String) = Unit
        override fun logColorSaved(pick: PickedColor, action: String) = Unit
        override fun logPaletteCreated(palette: Palette, source: String) = Unit
        override fun logPaletteUpdated(palette: Palette) = Unit
        override fun logPaletteDeleted(paletteId: String) = Unit
        override fun logRecentsCleared() = Unit
        override fun logSavedCleared() = Unit
        override fun logFirstColorPick(pick: PickedColor) = Unit
        override fun logFirstColorSaved(pick: PickedColor) = Unit
        override fun logFirstPaletteCreated(palette: Palette) = Unit
        override fun logFirstUse() = Unit
        override fun logScreenView(screenName: String, screenClass: String) = Unit
    }
}
