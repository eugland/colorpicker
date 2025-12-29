package com.primortex.color.app

data class PickedColor(
    val argb: Int,
    val source: String, // "camera", "gallery"
    val timestampMs: Long = System.currentTimeMillis()
)

