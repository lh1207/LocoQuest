package com.locoquest.app

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.locoquest.app.AppModule.Companion.user
import java.util.Locale

// Remove the line below after defining your own ad unit ID.
private const val TOAST_TEXT = "Test ads are being shown. " +
        "To show live ads, replace the ad unit ID in res/values/strings.xml " +
        "with your own ad unit ID."

class ExtraCoinAdMobActivity : AppCompatActivity() {

    private var rewardedAd: RewardedAd? = null
    private val TAG = "AdMobActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_ad_mob)

        MobileAds.initialize(this) { }
        // Load the InterstitialAd and set the adUnitId (defined in values/strings.xml).
        loadRewardAd()

        // Toasts the test ad message on the screen. Remove this after defining your own ad unit ID.
        Toast.makeText(this, TOAST_TEXT, Toast.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_ad_mob, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }

    private fun loadRewardAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, getString(R.string.reward_ad_unit_id_test), adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    // The interstitialAd reference will be null until
                    // an ad is loaded.
                    rewardedAd = ad
                    Toast.makeText(this@ExtraCoinAdMobActivity, "onAdLoaded()", Toast.LENGTH_SHORT).show()
                    showInterstitial()

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            // Called when fullscreen content is dismissed.
                            // Make sure to set your reference to null so you don't
                            // show it a second time.
                            rewardedAd = null
                            Log.d(TAG, "The ad was dismissed.")
                            finish()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            // Called when fullscreen content failed to show.
                            // Make sure to set your reference to null so you don't
                            // show it a second time.
                            rewardedAd = null
                            Log.d(TAG, "The ad failed to show.")
                            finish()
                        }

                        override fun onAdShowedFullScreenContent() {
                            // Called when fullscreen content is shown.
                            Log.d(TAG, "The ad was shown.")
                            finish()
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error
                    Log.i(TAG, loadAdError.message)
                    rewardedAd = null
                    val error: String = String.format(
                        Locale.ENGLISH,
                        "domain: %s, code: %d, message: %s",
                        loadAdError.domain,
                        loadAdError.code,
                        loadAdError.message
                    )
                    Toast.makeText(
                        this@ExtraCoinAdMobActivity,
                        "onAdFailedToLoad() with error: $error", Toast.LENGTH_SHORT
                    )
                        .show()
                }
            })
    }

    private fun showInterstitial() {
        // Show the ad if it"s ready. Otherwise toast and reload the ad.
        if (rewardedAd != null) {
            rewardedAd!!.show(this){
                user.balance++
                user.update()
                finish()
            }
        } else {
            Toast.makeText(this, "Ad did not load", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}