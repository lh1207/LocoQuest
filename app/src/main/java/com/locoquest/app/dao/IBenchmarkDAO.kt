package com.locoquest.app.dao

import com.locoquest.app.dto.Benchmark
import retrofit2.Call
import retrofit2.http.GET

interface IBenchmarkDAO {
    @GET()
    fun getAllBenchmark() : Call<ArrayList<Benchmark>>

}