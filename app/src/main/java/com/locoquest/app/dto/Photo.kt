package com.locoquest.app.dto

import java.util.*

data class Photo(
    val localURL: String,
    val remoteURL: String,
    val description: String,
    val benchmarkDate: Date,
    val locationID: String)
