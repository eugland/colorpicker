package com.primortex.color.analytics

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor

class FirebaseAnalyticsClient : AnalyticsClient {
    private val analytics by lazy { Firebase.analytics }

    override fun init(app: Application) {
        FirebaseApp.initializeApp(app)
    }

    override fun logColorPicked(pick: PickedColor, source: String) {
        analytics.logEvent("color_pick") {
            param("source", source)
            param("color_argb", pick.argb.toLong())
            param("color_name", pick.name)
            param("color_hex", pick.argb.toHex())
        }
    }

    override fun logColorSaved(pick: PickedColor, action: String) {
        analytics.logEvent("color_saved") {
            param("action", action)
            param("color_argb", pick.argb.toLong())
            param("color_name", pick.name)
            param("color_hex", pick.argb.toHex())
        }
    }

    override fun logPaletteCreated(palette: Palette, source: String) {
        analytics.logEvent("palette_create") {
            param("source", source)
            param("palette_id", palette.id)
            param("palette_name", palette.name)
            param("color_count", palette.colors.size.toLong())
        }
    }

    override fun logPaletteUpdated(palette: Palette) {
        analytics.logEvent("palette_update") {
            param("palette_id", palette.id)
            param("color_count", palette.colors.size.toLong())
            param("tag_count", palette.tags.size.toLong())
        }
    }

    override fun logPaletteDeleted(paletteId: String) {
        analytics.logEvent("palette_delete") {
            param("palette_id", paletteId)
        }
    }

    override fun logRecentsCleared() {
        analytics.logEvent("recents_clear", null)
    }

    override fun logSavedCleared() {
        analytics.logEvent("saved_clear", null)
    }

    override fun logFirstColorPick(pick: PickedColor) {
        analytics.logEvent("first_color_pick") {
            param("color_argb", pick.argb.toLong())
            param("color_name", pick.name)
            param("color_hex", pick.argb.toHex())
        }
    }

    override fun logFirstColorSaved(pick: PickedColor) {
        analytics.logEvent("first_color_saved") {
            param("color_argb", pick.argb.toLong())
            param("color_name", pick.name)
            param("color_hex", pick.argb.toHex())
        }
    }

    override fun logFirstPaletteCreated(palette: Palette) {
        analytics.logEvent("first_palette_create") {
            param("palette_id", palette.id)
            param("palette_name", palette.name)
            param("color_count", palette.colors.size.toLong())
        }
    }

    override fun logFirstUse() {
        analytics.logEvent("first_use", null)
    }

    override fun logScreenView(screenName: String, screenClass: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
    }

    private fun Int.toHex(): String = String.format("#%06X", 0xFFFFFF and this)
}
