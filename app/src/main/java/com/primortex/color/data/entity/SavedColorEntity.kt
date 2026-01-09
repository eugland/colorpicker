package com.primortex.color.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_colors")
data class SavedColorEntity(
    @PrimaryKey val argb: Int,
    val name: String,
    val createdAt: Long
)
