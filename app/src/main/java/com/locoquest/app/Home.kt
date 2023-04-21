package com.locoquest.app

import BenchmarkService
import IBenchmarkService
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
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
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.locoquest.app.AppModule.Companion.user
import com.locoquest.app.Converters.Companion.toMarkerOptions
import com.locoquest.app.dto.Benchmark
import kotlinx.coroutines.launch
import java.net.UnknownHostException

class Home : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener{

    private var googleMap: GoogleMap? = null
    private var selectedMarker: Marker? = null
    private var mapFragment: SupportMapFragment? = null
    private var tracking = true
    private var stopTracking = false
    private var cameraIsMoving = false
    private var loadingMarkers = false
    private var notifyUserOfNetwork = true
    private var markerToBenchmark: HashMap<Marker, Benchmark> = HashMap()
    private var benchmarkToMarker: HashMap<Benchmark, Marker> = HashMap()
    private lateinit var offlineImg: ImageView
    private lateinit var layersLayout: LinearLayout
    private lateinit var layersFab: FloatingActionButton
    private lateinit var myLocation: FloatingActionButton
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

        mapFragment =
            childFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment
        if (savedInstanceState != null)
            mapFragment?.onCreate(savedInstanceState)
        else mapFragment?.getMapAsync(this)

        myLocation = view.findViewById(R.id.my_location)
        myLocation.setImageResource(
            if (hasLocationPermissions() && isGpsOn()) R.drawable.my_location_not_tracking
            else R.drawable.location_disabled
        )
        myLocation.setOnClickListener {
            if(!hasLocationPermissions()){
                requestLocationPermission()
                return@setOnClickListener
            }
            if(!isGpsOn()){
                alertUserGps()
                return@setOnClickListener
            }
            stopLocationUpdates()
            startLocationUpdates(true)
            updateCameraWithLastLocation()
            stopTracking = false
        }

        layersLayout = view.findViewById(R.id.layers_layout)
        layersFab = view.findViewById(R.id.layersFab)
        layersFab.setOnClickListener { layersLayout.visibility = if(layersLayout.visibility == View.GONE) View.VISIBLE else View.GONE }
        layersFab.visibility = if(selectedMarker == null) View.VISIBLE else View.GONE

        val layersClickListener = View.OnClickListener {
            when(it.id){
                R.id.normalLayerFab -> {
                    mapType(GoogleMap.MAP_TYPE_NORMAL)
                }
                R.id.hybridLayerFab -> {
                    mapType(GoogleMap.MAP_TYPE_HYBRID)
                }
                R.id.satelliteLayerFab -> {
                    mapType(GoogleMap.MAP_TYPE_SATELLITE)
                }
                R.id.terrainLayerFab -> {
                    mapType(GoogleMap.MAP_TYPE_TERRAIN)
                }
            }
            googleMap?.mapType = mapType()
            layersFab.performClick()
        }

        view.findViewById<ExtendedFloatingActionButton>(R.id.normalLayerFab).setOnClickListener(layersClickListener)
        view.findViewById<ExtendedFloatingActionButton>(R.id.hybridLayerFab).setOnClickListener(layersClickListener)
        view.findViewById<ExtendedFloatingActionButton>(R.id.satelliteLayerFab).setOnClickListener(layersClickListener)
        view.findViewById<ExtendedFloatingActionButton>(R.id.terrainLayerFab).setOnClickListener(layersClickListener)

        offlineImg = view.findViewById(R.id.offline_img)
        updateNetworkStatus()

