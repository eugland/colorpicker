package com.primortex.color.analytics

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor

object AnalyticsTracker {
    private val analytics by lazy { Firebase.analytics }

    fun init(app: Application) {
        FirebaseApp.initializeApp(app)
    }

    fun logColorPicked(pick: PickedColor, source: String) {
        analytics.logEvent("color_pick") {
            param("source", source)
            param("color_argb", pick.argb.toLong())
            param("color_name", pick.name)
            param("color_hex", pick.argb.toHex())
        }
    }

    fun logColorSaved(pick: PickedColor, action: String) {
        analytics.logEvent("color_saved") {
            param("action", action)
            param("color_argb", pick.argb.toLong())
            param("color_name", pick.name)
            param("color_hex", pick.argb.toHex())
        }
    }

    fun logPaletteCreated(palette: Palette, source: String) {
        analytics.logEvent("palette_create") {
            param("source", source)
            param("palette_id", palette.id)
            param("palette_name", palette.name)
            param("color_count", palette.colors.size.toLong())
        }
    }

    private fun Int.toHex(): String = String.format("#%06X", 0xFFFFFF and this)
}
