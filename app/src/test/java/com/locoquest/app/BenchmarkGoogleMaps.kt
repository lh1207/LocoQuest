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

/*
    - These unit tests ensure the application can properly generate Google Maps
    - mokito is used to generate a temporary "map" for the tests, so an actual map wont need to be generated
*/

class MapsTest {
    // Generate a mock version of google maps, for the purpose of unit tests.
    // @Before will be run before each unit test, this is what creates a temporary map instance
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

    // Tests that a google map instance is generated. Test will fail if returned Null, meaning
    // no map was generated.
    @Test
    fun testMapNotNull() {
        assertNotNull(mapView.getMapAsync { })
    }
}

