package com.locoquest.app.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import com.locoquest.app.dto.Benchmark
import com.locoquest.app.dto.User

@Database(entities = [Benchmark::class, User::class], version =1)
abstract class Database : RoomDatabase(){
    abstract fun localBenchmarkDAO() : ILocalBenchmarkDAO
    abstract fun localUserDAO() : ILocalUserDAO
}



