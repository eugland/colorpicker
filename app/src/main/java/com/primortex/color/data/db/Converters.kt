package com.primortex.color.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @TypeConverter
    fun listToJson(value: List<String>?): String {
        return value?.let { json.encodeToString(it) } ?: "[]"
    }

    @TypeConverter
    fun jsonToList(value: String?): List<String> {
        return runCatching {
            value?.let { json.decodeFromString<List<String>>(it) }
        }.getOrDefault(emptyList())
    }
}
