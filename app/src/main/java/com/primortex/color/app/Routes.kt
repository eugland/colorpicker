package com.primortex.color.app

object Routes {
    object Tab {
        const val PALETTE = "tab/palette"
        const val CAMERA = "tab/camera"
        const val EXPLORE = "tab/explore"
    }

    object Camera {
        const val LIVE = "cam/live"
        private const val PHOTO_PICK = "cam/photoPick"
        const val PHOTO_PICK_ROUTE = "$PHOTO_PICK?uri={uri}"

        fun photoPickWith(encodedUri: String): String = "$PHOTO_PICK?uri=$encodedUri"
    }

    object Detail {
        const val COLOR = "color/details"
        const val COLOR_ROUTE = "$COLOR?argb={argb}&name={name}"
        const val PALETTE = "palette/details"
        const val PALETTE_ROUTE = "$PALETTE?id={id}&edit={edit}"

        fun to(argb: Int, name: String): String {
            val encName = java.net.URLEncoder.encode(name, "UTF-8")
            return "$COLOR?argb=$argb&name=$encName"
        }

        fun palette(id: String, edit: Boolean = false): String {
            val encodedId = java.net.URLEncoder.encode(id, "UTF-8")
            return "$PALETTE?id=$encodedId&edit=$edit"
        }
    }

    object List {
        const val SWATCH = "swatch/list"
        const val SWATCH_ROUTE = "$SWATCH?type={type}"
        const val PALETTE = "onOpenSavedPalettes"

        fun swatch(type: String): String = "$SWATCH?type=$type"
    }

    object Info {
        const val COPYRIGHT = "info/copyright"
        const val PRIVACY = "info/privacy"
        const val TERMS = "info/terms"
        const val USAGE = "info/usage"
    }

    object Tool {
        const val SLIDER = "tool/slider"
        const val COLOR_BLIND = "tool/color_blind"
    }

    object Settings {
        const val LANGUAGE = "settings/language"
        const val THEME = "settings/theme"
        const val CROSSHAIR = "settings/crosshair"
    }
}

