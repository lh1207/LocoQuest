package com.locoquest.app

import BenchmarkService
import IBenchmarkService
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.locoquest.app.dto.Benchmark
import com.locoquest.app.dto.User
import kotlinx.coroutines.launch

class Home : Fragment(), GoogleMap.OnMarkerClickListener {

    private lateinit var googleMap: GoogleMap
    private var benchmarks: ArrayList<Benchmark> = ArrayList()
    private var markers: ArrayList<Marker> = ArrayList()
    private var user: User = User("", "", ArrayList())
    private val hue = 200f
    private var loadingMarkers = false
    private var updateCamera = true
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private var mapFragment: SupportMapFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        mapFragment = childFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment

        mapFragment?.getMapAsync { map ->
            googleMap = map

            // Check network connectivity and start location updates accordingly
            val connectivityManager =
                getSystemService(requireContext(), ConnectivityManager::class.java) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            if (capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(
                    NetworkCapabilities.TRANSPORT_CELLULAR
                ))
            ) {
                startLocationUpdates()
            } else {
                Toast.makeText(
                    context,
                    "Unable to start location updates. Device is offline.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            map.setOnCameraMoveStartedListener { updateCamera = false }
            map.setOnMarkerClickListener(this)

            loadMarkers()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        mapFragment?.onResume()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        mapFragment?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapFragment?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapFragment?.onLowMemory()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        var inProximity = false
        lastLocation.let {
            inProximity = isWithin500Feet(marker.position.latitude,
                marker.position.longitude,
                lastLocation.latitude,
                lastLocation.longitude)
        }

        inProximity = true // for testing

        if(!user.benchmarks.contains(marker.position)) {
            if(inProximity) {
                user.benchmarks.add(marker.position)
                Thread{AppModule.db?.localUserDAO()?.insert(user)}.start()
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(hue))
                Toast.makeText(context, "Benchmark saved", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(context, "Not close enough to save", Toast.LENGTH_SHORT).show()
            }
        }

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false
    }

    private fun isWithin500Feet(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Boolean {
        val R = 6371e3 // Earth's radius in meters
        val φ1 = Math.toRadians(lat1)
        val φ2 = Math.toRadians(lat2)
        val Δφ = Math.toRadians(lat2 - lat1)
        val Δλ = Math.toRadians(lon2 - lon1)

        val a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
                Math.cos(φ1) * Math.cos(φ2) *
                Math.sin(Δλ / 2) * Math.sin(Δλ / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        val d = R * c // distance between the two points in meters
        val feet = d * 3.28084 // convert distance to feet

        return feet <= 500.0
    }

    private fun loadMarkers(){
        if(loadingMarkers) return
        loadingMarkers = true
        googleMap.clear()

        // Add the markers to the map
        val benchmarkService: IBenchmarkService = BenchmarkService()

        lifecycleScope.launch {
            try {
                // Fetch benchmark data in a background thread
                // Create marker options with benchmark data
                // Add marker to the map on the main/UI thread
                var target = googleMap.cameraPosition.target
                Thread {
                    val benchmarkList = benchmarkService.getBenchmarks(target, 10.0)
                    benchmarkList?.let { benchmarks = ArrayList(it) }
                    if (benchmarkList != null) {
                        benchmarkList.forEach { benchmark ->
                            var marker = MarkerOptions()
                                .position(
                                    LatLng(
                                        benchmark.lat.toDouble(),
                                        benchmark.lon.toDouble()
                                    )
                                )
                                .title(benchmark.name)
                                .snippet("PID: ${benchmark.pid}\nOrtho Height: ${benchmark.orthoHt}")

                            if(user.benchmarks.contains(LatLng(benchmark.lat.toDouble(), benchmark.lon.toDouble())))
                                marker = marker.icon(BitmapDescriptorFactory.defaultMarker(hue))

                            Handler(Looper.getMainLooper()).post { googleMap.addMarker(marker)?.let { markers.add(it) } }
                        }
                        Handler(Looper.getMainLooper()).post { Toast.makeText(context, "markers loaded", Toast.LENGTH_SHORT).show() }
                    } else {
                        println("Error: unable to retrieve benchmark data")
                    }
                }.start()
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
        loadingMarkers = false
    }

    fun startLocationUpdates() {
        // Check if location permissions are granted
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions if not granted
            requestLocationPermission()
            return
        }
        // Request location updates using fusedLocationClient
        fusedLocationClient.requestLocationUpdates(createLocationRequest(), locationCallback, Looper.getMainLooper())
        googleMap.isMyLocationEnabled = true
        googleMap.setOnMyLocationButtonClickListener { updateCamera = true; false }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MainActivity.MY_PERMISSIONS_REQUEST_LOCATION
            )
        }
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(5000) // Update location every 5 seconds
            .setFastestInterval(1000) // Update location at least every 1 second
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation.let { location ->
                lastLocation = location
                if (!updateCamera) return
                val latitude = location.latitude
                val longitude = location.longitude
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                    LatLng(latitude, longitude), 15f
                )
                googleMap.moveCamera(cameraUpdate)
            }
        }
    }
}