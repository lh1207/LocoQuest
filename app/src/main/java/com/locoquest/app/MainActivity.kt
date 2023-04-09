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
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.CustomTarget
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.locoquest.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    // The Firebase authentication instance.
    private val auth = FirebaseAuth.getInstance()

    // The One Tap client used for Google Sign-In.
    private lateinit var oneTapClient: SignInClient

    // The sign-in request used for Google Sign-In.
    private lateinit var signUpRequest: BeginSignInRequest

    // The Google Sign-In button.
    private lateinit var signInButton: SignInButton // Declare the variable here

    // The map fragment used for Google Maps.
    private var mMapFragment: SupportMapFragment? = null

    // The FusedLocationProviderClient instance.
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // The Google Map instance.
    private lateinit var googleMap: GoogleMap

    private var updateCamera = true
    private lateinit var lastLocation: Location
    private lateinit var menu: Menu

    //Bottom Navigation Bar
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        replaceFragment(Home())

        signInButton = findViewById(R.id.google_sign_in_button)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mMapFragment =
            supportFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment

        if (mMapFragment == null) {
            Log.e(TAG, "Error: map fragment not found")
            return
        }
        mMapFragment!!.getMapAsync(this)


        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> replaceFragment(Home())
                R.id.profile -> replaceFragment(Profile())
                R.id.settings -> replaceFragment(Settings())

                else -> {
                }
            }
            true
        }

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
                    .build()
            )
            .build()
    }

    /**
     * Called when a permission request has been completed.
     *
     * @param requestCode The request code that was passed to the permission request.
     * @param permissions An array of permission strings.
     * @param grantResults An array of grant results for the corresponding permissions.
     */
    override fun onPermissionRequestCompletion(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onPermissionRequestCompletion(requestCode, permissions, grantResults)
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
                    if (idToken != null) {
                        // Got an ID token from Google. Use it to authenticate
                        // with Firebase.
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        auth.signInWithCredential(firebaseCredential)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    // Sign in success, update UI with the signed-in user's information
                                    Log.d(TAG, "signInWithCredential:success")
                                    displayUserInfo()
                                    Log.d(TAG, "Got ID token.")
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                                }
                            }
                    } else {
                        // Shouldn't happen.
                        Log.d(TAG, "No ID token!")
                    }
                } catch (e: ApiException) {
                    when (e.statusCode) {
                        CommonStatusCodes.CANCELED -> {
                            Log.d(TAG, "One-tap dialog was closed.")
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
    }

    /**
     * Function: onCreateOptionsMenu
     * Description: Override function to create options menu.
     * @param menu: The menu object.
     * @return: Boolean value indicating if the menu creation was successful.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    /**
     * Function: onOptionsItemSelected
     * Description: Override function to handle options item selection.
     * @param item: The selected menu item.
     * @return: Boolean value indicating if the item selection was handled successfully.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_account -> {
                if (auth.currentUser == null) {
                    login()
                } else {
                    signOut()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Function: onStart
     * Description: Override function called when the activity is starting.
     */
    override fun onStart() {
        super.onStart()
        displayUserInfo()
    }

    /**
     * Function: onResume
     * Description: Override function called when the activity is resumed.
     */
    override fun onResume() {
        super.onResume()
        mMapFragment?.onResume()
    }

    /**
     * Function: onPause
     * Description: Override function called when the activity is paused.
     */
    override fun onPause() {
        super.onPause()
        mMapFragment?.onPause()
        stopLocationUpdates()
    }

    /**
     * Function: onDestroy
     * Description: Override function called when the activity is being destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        mMapFragment?.onDestroy()
    }

    /**
     * Function: onLowMemory
     * Description: Override function called when the system is running low on memory.
     */
    override fun onLowMemory() {
        super.onLowMemory()
        mMapFragment?.onLowMemory()
    }

    /**
     * Function: login
     * Description: Private function to handle user login.
     */
    private fun login() {
        try {
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
                    e.localizedMessage?.let { it1 -> Log.d(TAG, it1) }
                }
        } catch (ex: java.lang.Exception) {
            ex.localizedMessage?.let { Log.d(TAG, it) }
        }
    }

    /**
     * Function: displayUserInfo
     * Description: Private function to display user information.
     */
    private fun displayUserInfo() {
        auth.currentUser?.let { user ->
            supportActionBar?.let {
                it.title = user.displayName

                Glide.with(this)
                    .load(user.photoUrl)
                    .transform(CircleCrop())
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: com.bumptech.glide.request.transition.Transition<in Drawable>?
                        ) {
                            menu.findItem(R.id.menu_item_account).icon = resource
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }
        }
    }

    /**
     * Function: signOut
     * Description: Private function to handle user sign out.
     */
    private fun signOut() {
        Firebase.auth.signOut()
        supportActionBar?.let {
            it.title = "LocoQuest"
            menu.findItem(R.id.menu_item_account).icon =
                ContextCompat.getDrawable(this, R.drawable.account)
        }
    }

    /**
     * Function: requestLocationPermission
     * Description: Private function to request location permission.
     */
    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION
            )
        }
    }

    /**
     * Function: onMapReady
     * Description: Override function called when the map is ready.
     * @param map: The GoogleMap object.
     */
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Add the markers to the map
        val benchmarkService: IBenchmarkService = BenchmarkService()
        val pidList = listOf("AB1234", "CD5678")

        lifecycleScope.launch {
            try {
                // Fetch benchmark data in a background thread
                Thread {
                    val benchmarkList = benchmarkService.getBenchmarkData(pidList)
                    if (benchmarkList != null) {
                        // Create marker options with benchmark data
                        benchmarkList.forEach { benchmark ->
                            val marker = MarkerOptions()
                                .position(
                                    LatLng(
                                        benchmark.lat.toDouble(),
                                        benchmark.lon.toDouble()
                                    )
                                )
                                .title(benchmark.name)
                                .snippet("PID: ${benchmark.pid}\nOrtho Height: ${benchmark.orthoHt}")
                            // Add marker to the map on the main/UI thread
                            Handler(Looper.getMainLooper()).post { map.addMarker(marker) }
                        }
                    } else {
                        println("Error: unable to retrieve benchmark data")
                    }
                }.start()
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }

        // Check network connectivity and start location updates accordingly
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        if (capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR
            ))
        ) {
            startLocationUpdates()
        } else {
            Toast.makeText(
                this,
                "Unable to start location updates. Device is offline.",
                Toast.LENGTH_SHORT
            ).show()
        }

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

        map.setOnCameraMoveStartedListener { updateCamera = false }
    }

    /**
     * Function: stopLocationUpdates
     * Description: Function to stop location updates.
     */
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /**
     * Function: startLocationUpdates
     * Description: Function to start location updates.
     */
    private fun startLocationUpdates() {
        // Check if location permissions are granted
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions if not granted
            requestLocationPermission()
            return
        }
        // Request location updates using fusedLocationClient
        fusedLocationClient.requestLocationUpdates(createLocationRequest(), locationCallback, null)
        googleMap.isMyLocationEnabled = true
        googleMap.setOnMyLocationButtonClickListener { updateCamera = true; false }
    }

    /**
     * Function: createLocationRequest
     * Description: Function to create a LocationRequest object.
     * @return LocationRequest: The created LocationRequest object.
     */
    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(5000) // Update location every 5 seconds
            .setFastestInterval(1000) // Update location at least every 1 second
    }

    /**
     * LocationCallback object to handle location updates.
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                if (location != null) {
                    lastLocation = location
                }
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

    /**
     * Function: isOnline
     * Description: Function to check if the device is online.
     * @return Boolean: True if the device is online, false otherwise.
     */
    fun isOnline(): Boolean {
        val connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    companion object {
        const val MY_PERMISSIONS_REQUEST_LOCATION = 1
        const val REQ_ONE_TAP = 2
        private val TAG: String = MainActivity::class.java.name
    }

    /**
     * Function: replaceFragment
     * Description: Function to replace a fragment in the Bottom Navigation Bar.
     * @param fragment: The fragment to be replaced.
     */
    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }
}