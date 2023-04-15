package com.locoquest.app.dto
import androidx.room.Entity
@Entity(tableName="benchmark")
data class Benchmark(
    val pid: String,
    val name: String,
    val lat: String,
    val lon: String,
    val ellipHeight: String,
    val posDatum: String,
    val posSource: String,
    val posOrder: String,
    val orthoHt: String,
    val vertDatum: String,
    val vertSource: String,
    val vertOrder: String )