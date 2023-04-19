import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.locoquest.app.RetrofitClientInstance
import com.locoquest.app.dao.IBenchmarkDAO
import com.locoquest.app.dto.Benchmark

interface IBenchmarkService {
    fun getBenchmarks(bounds: LatLngBounds): List<Benchmark>?
    fun getBenchmarks(latLng: LatLng, r: Double): List<Benchmark>?
    fun getBenchmarks(pidList: List<String>): List<Benchmark>?
}

open class BenchmarkService : IBenchmarkService {
    override fun getBenchmarks(bounds: LatLngBounds): List<Benchmark>? {
        try {
            val benchmarkDAO =
                RetrofitClientInstance.retrofitInstance?.create(IBenchmarkDAO::class.java)
            val call = benchmarkDAO?.getBenchmarksByBounds(
                bounds.southwest.latitude.toString(),
                bounds.northeast.latitude.toString(),
                bounds.southwest.longitude.toString(),
                bounds.northeast.longitude.toString()
            )
            val response = call?.execute()

            return if (response?.isSuccessful == true && response.body() != null) {
                response.body()!!
            } else {
                null
            }
        }catch (e: IllegalStateException){
            Log.e("BenchmarkService", e.toString())
            return null
        }
    }

    override fun getBenchmarks(latLng: LatLng, r: Double): List<Benchmark>? {
        val benchmarkDAO = RetrofitClientInstance.retrofitInstance?.create(IBenchmarkDAO::class.java)
        val call = benchmarkDAO?.getBenchmarksByRadius(latLng.latitude, latLng.longitude, r)
        val response = call?.execute()

        return if (response?.isSuccessful == true && response.body() != null) {
            response.body()!!
        }else{
            null
        }
    }
    override fun getBenchmarks(pidList: List<String>): List<Benchmark>? {
        val benchmarkDAO = RetrofitClientInstance.retrofitInstance?.create(IBenchmarkDAO::class.java)
        val call = benchmarkDAO?.getBenchmarkByPid(pidList.joinToString(","))
        val response = call?.execute()

        return if (response?.isSuccessful == true && response.body() != null) {
            response.body()!!
        } else {
            null
        }
    }
}