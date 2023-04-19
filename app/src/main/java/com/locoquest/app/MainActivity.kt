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
import androidx.room.Room
import androidx.room.RoomDatabase
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.locoquest.app.dao.BenchmarkDatabase
import com.locoquest.app.databinding.ActivityMainBinding
import com.locoquest.app.dto.Benchmark
import com.locoquest.app.dto.User
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var oneTapClient: SignInClient
    private lateinit var signUpRequest: BeginSignInRequest
    private lateinit var signInButton: SignInButton // Declare the variable here
    private lateinit var menu: Menu
    private lateinit var home: Home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        signInButton = findViewById(R.id.google_sign_in_button)

        fun loadFragment(fragment: Fragment) {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.container,fragment)
            transaction.commit()
        }

        home = Home()
        loadFragment(home)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    loadFragment(home)
                }
                R.id.profile -> {
                    loadFragment(Profile())
                }
                R.id.settings -> {
                    loadFragment(Settings())
                }
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

        Thread{
            AppModule.db = Room.databaseBuilder(this, BenchmarkDatabase::class.java, "db")
                .fallbackToDestructiveMigration().build()
            if (auth.currentUser != null)
                AppModule.user = User(auth.currentUser!!.uid, auth.currentUser!!.displayName, HashMap())

            val userDao = AppModule.db!!.localUserDAO()
            val tmpUser = userDao.getUser(AppModule.user.uid)
            if(tmpUser == null) {
                userDao.insert(AppModule.user)
            }else AppModule.user = tmpUser
        }.start()
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
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        home.startLocationUpdates()
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
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        auth.signInWithCredential(firebaseCredential)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    displayUserInfo()
                                    Log.d(TAG, "signInWithCredential:success")
                                } else {
                                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                                }
                            }
                    } else {
                        Log.d(TAG, "No ID token!")
                    }
                } catch (e: ApiException) {
                    when (e.statusCode) {
                        CommonStatusCodes.CANCELED -> {
                            Log.d(TAG, "One-tap dialog was closed.")
                        }
                        CommonStatusCodes.NETWORK_ERROR -> {
                            Log.d(TAG, "One-tap encountered a network error.")
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
        displayUserInfo()
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

    companion object {
        const val MY_PERMISSIONS_REQUEST_LOCATION = 1
        const val REQ_ONE_TAP = 2
        private val TAG: String = MainActivity::class.java.name
    }
}