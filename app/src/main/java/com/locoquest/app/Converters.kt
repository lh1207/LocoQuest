package com.locoquest.app

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.locoquest.app.dto.Benchmark

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromPids(value: ArrayList<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toPids(value: String): ArrayList<String> {
        val listType = object : TypeToken<ArrayList<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    /*@TypeConverter
    fun fromBenchmarks(value: HashMap<String, Benchmark>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toBenchmarks(value: String): HashMap<String, Benchmark> {
        val listType = object : TypeToken<HashMap<String, Benchmark>>() {}.type
        return gson.fromJson(value, listType)
    }*/

    companion object {
        fun toMarkerOptions(benchmark: Benchmark): MarkerOptions {
            return MarkerOptions()
                .position(
                    LatLng(
                        benchmark.lat.toDouble(),
                        benchmark.lon.toDouble()
                    )
                )
                .title(benchmark.name)
                .snippet("PID: ${benchmark.pid}\nOrtho Height: ${benchmark.orthoHt}")
        }
    }
}