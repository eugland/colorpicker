package com.primortex.color.screens

sealed interface ColorDetailsEffect {
    data class CopyHex(val hex: String) : ColorDetailsEffect
    data class ShowMessage(val message: String) : ColorDetailsEffect
    data class OpenPalette(val id: String, val edit: Boolean) : ColorDetailsEffect
}

