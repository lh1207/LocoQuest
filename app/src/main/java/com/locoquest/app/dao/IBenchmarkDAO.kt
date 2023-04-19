package com.locoquest.app.dao

import com.locoquest.app.dto.Benchmark
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface IBenchmarkDAO {
    @GET("pid")
    fun getBenchmarkByPid(@Query("pid") pid: String): Call<List<Benchmark>>

    @GET("radial")
    fun getBenchmarksByRadius(@Query("lat") lat: Double, @Query("lon") lon: Double, @Query("radius") r: Double): Call<List<Benchmark>>

    @GET("bounds")
    fun getBenchmarksByBounds(@Query("minlat") minLat: String, @Query("maxlat") maxLat: String, @Query("minlon") minLon: String, @Query("maxlon") maxLon: String): Call<List<Benchmark>>
}

