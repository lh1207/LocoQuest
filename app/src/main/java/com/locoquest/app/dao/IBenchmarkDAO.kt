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

fun createRetrofitInstance(baseUrl: String): Retrofit {
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
}
fun main() {
    val retrofit = createRetrofitInstance("https://geodesy.noaa.gov/api/nde/")
    val benchmarkDAO = retrofit.create(IBenchmarkDAO::class.java)

    val pid = "AB1234"

    val call = benchmarkDAO.getBenchmarkByPid(pid)
    call.enqueue(object : Callback<Benchmark> {
        override fun onResponse(call: Call<Benchmark>, response: Response<Benchmark>) {
            if (response.isSuccessful) {
                val benchmark = response.body()
                println(benchmark)
            } else {
                println("Error: ${response.code()}")
            }
        }

        override fun onFailure(call: Call<Benchmark>, t: Throwable) {
            println("Error: ${t.message}")
        }
    })
}