package com.locoquest.app.dto

data class Benchmark(
    val pid: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    var lastVisitedSeconds: Long = 0,
    /*var ellipHeight: String = "",
    var posDatum: String = "",
    var posSource: String = "",
    var posOrder: String = "",
    var orthoHt: String = "",
    var vertDatum: String = "",
    var vertSource: String = "",
    var vertOrder: String = "" */)