package manual.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.os.bundleOf
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import manual.app.R

class InterstitialAdManager(private val context: Context) {

    private var interstitialAd: InterstitialAd? = null

    init {
        load()
    }

    private fun load() {
        InterstitialAd.load(
            context,
            context.getString(R.string.admob_interstitialAd_id),
            buildRequest(),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("InterstitialAdManager", error.toString())
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
            }
        )
    }


    fun show(activity: Activity, onFinished: () -> Unit) {
        interstitialAd?.let {
            it.setOnPaidEventListener { onFinished() }
            it.show(activity)
        }
        load()
    }

    private fun buildRequest() = AdRequest.Builder().apply {
        addNetworkExtrasBundle(
            AdMobAdapter::class.java,
            bundleOf("npa" to "1")
        )
    }.build()
}