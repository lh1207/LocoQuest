import com.locoquest.app.RetrofitClientInstance
import com.locoquest.app.dao.IBenchmarkDAO
import com.locoquest.app.dto.Benchmark
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

interface IBenchmarkService {
}

open class BenchmarkService{


    open fun parseBenchmarkData(benchmarkJson: String): Any? {
        TODO("Not yet implemented")
    }

    fun main() {
        val benchmarkDAO = RetrofitClientInstance.retrofitInstance?.create(IBenchmarkDAO::class.java)
        val pid = "AB1234"
        val call = benchmarkDAO?.getBenchmarkByPid(pid)
        call?.enqueue(object : Callback<Benchmark> {
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
}