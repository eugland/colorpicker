package com.primortex.color.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "palette_colors",
    indices = [Index(value = ["paletteId", "position"]) ]
)
data class PaletteColorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val paletteId: String,
    val argb: Int,
    val name: String,
    val position: Int
)
