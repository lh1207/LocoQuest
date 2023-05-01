package com.locoquest.app

import BenchmarkService
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
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
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.locoquest.app.AppModule.Companion.BOOSTED_DURATION
import com.locoquest.app.AppModule.Companion.DEBUG
import com.locoquest.app.AppModule.Companion.SECONDS_TO_RECOLLECT
import com.locoquest.app.AppModule.Companion.cancelNotification
import com.locoquest.app.AppModule.Companion.scheduleNotification
import com.locoquest.app.AppModule.Companion.user
import com.locoquest.app.Converters.Companion.toMarkerOptions
import com.locoquest.app.dto.Benchmark
import java.net.UnknownHostException

class Home(private val homeListener: HomeListener) : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener{

    var selectedBenchmark: Benchmark? = null
    lateinit var balance: TextView
    private var googleMap: GoogleMap? = null
    private var circle: Circle? = null
    private var selectedMarker: Marker? = null
    private var mapFragment: SupportMapFragment? = null
    private var tracking = true
    private var monitoringSelectedMarker = false
    private var monitoringBoost = false
    private var loadingMarkers = false
    private var cameraIsBeingMoved = false
    private var notifyUserOfNetwork = true
    private var markerToBenchmark: HashMap<Marker, Benchmark> = HashMap()
    private var benchmarkToMarker: HashMap<Benchmark, Marker> = HashMap()
    private lateinit var notifyFab: FloatingActionButton
    private lateinit var offlineImg: ImageView
    private lateinit var mushroom: ImageView
    private lateinit var timerTxt: TextView
    private lateinit var layersLayout: LinearLayout
    private lateinit var layersFab: FloatingActionButton
    private lateinit var myLocation: FloatingActionButton
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    interface HomeListener {
        fun onMushroomClicked()
        fun onCoinCollected(benchmark: Benchmark)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("tracker", "creating view")
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        mapFragment =
            childFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment
        if (savedInstanceState != null)
            mapFragment?.onCreate(savedInstanceState)
        else mapFragment?.getMapAsync(this)

        mapFragment?.view?.setOnTouchListener { _, _ ->
            Log.d("tracker", "map fragment touched")
            updateTrackingStatus(false)
            false
        }

        myLocation = view.findViewById(R.id.my_location)
        myLocation.setImageResource(
            if (hasLocationPermissions() && isGpsOn()) R.drawable.my_location_not_tracking
            else R.drawable.location_disabled
        )
        myLocation.setOnClickListener {
            Log.d("tracker", "my location clicked")
            if(!hasLocationPermissions()){
                requestLocationPermission()
                return@setOnClickListener
            }
            if(!isGpsOn()){
                alertUserGps()
                return@setOnClickListener
            }
            updateTrackingStatus(true)
            updateCameraWithLastLocation()
            Log.d("tracker", "end of my location click fun")
        }

        layersLayout = view.findViewById(R.id.layers_layout)
        layersFab = view.findViewById(R.id.layersFab)
        layersFab.setOnClickListener { layersLayout.visibility = if(layersLayout.visibility == View.GONE) View.VISIBLE else View.GONE }
        layersFab.visibility = if(selectedMarker == null) View.VISIBLE else View.GONE

        val layersClickListener = View.OnClickListener {
            Log.d("tracker", "layer selected")
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

        balance = view.findViewById(R.id.balance)
        balance.text = user.balance.toString()

        mushroom = view.findViewById(R.id.mushroom)
        mushroom.setOnClickListener { homeListener.onMushroomClicked() }

        timerTxt = view.findViewById(R.id.timerTxt)

        if(user.isBoosted()) monitorBoostedTimer()

        notifyFab = view.findViewById(R.id.notify_fab)
        notifyFab.setOnClickListener {
            val benchmark = markerToBenchmark[selectedMarker]!!
            val notify = benchmark.notify
            benchmark.notify = !notify
            notifyFab.setImageResource(if(benchmark.notify)R.drawable.notifications_active else R.drawable.notifications_off)

            if(collected(benchmark)) scheduleNotification(requireContext(), benchmark)
            user.visited[benchmark.pid] = benchmark
            user.update()

            if(!benchmark.notify) cancelNotification(requireContext(), benchmark)
        }

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d("tracker", "saving map state")
        mapFragment?.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        Log.d("tracker", "resuming")
        mapFragment?.onResume()
        if(hasLocationPermissions() && isGpsOn())
            startLocationUpdates()
        updateNetworkStatus()
        cameraIsBeingMoved = false
        notifyUserOfNetwork = true

        balance.text = user.balance.toString()
        if(user.isBoosted()) monitorBoostedTimer()
    }

    override fun onPause() {
        super.onPause()
        Log.d("tracker", "pausing")
        stopLocationUpdates()
        mapFragment?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("tracker", "destroying")
        mapFragment?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.d("tracker", "low memory")
        mapFragment?.onLowMemory()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        Log.d("tracker", "map is ready")
        googleMap = map
        map.mapType = mapType()
        map.setOnMarkerClickListener(this)
        map.setOnCameraIdleListener {
            Log.d("tracker", "camera stopped moving, loading markers")
            cameraIsBeingMoved = false
            loadMarkers()
        }
        map.setOnMapClickListener {
            Log.d("tracker", "map was clicked on")
            selectedMarker = null
            notifyFab.visibility = View.GONE
            layersLayout.visibility = View.GONE
            monitoringSelectedMarker = false
            Thread{
                Thread.sleep(500) // wait for direction btn to hide
                Handler(Looper.getMainLooper()).post{layersFab.visibility = View.VISIBLE}
            }.start()
        }
        map.setOnCameraMoveStartedListener {
            Log.d("tracker", "camera started moving: tracking:$tracking")
            updateTrackingStatus(cameraIsBeingMoved)
            if(!tracking) layersLayout.visibility = View.GONE
            Log.d("tracker", "end of moving fun: tracking:$tracking")
        }

        if(tracking) {
            Log.d("tracker", "initializing map camera")
            updateCameraWithLastLocation(false)
        }
        if(hasLocationPermissions() && isGpsOn()) startLocationUpdates()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        Log.d("tracker", "marker clicked on")

        selectedMarker = marker
        layersFab.visibility = View.GONE
        layersLayout.visibility = View.GONE
        updateTrackingStatus(false)

        if(!markerToBenchmark.contains(marker)) return true
        val benchmark = markerToBenchmark[marker]!!

        notifyFab.visibility = View.VISIBLE
        notifyFab.setImageResource(if(benchmark.notify)R.drawable.notifications_active else R.drawable.notifications_off)

        monitorSelectedMarker()

        if(!hasLocationPermissions() || !isGpsOn()) return false

        val lastLocation = lastLocation()
        val inProximity = if(DEBUG) true
            else inProximity(user.getReach(), marker.position, LatLng(lastLocation.latitude, lastLocation.longitude))

        //if coin is collectable
        if(!user.visited.contains(benchmark.pid) || (user.visited.contains(benchmark.pid) && canCollect(benchmark))) {
            if(inProximity) {
                benchmark.lastVisited = Timestamp.now().seconds
                markerToBenchmark[marker] = benchmark
                user.visited[benchmark.pid] = benchmark
                user.balance++
                balance.text = user.balance.toString()
                user.update()
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.hour_glass_6))
                marker.snippet = "Collected ${Converters.formatSeconds(benchmark.lastVisited)}"
                scheduleSetMarkerIcon(marker, benchmark)
                homeListener.onCoinCollected(benchmark)
            }else {
                marker.snippet = getSnippet(benchmark)
                Toast.makeText(context, "Not close enough to complete", Toast.LENGTH_SHORT).show()
            }
        }else{
            marker.snippet = getSnippet(benchmark)
        }

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false
    }

    private fun scheduleSetMarkerIcon(marker: Marker, benchmark: Benchmark){
        Handler(Looper.getMainLooper()).postDelayed({
            try{
                marker.setIcon(getMarkerRes(benchmark))
                if(collected(benchmark)) scheduleSetMarkerIcon(marker, benchmark)
            }catch (e: Exception){
                Log.e("marker set icon", e.toString())
            }
        }, (SECONDS_TO_RECOLLECT * 1000 / 7).toLong())
    }

    private fun toCountdownFormat(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secondsRemaining = seconds % 60

        return if(hours > 0) String.format("%02d:%02d:%02d", hours, minutes, secondsRemaining)
        else if(minutes > 0) String.format("%02d:%02d", minutes, secondsRemaining)
        else String.format("%02d", secondsRemaining)
    }


    private fun getSnippet(benchmark: Benchmark) : String{
        return if (benchmark.lastVisited + SECONDS_TO_RECOLLECT > System.currentTimeMillis() / 1000) {
            val secondsLeft = benchmark.lastVisited + SECONDS_TO_RECOLLECT - System.currentTimeMillis() / 1000
            "Collect in ${toCountdownFormat(secondsLeft)}"
        }else if(benchmark.lastVisited > 0) "Collected ${Converters.formatSeconds(benchmark.lastVisited)}"
        else "Never collected"
    }

    private fun monitorSelectedMarker(){
        if(monitoringSelectedMarker) return
        monitoringSelectedMarker = true
        Thread{
            Thread.sleep(1000)
            while (monitoringSelectedMarker){
                Handler(Looper.getMainLooper()).post{
                    if(selectedMarker == null) {
                        monitoringSelectedMarker = false
                        return@post
                    }
                    try {
                        selectedMarker!!.snippet = getSnippet(markerToBenchmark[selectedMarker]!!)
                        selectedMarker!!.showInfoWindow()
                        //selectedMarker!!.setIcon(getMarkerRes(markerToBenchmark[selectedMarker]!!))
                    }catch (e: Exception){
                        monitoringSelectedMarker = false
                        Log.e("selected marker", e.toString())
                    }
                }
                Thread.sleep(1000)
            }
        }.start()
    }

    private fun updateTrackingStatus(tracking: Boolean){
        this.tracking = tracking
        myLocation.setImageResource(
            if (!hasLocationPermissions() || !isGpsOn()) R.drawable.location_disabled
            else if (tracking) R.drawable.my_location
            else R.drawable.my_location_not_tracking
        )
    }

    private fun inProximity(meters: Double, latlng1: LatLng, latlng2: LatLng): Boolean {
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
        //val ft = d * 3.28084 // convert distance to feet

        return d <= meters
    }

    private fun loadMarkers(){loadMarkers(false)}
    fun loadMarkers(isUserSwitched: Boolean) {
        try {
            Log.d("tracker", "loading markers")
            goToSelectedBenchmark()
            if (!isOnline()) {
                if (notifyUserOfNetwork) {
                    Toast.makeText(context, "No network: Can't load markers", Toast.LENGTH_SHORT)
                        .show()
                    notifyUserOfNetwork = false
                }
                return
            }
            if ((loadingMarkers && !isUserSwitched) || googleMap == null) return

            loadingMarkers = true
            val map = googleMap!!
            val bounds = map.projection.visibleRegion.latLngBounds

            Thread {
                try {
                    val benchmarkList = BenchmarkService().getBenchmarks(bounds)
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
                    for (benchmark in benchmarkList) {
                        val mark = if (user.visited.containsKey(benchmark.pid))
                            user.visited[benchmark.pid]!! else benchmark
                        if (benchmarkToMarker.keys.contains(mark))
                            existingBenchmarks.add(mark)
                        else newBenchmarks.add(mark)
                    }

                    // Update marker colors after user switch
                    if (isUserSwitched) {
                        Handler(Looper.getMainLooper()).post {
                            existingBenchmarks.forEach {
                                benchmarkToMarker[it]?.setIcon(getMarkerRes(it))
                            }
                        }
                    }

                    // Remove markers for deleted benchmarks
                    val iterator = benchmarkToMarker.iterator()
                    val pids = benchmarkList.map { it.pid }
                    while (iterator.hasNext()) {
                        val entry = iterator.next()
                        if (!pids.contains(entry.key.pid)) {
                            val marker = entry.value
                            iterator.remove()
                            markerToBenchmark.remove(marker)
                            Handler(Looper.getMainLooper()).post { marker.remove() }
                        }
                    }

                    // Add markers for new benchmarks
                    Handler(Looper.getMainLooper()).post {
                        for (benchmark in newBenchmarks) {
                            val marker = addBenchmarkToMap(benchmark)
                            if(marker != null && collected(benchmark))
                                scheduleSetMarkerIcon(marker, benchmark)
                        }
                        loadingMarkers = false
                        Log.d("tracker", "markers loaded")
                    }
                } catch (e: ConcurrentModificationException) {
                    loadingMarkers = false
                    Log.e("LoadMarkers", e.toString())
                } catch (e: UnknownHostException) {
                    loadingMarkers = false
                    Log.e("LoadMarkers", e.toString())
                }
            }.start()
        } catch (e: Exception) {
            loadingMarkers = false
            println("Error: ${e.message}")
        }
    }

    private fun getMarkerRes(benchmark: Benchmark) : BitmapDescriptor {
        return BitmapDescriptorFactory.fromResource(
            if (user.visited.contains(benchmark.pid) && collected(benchmark)) {
                val secondsLeft = benchmark.lastVisited + SECONDS_TO_RECOLLECT - System.currentTimeMillis() / 1000
                when(mapToRange(secondsLeft, IntRange(0, SECONDS_TO_RECOLLECT), IntRange(0,6))){
                    0 -> R.drawable.hour_glass_0
                    1 -> R.drawable.hour_glass_1
                    2 -> R.drawable.hour_glass_2
                    3 -> R.drawable.hour_glass_3
                    4 -> R.drawable.hour_glass_4
                    5 -> R.drawable.hour_glass_5
                    6 -> R.drawable.hour_glass_6
                    else -> R.drawable.hour_glass
                }
            }
            else R.drawable.coin)
    }

    private fun mapToRange(number: Long, original: IntRange, target: IntRange): Int {
        val ratio = number.toFloat() / (original.last - original.first)
        return (ratio * (target.last - target.first)).toInt()
    }

    private fun isSameBenchmarks(benchmarkList: List<Benchmark>) : Boolean{
        return benchmarkList.map { it.pid }.sorted() == markerToBenchmark.values.map { it.pid }.sorted()
    }

    private fun goToSelectedBenchmark() {
            if (selectedBenchmark == null) return
            Log.d("tracker", "going to selected benchmark")

            val benchmark = selectedBenchmark!!
            tracking = false
            googleMap?.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        benchmark.lat,
                        benchmark.lon
                    ), 15f
                )
            )

            val selectedMarker =
                if (benchmarkToMarker.contains(benchmark))
                    benchmarkToMarker[benchmark]
                else addBenchmarkToMap(benchmark)

            selectedMarker?.showInfoWindow()

            selectedBenchmark = null
    }

    private fun addBenchmarkToMap(benchmark: Benchmark) : Marker? {
        val marker = googleMap!!.addMarker(toMarkerOptions(benchmark).icon(getMarkerRes(benchmark)))
        if(marker != null) {
            markerToBenchmark[marker] = benchmark
            benchmarkToMarker[benchmark] = marker
        }
        return marker
    }

    private fun collected(benchmark: Benchmark) : Boolean{
        return benchmark.lastVisited + SECONDS_TO_RECOLLECT > System.currentTimeMillis() / 1000
    }

    private fun canCollect(benchmark: Benchmark) : Boolean{
        return benchmark.lastVisited + SECONDS_TO_RECOLLECT < System.currentTimeMillis() / 1000
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        Log.d("tracker", "starting location updates: tracking:$tracking")

        fusedLocationClient.requestLocationUpdates(
            createLocationRequest(TRACKING_INTERVAL, TRACKING_FASTEST_INTERVAL),
            locationCallback,
            Looper.getMainLooper())

        googleMap?.let {
            it.isMyLocationEnabled = true
            it.uiSettings.isMyLocationButtonEnabled = false
        }
    }
    private fun stopLocationUpdates() {
        Log.d("tracker", "stopping location updates")
        fusedLocationClient.removeLocationUpdates(locationCallback)
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
        return manager != null && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
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

    private fun requestLocationPermission() {
        Log.d("tracker", "requesting location permissions")
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            MainActivity.MY_PERMISSIONS_REQUEST_LOCATION
        )
    }

    private fun createLocationRequest(interval: Long, fastestInterval: Long): LocationRequest {
        return LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(interval)
            .setFastestInterval(fastestInterval)
    }

    private fun updateCameraWithLastLocation(){updateCameraWithLastLocation(true)}
    fun updateCameraWithLastLocation(animate: Boolean) {
        Log.d("tracker", "moving camera to last location")
        val lastLocation = lastLocation()
        if (lastLocation.provider == "" || googleMap == null || cameraIsBeingMoved) return
        cameraIsBeingMoved = true
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(lastLocation.latitude, lastLocation.longitude), DEFAULT_ZOOM)
        if(animate) {
            googleMap?.animateCamera(cameraUpdate, CAMERA_ANIMATION_DURATION,
                object : CancelableCallback {
                    override fun onFinish() {
                        cameraIsBeingMoved = false
                        loadMarkers()
                    }

                    override fun onCancel() {
                        cameraIsBeingMoved = false
                        updateTrackingStatus(false)
                        layersLayout.visibility = View.GONE
                    }
                })
        }else{
            googleMap?.moveCamera(cameraUpdate)
            Log.d("tracker", "camera moved, loading markers")
            loadMarkers()
            cameraIsBeingMoved = false
        }
    }

    private fun updateNetworkStatus(){
        offlineImg.visibility = if(isOnline()) View.GONE else View.VISIBLE
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            Log.d("tracker", "got location update")
            locationResult.lastLocation.let { location ->
                lastLocation(location)
                circle?.remove()
                circle = googleMap?.addCircle(getMyLocationCircle())
                if (!tracking) return
                Log.d("tracker", "updating camera from location update")
                updateCameraWithLastLocation()
            }
        }
    }

    private fun getMyLocationCircle() : CircleOptions{
        val loc = lastLocation()
        val isBoosted = user.isBoosted()
        val color = if(isBoosted) Color.argb(50, 255, 255, 0)
        else Color.argb(50, 0, 0, 255)
        return CircleOptions()
            .center(LatLng(loc.latitude, loc.longitude))
            .fillColor(color)
            .strokeColor(Color.argb(0,0,0,0))
            .strokeWidth(0f)
            .radius(user.getReach())
    }

    fun monitorBoostedTimer(){
        if(monitoringBoost) return
        monitoringBoost = true
        mushroom.visibility = View.GONE
        timerTxt.visibility = View.VISIBLE
        circle?.remove()
        circle = googleMap?.addCircle(getMyLocationCircle())
        Thread{
            while (user.isBoosted()){
                val remainingSeconds = user.lastRadiusBoost.seconds + BOOSTED_DURATION - Timestamp.now().seconds
                Handler(Looper.getMainLooper()).post {
                    timerTxt.text = "Radius Boost\nExpires in ${toCountdownFormat(remainingSeconds)}"
                }
                Thread.sleep(1000)
            }
            Handler(Looper.getMainLooper()).post {
                mushroom.visibility = View.VISIBLE
                timerTxt.visibility = View.GONE
                circle?.remove()
                circle = googleMap?.addCircle(getMyLocationCircle())
            }
            monitoringBoost = false
        }.start()
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
        private const val DEFAULT_ZOOM = 15f
        private const val TRACKING_INTERVAL = 5000L
        private const val TRACKING_FASTEST_INTERVAL = 1000L
        private const val CAMERA_ANIMATION_DURATION = 2000
    }
}
