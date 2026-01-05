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

        fun to(argb: Int, name: String): String {
            val encName = java.net.URLEncoder.encode(name, "UTF-8")
            return "$COLOR?argb=$argb&name=$encName"
        }
    }

    object Info {
        const val COPYRIGHT = "info/copyright"
        const val PRIVACY = "info/privacy"
        const val USAGE = "info/usage"
    }

    object Tool {
        const val SLIDER = "tool/slider"
    }

    object Settings {
        const val LANGUAGE = "settings/language"
    }
}
