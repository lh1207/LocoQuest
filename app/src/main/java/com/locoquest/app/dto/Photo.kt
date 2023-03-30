package com.locoquest.app.dto

import java.util.*

data class Photo(
    var localURL: String,
    var remoteURL: String,
    var description: String,
    var benchmarkDate: Date,
    var locationID: String)
