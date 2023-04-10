package com.locoquest.app.dto

data class LocationDetails(
    val lat: String,
    val lon: String,
    val ellipseHeight: String,
    val posDatum: String,
    val posSource: String,
    val posOrder: String,
    val orthoHeight: String,
    val vertDatum: String,
    val vertSource: String,
    val vertOrder: String
)
