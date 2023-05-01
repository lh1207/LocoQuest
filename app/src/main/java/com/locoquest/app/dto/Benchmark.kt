package com.locoquest.app.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
data class Benchmark(
    val pid: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    var lastVisited: Long = 0,
    var notify: Boolean = false,
    /*var ellipHeight: String = "",
    var posDatum: String = "",
    var posSource: String = "",
    var posOrder: String = "",
    var orthoHt: String = "",
    var vertDatum: String = "",
    var vertSource: String = "",
    var vertOrder: String = "" */){
    companion object{
        fun new(pid: String,
                name: String,
                location: GeoPoint,
                lastVisited: Timestamp = Timestamp(0,0),
                notify: Boolean
        ) : Benchmark {
            return Benchmark(pid, name, location.latitude, location.longitude, lastVisited.seconds, notify)
        }
    }
}