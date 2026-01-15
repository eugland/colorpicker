package com.primortex.color.app

import androidx.navigation.NavController

class NavColorNavigator(
    private val nav: NavController
) : ColorNavigator {
    override fun openPaletteTab() {
        nav.navigate(Routes.Tab.PALETTE) {
            popUpTo(Routes.Tab.CAMERA) { inclusive = false }
            launchSingleTop = true
        }
    }

    override fun openCameraTab() {
        nav.navigate(Routes.Tab.CAMERA) {
            popUpTo(Routes.Tab.CAMERA) { inclusive = true }
            launchSingleTop = true
        }
    }

    override fun openExploreTab() {
        nav.navigate(Routes.Tab.EXPLORE) {
            popUpTo(Routes.Tab.CAMERA) { inclusive = false }
            launchSingleTop = true
        }
    }

    override fun openLiveCamera() {
        nav.navigate(Routes.Camera.LIVE)
    }

    override fun openColorSlider() {
        nav.navigate(Routes.Tool.SLIDER)
    }

    override fun openColorBlindEnhancer() {
        nav.navigate(Routes.Tool.COLOR_BLIND)
    }

    override fun openPhotoPick(uriString: String) {
        val encoded = java.net.URLEncoder.encode(uriString, "UTF-8")
        nav.navigate(Routes.Camera.photoPickWith(encoded))
    }

    override fun openPaletteDetail(id: String, edit: Boolean) {
        nav.navigate(Routes.Detail.palette(id, edit))
    }

    override fun openColorDetail(argb: Int, name: String) {
        nav.navigate(Routes.Detail.to(argb, name))
    }

    override fun openInfoCopyright() {
        nav.navigate(Routes.Info.COPYRIGHT)
    }

    override fun openInfoPrivacy() {
        nav.navigate(Routes.Info.PRIVACY)
    }

    override fun openInfoTerms() {
        nav.navigate(Routes.Info.TERMS)
    }

    override fun openInfoUsage() {
        nav.navigate(Routes.Info.USAGE)
    }

    override fun openLanguageSettings() {
        nav.navigate(Routes.Settings.LANGUAGE)
    }

    override fun openThemeSettings() {
        nav.navigate(Routes.Settings.THEME)
    }

    override fun openCrosshairSettings() {
        nav.navigate(Routes.Settings.CROSSHAIR)
    }

    override fun openRecentColors() {
        nav.navigate(Routes.List.swatch("recent"))
    }

    override fun openSavedColors() {
        nav.navigate(Routes.List.swatch("saved"))
    }

    override fun back() {
        nav.popBackStack()
    }
}
