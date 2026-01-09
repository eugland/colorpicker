package com.primortex.color.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.primortex.color.app.PickedColor

@Entity(tableName = "palettes")
data class PaletteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colors: List<PickedColor>,
    val tags: List<String>,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long
)
