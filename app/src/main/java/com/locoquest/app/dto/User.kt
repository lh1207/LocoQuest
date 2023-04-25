package com.locoquest.app.dto

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey
    val uid: String,
    var displayName: String = "",
    val pids: ArrayList<String> = ArrayList(),
    val friends: ArrayList<String> = ArrayList()
)
