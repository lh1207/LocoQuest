/**

MainActivity is the main entry point of the application. It is responsible for initializing
the application, requesting location permission, showing Google Maps, retrieving data from a
remote server, and authenticating with Firebase.
@constructor Creates a new instance of the MainActivity class.
 */
package com.locoquest.app

import BenchmarkService
import IBenchmarkService
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.GoogleMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    // The name of the MainActivity class.
    private val TAG : String = MainActivity::class.java.name

    // The Firebase authentication instance.
    private val auth = FirebaseAuth.getInstance()

    // The One Tap client used for Google Sign-In.
    private lateinit var oneTapClient: SignInClient

    // The sign-in request used for Google Sign-In.
    private lateinit var signUpRequest: BeginSignInRequest

    // The Google Sign-In button.
    private lateinit var signInButton: SignInButton // Declare the variable here

    // A flag indicating whether or not to show the One Tap UI.
    private var showOneTapUI = true

    // The map fragment used for Google Maps.
    private var mMapFragment: SupportMapFragment? = null

    // The FusedLocationProviderClient instance.
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // The Google Map instance.
    private lateinit var googleMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        buildMap()
    }

    /**
     * Called when a permission request has been completed.
     *
     * @param requestCode The request code that was passed to the permission request.
     * @param permissions An array of permission strings.
     * @param grantResults An array of grant results for the corresponding permissions.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        startLocationUpdates()
    }

    /**
     * Called when the activity receives a result from another activity.
     * In this case, it handles the result of the Google One-Tap sign-in dialog.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_ONE_TAP -> {
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(data)
                    val idToken = credential.googleIdToken
                    idToken?.let{
                        // Got an ID token from Google. Use it to authenticate with Firebase.
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        auth.signInWithCredential(firebaseCredential)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    // Sign in success, update UI with the signed-in user's information
                                    Log.d(TAG, "You are logged in")
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Log.w(TAG, "Error logging in" + task.exception)
                            }
                        }
                    }
                } catch (e: ApiException) {
                    when (e.statusCode) {
                        CommonStatusCodes.CANCELED -> {
                            Log.d(TAG, "One-tap dialog was closed.")
                            // Don't re-prompt the user.
                            showOneTapUI = false
                        }
                        CommonStatusCodes.NETWORK_ERROR -> {
                            Log.d(TAG, "One-tap encountered a network error.")
                            // Try again or just ignore.
                        }
                        else -> {
                            Log.d(
                                TAG, "Couldn't get credential from result." +
                                        " (${e.localizedMessage})"
                            )
                        }
                    }
                }
            }
        }

        // Find the map fragment and initialize it
        mMapFragment = supportFragmentManager
            .findFragmentById(R.id.map_container) as SupportMapFragment?
        mMapFragment?.getMapAsync(this)


        // Firebase Sign-in
        oneTapClient = Identity.getSignInClient(this)
        signUpRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.web_client_id))
                    // Show all accounts on the device.
                    .setFilterByAuthorizedAccounts(false)
                    .build())
            .build()


        // Find the SignInButton view in the layout
        signInButton = findViewById(R.id.google_sign_in_button)

        if(!showOneTapUI) signInButton.visibility = View.INVISIBLE
        signInButton.setOnClickListener {
            oneTapClient.beginSignIn(signUpRequest)
                .addOnSuccessListener(this) { result ->
                    try {
                        startIntentSenderForResult(
                            result.pendingIntent.intentSender, REQ_ONE_TAP,
                            null, 0, 0, 0
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                    }
                }
                .addOnFailureListener(this) { e ->
                    // No Google Accounts found. Just continue presenting the signed-out UI.
                    Log.d(TAG, e.localizedMessage)
                }
        }
        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_container) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    }

    private fun buildMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment
        if (mapFragment == null) {
            Log.e(TAG, "Error: map fragment not found")
            return
        }
        mapFragment.getMapAsync { map ->
            val googleMap = map
            val benchmarkService: IBenchmarkService = BenchmarkService()
            val pidList = listOf("AB1234", "CD5678")
            lifecycleScope.launch {
                try {
                    val benchmarkList = benchmarkService.getBenchmarkData(pidList)
                    if (benchmarkList != null) {
                        benchmarkList.forEach { benchmark ->
                            val marker = MarkerOptions()
                                .position(LatLng(benchmark.lat.toDouble(), benchmark.lon.toDouble()))
                                .title(benchmark.name)
                                .snippet("PID: ${benchmark.pid}\nOrtho Height: ${benchmark.orthoHt}")
                            map.addMarker(marker)
                        }
                    } else {
                        println("Error: unable to retrieve benchmark data")
                    }
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                }
            }
        }
    }

/**
    private fun addMapListeners() {
        // Add listeners for various map events
        googleMap.setOnMapClickListener(onMapClickListener)
    }
    **/

    override fun onStart() {
        super.onStart()
        var currentUser = auth.currentUser
        //TODO: hideSignInButton()
    }

    override fun onResume() {
        super.onResume()
        mMapFragment?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mMapFragment?.onPause()
        stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        mMapFragment?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mMapFragment?.onLowMemory()
    }

    //TODO: kotlin.UninitializedPropertyAccessException: lateinit property signInButton has not been initialized
    /**
    private fun hideSignInButton(){
        signInButton.visibility = View.GONE
    }
    **/

    private fun signOut(){
        Firebase.auth.signOut()
    }

// Map section
companion object {
    const val MY_PERMISSIONS_REQUEST_LOCATION = 1
    const val REQ_ONE_TAP = 2
    const val REQ_LOCATION = 3
}
    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        if (capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))) {
            startLocationUpdates()
        } else {
            Toast.makeText(this, "Unable to start location updates. Device is offline.", Toast.LENGTH_SHORT).show()
        }
        this.googleMap = googleMap

        val position = CameraPosition.Builder()
            .target(LatLng(40.7128, -74.0060))
            .zoom(12.0F)
            .build()

        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(position))

        // Add markers to the map
        val markerOptions = MarkerOptions()
            .position(LatLng(37.7749, -122.4194))
            .title("San Francisco")
        map.addMarker(markerOptions)

        // Add polygons to the map
        val polygonOptions = PolygonOptions()
            .add(LatLng(37.7765, -122.4351))
            .add(LatLng(37.7604, -122.4142))
            .add(LatLng(37.7615, -122.4093))
            .add(LatLng(37.7707, -122.4089))
            .fillColor(Color.argb(128, 59, 178, 208)) // Set alpha with argb method
        map.addPolygon(polygonOptions)

        // Add listener for map click events
        map.setOnMapClickListener { latLng ->
            // Handle map click events
            true
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    private fun startLocationUpdates(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), REQ_LOCATION)
            return
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
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude), 15f)
                googleMap.moveCamera(cameraUpdate)
            }
        }
    }

    fun isOnline(): Boolean {
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}