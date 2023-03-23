package com.locoquest.app.dto

import androidx.room.Entity


@Entity(tableName = "benchmark")
data class Benchmark(
    val coordinates: String,
    val name: String,
    val description: Double,
    val d: Double,
    val d1: Double,
    val id: Any,
    val elevation: Double,
    val longitude: Double,
    val latitude: Double,
) // based on class diagram


