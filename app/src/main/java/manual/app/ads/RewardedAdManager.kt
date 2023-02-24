package manual.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.os.bundleOf
import com.google.ads.consent.ConsentStatus
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import manual.app.R

class RewardedAdManager(
    private val context: Context,
    private val gdprHelper: GDPRHelper
) {

    private val isLoadedState = MutableStateFlow(false)
    private var currentActivity: Activity? = null
    private var currentCallback: RewardedVideoCallback? = null
    private var isVideoRunning = false
    private var rewardedAd: RewardedAd? = null
        set(value) {
            field = value
            isLoadedState.value = value != null
        }
    private val isPersonalized get() = gdprHelper.isEEA && gdprHelper.consentStatus == ConsentStatus.PERSONALIZED

    private val fullScreenContentCallback = object : FullScreenContentCallback() {
        override fun onAdShowedFullScreenContent() {
            rewardedAd = null
        }

        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
            Log.e("RewardedAdManager", adError.toString())
            currentCallback?.let {
                isVideoRunning = false
                currentCallback = null
                rewardedAd = null
            }
        }

        override fun onAdDismissedFullScreenContent() {
            rewardedAd = null
            isVideoRunning = false
            currentCallback = null
        }
    }

    private val onUserEarnedRewardListener = OnUserEarnedRewardListener {
        currentCallback?.onReward()
        isVideoRunning = false
    }

    private val rewardedAdLoadCallback = object : RewardedAdLoadCallback() {
        override fun onAdLoaded(rewardedAd: RewardedAd) {
            super.onAdLoaded(rewardedAd)
            if (currentActivity != null && currentCallback != null && !isVideoRunning) {
                isVideoRunning = true
                rewardedAd.fullScreenContentCallback = fullScreenContentCallback
                rewardedAd.show(currentActivity!!, onUserEarnedRewardListener)
            }
        }

        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            super.onAdFailedToLoad(loadAdError)
            Log.e("RewardedAdManager", loadAdError.toString())
            isVideoRunning = false
            currentCallback = null
            rewardedAd = null
        }
    }

    fun isLoadedFlow(): Flow<Boolean> {
        load()
        return isLoadedState
    }

    fun showRewardedVideo(activity: Activity, callback: RewardedVideoCallback) {
        gdprHelper.checkConsent(activity) { status ->
            currentActivity = activity
            currentCallback = callback
            if (rewardedAd != null && !isVideoRunning) {
                rewardedAd?.show(activity, onUserEarnedRewardListener)
            } else {
                load()
            }
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

    private fun load() {
        RewardedAd.load(
            context,
            context.getString(R.string.admob_rewardedAd_id),
            buildRequest(isPersonalized),
            rewardedAdLoadCallback
        )
    }

    fun release() {
        currentActivity = null
        currentCallback = null
        rewardedAd = null
    }

    interface RewardedVideoCallback {
        fun onReward()
    }
}