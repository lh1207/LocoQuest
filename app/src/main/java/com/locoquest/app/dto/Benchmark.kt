package com.locoquest.app.dto

import androidx.room.Entity

@Entity(tableName="benchmark")
data class Benchmark(
    var pid: String,
    var name: String,
    var lat: String,
    var lon: String,
    var ellipHeight: String,
    var posDatum: String,
    var posSource: String,
    var posOrder: String,
    var orthoHt: String,
    var vertDatum: String,
    var vertSource: String,
    var vertOrder: String )




