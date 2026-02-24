package com.primortex.color.app

import kotlinx.serialization.Serializable

@Serializable
data class PickedColor(
    val argb: Int,
    val name: String
)

@Serializable
data class Palette(
    val id: String,
    val name: String,
    val colors: List<PickedColor>,
    val tags: List<String> = emptyList(),
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

