package com.locoquest.app.dto

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey
    val uid: String,
    val displayName: String?,
    val benchmarks: HashMap<String, Benchmark>
)
