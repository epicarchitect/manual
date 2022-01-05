package manual.app.ads

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.core.os.bundleOf
import com.google.ads.consent.ConsentStatus
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import manual.app.R

class InterstitialAdManager(
    private val context: Context,
    private val gdprHelper: GDPRHelper
) {

    private var interstitialAd: InterstitialAd? = null
    private val isPersonalized get() = gdprHelper.isEEA && gdprHelper.consentStatus == ConsentStatus.PERSONALIZED

    init {
        load()
    }

    private fun load() {
        InterstitialAd.load(
            context,
            context.getString(R.string.admob_interstitialAd_id),
            buildRequest(isPersonalized),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Toast.makeText(context, adError.message, Toast.LENGTH_SHORT).show()
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Toast.makeText(context, adError.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }


    fun show(activity: Activity, onFinished: () -> Unit) {
        gdprHelper.checkConsent(activity) {
            interstitialAd?.let {
                it.setOnPaidEventListener { onFinished() }
                it.show(activity)
            }
            load()
        }
    }

    private fun buildRequest(isPersonalized: Boolean) = AdRequest.Builder().apply {
        if (!isPersonalized) {
            addNetworkExtrasBundle(
                AdMobAdapter::class.java,
                bundleOf("npa" to "1")
            )
        }
    }.build()
}