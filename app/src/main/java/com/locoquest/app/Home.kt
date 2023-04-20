package com.locoquest.app

import BenchmarkService
import IBenchmarkService
import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.CancelableCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.locoquest.app.AppModule.Companion.user
import com.locoquest.app.Converters.Companion.toMarkerOptions
import com.locoquest.app.dto.Benchmark
import kotlinx.coroutines.launch

class Home : Fragment(), GoogleMap.OnMarkerClickListener {

    private var googleMap: GoogleMap? = null
    private var markers: ArrayList<Marker> = ArrayList()
    private val hue = 200f
    private var cameraIsMoving = false
    private val cameraAnimationDuration = 2000
    private val defaultCameraZoom = 15f
    private var loadingMarkers = false
    private var cameraMovedByUser = false
    private var updateCameraOnLocationUpdate = true
    private var mapFragment: SupportMapFragment? = null
    private var markerToBenchmark: HashMap<Marker, Benchmark> = HashMap()
    private var benchmarkToMarker: HashMap<Benchmark, Marker> = HashMap()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

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
            startLocationUpdates()

            map.setOnCameraMoveListener {
                loadMarkers()
            }
            map.setOnCameraMoveStartedListener{
                updateCameraOnLocationUpdate = !cameraMovedByUser
                cameraMovedByUser = true
            }
            map.setOnMarkerClickListener(this)
            map.setOnMyLocationButtonClickListener {
                updateCameraWithLastLocation()
                cameraMovedByUser = false
                updateCameraOnLocationUpdate = true
                true
            }

            updateCameraWithLastLocation()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        mapFragment?.onResume()
        startLocationUpdates(false)
        cameraIsMoving = false
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
        val lastLocation = lastLocation()
        val inProximity = isWithin500Feet(
            marker.position,
            LatLng(lastLocation.latitude, lastLocation.longitude)
        )

        if(!markerToBenchmark.contains(marker)) return true

        val benchmark = markerToBenchmark[marker]
        if(!user.benchmarks.contains(benchmark?.pid)) {
            if(inProximity) {
                user.benchmarks[benchmark!!.pid] = benchmark
                Thread{AppModule.db?.localUserDAO()?.insert(user)}.start()
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(hue))
                Toast.makeText(context, "Benchmark completed", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(context, "Not close enough to complete", Toast.LENGTH_SHORT).show()
            }
        }

