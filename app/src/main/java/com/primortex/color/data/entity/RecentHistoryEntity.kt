package com.primortex.color.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_history")
data class RecentHistoryEntity(
    @PrimaryKey val argb: Int,
    val name: String,
    val createdAt: Long
)
