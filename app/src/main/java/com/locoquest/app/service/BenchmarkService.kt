package com.locoquest.app.service

import android.app.Application
import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.room.Room
import com.google.android.gms.tasks.Tasks.await
import com.locoquest.app.RetrofitClientInstance
import com.locoquest.app.dao.BenchmarkDatabase
import com.locoquest.app.dao.IBenchmarkDAO
import com.locoquest.app.dao.ILocalBenchmarkDAO
import com.locoquest.app.dto.Photo
import com.locoquest.app.dto.User
import com.locoquest.app.dto.LocationDetails
import com.locoquest.app.dto.Benchmark
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

interface IBenchmarkService {
}

open class BenchmarkService(application:Application) {

    private val application = application

    open suspend fun parseBenchmarkData(benchmarkJson: String) {
        withContext(Dispatchers.IO) {
            val service = RetrofitClientInstance.retrofitInstance?.create(IBenchmarkDAO::class.java)
            // perform any IO operation here
        }
    }
}