        updateCameraOnLocationUpdate = false
        cameraMovedByUser = true

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false
    }

    private fun isWithin500Feet(latlng1: LatLng, latlng2: LatLng): Boolean {
        val R = 6371e3 // Earth's radius in meters
        val φ1 = Math.toRadians(latlng1.latitude)
        val φ2 = Math.toRadians(latlng2.latitude)
        val Δφ = Math.toRadians(latlng2.latitude - latlng1.latitude)
        val Δλ = Math.toRadians(latlng2.longitude - latlng1.longitude)

        val a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
                Math.cos(φ1) * Math.cos(φ2) *
                Math.sin(Δλ / 2) * Math.sin(Δλ / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        val d = R * c // distance between the two points in meters
        val feet = d * 3.28084 // convert distance to feet

        return feet <= 500.0
    }

    fun loadMarkers(){loadMarkers(false)}
    fun loadMarkers(isUserSwitched: Boolean){
        if((loadingMarkers && !isUserSwitched)|| googleMap == null) return
        loadingMarkers = true
        val map = googleMap!!
        val benchmarkService: IBenchmarkService = BenchmarkService()

        lifecycleScope.launch {
            try {
                val bounds = map.projection.visibleRegion.latLngBounds
                Thread {
                    try {
                        val benchmarkList = benchmarkService.getBenchmarks(bounds)
                        if (benchmarkList != null) {
                            if (benchmarkList.isEmpty() || (isSameBenchmarks(benchmarkList) && !isUserSwitched)) {
                                loadingMarkers = false
                                return@Thread
                            }

                            markerToBenchmark.clear()
                            benchmarkToMarker.clear()

                            Handler(Looper.getMainLooper()).post {
                                map.clear()
                                goToSelectedBenchmark()
                                benchmarkList.forEach { addBenchmarkToMap(it) }
                            }
                        } else {
                            println("Error: unable to retrieve benchmark data")
                        }
                    }catch (e: ConcurrentModificationException){
                        Log.e("LoadMarkers", e.toString())
                    }
                    loadingMarkers = false
                }.start()
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }

    private fun isSameBenchmarks(benchmarkList: List<Benchmark>) : Boolean{
        return benchmarkList.sortedBy { x -> x.pid } == ArrayList(markerToBenchmark.values.toList()).sortedBy { x -> x.pid }
    }

    private fun goToSelectedBenchmark() {
        if (selectedBenchmark == null) return

        updateCameraOnLocationUpdate = false
        cameraMovedByUser = true
        googleMap?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    selectedBenchmark?.lat?.toDouble()!!,
                    selectedBenchmark?.lon?.toDouble()!!
                ), 15f
            )
        )

        val selectedMarker =
            if (benchmarkToMarker.contains(selectedBenchmark))
                benchmarkToMarker[selectedBenchmark]
            else addBenchmarkToMap(selectedBenchmark!!)

        selectedMarker?.showInfoWindow()

        selectedBenchmark = null
    }

    private fun addBenchmarkToMap(benchmark: Benchmark) : Marker? {
        var options = toMarkerOptions(benchmark)
        if(user.benchmarks.contains(benchmark.pid))
            options = options.icon(BitmapDescriptorFactory.defaultMarker(hue))
        val marker = googleMap!!.addMarker(options)
        return if(marker?.let { markers.add(it) } == true) {
            markerToBenchmark[marker] = benchmark
            benchmarkToMarker[benchmark] = marker
            marker
        } else null
    }

    fun startLocationUpdates(){startLocationUpdates(true)}
    private fun startLocationUpdates(request: Boolean) {
        // Check network connectivity and start location updates accordingly
        val connectivityManager =
            getSystemService(requireContext(), ConnectivityManager::class.java) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        if (capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR
            ))
        ) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if(request) requestLocationPermission()
                return
            }
            // Request location updates using fusedLocationClient
            fusedLocationClient.requestLocationUpdates(createLocationRequest(), locationCallback, Looper.getMainLooper())
            googleMap?.let { it.isMyLocationEnabled = true }
        } else {
            Toast.makeText(
                context,
                "Unable to start location updates. Device is offline.",
                Toast.LENGTH_SHORT
            ).show()
        }
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

    private fun updateCameraWithLastLocation() {
        val lastLocation = lastLocation()
        if (lastLocation.provider == "" || googleMap == null || cameraIsMoving) return
        cameraMovedByUser = false
        cameraIsMoving = true
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(lastLocation.latitude, lastLocation.longitude), defaultCameraZoom
            ), cameraAnimationDuration, object : CancelableCallback {
                override fun onFinish() {
                    loadMarkers()
                    cameraIsMoving = false
                }
                override fun onCancel() { cameraIsMoving = false }
            })
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation.let { location ->
                lastLocation(location)
                if (!updateCameraOnLocationUpdate) return
                updateCameraWithLastLocation()
            }
        }
    }

    private fun prefs(): SharedPreferences {
        return requireContext().getSharedPreferences("LocoQuest", Context.MODE_PRIVATE)
    }

    private fun lastLocation(location: Location){
        with (prefs().edit()) {
            putFloat("lat", location.latitude.toFloat())
            putFloat("lon", location.longitude.toFloat())
            putString("provider", location.provider)
            apply()
        }
    }

    private fun lastLocation() : Location {
        val location = Location(prefs().getString("provider", ""))
        location.latitude = prefs().getFloat("lat", 0f).toDouble()
        location.longitude = prefs().getFloat("lon", 0f).toDouble()
        return location
    }

    companion object{
        var selectedBenchmark: Benchmark? = null
    }
}