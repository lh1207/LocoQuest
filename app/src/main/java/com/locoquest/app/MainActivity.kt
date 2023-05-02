/**

MainActivity is the main entry point of the application. It is responsible for initializing
the application, requesting location permission, showing Google Maps, retrieving data from a
remote server, and authenticating with Firebase.
@constructor Creates a new instance of the MainActivity class.
 */
package com.locoquest.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.CustomTarget
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.locoquest.app.AppModule.Companion.db
import com.locoquest.app.AppModule.Companion.guest
import com.locoquest.app.AppModule.Companion.scheduleNotification
import com.locoquest.app.AppModule.Companion.user
import com.locoquest.app.dao.DB
import com.locoquest.app.dto.Benchmark
import com.locoquest.app.dto.User
import kotlin.math.max


class MainActivity : AppCompatActivity(), ISecondaryFragment, Profile.ProfileListener, Home.HomeListener,
    CoinCollectedDialog.IWatchAdButtonClickListener {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var oneTapClient: SignInClient
    private lateinit var signUpRequest: BeginSignInRequest
    private lateinit var menu: Menu
    private lateinit var home: Home
    private var profile: Profile? = null
    private var switchingUser = false
    private val fDb = Firebase.firestore

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("event", "MainActivity.onCreate")
        setContentView(R.layout.activity_main)

        // Check if the app has been launched before
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)

        if (isFirstLaunch) {
            // Show the onboarding fragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.primary_container, Onboarding())
                .commit()

            // Set the flag to indicate that the app has been launched before
            prefs.edit().putBoolean("is_first_launch", false).apply()
        } else {
            // Show the home fragment
            home = Home(this)
            supportFragmentManager.beginTransaction()
                .replace(R.id.primary_container, home)
                .commit()
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
            db = Room.databaseBuilder(this, DB::class.java, "db")
                .fallbackToDestructiveMigration().build()
            if (auth.currentUser != null)
                user = User(auth.currentUser!!.uid)
            Log.d("user", "Database loaded, switching to user:${user.uid}")
            switchUser()
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
        Log.d("event", "MainActivity.onRequestPermissionsResult")
        if(grantResults.isEmpty()) return
        if(grantResults[0] == 0) {
            home.startLocationUpdates()
            home.updateCameraWithLastLocation(false)
        }
        else if(grantResults[0] == -1 && grantResults[1] == -1){
            AlertDialog.Builder(this)
                .setTitle("Location Permissions")
                .setMessage("LocoQuest needs your location to provide accurate directions. Please grant location permissions in settings.")
                .setPositiveButton("Open Settings") { _, _ ->
                    startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null)))
                }.show()
        }
    }

    /**
     * Called when the activity receives a result from another activity.
     * In this case, it handles the result of the Google One-Tap sign-in dialog.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("event", "MainActivity.onActivityResult")
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
                                    user = User(auth.currentUser!!.uid,
                                        auth.currentUser!!.displayName!!,
                                        auth.currentUser!!.photoUrl.toString())
                                    switchUser()
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
        Log.d("event", "MainActivity.onCreateOptionsMenu")
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
        Log.d("event", "MainActivity.onOptionsItemSelected")
        return when (item.itemId) {
            R.id.menu_item_account -> {
                if (profile == null) {
                    profile = Profile(user, true,this, this)
                    supportFragmentManager.beginTransaction().replace(R.id.secondary_container, profile!!).commit()
                } else {
                    hideProfile()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBenchmarkClicked(benchmark: Benchmark) {
        Log.d("event", "MainActivity.onBenchmarkClicked")
        hideProfile()
        home.selectedBenchmark = benchmark
        home.loadMarkers(false)
    }

    override fun onCoinCollected(benchmark: Benchmark) {
        Log.d("event", "MainActivity.onCoinCollected")
        Toast.makeText(this, "Coin collected", Toast.LENGTH_SHORT).show()
        supportFragmentManager.beginTransaction().replace(R.id.secondary_container, CoinCollectedDialog(this, this)).commit()
        if(benchmark.notify) scheduleNotification(this, benchmark)
    }

    override fun onWatchAdButtonClicked() {
        Log.d("event", "MainActivity.onWatchAdButtonClicked")
        startActivity(Intent(this, ExtraCoinAdMobActivity::class.java))
    }

    override fun onLogin() {
        try {
            Log.d("event", "MainActivity.onLogin")
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

    override fun onSignOut() {
        Log.d("event", "MainActivity.onSignOut")
        Firebase.auth.signOut()
        supportActionBar?.let {
            it.title = "LocoQuest"
            menu.findItem(R.id.menu_item_account).icon =
                ContextCompat.getDrawable(this, R.drawable.account)

            user = guest
            switchUser()
        }
    }

    override fun onClose(fragment: Fragment) {
        Log.d("event", "MainActivity.onClose")
        supportFragmentManager.beginTransaction().remove(fragment).commit()
        if(user.isBoosted()) home.monitorBoostedTimer()
        home.balance.text = user.balance.toString()
    }

    override fun onMushroomClicked() {
        Log.d("event", "MainActivity.onMushroomClicked")
        supportFragmentManager.beginTransaction().replace(R.id.secondary_container, Store(this)).commit()
    }

    /**
     * Description: see if app is launched for first time
     */
    private fun isFirstTimeAppLaunch(): Boolean {
        val sharedPreferences = getSharedPreferences("my_preferences", Context.MODE_PRIVATE)
        val isFirstTime = sharedPreferences.getBoolean("is_first_time", true)
        if (isFirstTime) {
            sharedPreferences.edit().putBoolean("is_first_time", false).apply()
        }
        return isFirstTime
    }

    private fun displayUserInfo() {
        if (user.displayName == "") {
            user.displayName = auth.currentUser?.displayName.toString()
        }

        supportActionBar?.title = user.displayName

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

        try{
            home.balance.text = user.balance.toString()
        }catch (e:Exception) {
            Log.e("balance", e.toString())
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun switchUser() {
        if (switchingUser) return
        Thread {
            switchingUser = true
            val userDao = db!!.localUserDAO()
            val tmpUser = userDao.getUser(user.uid)

            if (tmpUser == null) {
                userDao.insert(user)
            } else user = tmpUser

            Log.d("user", "user loaded from db; ${user.dump()}")

            Handler(Looper.getMainLooper()).post {
                supportActionBar?.title = user.displayName
                hideProfile()

                if (user.uid == guest.uid) {
                    home.loadMarkers(true)
                    displayUserInfo()
                    Log.d("user", "user switched; ${user.dump()}")
                    switchingUser = false
                    return@post
                }

                fDb.collection("users").document(user.uid)
                    .get()
                    .addOnSuccessListener {
                        if (it.data == null) {
                            user.push()
                            return@addOnSuccessListener
                        }
                        Log.d(TAG, "${it.id} => ${it.data}")

                        val name =
                            if (it["name"] == null) user.displayName else it["name"] as String

                        val photoUrl = if (it["photoUrl"] == null) {
                            user.photoUrl = auth.currentUser?.photoUrl.toString()
                            user.photoUrl
                        } else it["photoUrl"] as String

                        val lastRadiusBoost =
                            if (it["lastRadiusBoost"] == null) user.lastRadiusBoost
                            else {
                                val tmpVal = it["lastRadiusBoost"] as Timestamp
                                if (tmpVal.seconds > user.lastRadiusBoost.seconds) tmpVal
                                else user.lastRadiusBoost
                            }

                        val balance = if (it["balance"] == null) user.balance
                        else max(user.balance, it["balance"] as Long)

                        var visited = HashMap<String, Benchmark>()
                        val visitedList =
                            if (it["visited"] == null) ArrayList() else it["visited"] as ArrayList<HashMap<String, Any>>
                        visitedList.forEach { x ->
                            val pid = x["pid"] as String
                            val lastVisited = if(x["lastVisited"] == null) Timestamp(0,0) else x["lastVisited"] as Timestamp
                            val notify = if(x["notify"] == null) false else x["notify"] as Boolean

                            visited[pid] = Benchmark.new(
                                pid,
                                x["name"] as String,
                                x["location"] as GeoPoint,
                                lastVisited,
                                notify
                            )
                        }
                        if (visited.isNotEmpty() && user.visited.isNotEmpty() &&
                            visited.values.sortedByDescending { x -> x.lastVisited }[0].lastVisited < user.visited.values.sortedByDescending { x -> x.lastVisited }[0].lastVisited
                        )
                            visited = user.visited

                        val friends =
                            if (it["friends"] == null) ArrayList() else it["friends"] as ArrayList<String>

                        user = User(
                            user.uid,
                            name,
                            photoUrl,
                            balance,
                            lastRadiusBoost,
                            visited,
                            friends
                        )
                        user.update()
                        home.loadMarkers(true)
                    }
                    .addOnFailureListener {
                        Log.d(TAG, it.toString())
                        user.push()
                        home.loadMarkers(true)
                    }.addOnCompleteListener {
                        //home.balance.text = user.balance.toString()
                        displayUserInfo()
                        Log.d("user", "user switched; ${user.dump()}")
                        switchingUser = false
                    }
            }
        }.start()
    }

    private fun hideProfile(){
        if(profile != null){
            supportFragmentManager.beginTransaction().remove(profile!!).commit()
            profile = null
        }
    }

    companion object {
        const val MY_PERMISSIONS_REQUEST_LOCATION = 1
        const val REQ_ONE_TAP = 2
        private val TAG: String = MainActivity::class.java.name
    }
}