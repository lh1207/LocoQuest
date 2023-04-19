package com.locoquest.app

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromBenchmarks(value: ArrayList<LatLng>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toBenchmarks(value: String): ArrayList<LatLng> {
        val listType = object : TypeToken<ArrayList<LatLng>>() {}.type
        return gson.fromJson(value, listType)
    }
}