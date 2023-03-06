package com.locoquest.app.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.locoquest.app.dto.Benchmark


@Dao
interface ILocalBenchmarkDAO {
    @Query("SELECT * FROM Benchmark")
    fun getBenchmark() : LiveData<List<Benchmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(benchmark: ArrayList<Benchmark>)

    @Delete
    fun delete(benchmark: Benchmark)
}