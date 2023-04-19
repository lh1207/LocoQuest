package com.locoquest.app

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.locoquest.app.dto.Benchmark

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromBenchmarks(value: HashMap<String, Benchmark>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toBenchmarks(value: String): HashMap<String, Benchmark> {
        val listType = object : TypeToken<HashMap<String, Benchmark>>() {}.type
        return gson.fromJson(value, listType)
    }
}