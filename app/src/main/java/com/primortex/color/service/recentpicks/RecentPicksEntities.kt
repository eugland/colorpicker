package com.primortex.color.service.recentpicks

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.primortex.color.app.PickedColor

@Entity(tableName = "recent_pick_history")
data class RecentPickHistoryEntity(
    @PrimaryKey val argb: Int,
    val name: String,
    val updatedAt: Long
)

@Entity(tableName = "saved_pick")
data class SavedPickEntity(
    @PrimaryKey val argb: Int,
    val name: String,
    val updatedAt: Long
)

internal fun RecentPickHistoryEntity.toPickedColor(): PickedColor = PickedColor(argb = argb, name = name)

internal fun SavedPickEntity.toPickedColor(): PickedColor = PickedColor(argb = argb, name = name)
