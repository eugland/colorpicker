package com.primortex.color.app

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

class NavColorNavigator(
    private val nav: NavController
) : ColorNavigator {
    private fun NavController.navigateIfNotCurrent(route: String) {
        if (currentDestination?.route == route) return
        navigate(route)
    }

    private fun NavController.navigateIfNotCurrent(
        route: String,
        builder: NavOptionsBuilder.() -> Unit
    ) {
        if (currentDestination?.route == route) return
        navigate(route, builder)
    }

    override fun openPaletteTab() {
        nav.navigateIfNotCurrent(Routes.Tab.PALETTE) {
            popUpTo(Routes.Tab.PALETTE) { inclusive = false }
        }
    }

    override fun openCameraTab() {
        nav.navigateIfNotCurrent(Routes.Tab.CAMERA) {
            popUpTo(Routes.Tab.PALETTE) { inclusive = false }
        }
    }

    override fun openExploreTab() {
        nav.navigateIfNotCurrent(Routes.Tab.EXPLORE) {
            popUpTo(Routes.Tab.PALETTE) { inclusive = false }
        }
    }

    override fun openLiveCamera() {
        nav.navigateIfNotCurrent(Routes.Camera.LIVE)
    }

    override fun openColorSlider() {
        nav.navigateIfNotCurrent(Routes.Tool.SLIDER)
    }

    override fun openColorBlindEnhancer() {
        nav.navigateIfNotCurrent(Routes.Tool.COLOR_BLIND)
    }

    override fun openPhotoPick(uriString: String) {
        val encoded = java.net.URLEncoder.encode(uriString, "UTF-8")
        nav.navigate(Routes.Camera.photoPickWith(encoded))
    }

    override fun openPaletteDetail(id: String, edit: Boolean) {
        nav.navigate(Routes.Detail.palette(id, edit)) {
            popUpTo(Routes.Detail.PALETTE) { inclusive = true }
        }
    }

    override fun openColorDetail(argb: Int, name: String) {
        nav.navigate(Routes.Detail.to(argb, name)) {
            popUpTo(Routes.Detail.COLOR) { inclusive = true }
        }
    }

    override fun openInfoCopyright() {
        nav.navigateIfNotCurrent(Routes.Info.COPYRIGHT)
    }

    override fun openInfoPrivacy() {
        nav.navigateIfNotCurrent(Routes.Info.PRIVACY)
    }

    override fun openInfoTerms() {
        nav.navigateIfNotCurrent(Routes.Info.TERMS)
    }

    override fun openInfoUsage() {
        nav.navigateIfNotCurrent(Routes.Info.USAGE)
    }

    override fun openLanguageSettings() {
        nav.navigateIfNotCurrent(Routes.Settings.LANGUAGE)
    }

    override fun openThemeSettings() {
        nav.navigateIfNotCurrent(Routes.Settings.THEME)
    }

    override fun openColorCatalogSettings() {
        nav.navigateIfNotCurrent(Routes.Settings.COLOR_CATALOGS)
    }

    override fun openCrosshairSettings() {
        nav.navigateIfNotCurrent(Routes.Settings.CROSSHAIR)
    }

    override fun openRecentColors() {
        if (nav.currentDestination?.route == Routes.List.SWATCH_ROUTE &&
            nav.currentBackStackEntry?.arguments?.getString("type") == "recent"
        ) return
        nav.navigate(Routes.List.swatch("recent"))
    }

    override fun openSavedColors() {
        if (nav.currentDestination?.route == Routes.List.SWATCH_ROUTE &&
            nav.currentBackStackEntry?.arguments?.getString("type") == "saved"
        ) return
        nav.navigate(Routes.List.swatch("saved"))
    }

    override fun openSavedPalettes() {
        nav.navigateIfNotCurrent(Routes.List.PALETTE)
    }

    override fun back() {
        nav.popBackStack()
    }
}
