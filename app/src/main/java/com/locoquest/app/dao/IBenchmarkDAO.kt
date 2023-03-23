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
    fun getBenchmarkByPid(@Query("pid") pid: String): Call<Benchmark>


}




