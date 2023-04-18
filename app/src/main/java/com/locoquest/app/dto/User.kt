package com.locoquest.app.dto

import com.google.android.gms.maps.model.LatLng

data class User(
    val uid: String,
    val displayName: String?,
    val benchmarks: ArrayList<LatLng>)
