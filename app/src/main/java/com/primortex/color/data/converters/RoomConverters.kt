package com.primortex.color.data.converters

import androidx.room.TypeConverter
import com.primortex.color.app.PickedColor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomConverters {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromPickedColorList(value: List<PickedColor>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toPickedColorList(value: String): List<PickedColor> {
        return runCatching { json.decodeFromString<List<PickedColor>>(value) }
            .getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return runCatching { json.decodeFromString<List<String>>(value) }
            .getOrDefault(emptyList())
    }
}
