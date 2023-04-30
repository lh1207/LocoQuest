package com.locoquest.app

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.locoquest.app.dto.Benchmark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromArrayListString(value: ArrayList<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toArrayListString(value: String): ArrayList<String> {
        val listType = object : TypeToken<ArrayList<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromBenchmarks(value: HashMap<String, Benchmark>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toBenchmarks(value: String): HashMap<String, Benchmark> {
        val listType = object : TypeToken<HashMap<String, Benchmark>>() {}.type
        return gson.fromJson(value, listType)
    }

    companion object {
        fun toMarkerOptions(benchmark: Benchmark): MarkerOptions {
            return MarkerOptions()
                .position(
                    LatLng(
                        benchmark.lat,
                        benchmark.lon
                    )
                )
                .title(benchmark.name)
        }

        fun formatSeconds(seconds: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val date = Date(seconds * 1000)
            return sdf.format(date)
        }
    }
}