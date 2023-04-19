package com.locoquest.app

import BenchmarkService
import IBenchmarkService
import android.Manifest
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
import com.locoquest.app.AppModule.Companion.user
import com.locoquest.app.dto.Benchmark
import kotlinx.coroutines.launch

class Home : Fragment(), GoogleMap.OnMarkerClickListener {

    private var googleMap: GoogleMap? = null
    private var benchmarks: ArrayList<Benchmark> = ArrayList()
    private var markers: ArrayList<Marker> = ArrayList()
    private val hue = 200f
    private var loadingMarkers = false
    private var updateCamera = true
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var mapFragment: SupportMapFragment? = null
    private var markerToBenchmark: HashMap<LatLng, Benchmark> = HashMap()

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

            updateCameraWithLastLocation()
            startLocationUpdates()

            map.setOnCameraMoveStartedListener { updateCamera = false }
            map.setOnMarkerClickListener(this)

            loadMarkers()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        mapFragment?.onResume()
        startLocationUpdates()
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
        if(lastLocation != null) {
            inProximity = isWithin500Feet(
                marker.position.latitude,
                marker.position.longitude,
                lastLocation!!.latitude,
                lastLocation!!.longitude
            )
        }

        inProximity = true // for testing

        if(!markerToBenchmark.contains(marker.position)) return true

        val benchmark = markerToBenchmark[marker.position]
        if(!user.benchmarks.contains(benchmark?.pid)) {
            if(inProximity) {
                user.benchmarks[benchmark!!.pid] = benchmark
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

    fun loadMarkers(){
        if(loadingMarkers || googleMap == null) return
        loadingMarkers = true
        val map = googleMap!!
        map.clear()

        val benchmarkService: IBenchmarkService = BenchmarkService()

        lifecycleScope.launch {
            try {
                val target = map.cameraPosition.target
                Thread {
                    val benchmarkList = benchmarkService.getBenchmarks(target, 10.0)
                    if (benchmarkList != null) {
                        if(benchmarkList.isEmpty()){
                            loadingMarkers = false
                            return@Thread
                        }
                        markerToBenchmark.clear()
                        benchmarks = ArrayList(benchmarkList)

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

                            markerToBenchmark[marker.position] = benchmark

                            if(user.benchmarks.contains(benchmark.pid))
                                marker = marker.icon(BitmapDescriptorFactory.defaultMarker(hue))

                            Handler(Looper.getMainLooper()).post { map.addMarker(marker)?.let { markers.add(it) } }
                        }

                        if(selectedBenchmark != null) {
                            Handler(Looper.getMainLooper()).post {
                                map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(
                                            selectedBenchmark?.lat?.toDouble()!!,
                                            selectedBenchmark?.lon?.toDouble()!!
                                        ), 15f
                                    )
                                )
                                selectedBenchmark = null
                            }
                        }
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
                // Request location permissions if not granted
                requestLocationPermission()
                return
            }
            // Request location updates using fusedLocationClient
            fusedLocationClient.requestLocationUpdates(createLocationRequest(), locationCallback, Looper.getMainLooper())
            googleMap?.let {
                it.isMyLocationEnabled = true
                it.setOnMyLocationButtonClickListener { updateCamera = true; false }
            }
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

    private fun updateCameraWithLastLocation(){
        if(lastLocation == null || googleMap == null) return
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(
            LatLng(lastLocation!!.latitude, lastLocation!!.longitude), 15f
        ))
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation.let { location ->
                lastLocation = location
                if (!updateCamera) return
                updateCameraWithLastLocation()
                loadMarkers()
            }
        }
    }

    companion object{
        var lastLocation: Location? = null
        var selectedBenchmark: Benchmark? = null
    }
}