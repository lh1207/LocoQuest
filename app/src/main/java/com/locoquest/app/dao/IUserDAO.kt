package com.locoquest.app.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.locoquest.app.dto.User

@Dao
interface IUserDAO {
    @Query("SELECT * FROM user WHERE uid = :arg0")
    fun getUser(arg0: String) : User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(user: User)

    @Delete
    fun delete(user: User)

    @Update
    fun update(user: User)
}