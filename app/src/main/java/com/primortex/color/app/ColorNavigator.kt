package com.primortex.color.app

interface ColorNavigator {
    fun openPaletteTab()
    fun openCameraTab()
    fun openExploreTab()
    fun openLiveCamera()
    fun openColorSlider()
    fun openColorBlindEnhancer()
    fun openPhotoPick(uriString: String)
    fun openPaletteDetail(id: String, edit: Boolean = false)
    fun openColorDetail(argb: Int, name: String = "")
    fun openInfoCopyright()
    fun openInfoPrivacy()
    fun openInfoTerms()
    fun openInfoUsage()
    fun openLanguageSettings()
    fun back()
}
