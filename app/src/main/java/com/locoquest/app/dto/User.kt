package com.locoquest.app.dto

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity
data class User(
    @PrimaryKey
    val uid: String,
    val displayName: String?,
    val benchmarks: ArrayList<LatLng>
)
