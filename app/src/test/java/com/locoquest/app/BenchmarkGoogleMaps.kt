package com.locoquest.app

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class BenchmarkMapGeneration {
    @Mock
    private lateinit var mapView: MapView

    @Mock
    private lateinit var googleMap: GoogleMap

    @Before
    fun mapGenerate() {
        MockitoAnnotations.initMocks(this)
        `when`(mapView.getMapAsync(any())).then {
            val callback = it.arguments[0] as OnMapReadyCallback
            callback.onMapReady(googleMap)
        }
    }
}