package manual.app.ads

import android.app.Activity
import androidx.core.os.bundleOf
import com.google.ads.consent.ConsentStatus
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import manual.app.R

class RewardedVideoManager(
    private val activity: Activity,
    private val gdprHelper: GDPRHelper
) {

    private var currentCallback: RewardedVideoCallback? = null
    private var isVideoRunning = false
    private var rewardedAd: RewardedAd? = null

    private var isPersonalized = if (gdprHelper.isEEA) {
        gdprHelper.consentStatus === ConsentStatus.PERSONALIZED
    } else {
        true
    }

    private val fullScreenContentCallback = object : FullScreenContentCallback() {
        override fun onAdShowedFullScreenContent() {
            rewardedAd = null
        }

        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
            currentCallback?.let {
                isVideoRunning = false
                it.onFailed(adError.code, adError.message)
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
            if (currentCallback != null && !isVideoRunning) {
                isVideoRunning = true
                rewardedAd.fullScreenContentCallback = fullScreenContentCallback
                rewardedAd.show(activity, onUserEarnedRewardListener)
            }
        }

        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            super.onAdFailedToLoad(loadAdError)
            currentCallback?.let {
                isVideoRunning = false
                it.onFailed(loadAdError.code, loadAdError.message)
                currentCallback = null
                rewardedAd = null
            }
        }
    }

    init {
        load()
    }

    fun showRewardedVideo(callback: RewardedVideoCallback) {
        gdprHelper.checkConsent { status ->
            isPersonalized = status === ConsentStatus.PERSONALIZED
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
            activity,
            activity.getString(R.string.admob_rewardedAd_id),
            buildRequest(isPersonalized),
            rewardedAdLoadCallback
        )
    }

    interface RewardedVideoCallback {
        fun onReward()
        fun onFailed(code: Int, message: String?)
    }
}