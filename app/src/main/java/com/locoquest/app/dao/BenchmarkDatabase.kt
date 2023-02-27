package com.locoquest.app.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import com.locoquest.app.dto.Benchmark

@Database(entities = [Benchmark::class], version =1)
abstract class BenchmarkDatabase : RoomDatabase(){
    abstract fun localBenchmarkDAO() : ILocalBenchmarkDAO
}



