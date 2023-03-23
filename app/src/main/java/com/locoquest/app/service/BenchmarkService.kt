package com.locoquest.app.service

import android.app.Application
import com.locoquest.app.RetrofitClientInstance
import com.locoquest.app.dao.IBenchmarkDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse




open class BenchmarkService(application:Application){
    private val application = application

    open suspend fun parseBenchmarkData(benchmarkJson: String): Any? {
        return withContext(Dispatchers.IO) {
            val service = RetrofitClientInstance.retrofitInstance?.create(IBenchmarkDAO::class.java)
            val allBenchmarks = async {service?.getBenchmarkByPid("")}
            var result = allBenchmarks.await()?.awaitResponse()?.body()
            return@withContext result

        }
        
    }

}

