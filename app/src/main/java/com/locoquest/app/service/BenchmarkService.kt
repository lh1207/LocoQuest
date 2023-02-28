package com.locoquest.app.service

import android.app.Application
import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.room.Room
import com.google.android.gms.tasks.Tasks.await
import com.locoquest.dao.BenchmarkDatabase
import com.locoquest.dao.IBenchmarkDAO
import com.locoquest.dao.ILocalBenchmarkDAO
import com.locoquest.dto.Photo
import com.locoquest.dto.User
import com.locoquest.dto.LocationDetails
import com.locoquest.dto.Benchmark
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

interface IBenchmarkService {
}

open class BenchmarkService(application:Application){
    private val application = application

    open fun parseBenchmarkData(benchmarkJson: String) {
        
    }

}