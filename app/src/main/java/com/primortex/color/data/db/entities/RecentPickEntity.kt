package com.primortex.color.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recent_picks",
    indices = [Index(value = ["kind", "createdAt"]) ]
)
data class RecentPickEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val argb: Int,
    val name: String,
    val kind: String,
    val createdAt: Long
)
