package com.locoquest.app

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class MapsTest {
    @Mock
    private lateinit var mapView: MapView

    @Mock
    private lateinit var googleMap: GoogleMap

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        `when`(mapView.getMapAsync(any())).then {
            val callback = it.arguments[0] as OnMapReadyCallback
            callback.onMapReady(googleMap)
        }
    }

    @Test
    fun testMapNotNull() {
        assertNotNull(mapView.getMapAsync { })
    }

    @Test
    fun testMarkerAdded() {
        val markerOptions = MarkerOptions()
            .position(LatLng(37.4219999,-122.0840575))
            .title("Googleplex")
        `when`(googleMap.addMarker(markerOptions)).thenReturn(mock(Marker::class.java))
        val marker = googleMap.addMarker(markerOptions)
        assertNotNull(marker)
        verify(googleMap, times(1)).addMarker(markerOptions)
    }
}