        return view
    }
    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.mapType = mapType()
        map.setOnMarkerClickListener(this)
        map.setOnCameraIdleListener { loadMarkers() }
        map.setOnMapClickListener {
            selectedMarker = null
            layersLayout.visibility = View.GONE
            Thread{
                Thread.sleep(500) // wait for direction btn to hide
                Handler(Looper.getMainLooper()).post{showLayersFab()}
            }.start()
        }
        map.setOnCameraMoveStartedListener {
            layersLayout.visibility = View.GONE
            tracking = !stopTracking
            stopTracking = true
            if(!tracking){
                stopLocationUpdates()
                startLocationUpdates(tracking)
            }
        }

        updateCameraWithLastLocation(false)
        if(hasLocationPermissions() && isGpsOn()) startLocationUpdates(tracking)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapFragment?.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        mapFragment?.onResume()
        if(hasLocationPermissions() && isGpsOn())
            startLocationUpdates(tracking)
        updateNetworkStatus()
        cameraIsMoving = false
        notifyUserOfNetwork = true
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
        selectedMarker = marker
        hideLayersFab()
        tracking = false
        stopTracking = true
        if(!hasLocationPermissions() || !isGpsOn()) return false

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
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(HUE))
                Toast.makeText(context, "Benchmark completed", Toast.LENGTH_SHORT).show()
            }else {
                Toast.makeText(context, "Not close enough to complete", Toast.LENGTH_SHORT).show()
            }
        }

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false
    }

    private fun hideLayersFab() {
        layersFab.visibility = View.GONE
        layersLayout.visibility = View.GONE
    }

    private fun showLayersFab(){
        layersFab.visibility = View.VISIBLE
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
        goToSelectedBenchmark()
        if(!isOnline()){
            if(notifyUserOfNetwork){
                Toast.makeText(context, "No network: Can't load markers", Toast.LENGTH_SHORT).show()
                notifyUserOfNetwork = false
            }
            return
        }
        if((loadingMarkers && !isUserSwitched) || googleMap == null) return
        loadingMarkers = true
        val map = googleMap!!
        val benchmarkService: IBenchmarkService = BenchmarkService()

        lifecycleScope.launch {
            try {
                val bounds = map.projection.visibleRegion.latLngBounds
                Thread {
                    try {
                        val benchmarkList = benchmarkService.getBenchmarks(bounds)
                        if (benchmarkList == null) {
                            println("Error: unable to retrieve benchmark data")
                            return@Thread
                        }
                        if (benchmarkList.isEmpty() || (isSameBenchmarks(benchmarkList) && !isUserSwitched)) {
                            loadingMarkers = false
                            return@Thread
                        }

                        val newBenchmarks = mutableListOf<Benchmark>()
                        val existingBenchmarks = mutableListOf<Benchmark>()

                        // Find new and existing benchmarks
                        for (benchmark in benchmarkList)
                            if (benchmarkToMarker.keys.contains(benchmark))
                                existingBenchmarks.add(benchmark)
                            else newBenchmarks.add(benchmark)

                        // Remove markers for deleted benchmarks
                        val iterator = benchmarkToMarker.iterator()
                        while (iterator.hasNext()) {
                            val entry = iterator.next()
                            if (!benchmarkList.contains(entry.key)) {
                                val marker = entry.value
                                iterator.remove()
                                markerToBenchmark.remove(marker)
                                Handler(Looper.getMainLooper()).post { marker.remove() }
                            }
                        }

                        // Add markers for new benchmarks
                        Handler(Looper.getMainLooper()).post {
                            for (benchmark in newBenchmarks)
                                addBenchmarkToMap(benchmark)
                        }
                    }catch (e: ConcurrentModificationException){
                        Log.e("LoadMarkers", e.toString())
                    }catch (e: UnknownHostException){
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

        tracking = false
        stopTracking = true
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
            options = options.icon(BitmapDescriptorFactory.defaultMarker(HUE))
        val marker = googleMap!!.addMarker(options)
        if(marker != null) {
            markerToBenchmark[marker] = benchmark
            benchmarkToMarker[benchmark] = marker
        }
        return marker
    }

    fun startLocationUpdates(){
        startLocationUpdates(tracking)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(tracking: Boolean) {
        this.tracking = tracking
        myLocation.setImageResource(
            if (!hasLocationPermissions() || !isGpsOn()) R.drawable.location_disabled
            else if (tracking) R.drawable.my_location
            else R.drawable.my_location_not_tracking
        )
        val interval = if (tracking) TRACKING_INTERVAL else STATIC_INTERVAL
        val fastestInterval = if (tracking) TRACKING_FASTEST_INTERVAL else STATIC_FASTEST_INTERVAL

        fusedLocationClient.requestLocationUpdates(
            createLocationRequest(interval, fastestInterval),
            locationCallback,
            Looper.getMainLooper())

        if(tracking) googleMap?.let {
            it.isMyLocationEnabled = true
            it.uiSettings.isMyLocationButtonEnabled = false
        }
    }

    private fun alertUserGps() {
        AlertDialog.Builder(requireContext())
            .setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setPositiveButton("Yes") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }.show()
    }

    private fun isGpsOn() : Boolean{
        val manager: LocationManager? = getSystemService(requireContext(), LocationManager::class.java)
        if(manager != null && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return true
        return false
    }

    private fun hasLocationPermissions() : Boolean {
        return (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
    }
    private fun isOnline() : Boolean{
        // Check network connectivity and start location updates accordingly
        val connectivityManager = getSystemService(requireContext(), ConnectivityManager::class.java) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        return (capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)))
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            MainActivity.MY_PERMISSIONS_REQUEST_LOCATION
        )
    }

    private fun createLocationRequest(interval: Long, fastestInterval: Long): LocationRequest {
        return LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(interval) // Update location every 5 seconds
            .setFastestInterval(fastestInterval) // Update location at least every 1 second
    }

    private fun updateCameraWithLastLocation(){updateCameraWithLastLocation(true)}
    fun updateCameraWithLastLocation(animate: Boolean) {
        val lastLocation = lastLocation()
        if (lastLocation.provider == "" || googleMap == null || cameraIsMoving) return
        stopTracking = false
        cameraIsMoving = true
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(lastLocation.latitude, lastLocation.longitude), DEFAULT_ZOOM)
        if(animate) {
            googleMap?.animateCamera(cameraUpdate, CAMERA_ANIMATION_DURATION,
                object : CancelableCallback {
                    override fun onFinish() {
                        loadMarkers()
                        cameraIsMoving = false
                    }

                    override fun onCancel() {
                        cameraIsMoving = false
                    }
                })
        }else{
            googleMap?.moveCamera(cameraUpdate)
            loadMarkers()
            cameraIsMoving = false
        }
    }

    private fun updateNetworkStatus(){
        offlineImg.visibility = if(isOnline()) View.GONE else View.VISIBLE
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation.let { location ->
                lastLocation(location)
                if (!tracking) return
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

    private fun mapType(type: Int){
        with (prefs().edit()) {
            putInt("mapType", type)
            apply()
        }
    }

    private fun mapType() : Int {
        return prefs().getInt("mapType", GoogleMap.MAP_TYPE_NORMAL)
    }

    companion object{
        var selectedBenchmark: Benchmark? = null
        private const val HUE = 200f
        private const val DEFAULT_ZOOM = 15f
        private const val CAMERA_ANIMATION_DURATION = 2000
        private const val TRACKING_INTERVAL = 5000L
        private const val TRACKING_FASTEST_INTERVAL = 1000L
        private const val STATIC_INTERVAL = 30000L
        private const val STATIC_FASTEST_INTERVAL = 10000L
    }
}