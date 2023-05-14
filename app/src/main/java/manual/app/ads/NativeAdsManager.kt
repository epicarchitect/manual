package manual.app.ads

import android.content.Context
import android.util.Log
import androidx.core.os.bundleOf
import com.google.ads.consent.ConsentStatus
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import manual.app.R
import kotlin.random.Random

class NativeAdsManager(
    private val context: Context,
    private val gdprHelper: GDPRHelper
) {

    private val nativeAds = mutableListOf<NativeAd>()
//    private val isPersonalized get() = gdprHelper.isEEA && gdprHelper.consentStatus == ConsentStatus.PERSONALIZED
    private val isPersonalized = false
    private val random = Random(System.currentTimeMillis())

    init {
        load()
    }

    fun load() {
        AdLoader.Builder(context, context.getString(R.string.admob_nativeAd_id))
            .forNativeAd {
                nativeAds.add(it)
            }
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e("NativeAdsManager", error.toString())
                    }
                }
            )
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
            .loadAds(buildRequest(isPersonalized), 20)
    }

    fun randomNativeAd() = try {
        nativeAds[random.nextInt(nativeAds.size)]
    } catch (e: Exception) {
        e.printStackTrace()
        null
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