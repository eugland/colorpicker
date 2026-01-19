package com.primortex.color.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "palettes")
data class PaletteEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val tags: List<String>,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long
)
