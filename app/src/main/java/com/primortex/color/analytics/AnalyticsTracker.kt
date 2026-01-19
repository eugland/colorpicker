package com.primortex.color.analytics

import android.app.Application
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor

interface AnalyticsClient {
    fun init(app: Application)
    fun logColorPicked(pick: PickedColor, source: String)
    fun logColorSaved(pick: PickedColor, action: String)
    fun logPaletteCreated(palette: Palette, source: String)
    fun logPaletteUpdated(palette: Palette)
    fun logPaletteDeleted(paletteId: String)
    fun logRecentsCleared()
    fun logSavedCleared()
    fun logFirstColorPick(pick: PickedColor)
    fun logFirstColorSaved(pick: PickedColor)
    fun logFirstPaletteCreated(palette: Palette)
    fun logFirstUse()
    fun logScreenView(screenName: String, screenClass: String)
}

object AnalyticsTracker {
    @Volatile
    private var client: AnalyticsClient = FirebaseAnalyticsClient()

    fun setClient(analyticsClient: AnalyticsClient) {
        client = analyticsClient
    }

    fun init(app: Application) {
        client.init(app)
    }

    fun logColorPicked(pick: PickedColor, source: String) {
        client.logColorPicked(pick, source)
    }

    fun logColorSaved(pick: PickedColor, action: String) {
        client.logColorSaved(pick, action)
    }

    fun logPaletteCreated(palette: Palette, source: String) {
        client.logPaletteCreated(palette, source)
    }

    fun logPaletteUpdated(palette: Palette) {
        client.logPaletteUpdated(palette)
    }

    fun logPaletteDeleted(paletteId: String) {
        client.logPaletteDeleted(paletteId)
    }

    fun logRecentsCleared() {
        client.logRecentsCleared()
    }

    fun logSavedCleared() {
        client.logSavedCleared()
    }

    fun logFirstColorPick(pick: PickedColor) {
        client.logFirstColorPick(pick)
    }

    fun logFirstColorSaved(pick: PickedColor) {
        client.logFirstColorSaved(pick)
    }

    fun logFirstPaletteCreated(palette: Palette) {
        client.logFirstPaletteCreated(palette)
    }

    fun logFirstUse() {
        client.logFirstUse()
    }

    fun logScreenView(screenName: String, screenClass: String) {
        client.logScreenView(screenName, screenClass)
    }
}
