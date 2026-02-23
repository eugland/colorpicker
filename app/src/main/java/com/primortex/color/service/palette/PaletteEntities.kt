package com.primortex.color.service.palette

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.primortex.color.app.Palette
import com.primortex.color.app.PickedColor
import kotlinx.serialization.json.Json

@Entity(tableName = "palette")
data class PaletteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorsJson: String,
    val tagsJson: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "palette_meta")
data class PaletteMetaEntity(
    @PrimaryKey val key: String,
    val value: String
)

private val paletteJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

internal fun Palette.toEntity(): PaletteEntity = PaletteEntity(
    id = id,
    name = name,
    colorsJson = paletteJson.encodeToString(colors),
    tagsJson = paletteJson.encodeToString(tags),
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun PaletteEntity.toDomain(): Palette = Palette(
    id = id,
    name = name,
    colors = runCatching { paletteJson.decodeFromString<List<PickedColor>>(colorsJson) }.getOrDefault(emptyList()),
    tags = runCatching { paletteJson.decodeFromString<List<String>>(tagsJson) }.getOrDefault(emptyList()),
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)
