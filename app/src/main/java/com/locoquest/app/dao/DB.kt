package com.locoquest.app.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.locoquest.app.Converters
import com.locoquest.app.dto.User

@Database(entities = [User::class], version = 1)
@TypeConverters(Converters::class)
abstract class DB : RoomDatabase() {
    abstract fun localUserDAO() : IUserDAO
}